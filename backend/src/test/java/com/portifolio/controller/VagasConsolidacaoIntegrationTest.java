package com.portifolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portifolio.model.*;
import com.portifolio.model.enums.*;
import com.portifolio.repository.*;
import com.portifolio.security.JwtService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.portifolio.controller.VagasConsolidacaoIntegrationTest$SqlInspector")
@AutoConfigureMockMvc
class VagasConsolidacaoIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilContratanteRepository contratantes;
    @Autowired PerfilArtistaRepository artistas;
    @Autowired JwtService jwt;
    private final ObjectMapper mapper = new ObjectMapper();
    private Usuario dono;
    private final List<Long> funcoes = new ArrayList<>();
    private final List<Long> specs = new ArrayList<>();

    @BeforeEach void preparar() {
        db.execute("TRUNCATE usuarios, funcoes, especializacoes RESTART IDENTITY CASCADE");
        dono = usuario(TipoUsuario.CONTRATANTE);
        funcoes.clear(); specs.clear();
        // Dados normais apenas no container efêmero; a carga oficial permanece intacta.
        for (int i = 0; i < 4; i++) {
            long funcao = db.queryForObject("insert into funcoes(area_id,nome) values (1,?) returning id",
                    Long.class, "Função teste " + i);
            long spec = db.queryForObject("insert into especializacoes(nome) values (?) returning id",
                    Long.class, "Especialização teste " + i);
            db.update("insert into funcao_especializacao values (?,?)", funcao, spec);
            funcoes.add(funcao); specs.add(spec);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"SEM_EXPERIENCIA","INICIANTE","INTERMEDIARIO","EXPERIENTE","ESPECIALISTA"})
    void experienciasCanonicas(String experiencia) throws Exception {
        ObjectNode p = payload(); p.put("experiencia", experiencia);
        criar(p).andExpect(status().isCreated()).andExpect(jsonPath("$.experiencia").value(experiencia));
    }

    @ParameterizedTest
    @CsvSource({"CLT,CLT","PJ / Prestação de Serviço,PJ","Freelancer / Autônomo,FREELANCER",
            "Temporário,TEMPORARIO","Estágio,ESTAGIO","Contrato por Projeto / Evento,PROJETO_EVENTO"})
    void contratosFechadosComCompatibilidade(String entrada, String codigo) throws Exception {
        ObjectNode p = payload(); p.put("tipoContrato", entrada);
        criar(p).andExpect(status().isCreated()).andExpect(jsonPath("$.tipoContrato").value(codigo));
    }

    @ParameterizedTest @EnumSource(FormaRemuneracao.class)
    void formasPreservamFaixaOuAusenciaReal(FormaRemuneracao forma) throws Exception {
        ObjectNode p = payload(); p.put("formaRemuneracao", forma.name());
        if (forma == FormaRemuneracao.A_COMBINAR) { p.remove("valorMinimo"); p.remove("valorMaximo"); }
        JsonNode n = resposta(criar(p).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        if (forma == FormaRemuneracao.A_COMBINAR) {
            assertThat(n.path("valorMinimo").isNull()).isTrue();
            assertThat(n.path("valorMaximo").isNull()).isTrue();
            assertThat(n.path("remuneraValor").isNull()).isTrue();
        } else {
            assertThat(n.path("valorMinimo").decimalValue()).isEqualByComparingTo("1000");
            assertThat(n.path("valorMaximo").decimalValue()).isEqualByComparingTo("3000");
            assertThat(n.path("remuneraValor").isNull()).isTrue();
        }
    }

    @ParameterizedTest @EnumSource(ModeloTrabalho.class)
    void modelosFisicosValidos(ModeloTrabalho modelo) throws Exception {
        ObjectNode p = payload(); p.put("modeloTrabalho", modelo.name());
        criar(p).andExpect(status().isCreated()).andExpect(jsonPath("$.modeloTrabalho").value(modelo.name()));
    }

    @ParameterizedTest @ValueSource(strings = {"LOCAL","NACIONAL"})
    void abrangenciasConsolidadas(String abrangencia) throws Exception {
        ObjectNode p = payload(); p.put("abrangencia", abrangencia);
        criar(p).andExpect(status().isCreated()).andExpect(jsonPath("$.abrangencia").value(abrangencia));
    }

    @ParameterizedTest @ValueSource(strings = {"REGIONAL","MUNICIPAL","INTERNACIONAL","REMOTO"})
    void abrangenciaLegadaNaoAutorizaNovaEscrita(String abrangencia) throws Exception {
        ObjectNode p = payload(); p.put("abrangencia", abrangencia);
        criar(p).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("D09")));
        assertThat(contarVagas()).isZero();
    }

    @Test void estadualNaoViraRegional() throws Exception {
        ObjectNode p = payload(); p.put("abrangencia","ESTADUAL");
        criar(p).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vagas").param("abrangencia","ESTADUAL")).andExpect(status().isBadRequest());
        assertThat(contarVagas()).isZero();
    }

    @ParameterizedTest @CsvSource({"experiencia,Pleno","tipoContrato,LIVRE"})
    void textoArbitrarioNaoDefineCatalogo(String campo, String valor) throws Exception {
        ObjectNode p = payload(); p.put(campo, valor);
        criar(p).andExpect(status().isUnprocessableEntity());
        assertThat(contarVagas()).isZero();
    }

    @Test void experienciaObrigatoriaAoPublicarMasNaoNoRascunho() throws Exception {
        ObjectNode p = payload(); p.remove("experiencia");
        criar(p).andExpect(status().isUnprocessableEntity());
        p.put("status","RASCUNHO");
        long id = idCriado(p);
        mvc.perform(patch("/api/vagas/{id}/status", id).header("Authorization", token(dono))
                .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"PUBLICAR\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select status::text from vagas where id=?",String.class,id)).isEqualTo("RASCUNHO");
    }

    @Test void limiteTresTresPersistidoERetornoSemDuplicacao() throws Exception {
        ObjectNode p = payload();
        array(p,"funcaoIds",funcoes.subList(0,3)); array(p,"especializacaoIds",specs.subList(0,3));
        long id = idCriado(p);
        mvc.perform(get("/api/vagas/{id}",id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.funcaoIds.length()").value(3))
                .andExpect(jsonPath("$.especializacaoIds.length()").value(3))
                .andExpect(jsonPath("$.valorMinimo").value(1000)).andExpect(jsonPath("$.valorMaximo").value(3000))
                .andExpect(jsonPath("$.capaUrl").value("https://example.com/capa.jpg"))
                .andExpect(jsonPath("$.enderecoCompleto").doesNotExist());
    }

    @ParameterizedTest @ValueSource(strings={"funcaoIds","especializacaoIds"})
    void quartaSelecaoEDuplicatasSaoRejeitadasSemPersistencia(String campo) throws Exception {
        ObjectNode p = payload(); array(p,"funcaoIds",funcoes.subList(0,3));
        array(p,campo,campo.equals("funcaoIds")?funcoes:specs);
        criar(p).andExpect(status().isUnprocessableEntity()); assertThat(contarVagas()).isZero();
        array(p,campo,List.of(funcoes.getFirst(),funcoes.getFirst()));
        criar(p).andExpect(status().isUnprocessableEntity()); assertThat(contarVagas()).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"funcaoIds","especializacaoIds"})
    void edicaoInvalidaPreservaTaxonomiaEPublicacao(String campo) throws Exception {
        ObjectNode p=payload(); array(p,"funcaoIds",funcoes.subList(0,3)); array(p,"especializacaoIds",specs.subList(0,3));
        long id=idCriado(p);
        String antes=db.queryForObject("select data_publicacao::text from vagas where id=?",String.class,id);
        p.put("titulo","Não persistir"); array(p,campo,campo.equals("funcaoIds")?funcoes:specs);
        editar(id,p).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select titulo from vagas where id=?",String.class,id)).isEqualTo("Vaga auditada");
        assertThat(db.queryForList("select funcao_id from vaga_funcao where vaga_id=?",Long.class,id)).containsExactlyInAnyOrderElementsOf(funcoes.subList(0,3));
        assertThat(db.queryForList("select especializacao_id from vaga_especializacao where vaga_id=?",Long.class,id)).containsExactlyInAnyOrderElementsOf(specs.subList(0,3));
        assertThat(db.queryForObject("select data_publicacao::text from vagas where id=?",String.class,id)).isEqualTo(antes);
    }

    @Test void incompatibilidadeDeEspecializacaoReverteTudo() throws Exception {
        ObjectNode p=payload(); array(p,"funcaoIds",List.of(funcoes.getFirst())); array(p,"especializacaoIds",List.of(specs.getFirst()));
        long id=idCriado(p);
        p.put("titulo","Rollback"); array(p,"especializacaoIds",List.of(specs.get(1)));
        editar(id,p).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select titulo from vagas where id=?",String.class,id)).isEqualTo("Vaga auditada");
        assertThat(db.queryForList("select especializacao_id from vaga_especializacao where vaga_id=?",Long.class,id)).containsExactly(specs.getFirst());
    }

    @Test void aliasFixoSomenteQuandoCoerente() throws Exception {
        ObjectNode p=payload(); p.remove("valorMinimo");p.remove("valorMaximo");p.put("remuneraValor",1250);
        criar(p).andExpect(status().isCreated()).andExpect(jsonPath("$.valorMinimo").value(1250))
                .andExpect(jsonPath("$.valorMaximo").value(1250)).andExpect(jsonPath("$.remuneraValor").value(1250));
        p=payload();p.put("remuneraValor",1000); criar(p).andExpect(status().isUnprocessableEntity());
        assertThat(contarVagas()).isOne();
    }

    @ParameterizedTest @ValueSource(strings={"valorMinimo","valorMaximo"})
    void faixaIncompletaOuNegativaNaoPublica(String campo) throws Exception {
        ObjectNode p=payload();p.remove(campo);criar(p).andExpect(status().isUnprocessableEntity());
        p=payload();p.put(campo,-1);criar(p).andExpect(status().isBadRequest());
        assertThat(contarVagas()).isZero();
    }

    @Test void aCombinarNaoAceitaValoresNemFiltroNumericoMesmoLegado() throws Exception {
        ObjectNode p=payload();p.put("formaRemuneracao","A_COMBINAR");criar(p).andExpect(status().isUnprocessableEntity());
        long id=vaga(dono,StatusVaga.ABERTA,"A combinar");
        db.update("update vagas set forma_remuneracao='A_COMBINAR',valor_minimo=0,valor_maximo=0 where id=?",id);
        assertThat(ids(pagina("valorMaximo","100"))).isEmpty();
        mvc.perform(get("/api/vagas/{id}",id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.valorMinimo").isEmpty()).andExpect(jsonPath("$.valorMaximo").isEmpty())
                .andExpect(jsonPath("$.remuneraValor").isEmpty());
    }

    @ParameterizedTest @CsvSource({"valorMinimo,2000","valorMaximo,1500","valorMinimo,3000","valorMaximo,1000"})
    void intersecaoIncluiFaixaAmplaELimites(String campo,String valor) throws Exception {
        long id=vaga(dono,StatusVaga.ABERTA,"Faixa ampla");
        assertThat(ids(pagina(campo,valor))).containsExactly(id);
    }

    @Test void filtroDeFaixaComDoisExtremosEConflitos() throws Exception {
        long ampla=vaga(dono,StatusVaga.ABERTA,"Faixa ampla");
        long distante=vaga(dono,StatusVaga.ABERTA,"Distante");
        db.update("update vagas set valor_minimo=4000,valor_maximo=5000 where id=?",distante);
        assertThat(ids(pagina("valorMinimo","2000","valorMaximo","2500"))).containsExactly(ampla);
        assertThat(ids(pagina("valorMinimo","3100","valorMaximo","3900"))).isEmpty();
        mvc.perform(get("/api/vagas").param("valorMinimo","2000").param("faixaSalarialMin","1")).andExpect(status().isBadRequest());
    }

    @Test void buscaSomenteTituloEEscapaCaracteresLike() throws Exception {
        long id=vaga(dono,StatusVaga.ABERTA,"Músico 100%_Jazz");
        assertThat(ids(pagina("q","%_"))).containsExactly(id);
        assertThat(ids(pagina("q","Empresa privada"))).isEmpty();
        assertThat(ids(pagina("q","descricao privada"))).isEmpty();
        assertThat(ids(pagina("q","' OR 1=1 --"))).isEmpty();
        assertThat(ids(pagina("q","músico"))).containsExactly(id);
        assertThat(ids(pagina("q","'"))).isEmpty();
    }

    @Test void dataPublicacaoPrazoENulosFiltramAntesDaPagina() throws Exception {
        long com=vaga(dono,StatusVaga.ABERTA,"Com prazo");
        long sem=vaga(dono,StatusVaga.ABERTA,"Sem prazo");
        db.update("update vagas set data_limite_candidatura='2040-03-15',data_publicacao='2026-10-05 23:59:59' where id=?",com);
        db.update("update vagas set data_publicacao='2026-10-06' where id=?",sem);
        assertThat(ids(pagina("comPrazo","true"))).containsExactly(com);
        assertThat(ids(pagina("comPrazo","false"))).containsExactly(sem);
        assertThat(ids(pagina("dataPublicacao","2026-10-05"))).containsExactly(com);
        assertThat(ids(pagina("dataPublicacaoInicio","2026-10-05","dataPublicacaoFim","2026-10-05"))).containsExactly(com);
        assertThat(ids(pagina("dataLimite","2040-03-15","comPrazo","true"))).containsExactly(com);
        assertThat(ids(pagina("dataLimiteInicio","2040-03-14","dataLimiteFim","2040-03-16"))).containsExactly(com);
        mvc.perform(get("/api/vagas").param("dataPublicacaoInicio","2026-10-07").param("dataPublicacaoFim","2026-10-05")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vagas").param("comPrazo","false").param("dataLimite","2040-03-15")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest @ValueSource(strings={"somenteMinhasVagas","somenteCandidatei","somenteFavoritas"})
    void recortesPessoaisExigemJwt(String filtro) throws Exception {
        mvc.perform(get("/api/vagas").param(filtro,"true")).andExpect(status().isUnauthorized());
    }

    @Test void minhasUsaJwtIncluiCincoEstadosESemOwnerArbitrario() throws Exception {
        for (StatusVaga s:StatusVaga.values()) vaga(dono,s,"Minha "+s);
        Usuario terceiro=usuario(TipoUsuario.CONTRATANTE);vaga(terceiro,StatusVaga.ABERTA,"Outra");
        JsonNode p=pagina(auth(dono).param("somenteMinhasVagas","true"));
        assertThat(p.path("totalElements").asInt()).isEqualTo(5);assertThat(ids(p)).hasSize(5);
        p.path("content").forEach(v->assertThat(v.path("contratanteId").asLong()).isEqualTo(dono.getId()));
        mvc.perform(auth(dono).param("somenteMinhasVagas","true").param("ownerId",terceiro.getId().toString())).andExpect(status().isBadRequest());
        mvc.perform(auth(usuario(TipoUsuario.ARTISTA)).param("somenteMinhasVagas","true")).andExpect(status().isForbidden());
        mvc.perform(auth(dono).param("somenteCandidatei","true")).andExpect(status().isForbidden());
    }

    @Test void minhasCandidaturasSaoHistoricasESemTentativasDuplicaremPagina() throws Exception {
        Usuario a=usuario(TipoUsuario.ARTISTA),b=usuario(TipoUsuario.ARTISTA);
        long encerrada=vaga(dono,StatusVaga.ENCERRADA,"Encerrada");
        long cancelada=vaga(dono,StatusVaga.CANCELADA,"Cancelada");
        long outra=vaga(dono,StatusVaga.ABERTA,"Candidatura de B");
        candidatura(a,encerrada,"RETIRADA");candidatura(a,encerrada,"PENDENTE");candidatura(a,cancelada,"CANCELADA_POR_VAGA");candidatura(b,outra,"PENDENTE");
        JsonNode p=pagina(auth(a).param("somenteCandidatei","true").param("size","1"));
        assertThat(p.path("totalElements").asInt()).isEqualTo(2);assertThat(ids(p)).containsExactly(encerrada);
        assertThat(ids(pagina(auth(a).param("somenteCandidatei","true").param("cursor",p.path("nextCursor").asText()).param("size","1")))).containsExactly(cancelada);
        assertThat(ids(pagina(auth(b).param("somenteCandidatei","true")))).containsExactly(outra);
    }

    @Test void favoritosIsoladosDetalheHistoricoERascunhoNaoEnumeravel() throws Exception {
        Usuario a=usuario(TipoUsuario.ARTISTA),b=usuario(TipoUsuario.ARTISTA);
        long aberta=vaga(dono,StatusVaga.ABERTA,"Aberta"),encerrada=vaga(dono,StatusVaga.ENCERRADA,"Encerrada");
        long rascunho=vaga(dono,StatusVaga.RASCUNHO,"Rascunho");
        favorito(a,aberta);favorito(a,encerrada);favorito(a,rascunho);
        assertThat(ids(pagina(auth(a).param("somenteFavoritas","true")))).containsExactly(aberta,encerrada);
        assertThat(ids(pagina(auth(b).param("somenteFavoritas","true")))).isEmpty();
        mvc.perform(get("/api/vagas/{id}",encerrada).header("Authorization",token(a))).andExpect(status().isOk())
                .andExpect(jsonPath("$.favorito").value(true)).andExpect(jsonPath("$.enderecoCompleto").doesNotExist())
                .andExpect(jsonPath("$.propriaDoContratante").value(false));
        mvc.perform(get("/api/vagas/{id}",encerrada).header("Authorization",token(b))).andExpect(status().isNotFound());
        mvc.perform(get("/api/vagas/{id}",rascunho).header("Authorization",token(a))).andExpect(status().isNotFound());
        mvc.perform(get("/api/vagas/{id}",aberta).header("Authorization",token(b))).andExpect(status().isOk()).andExpect(jsonPath("$.favorito").value(false));
        mvc.perform(get("/api/vagas/{id}",aberta)).andExpect(status().isOk()).andExpect(jsonPath("$.favorito").doesNotExist());
    }

    @Test void countTaxonomiaPaginasEStatementsSaoLimitadosEmLote() throws Exception {
        Usuario a=usuario(TipoUsuario.ARTISTA);
        for(int i=0;i<55;i++){
            long id=vaga(dono,StatusVaga.ABERTA,"Vaga "+i);
            for(int j=0;j<3;j++){ db.update("insert into vaga_funcao values (?,?)",id,funcoes.get(j));db.update("insert into vaga_especializacao values (?,?)",id,specs.get(j)); }
            db.update("insert into fotos_vaga(vaga_id,ordem,url) values (?,0,'https://example.com/capa.jpg')",id);
            favorito(a,id);
        }
        SqlInspector.sql.get().clear();
        JsonNode pequena=pagina(auth(a).param("size","10").param("funcaoIds",csv(funcoes.subList(0,3))).param("especializacaoIds",csv(specs.subList(0,3))));
        int statements10=SqlInspector.sql.get().size();
        SqlInspector.sql.get().clear();
        JsonNode grande=pagina(auth(a).param("size","999").param("funcaoIds",csv(funcoes.subList(0,3))).param("especializacaoIds",csv(specs.subList(0,3))));
        int statements50=SqlInspector.sql.get().size();
        assertThat(statements50).isLessThanOrEqualTo(statements10+1).isLessThan(16);
        assertThat(grande.path("totalElements").asInt()).isEqualTo(55);assertThat(ids(grande)).hasSize(50).doesNotHaveDuplicates();
        assertThat(pequena.path("totalElements").asInt()).isEqualTo(55);
        JsonNode ultima=pagina(auth(a).param("size","50").param("cursor",grande.path("nextCursor").asText()));
        assertThat(ids(ultima)).hasSize(5).doesNotContainAnyElementsOf(ids(grande));
        assertThat(ids(pagina("page","1","size","50"))).containsExactlyElementsOf(ids(ultima));
        assertThat(SqlInspector.sql.get()).anyMatch(s->s.toLowerCase(Locale.ROOT).contains("offset")&&s.toLowerCase(Locale.ROOT).contains("fetch first"));
    }

    @ParameterizedTest @CsvSource({"sort,senha","ownerId,1","usuarioId,1","contratanteId,1","page,-1","size,-1","size,0","dataLimite,invalida"})
    void parametrosInvalidosNaoSaoIgnorados(String parametro,String valor) throws Exception {
        mvc.perform(get("/api/vagas").param(parametro,valor)).andExpect(status().isBadRequest());
    }

    @Test void filtrosRetiradosNaoSaoInventados() throws Exception {
        mvc.perform(get("/api/vagas").param("empresa","Palco")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/vagas").param("beneficios","ALIMENTACAO")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("D08")));
    }

    @Test void afirmativaPluralEValoresLegadosNaoSaoSimulados() throws Exception {
        ObjectNode p=payload();p.putArray("categoriaAfirmativaIds").add(1).add(3);
        criar(p).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("C15")));
        for(int legado:List.of(2,4)){p.putArray("categoriaAfirmativaIds").add(legado);criar(p).andExpect(status().isUnprocessableEntity());}
        assertThat(contarVagas()).isZero();
        p.putArray("categoriaAfirmativaIds").add(3);idCriado(p);
        assertThat(pagina("afirmativa","true","categoriaAfirmativaIds","3").path("totalElements").asInt()).isEqualTo(1);
    }

    @Test void encerradaEditaDadosSemReabrirNemRedefinirHistorico() throws Exception {
        long id=vaga(dono,StatusVaga.ENCERRADA,"Encerrada");
        Usuario artista=usuario(TipoUsuario.ARTISTA);candidatura(artista,id,"RETIRADA");
        db.update("update vagas set data_limite_candidatura='2020-01-01',data_publicacao='2019-01-01',ultima_atualizacao='2019-01-01' where id=?",id);
        String candidaturaAntes=db.queryForObject("select row_to_json(c)::text from candidaturas c where vaga_id=?",String.class,id);
        ObjectNode p=payload();p.put("titulo","Edição histórica");p.put("dataLimiteCandidatura","2020-01-01");p.put("status","ABERTA");
        JsonNode n=resposta(editar(id,p).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(n.path("status").asText()).isEqualTo("ENCERRADA");
        assertThat(n.path("dataPublicacao").asText()).startsWith("2019-01-01");
        assertThat(n.path("ultimaAtualizacao").asText()).doesNotStartWith("2019");
        assertThat(db.queryForObject("select row_to_json(c)::text from candidaturas c where vaga_id=?",String.class,id)).isEqualTo(candidaturaAntes);
        p.put("dataLimiteCandidatura","2021-01-01");editar(id,p).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select data_limite_candidatura::text from vagas where id=?",String.class,id)).isEqualTo("2020-01-01");
    }

    @Test void edicaoApenasDeVinculosAtualizaTimestampFisico() throws Exception {
        ObjectNode p=payload();
        JsonNode criada=resposta(criar(p).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id=criada.path("id").asLong();
        LocalDateTime antes=LocalDateTime.parse(criada.path("ultimaAtualizacao").asText());
        array(p,"funcaoIds",List.of(funcoes.getFirst()));
        array(p,"especializacaoIds",List.of(specs.getFirst()));
        JsonNode editada=resposta(editar(id,p).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        LocalDateTime depois=LocalDateTime.parse(editada.path("ultimaAtualizacao").asText());
        assertThat(depois).isAfter(antes);
        assertThat(depois).isEqualTo(db.queryForObject("select ultima_atualizacao from vagas where id=?",
                java.sql.Timestamp.class,id).toLocalDateTime());
        assertThat(editada.path("dataPublicacao")).isEqualTo(criada.path("dataPublicacao"));
        assertThat(editada.path("titulo")).isEqualTo(criada.path("titulo"));
    }

    private ObjectNode payload(){
        ObjectNode p=mapper.createObjectNode();p.put("titulo","Vaga auditada");p.put("descricao","Descrição pública");
        p.put("areaId",1);p.put("abrangencia","LOCAL");p.put("formaRemuneracao","POR_PROJETO");p.put("valorMinimo",1000);p.put("valorMaximo",3000);
        p.put("cidade","Campinas");p.put("estado","SP");p.put("tipoContrato","FREELANCER");p.put("experiencia","INICIANTE");p.put("modeloTrabalho","HIBRIDO");
        p.put("enderecoCompleto","endereço privado");p.putArray("fotos").add("https://example.com/capa.jpg");return p;
    }
    private org.springframework.test.web.servlet.ResultActions criar(ObjectNode p)throws Exception{return mvc.perform(post("/api/vagas").header("Authorization",token(dono)).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(p)));}
    private org.springframework.test.web.servlet.ResultActions editar(long id,ObjectNode p)throws Exception{return mvc.perform(put("/api/vagas/{id}",id).header("Authorization",token(dono)).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(p)));}
    private long idCriado(ObjectNode p)throws Exception{return resposta(criar(p).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();}
    private JsonNode resposta(String json)throws Exception{return mapper.readTree(json);}
    private String token(Usuario u){return "Bearer "+jwt.gerarToken(u);}
    private MockHttpServletRequestBuilder auth(Usuario u){return get("/api/vagas").header("Authorization",token(u));}
    private JsonNode pagina(String... filtros)throws Exception{var r=get("/api/vagas");for(int i=0;i<filtros.length;i+=2)r.param(filtros[i],filtros[i+1]);return pagina(r);}
    private JsonNode pagina(MockHttpServletRequestBuilder r)throws Exception{return resposta(mvc.perform(r).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
    private List<Long> ids(JsonNode n){var ids=new ArrayList<Long>();n.path("content").forEach(v->ids.add(v.path("id").asLong()));return ids;}
    private String csv(List<Long> ids){return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));}
    private void array(ObjectNode p,String campo,List<Long> ids){var a=p.putArray(campo);ids.forEach(a::add);}
    private long contarVagas(){return db.queryForObject("select count(*) from vagas",Long.class);}
    private void favorito(Usuario u,long id){db.update("insert into itens_salvos(usuario_id,tipo_alvo,alvo_id) values (?,'VAGA',?)",u.getId(),id);}
    private void candidatura(Usuario u,long id,String status){db.update("insert into candidaturas(artista_id,vaga_id,status,data_candidatura) values (?,?,?::status_candidatura_enum,current_timestamp)",u.getId(),id,status);}
    private long vaga(Usuario u,StatusVaga status,String titulo){return db.queryForObject(
            "insert into vagas(contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,endereco_completo,tipo_contrato,experiencia,modelo_trabalho,abrangencia,status,forma_remuneracao,valor_minimo,valor_maximo) values (?,1,?,'descricao privada','','Campinas','SP','endereço privado','Freelance','INICIANTE','HIBRIDO','LOCAL',?::status_vaga_enum,'POR_PROJETO',1000,3000) returning id",Long.class,u.getId(),titulo,status.name());}
    private Usuario usuario(TipoUsuario tipo){
        Usuario u=com.portifolio.support.OfficialSchemaFixtures.usuario();u.setNome("Identidade privada");
        u.setEmail(UUID.randomUUID()+"@vagas.test");u.setSenha("{noop}fixture");u.setTipoUsuario(tipo);
        u.setTelefone("11999999999");
        u.setDataNascimento(LocalDate.of(1990,1,1));u.setStatusConta(StatusConta.ATIVA);u.setEmailVerificado(true);u.setPerfilCompleto(false);u.setDataCriacao(LocalDateTime.now());
        u=usuarios.saveAndFlush(u);
        if(tipo==TipoUsuario.CONTRATANTE){PerfilContratante p=new PerfilContratante();p.setUsuario(u);p.setTipoPerfil("PESSOA_FISICA");p.setNomeEmpresa("Empresa privada");contratantes.saveAndFlush(p);}
        else{PerfilArtista p=new PerfilArtista();p.setUsuario(u);p.setTipoPerfilArtistico(TipoPerfilArtistico.ARTISTA_SOLO);p.setRaioAtuacao(Abrangencia.LOCAL);artistas.saveAndFlush(p);}
        return u;
    }
    public static class SqlInspector implements StatementInspector{
        static final ThreadLocal<List<String>> sql=ThreadLocal.withInitial(ArrayList::new);
        public String inspect(String statement){sql.get().add(statement);return statement;}
    }
}
