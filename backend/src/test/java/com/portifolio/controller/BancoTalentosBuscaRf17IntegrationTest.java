package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.support.OfficialPostgreSQLContainer;
import com.portifolio.support.OfficialSchemaFixtures;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers @SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.portifolio.controller.BancoTalentosBuscaRf17IntegrationTest$SqlInspector")
public class BancoTalentosBuscaRf17IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @MockitoSpyBean JdbcTemplate db;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired Clock clock;
    @Autowired Environment environment;
    final ObjectMapper mapper = new ObjectMapper();
    long dono, outro, f1, f2, fOutra, e1, e2;
    String token;

    @BeforeEach void dados() {
        limpar();
        dono = usuario("CONTRATANTE", "Contratante A", 30);
        outro = usuario("CONTRATANTE", "Contratante B", 30);
        db.update("insert into perfis_contratantes(usuario_id,tipo_contratante) values (?,'PESSOA_FISICA'),(?,'PESSOA_FISICA')", dono, outro);
        f1 = funcao(1, "Voz"); f2 = funcao(1, "Instrumento"); fOutra = funcao(2, "Outra função");
        e1 = especializacao("Jazz", f1); e2 = especializacao("Popular", f2);
        token = bearer(dono);
    }
    @AfterEach void limpar() {
        db.execute("truncate salas_chat, usuarios, funcoes, especializacoes restart identity cascade");
        SqlInspector.sql.get().clear();
    }

    @Test void autenticacaoAusenteOuInvalida401() throws Exception {
        mvc.perform(get("/api/talentos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/talentos").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @ValueSource(strings = {"ARTISTA", "ADMIN", "MODERADOR"})
    void somenteContratantePodeGerir(String papel) throws Exception {
        long ator = usuario(papel, "Outro papel", 30);
        mvc.perform(get("/api/talentos").header("Authorization", bearer(ator))).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings = {"PENDENTE_VERIFICACAO_EMAIL", "PENDENTE_TIPO_PERFIL", "PENDENTE_CONSENTIMENTO", "BLOQUEADA"})
    void tokenAntigoNaoContornaContaAtual(String estado) throws Exception {
        db.update("update usuarios set status_conta=?::status_conta_enum where id=?", estado, dono);
        mvc.perform(req()).andExpect(status().isUnauthorized());
    }
    @Test void tokenAntigoNaoContornaPapelAtual() throws Exception {
        db.update("update usuarios set tipo_usuario='ARTISTA' where id=?", dono);
        mvc.perform(req()).andExpect(status().isForbidden());
    }
    @Test void bancoProprioIsolaMembrosPublicosSalvosEOutroContratante() throws Exception {
        long a1 = artista("A1", 30), a2 = artista("A2", 30), b = artista("B1", 30);
        long publico = artista("Público global", 30), salvoSemVinculo = artista("Somente salvo", 30);
        membro(dono, a1); membro(dono, a2); membro(outro, b);
        salvo(dono, salvoSemVinculo);
        mvc.perform(get("/api/perfis/publicos/ARTISTA/" + publico)).andExpect(status().isOk());
        assertThat(ids(pagina())).containsExactly(a1, a2);
        assertThat(ids(pagina(get("/api/talentos").header("Authorization", bearer(outro))))).containsExactly(b);
        assertThat(pagina().path("content").get(0).path("salvo").asBoolean()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings = {"contratanteId", "usuarioId", "dono", "artistaId"})
    void idsExtrasNaoConcedemBancoAlheio(String parametro) throws Exception {
        long a = artista("Próprio", 30), b = artista("Alheio", 30);
        membro(dono, a); membro(outro, b);
        assertThat(ids(pagina(req().param(parametro, "" + outro).param("page", "0").param("size", "1")))).containsExactly(a);
        assertThat(ids(pagina(req().param(parametro, "" + outro).param("q", "Alheio")))).isEmpty();
    }
    @Test void bodyEPathNaoPermitemOwnershipAlheio() throws Exception {
        long a = artista("Próprio", 30), b = artista("Alheio", 30);
        membro(dono, a); membro(outro, b);
        assertThat(ids(pagina(req().contentType(MediaType.APPLICATION_JSON).content("{\"contratanteId\":" + outro + "}")))).containsExactly(a);
        mvc.perform(get("/api/talentos/contratantes/" + outro + "/participacao").header("Authorization", token))
                .andExpect(status().isForbidden());
    }
    @Test void participacaoRf13RealAlimentaBuscaSemNovaRelacao() throws Exception {
        long a = artista("Novo participante", 30);
        OfficialSchemaFixtures.completarArtista(db, a, f1);
        assertThat(ids(pagina())).isEmpty();
        mvc.perform(post("/api/talentos/contratantes/" + dono + "/participacao")
                .header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmado\":true}")).andExpect(status().isCreated());
        var relacao = vinculos();
        assertThat(ids(pagina())).containsExactly(a);
        assertThat(vinculos()).isEqualTo(relacao);
    }
    @ParameterizedTest @CsvSource({"João,1", "JOÃO,1", "Joao,0", "artístico,1", "inexistente,0"})
    void nomePublicoENomeArtisticoSeguemCaseEAcentos(String busca, int total) throws Exception {
        long a = artista("João artístico", 30); membro(dono, a);
        assertThat(pagina("q", busca).path("totalElements").asInt()).isEqualTo(total);
    }
    @Test void buscaNaoPercorreDadosPrivados() throws Exception {
        long a = artista("Nome público", 17); membro(dono, a); autorizar(a);
        db.update("update usuarios set cpf='98765432109',telefone='11777777777',email='contato_privado@rf17.test',token_recuperacao='segredo_recuperacao' where id=?", a);
        for (String busca : List.of("98765432109", "11777777777", "contato_privado", "Responsável secreto", "segredo_recuperacao"))
            assertThat(ids(pagina("q", busca))).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings = {"%", "_", "\\", "/", "' OR 1=1 --"})
    void caracteresESqlSaoLiteraisComBinding(String trecho) throws Exception {
        long a = artista("Nome " + trecho + " literal", 30), outroNome = artista("Outro nome", 30);
        membro(dono, a); membro(dono, outroNome);
        clearInvocations(db);
        assertThat(ids(pagina("q", trecho))).containsExactly(a);
        assertThat(sqlJdbc()).allSatisfy(sql -> assertThat(sql).doesNotContain("Nome " + trecho, "' OR 1=1 --"));
    }
    @Test void principalESecundariaSaoElegiveisSemPesoExtra() throws Exception {
        long principal = artista("Principal", 30), secundario = artista("Secundário", 30);
        db.update("update perfil_artista_area set principal=false where perfil_artista_id=?", secundario);
        area(secundario, 2, true, "INICIANTE", fOutra);
        membro(dono, principal); membro(dono, secundario);
        assertThat(ids(pagina("areaId", "1"))).containsExactly(principal, secundario);
        assertThat(ids(pagina("areaId", "2"))).containsExactly(secundario);
    }
    @Test void funcoesESpecializacoesOrDentroAndEntreCategorias() throws Exception {
        long a = artista("Voz", 30), b = artista("Instrumento", 30), c = artista("Sem especialização compatível", 30);
        db.update("delete from perfil_artista_especializacao where perfil_artista_id=?", b);
        db.update("delete from perfil_artista_funcao where perfil_artista_id=?", b);
        db.update("insert into perfil_artista_funcao values (?,1,?)", b, f2); spec(b, 1, e2);
        db.update("delete from perfil_artista_especializacao where perfil_artista_id=?", c);
        // Relação legada semanticamente incompatível não deve satisfazer o filtro.
        spec(c, 1, e2);
        membro(dono, a); membro(dono, b); membro(dono, c);
        assertThat(ids(pagina("areaId", "1", "funcaoIds", "" + f1))).containsExactly(a, c);
        assertThat(ids(pagina("areaId", "1", "funcaoIds", f1 + "," + f2, "especializacaoIds", e1 + "," + e2))).containsExactly(a, b);
        assertThat(ids(pagina("areaId", "1", "funcaoIds", "" + f2, "especializacaoIds", "" + e2))).containsExactly(b);
    }
    @ParameterizedTest @CsvSource({"areaId,999", "funcaoIds,999999", "especializacaoIds,999999"})
    void idsInexistentesNaoGeramConsultaEnganosa(String parametro, String valor) throws Exception {
        var request = req();
        if (!"areaId".equals(parametro)) request.param("areaId", "1");
        if (!"funcaoIds".equals(parametro)) request.param("funcaoIds", "" + f1);
        mvc.perform(request.param(parametro, valor))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test void hierarquiaIncompativelRejeitada() throws Exception {
        mvc.perform(req().param("areaId", "1").param("funcaoIds", "" + fOutra)).andExpect(status().isUnprocessableEntity());
        mvc.perform(req().param("areaId", "1").param("funcaoIds", "" + f1).param("especializacaoIds", "" + e2))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(req().param("funcaoIds", "" + f1)).andExpect(status().isUnprocessableEntity());
    }
    @Test void cidadeUfFiltrosObjetivosLiteralmenteCombinados() throws Exception {
        long a = artista("Recife", 30), b = artista("Campinas", 30);
        membro(dono, a); membro(dono, b);
        db.update("update perfis_artistas set cidade='Campinas',estado='SP' where usuario_id=?", b);
        assertThat(ids(pagina("cidade", " recife ", "estado", "pe"))).containsExactly(a);
        assertThat(ids(pagina("estado", "SP"))).containsExactly(b);
        assertThat(ids(pagina("cidade", "Rec"))).isEmpty();
        assertThat(ids(pagina("cidade", "Campinas", "estado", "PE"))).isEmpty();
        assertThat(ids(pagina("cidade", "%' OR 1=1 --"))).isEmpty();
    }
    @ParameterizedTest @CsvSource({"SEM_EXPERIENCIA,5", "INICIANTE,4", "INTERMEDIARIO,3", "EXPERIENTE,2", "ESPECIALISTA,1"})
    void experienciaMinimaAdultoSegueOrdemDoEnumOficial(String minimo, int total) throws Exception {
        for (String nivel : List.of("SEM_EXPERIENCIA", "INICIANTE", "INTERMEDIARIO", "EXPERIENTE", "ESPECIALISTA")) {
            long a = artista(nivel, 30); membro(dono, a);
            db.update("update perfil_artista_area set nivel_experiencia=?::nivel_experiencia_enum where perfil_artista_id=?", nivel, a);
        }
        assertThat(pagina("areaId", "1", "experienciaMinima", minimo).path("totalElements").asInt()).isEqualTo(total);
    }
    @Test void experienciaDeOutraAreaNaoSatisfazMinimo() throws Exception {
        long a = artista("Duas áreas", 30); membro(dono, a);
        area(a, 2, false, "ESPECIALISTA", fOutra);
        assertThat(ids(pagina("areaId", "1", "experienciaMinima", "ESPECIALISTA"))).isEmpty();
        assertThat(ids(pagina("areaId", "2", "experienciaMinima", "ESPECIALISTA"))).containsExactly(a);
        mvc.perform(req().param("experienciaMinima", "EXPERIENTE")).andExpect(status().isUnprocessableEntity());
    }
    @ParameterizedTest @ValueSource(strings = {"true", "false"})
    void disponibilidadeExataSemExclusaoImplicita(String valor) throws Exception {
        long a = artista("Disponível", 30), b = artista("Indisponível", 30), c = artista("Não preenchida", 30);
        membro(dono, a); membro(dono, b); membro(dono, c);
        db.update("update perfis_artistas set disponivel_oportunidades=true where usuario_id=?", a);
        db.update("update perfis_artistas set disponivel_oportunidades=false where usuario_id=?", b);
        assertThat(ids(pagina())).containsExactly(a, b, c);
        assertThat(ids(pagina("disponivel", valor))).containsExactly("true".equals(valor) ? a : b);
    }
    @Test void disponibilidadeECompletudePosterioresPreservamMembroHistorico() throws Exception {
        long a = artista("Histórico", 30); membro(dono, a);
        var relacao = vinculos();
        db.update("update perfis_artistas set disponivel_oportunidades=false where usuario_id=?", a);
        db.update("update usuarios set perfil_completo=false where id=?", a);
        assertThat(ids(pagina())).containsExactly(a);
        assertThat(vinculos()).isEqualTo(relacao);
        mvc.perform(get("/api/perfis/publicos/ARTISTA/" + a)).andExpect(status().isOk());
    }
    @ParameterizedTest @ValueSource(ints = {14, 17, 18})
    void menorAutorizadoEAdultoNoAniversarioAparecem(int idade) throws Exception {
        long a = artista("Conta apta", idade); membro(dono, a);
        if (idade < 18) autorizar(a);
        assertThat(ids(pagina())).containsExactly(a);
        JsonNode card = pagina().path("content").get(0);
        assertThat(card.path("username").asText()).isEqualTo(usuarios.findById(a).orElseThrow().getUsername());
        if (idade < 18) assertThat(card.path("areas").get(0).has("nivelExperiencia")).isFalse();
        mvc.perform(get("/api/perfis/publicos/ARTISTA/" + card.path("artistaId").asLong()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.experiencia").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings = {"SEM_EXPERIENCIA", "INICIANTE", "INTERMEDIARIO", "EXPERIENTE", "ESPECIALISTA", "NULL"})
    void filtroNuncaInfereExperienciaPrivadaDoMenor(String nivel) throws Exception {
        long a = artista("Menor autorizado", 17); membro(dono, a); autorizar(a);
        db.update("update perfil_artista_area set nivel_experiencia=cast(? as nivel_experiencia_enum) where perfil_artista_id=?",
                "NULL".equals(nivel) ? null : nivel, a);
        JsonNode anterior = pagina();
        for (String minimo : List.of("SEM_EXPERIENCIA", "INICIANTE", "INTERMEDIARIO", "EXPERIENTE", "ESPECIALISTA"))
            assertThat(pagina("areaId", "1", "experienciaMinima", minimo)).isEqualTo(anterior);
        assertThat(anterior.path("content").get(0).path("areas").get(0).has("nivelExperiencia")).isFalse();
    }
    @ParameterizedTest @ValueSource(strings = {"ausente", "pendente", "revogado", "idade13"})
    void menorInaptoOcultoSemApagarVinculo(String condicao) throws Exception {
        long a = artista("Menor", "idade13".equals(condicao) ? 13 : 17); membro(dono, a);
        if (!"ausente".equals(condicao)) autorizar(a);
        if ("pendente".equals(condicao)) db.update("update responsaveis_legais set data_consentimento=null where usuario_id=?", a);
        if ("revogado".equals(condicao)) db.update("update responsaveis_legais set consentimento_revogado=true where usuario_id=?", a);
        var relacao = vinculos();
        assertThat(ids(pagina())).isEmpty();
        assertThat(vinculos()).isEqualTo(relacao);
    }
    @ParameterizedTest @ValueSource(strings = {"PENDENTE_VERIFICACAO_EMAIL", "PENDENTE_TIPO_PERFIL", "PENDENTE_CONSENTIMENTO", "BLOQUEADA"})
    void membroNaoAptoOcultoSemDestruirHistorico(String estado) throws Exception {
        long a = artista("Membro", 30); membro(dono, a);
        var relacao = vinculos();
        db.update("update usuarios set status_conta=?::status_conta_enum where id=?", estado, a);
        assertThat(ids(pagina())).isEmpty();
        assertThat(vinculos()).isEqualTo(relacao);
    }
    @Test void revogacaoPosteriorOcultaSemModificarRelacaoOuExperiencia() throws Exception {
        long a = artista("Menor autorizado", 17); membro(dono, a); autorizar(a);
        assertThat(ids(pagina())).containsExactly(a);
        var relacao = vinculos();
        db.update("update responsaveis_legais set consentimento_revogado=true where usuario_id=?", a);
        assertThat(ids(pagina())).isEmpty();
        assertThat(vinculos()).isEqualTo(relacao);
    }
    @Test void whitelistNaoExposeCamposPrivadosNemIndicadoresDeRanking() throws Exception {
        long a = artista("Card", 17); membro(dono, a); autorizar(a);
        JsonNode card = pagina().path("content").get(0);
        Set<String> fields = new HashSet<>(); card.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).isSubsetOf("artistaId", "username", "nomeExibicao", "avatarUrl", "biografia",
                "localizacao", "cidade", "estado", "urlPortfolio", "tipoPerfilArtistico", "disponivelOportunidades",
                "areas", "salvo", "ultimaAtualizacao");
        assertThat(fields).contains("artistaId", "username", "nomeExibicao", "salvo");
        assertThat(card.toString()).doesNotContain("nivelExperiencia", "dataNascimento", "responsavel", "consentimento",
                "cpf", "cnpj", "telefone", "email", "senha", "token", "enderecoCompleto", "score",
                "quantidadeFuncoesCoincidentes", "quantidadeEspecializacoesCoincidentes", "medalha", "raioAtuacao");
    }
    @Test void paginasDistintasCountEstavelEUnicoComTaxonomiaMultipla() throws Exception {
        for (int i = 0; i < 53; i++) {
            long a = artista("Membro " + i, 30); membro(dono, a);
            db.update("insert into perfil_artista_funcao values (?,1,?)", a, f2); spec(a, 1, e2);
            area(a, 2, false, "INICIANTE", fOutra);
        }
        JsonNode first = pagina("size", "20"), next = pagina("size", "20", "page", "1"), last = pagina("size", "20", "page", "2");
        assertThat(first.path("totalElements").asLong()).isEqualTo(53);
        assertThat(first.path("hasMore").asBoolean()).isTrue();
        assertThat(ids(first)).hasSize(20).doesNotHaveDuplicates();
        assertThat(ids(next)).hasSize(20).doesNotContainAnyElementsOf(ids(first));
        assertThat(ids(last)).hasSize(13).doesNotContainAnyElementsOf(ids(first)).doesNotContainAnyElementsOf(ids(next));
        assertThat(last.path("hasMore").asBoolean()).isFalse();
        assertThat(pagina("size", "20")).isEqualTo(first);
        assertThat(ids(pagina("size", "50"))).hasSize(50);
        assertThat(ids(pagina("page", "9"))).isEmpty();
        assertThat(pagina("page", "9").path("totalElements").asLong()).isEqualTo(53);
    }
    @Test void paginaSemMembrosEh200Vazia() throws Exception {
        artista("Público sem relação", 30);
        assertThat(ids(pagina())).isEmpty();
        assertThat(pagina().path("totalElements").asLong()).isZero();
    }
    @ParameterizedTest @CsvSource({"page,-1", "page,2147483647", "size,0", "size,51", "size,abc", "ordenacao,id desc", "areaId,-1", "funcaoIds,0", "disponivel,talvez", "experienciaMinima,INVALIDA", "raios,LOCAL"})
    void parametrosInvalidos400SemDetalhesSql(String campo, String valor) throws Exception {
        var response = mvc.perform(req().param(campo, valor)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("PreparedStatement", "org.postgresql", "stackTrace", "SQLSTATE");
    }
    @Test void limitesTextuaisETaxonomia400Ou422ConformeHandlerExistente() throws Exception {
        mvc.perform(req().param("q", "a".repeat(151))).andExpect(status().isBadRequest());
        mvc.perform(req().param("q", "cont\nrole")).andExpect(status().isBadRequest());
        mvc.perform(req().param("estado", "PERNAMBUCO")).andExpect(status().isUnprocessableEntity());
        mvc.perform(req().param("cidade", "a".repeat(101))).andExpect(status().isUnprocessableEntity());
        String many = String.join(",", java.util.stream.IntStream.rangeClosed(1, 51).mapToObj(Integer::toString).toList());
        mvc.perform(req().param("areaId", "1").param("funcaoIds", many)).andExpect(status().isBadRequest());
    }
    @Test void compatibilidadeFuncaoPrecedeEspecializacaoAtualizacaoEId() throws Exception {
        long uma = artista("Uma função", 30), duas = artista("Duas funções", 30), outraArea = artista("Área incompatível", 30);
        db.update("insert into perfil_artista_funcao values (?,1,?)", duas, f2);
        db.update("insert into funcao_especializacao values (?,?)", f1, e2);
        spec(uma, 1, e2); // Duas especializações não vencem duas funções com uma especialização.
        db.update("delete from perfil_artista_area where perfil_artista_id=?", outraArea);
        area(outraArea, 2, true, "INICIANTE", fOutra);
        membro(dono, uma); membro(dono, duas); membro(dono, outraArea);
        long vaga = contexto();
        db.update("update perfis_artistas set ultima_atualizacao='2030-01-01' where usuario_id=?", uma);
        assertThat(ids(pagina("vagaId", "" + vaga))).containsExactly(duas, uma);
        assertThat(ids(pagina("vagaId", "" + vaga, "ordenacao", "ATUALIZACAO"))).containsExactly(uma, duas);
    }
    @Test void especializacaoDesempataDepoisDasFuncoesSemDarPesoAPrincipal() throws Exception {
        long a = artista("Principal", 30), b = artista("Secundário", 30), c = artista("Empate", 30);
        for (long id : List.of(a, b, c)) db.update("insert into perfil_artista_funcao values (?,1,?)", id, f2);
        spec(b, 1, e2); spec(c, 1, e2);
        db.update("update perfil_artista_area set principal=false where perfil_artista_id=?", b);
        area(b, 2, true, "INICIANTE", fOutra);
        membro(dono, a); membro(dono, b); membro(dono, c);
        long vaga = contexto();
        assertThat(ids(pagina("vagaId", "" + vaga))).containsExactly(b, c, a);
        assertThat(pagina("vagaId", "" + vaga)).isEqualTo(pagina("vagaId", "" + vaga));
    }
    @Test void engajamentoSocialSalvosCandidaturasMensagensMedalhasNaoAlteramOrdenacao() throws Exception {
        long a = artista("Primeiro", 30), b = artista("Segundo", 30); membro(dono, a); membro(dono, b);
        long vaga = contexto();
        List<Long> anterior = ids(pagina("vagaId", "" + vaga));
        salvo(outro, b);
        db.update("insert into candidaturas(vaga_id,artista_id,status) values (?,?,'PENDENTE')", vaga, b);
        long sala = db.queryForObject("insert into salas_chat default values returning id", Long.class);
        db.update("insert into participantes_chat values (?,?),(?,?)", sala, dono, sala, b);
        db.update("insert into mensagens_chat(sala_id,remetente_id,texto_mensagem) values (?,?,'Mensagem privada')", sala, b);
        db.update("insert into visualizacoes_perfil(perfil_visitado_id) values (?)", b);
        db.update("insert into historico_medalhas(artista_id,nivel_novo) values (?,5)", b);
        db.update("insert into ranking_top_da_semana(artista_id,score_semanal,data_inicio_ciclo,data_fim_ciclo,posicao_ranking) values (?,999,current_date,current_date,1)", b);
        assertThat(ids(pagina("vagaId", "" + vaga, "seguidores", "999", "scoreEngajamento", "999", "nivelMedalha", "5"))).isEqualTo(anterior);
    }
    @Test void atualizacaoDoPerfilNaoEhDataDeEntradaNoBanco() throws Exception {
        long a = artista("Perfil recente", 30), b = artista("Entrada recente", 30);
        membro(dono, a); membro(dono, b);
        db.update("update perfis_artistas set ultima_atualizacao='2026-05-01' where usuario_id=?", a);
        db.update("update banco_talentos set data_adicao='2030-01-01' where artista_id=?", b);
        assertThat(ids(pagina("ordenacao", "ATUALIZACAO"))).containsExactly(a, b);
        db.update("update perfis_artistas set ultima_atualizacao=null where usuario_id=?", a);
        assertThat(ids(pagina("ordenacao", "ATUALIZACAO"))).containsExactly(b, a);
    }
    @Test void estadoSalvoEhPrivadoDoJwtEIndependenteDoVinculo() throws Exception {
        long a = artista("Favorito", 30), b = artista("Favorito alheio", 30); membro(dono, a); membro(dono, b);
        salvo(outro, b);
        var relacao = vinculos();
        mvc.perform(post("/api/salvos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAlvo\":\"PERFIL_ARTISTA\",\"alvoId\":" + a + "}")).andExpect(status().isCreated());
        JsonNode page = pagina();
        assertThat(page.path("content").get(0).path("salvo").asBoolean()).isTrue();
        assertThat(page.path("content").get(1).path("salvo").asBoolean()).isFalse();
        mvc.perform(delete("/api/salvos/PERFIL_ARTISTA/" + a).header("Authorization", token)).andExpect(status().isNoContent());
        assertThat(pagina().path("content").get(0).path("salvo").asBoolean()).isFalse();
        assertThat(vinculos()).isEqualTo(relacao);
    }
    @Test void identificadorPermiteRf10ERf35MasBuscaNaoCriaSala() throws Exception {
        long a = artista("Adulto", 30); membro(dono, a);
        long id = pagina().path("content").get(0).path("artistaId").asLong();
        assertThat(db.queryForObject("select count(*) from salas_chat", Long.class)).isZero();
        mvc.perform(get("/api/perfis/publicos/ARTISTA/" + id)).andExpect(status().isOk());
        mvc.perform(post("/api/chat/salas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioDestinoId\":" + id + "}")).andExpect(status().isCreated());
    }
    @Test void menorSoComRf13MantemPendenciaRf35SemAmpliarChatSilenciosamente() throws Exception {
        long a = artista("Menor", 17); membro(dono, a); autorizar(a);
        assertThat(ids(pagina())).containsExactly(a);
        mvc.perform(post("/api/chat/salas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioDestinoId\":" + a + "}")).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select count(*) from salas_chat", Long.class)).isZero();
    }
    @Test void leituraNaoModificaNenhumaTabelaNemGeraConviteEventoOuNotificacao() throws Exception {
        long a = artista("Membro", 17); membro(dono, a); autorizar(a);
        var antes = contagens();
        var relacao = vinculos();
        for (int i = 0; i < 3; i++) pagina("areaId", "1", "experienciaMinima", "ESPECIALISTA");
        assertThat(contagens()).isEqualTo(antes);
        assertThat(vinculos()).isEqualTo(relacao);
    }
    @Test void queriesConstantesComUmOuCinquentaCardsIncluindoSalvoETaxonomia() throws Exception {
        for (int i = 0; i < 50; i++) {
            long a = artista("Membro " + i, i % 2 == 0 ? 17 : 30); membro(dono, a);
            if (i % 2 == 0) autorizar(a);
            db.update("insert into perfil_artista_funcao values (?,1,?)", a, f2); spec(a, 1, e2);
            area(a, 2, false, "INICIANTE", fOutra); salvo(dono, a);
        }
        clearInvocations(db); SqlInspector.sql.get().clear();
        assertThat(ids(pagina("size", "1"))).hasSize(1);
        long jdbcUma = queriesJdbc(); int jpaUma = SqlInspector.sql.get().size();
        clearInvocations(db); SqlInspector.sql.get().clear();
        assertThat(ids(pagina("size", "50"))).hasSize(50);
        assertThat(queriesJdbc()).isEqualTo(jdbcUma).isEqualTo(5);
        assertThat(SqlInspector.sql.get()).hasSize(jpaUma);
        assertThat(sqlJdbc()).anySatisfy(sql -> assertThat(sql).contains("banco_talentos", "limit ?", "offset ?"));
        System.out.println("RF17 N+1 JDBC 1/50: " + jdbcUma + "/" + queriesJdbc() + "; JPA 1/50: " + jpaUma + "/" + SqlInspector.sql.get().size());
    }
    @Test void database04ValidateEPkCompostaPermanecemAutoridade() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(db.queryForObject("select pg_get_constraintdef(oid) from pg_constraint where conrelid='banco_talentos'::regclass and contype='p'", String.class))
                .isEqualTo("PRIMARY KEY (contratante_id, artista_id)");
        assertThat(postgres.getDockerImageName()).contains("postgres:18");
    }

    MockHttpServletRequestBuilder req() { return get("/api/talentos").header("Authorization", token); }
    JsonNode pagina(String... params) throws Exception {
        var req = req();
        for (int i = 0; i < params.length; i += 2) req.param(params[i], params[i + 1]);
        return pagina(req);
    }
    JsonNode pagina(MockHttpServletRequestBuilder request) throws Exception {
        return mapper.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>(); page.path("content").forEach(card -> ids.add(card.path("artistaId").asLong())); return ids;
    }
    String bearer(long id) { return "Bearer " + jwt.gerarToken(usuarios.findById(id).orElseThrow()); }
    long usuario(String papel, String nome, int idade) {
        String unico = UUID.randomUUID().toString().replace("-", "");
        return db.queryForObject("""
                insert into usuarios(username,nome,data_nascimento,telefone,email,senha,tipo_usuario,status_conta,email_verificado)
                values (?,?,?,'11999999999',?,'hash-teste',?::tipo_usuario_enum,'ATIVA',true) returning id
                """, Long.class, "rf17_" + unico.substring(0, 20), nome, LocalDate.now(clock).minusYears(idade), unico + "@rf17.test", papel);
    }
    long artista(String nome, int idade) {
        long id = usuario("ARTISTA", nome, idade);
        db.update("""
                insert into perfis_artistas(usuario_id,biografia,cidade,estado,url_portfolio,tipo_perfil_artistico,ultima_atualizacao)
                values (?,'Biografia pública','Recife','PE','https://example.test/portfolio','ARTISTA_SOLO','2026-01-01')
                """, id);
        area(id, 1, true, "INICIANTE", f1); spec(id, 1, e1);
        return id;
    }
    void membro(long contratante, long artista) { db.update("insert into banco_talentos(contratante_id,artista_id) values (?,?)", contratante, artista); }
    void salvo(long usuario, long alvo) { db.update("insert into itens_salvos(usuario_id,tipo_alvo,alvo_id) values (?,'PERFIL_ARTISTA',?)", usuario, alvo); }
    void autorizar(long id) {
        db.update("""
                insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,versao_termo,data_consentimento,consentimento_revogado)
                values (?,'Responsável secreto','11888888888','responsavel_privado@rf17.test','v1',current_timestamp,false)
                """, id);
    }
    void area(long id, int area, boolean principal, String nivel, long... funcs) {
        db.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal,nivel_experiencia) values (?,?,?,?::nivel_experiencia_enum)", id, area, principal, nivel);
        for (long func : funcs) db.update("insert into perfil_artista_funcao values (?,?,?)", id, area, func);
    }
    void spec(long id, int area, long spec) { db.update("insert into perfil_artista_especializacao values (?,?,?)", id, area, spec); }
    long funcao(int area, String nome) { return db.queryForObject("insert into funcoes(area_id,nome) values (?,?) returning id", Long.class, area, nome); }
    long especializacao(String nome, long funcao) {
        long id = db.queryForObject("insert into especializacoes(nome) values (?) returning id", Long.class, nome);
        db.update("insert into funcao_especializacao values (?,?)", funcao, id); return id;
    }
    long contexto() {
        long id = db.queryForObject("""
                insert into vagas(contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,tipo_contrato,abrangencia,status)
                values (?,1,'Contexto próprio','Descrição','Requisitos','Recife','PE','Evento','LOCAL','ABERTA') returning id
                """, Long.class, dono);
        db.update("insert into vaga_funcao values (?,?),(?,?)", id, f1, id, f2);
        db.update("insert into vaga_especializacao values (?,?),(?,?)", id, e1, id, e2); return id;
    }
    List<Map<String, Object>> vinculos() { return db.queryForList("select * from banco_talentos order by contratante_id,artista_id"); }
    Map<String, Long> contagens() {
        Map<String, Long> result = new TreeMap<>();
        for (String table : db.queryForList("select tablename from pg_tables where schemaname='public' order by tablename", String.class))
            result.put(table, db.queryForObject("select count(*) from \"" + table.replace("\"", "\"\"") + "\"", Long.class));
        return result;
    }
    long queriesJdbc() {
        return mockingDetails(db).getInvocations().stream().filter(i -> i.getMethod().getName().equals("query")
                && i.getArguments().length == 3 && i.getArguments()[0] instanceof org.springframework.jdbc.core.PreparedStatementCreator).count();
    }
    List<String> sqlJdbc() {
        return mockingDetails(db).getInvocations().stream().filter(i -> i.getArguments().length > 0
                && i.getArguments()[0] instanceof org.springframework.jdbc.core.SqlProvider)
                .map(i -> ((org.springframework.jdbc.core.SqlProvider) i.getArguments()[0]).getSql()).toList();
    }
    public static class SqlInspector implements StatementInspector {
        static final ThreadLocal<List<String>> sql = ThreadLocal.withInitial(ArrayList::new);
        public String inspect(String query) { sql.get().add(query); return query; }
    }
}
