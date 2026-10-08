package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.security.JwtService;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers @SpringBootTest @AutoConfigureMockMvc
class ModeracaoRf18IntegrationTest {
    @Container @ServiceConnection static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer();
    @TempDir static Path storage;
    @DynamicPropertySource static void propriedades(DynamicPropertyRegistry r) { r.add("app.portfolio.storage-root",()->storage.toString()); }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate db; @Autowired UsuarioRepository usuarios; @Autowired JwtService jwt;
    @Autowired VagaRepository vagas;
    private final ObjectMapper json=new ObjectMapper();
    @BeforeEach void dados() {
        db.execute("truncate usuarios,funcoes,especializacoes restart identity cascade");
        db.execute("""
            insert into usuarios(id,username,nome,data_nascimento,telefone,email,senha,tipo_usuario,status_conta) values
            (1,'rf18_artista','Artista RF18','1990-01-01','11900000001','a@rf18.invalid','fixture','ARTISTA','ATIVA'),
            (2,'rf18_dono','Contratante RF18','1990-01-01','11900000002','c@rf18.invalid','fixture','CONTRATANTE','ATIVA'),
            (3,'rf18_admin','Equipe RF18','1990-01-01','11900000003','admin@rf18.invalid','fixture','ADMIN','ATIVA'),
            (4,'rf18_mod','Equipe RF18','1990-01-01','11900000004','mod@rf18.invalid','fixture','MODERADOR','ATIVA'),
            (5,'rf18_mod2','Equipe RF18','1990-01-01','11900000005','mod2@rf18.invalid','fixture','MODERADOR','ATIVA');
            insert into perfis_artistas(usuario_id,tipo_perfil_artistico,raio_atuacao,cidade,estado) values(1,'ARTISTA_SOLO','LOCAL','São Paulo','SP');
            insert into perfis_contratantes(usuario_id,nome_empresa,tipo_contratante) values(2,'Empresa RF18','SETOR_PRIVADO');
            insert into vagas(id,contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,tipo_contrato,abrangencia,status) values
            (10,2,1,'Vaga primeira','Descrição','Requisitos','São Paulo','SP','Projeto','LOCAL','ABERTA'),
            (11,2,1,'Vaga segunda','Descrição','Requisitos','São Paulo','SP','Projeto','LOCAL','ABERTA'),
            (12,2,1,'Vaga privada','Descrição','Requisitos','São Paulo','SP','Projeto','LOCAL','RASCUNHO');
            insert into funcoes(id,area_id,nome) values(10,1,'Fotografia RF18');
            insert into vaga_funcao(vaga_id,funcao_id) values(10,10),(11,10);
            insert into comunidades(id,criador_id,nome,descricao,categoria_artistica,privacidade) values
            (20,2,'Pública A','Descrição','Arte','PUBLICA'),(21,2,'Pública B','Descrição','Arte','PUBLICA'),
            (22,2,'Privada','Descrição','Arte','PRIVADA'),(23,null,'Sem autor','Descrição','Arte','PUBLICA');
            """);
    }
    @AfterEach void limpeza() throws Exception {
        try(var p=Files.walk(storage)) { for(var f:p.sorted(Comparator.reverseOrder()).filter(f->!f.equals(storage)).toList()) Files.deleteIfExists(f); }
    }
    private String token(long id) { return "Bearer "+jwt.gerarToken(usuarios.findById(id).orElseThrow()); }
    private ResultActions acao(long ator,String tipo,long alvo,String estado) throws Exception {
        return mvc.perform(post("/api/moderacao/"+tipo+"/"+alvo+"/acoes").header("Authorization",token(ator))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",estado,"justificativa","Regra objetiva registrada"))));
    }
    private ResultActions denuncia(long ator,String tipo,long alvo,String motivo,String descricao) throws Exception {
        return mvc.perform(post("/api/denuncias").header("Authorization",token(ator)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("tipoAlvo",tipo,"alvoId",alvo,"motivo",motivo,"descricao",descricao))));
    }
    private long quantidade(String tabela) {
        // Identificador constante dos testes, nunca recebido pela API.
        return db.queryForObject("select count(*) from "+tabela,Long.class);
    }
    @Test void bloqueioAuditavelOcultaAntesDePaginarSemExcluirEAdminRestaura() throws Exception {
        acao(4,"VAGA",10,"BLOQUEADO").andExpect(status().isCreated()).andExpect(jsonPath("$.moderadorId").value(4))
            .andExpect(jsonPath("$.data").isNotEmpty());
        mvc.perform(get("/api/vagas?size=1")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(11))
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.hasMore").value(false));
        mvc.perform(get("/api/vagas/10")).andExpect(status().isNotFound());
        mvc.perform(get("/api/vagas/10/similares")).andExpect(status().isNotFound());
        mvc.perform(get("/api/vagas/11/similares")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/vagas/10").header("Authorization",token(2))).andExpect(status().isOk());
        assertThat(quantidade("vagas")).isEqualTo(3);
        db.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal) values(1,1,true)");
        assertThat(vagas.findRecomendadasParaArtista(1L,java.time.LocalDate.now(),org.springframework.data.domain.PageRequest.of(0,1)).getTotalElements()).isEqualTo(1);
        acao(4,"VAGA",10,"APROVADO").andExpect(status().isForbidden());
        acao(3,"VAGA",10,"APROVADO").andExpect(status().isCreated());
        mvc.perform(get("/api/vagas/10")).andExpect(status().isOk());
        mvc.perform(get("/api/moderacao/VAGA/10/historico?size=1").header("Authorization",token(3)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].status").value("APROVADO"))
            .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.hasMore").value(true));
        assertThat(db.queryForList("select status_moderacao::text from moderacao_conteudo order by id",String.class)).containsExactly("BLOQUEADO","APROVADO");
    }
    @Test void analiseDoProprioModeradorTransicaoNoOpEOtherActor() throws Exception {
        acao(4,"VAGA",10,"SOB_ANALISE").andExpect(status().isCreated());
        acao(5,"VAGA",10,"BLOQUEADO").andExpect(status().isForbidden());
        acao(4,"VAGA",10,"SOB_ANALISE").andExpect(status().isConflict());
        acao(4,"VAGA",10,"BLOQUEADO").andExpect(status().isCreated());
        acao(3,"VAGA",10,"SOB_ANALISE").andExpect(status().isCreated());
        assertThat(quantidade("moderacao_conteudo")).isEqualTo(3);
    }
    @ParameterizedTest @ValueSource(strings={"GALERIA","MENSAGEM","PERFIL_ARTISTA","ARQUIVO","PROJETO","VAGA;delete"})
    void tipoNaoAutorizadoNaoUsaIdDeOutroDominio(String tipo) throws Exception {
        acao(3,tipo,10,"BLOQUEADO").andExpect(status().isBadRequest());
        assertThat(quantidade("moderacao_conteudo")).isZero();
    }
    @Test void alvoEstadoJustificativaInvalidosNaoPersistem() throws Exception {
        acao(3,"VAGA",999,"BLOQUEADO").andExpect(status().isNotFound());
        acao(3,"VAGA",0,"BLOQUEADO").andExpect(status().isBadRequest());
        acao(3,"VAGA",12,"BLOQUEADO").andExpect(status().isUnprocessableEntity());
        acao(3,"COMUNIDADE",22,"BLOQUEADO").andExpect(status().isUnprocessableEntity());
        acao(3,"COMUNIDADE",23,"BLOQUEADO").andExpect(status().isUnprocessableEntity());
        acao(3,"VAGA",10,"ENCERRADA").andExpect(status().isBadRequest());
        mvc.perform(post("/api/moderacao/VAGA/10/acoes").header("Authorization",token(3)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"BLOQUEADO\",\"justificativa\":\"<script>teste</script>\"}")).andExpect(status().isUnprocessableEntity());
        assertThat(quantidade("moderacao_conteudo")).isZero();
    }
    @Test void atorDonoEDataInjetadosNaoAlteramAuditoria() throws Exception {
        mvc.perform(post("/api/moderacao/VAGA/10/acoes").header("Authorization",token(3)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"BLOQUEADO\",\"justificativa\":\"Regra\",\"moderadorId\":1,\"autorId\":1,\"data\":\"2000-01-01\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.moderadorId").value(3));
        assertThat(db.queryForObject("select autor_id from moderacao_conteudo",Long.class)).isEqualTo(2);
        assertThat(db.queryForObject("select data_analise>current_timestamp-interval '1 minute' from moderacao_conteudo",Boolean.class)).isTrue();
    }
    @Test void comunidadePublicaPodeDenunciarOcultarRestaurarSemCriarProdutor() throws Exception {
        denuncia(1,"COMUNIDADE",21,"Outro","Contexto").andExpect(status().isCreated());
        acao(4,"COMUNIDADE",21,"SOB_ANALISE").andExpect(status().isCreated());
        mvc.perform(get("/api/comunidades?size=1").header("Authorization",token(1))).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(23));
        mvc.perform(get("/api/comunidades/21").header("Authorization",token(1))).andExpect(status().isNotFound());
        denuncia(1,"COMUNIDADE",21,"Outro","Contexto").andExpect(status().isNotFound());
        acao(3,"COMUNIDADE",21,"APROVADO").andExpect(status().isCreated());
        mvc.perform(get("/api/comunidades/21").header("Authorization",token(1))).andExpect(status().isOk());
        assertThat(quantidade("comunidades")).isEqualTo(4);
    }
    @Test void denunciaNaoRevelaPrivadaSemAutorOuVagaRascunhoMesmoParaDono() throws Exception {
        denuncia(2,"COMUNIDADE",22,"Outro","Contexto").andExpect(status().isNotFound());
        denuncia(1,"COMUNIDADE",23,"Outro","Contexto").andExpect(status().isNotFound());
        denuncia(2,"VAGA",12,"Outro","Contexto").andExpect(status().isNotFound());
        assertThat(quantidade("reportes_usuario")).isZero();
    }
    @Test void semEquipeTokenOuContaAtivaNaoHaAcessoAdministrativo() throws Exception {
        for(String url:List.of("/api/moderacao/denuncias","/api/moderacao/VAGA/10/historico")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
            mvc.perform(get(url).header("Authorization","Bearer invalid.rf18.token")).andExpect(status().isUnauthorized());
            mvc.perform(get(url).header("Authorization",token(1))).andExpect(status().isForbidden());
            mvc.perform(get(url).header("Authorization",token(2))).andExpect(status().isForbidden());
        }
        acao(1,"VAGA",10,"BLOQUEADO").andExpect(status().isForbidden());
        String antigo=token(3); db.update("update usuarios set status_conta='BLOQUEADA' where id=3");
        mvc.perform(get("/api/moderacao/denuncias").header("Authorization",antigo)).andExpect(status().isUnauthorized());
    }
    @Test void metadadosAdministrativosPaginadosSemRelatoPrivadoOuIdentidadeDenunciante() throws Exception {
        denuncia(1,"VAGA",10,"Outro","Relato privado não divulgado").andExpect(status().isCreated());
        denuncia(1,"COMUNIDADE",20,"Outro","Contexto").andExpect(status().isCreated());
        var r=mvc.perform(get("/api/moderacao/denuncias?size=1").header("Authorization",token(4)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.hasMore").value(true)).andReturn();
        assertThat(r.getResponse().getContentAsString()).doesNotContain("denunciante","email","telefone","cpf","responsavel","consentimento","dataNascimento","token","descricao","Relato privado");
        mvc.perform(get("/api/moderacao/denuncias?size=51").header("Authorization",token(3))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/moderacao/denuncias?page=-1").header("Authorization",token(3))).andExpect(status().isBadRequest());
    }
    @Test void duplicataGeralEhIdempotentePorSessentaSegundosSemImpedirRelatoPosterior() throws Exception {
        denuncia(1,"VAGA",10,"Outro","Contexto").andExpect(status().isCreated());
        denuncia(1,"VAGA",10,"Outro","Contexto").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        assertThat(quantidade("reportes_usuario")).isOne();
        db.update("update reportes_usuario set data_reporte=current_timestamp-interval '61 seconds'");
        denuncia(1,"VAGA",10,"Outro","Contexto").andExpect(status().isCreated());
        assertThat(quantidade("reportes_usuario")).isEqualTo(2);
    }
    @Test void plagioDuplicaSomenteEnquantoAtivoENaoMudaStatus() throws Exception {
        denuncia(2,"PERFIL_ARTISTA",1,"OUTRO","Contexto").andExpect(status().isCreated());
        denuncia(2,"PERFIL_ARTISTA",1,"OUTRO","Contexto").andExpect(status().isOk());
        db.update("update denuncias_plagio set status_denuncia='ENCERRADA'");
        denuncia(2,"PERFIL_ARTISTA",1,"OUTRO","Contexto").andExpect(status().isCreated());
        assertThat(db.queryForList("select status_denuncia::text from denuncias_plagio order by id",String.class)).containsExactly("ENCERRADA","RECEBIDA");
    }
    @Test void duplicatasConcorrentesCriamSomenteUmaLinha() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var inicio=new CountDownLatch(1);
            Callable<Integer> envio=()->{inicio.await();return denuncia(1,"VAGA",10,"Outro","Concorrente").andReturn().getResponse().getStatus();};
            var a=pool.submit(envio); var b=pool.submit(envio); inicio.countDown();
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,201);
        }
        assertThat(quantidade("reportes_usuario")).isOne();
    }
    @ParameterizedTest @ValueSource(strings={"<script>teste</script>","<img src=x onerror=teste>","javascript:teste","\u202eteste"})
    void complementoAtivoNaoPersiste(String texto) throws Exception {
        denuncia(1,"VAGA",10,"Outro",texto).andExpect(status().isUnprocessableEntity());
        assertThat(quantidade("reportes_usuario")).isZero();
    }
    @Test void nullLegadoFalhaFechadoEmVagaEComunidade() throws Exception {
        db.execute("insert into moderacao_conteudo(tipo_conteudo,conteudo_id,autor_id,status_moderacao) values('VAGA',10,2,null),('COMUNIDADE',21,2,null)");
        mvc.perform(get("/api/vagas/10")).andExpect(status().isNotFound());
        mvc.perform(get("/api/vagas?size=1")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(11));
        mvc.perform(get("/api/comunidades/21").header("Authorization",token(1))).andExpect(status().isNotFound());
        acao(3,"VAGA",10,"APROVADO").andExpect(status().isCreated());
    }
    @ParameterizedTest @ValueSource(strings={"reporte","moderacao"})
    void deleteArquivoComVinculoRealDeGaleriaPreservaBytesERegistro(String origem) throws Exception {
        var response=mvc.perform(multipart("/api/portfolio/arquivos").file(new MockMultipartFile("arquivo","obra.png","image/png",com.portifolio.validation.PortfolioFixtures.imagem("png")))
            .header("Authorization",token(1))).andExpect(status().isCreated()).andReturn();
        long arquivo=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        String referencia=db.queryForObject("select url_arquivo from portfolio_arquivos where id=?",String.class,arquivo);
        long galeria=db.queryForObject("insert into galerias_virtuais(dono_id,titulo,descricao,categoria,tipo_galeria) values(1,'Obra','Arte','Arte','INDIVIDUAL') returning id",Long.class);
        db.update("insert into itens_galeria(galeria_id,arquivo_id) values(?,?)",galeria,arquivo);
        if(origem.equals("reporte")) db.update("insert into reportes_usuario(denunciante_id,tipo_conteudo,conteudo_id,motivo_reporte) values(2,'GALERIA',?,'Outro')",galeria);
        else db.update("insert into moderacao_conteudo(tipo_conteudo,conteudo_id,autor_id,status_moderacao) values('GALERIA',?,1,'SOB_ANALISE')",galeria);
        mvc.perform(delete("/api/portfolio/arquivos/"+arquivo).header("Authorization",token(1))).andExpect(status().isConflict());
        assertThat(quantidade("portfolio_arquivos")).isOne();
        assertThat(Files.readAllBytes(storage.resolve(referencia))).isEqualTo(com.portifolio.validation.PortfolioFixtures.imagem("png"));
    }
    @Test void textosDoPerfilEEmbedsTambemRecebemPrevencaoBackend() throws Exception {
        mvc.perform(put("/api/perfis-artistas/1").header("Authorization",token(1)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"usuarioId\":1,\"biografia\":\"<svg onload=teste>\"}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/portfolio/videos").header("Authorization",token(1)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"url\":\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\",\"legenda\":\"<script>teste</script>\"}"))
            .andExpect(status().isUnprocessableEntity());
        assertThat(quantidade("embeds_externos")).isZero();
    }
    private com.fasterxml.jackson.databind.node.ObjectNode publicacao() {
        var p=json.createObjectNode();
        p.put("titulo","Vaga RF18"); p.put("descricao","Trabalho artístico válido"); p.put("requisitos","Portfólio");
        p.put("areaId",1);p.put("abrangencia","LOCAL");p.put("valorMinimo",100);p.put("valorMaximo",100);
        p.put("formaRemuneracao","POR_EVENTO");p.put("cidade","São Paulo");p.put("estado","SP");
        p.put("modeloTrabalho","PRESENCIAL");p.put("tipoContrato","Projeto");p.put("experiencia","SEM_EXPERIENCIA");return p;
    }
    @ParameterizedTest @ValueSource(strings={"<script>teste</script>","<img src=x onerror=teste>","javascript:teste","data:text/html,teste"})
    void vagaRejeitaConteudoAtivoNaCriacaoEEdicaoSemAlterarObraValida(String texto) throws Exception {
        var p=publicacao();p.put("descricao",texto);
        mvc.perform(post("/api/vagas").header("Authorization",token(2)).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
            .andExpect(status().isUnprocessableEntity());
        assertThat(quantidade("vagas")).isEqualTo(3);
        p.put("descricao","Trabalho artístico válido");
        var criada=mvc.perform(post("/api/vagas").header("Authorization",token(2)).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
            .andExpect(status().isCreated()).andReturn();
        long id=json.readTree(criada.getResponse().getContentAsString()).get("id").asLong();
        p.put("beneficios",texto);
        mvc.perform(put("/api/vagas/"+id).header("Authorization",token(2)).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
            .andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select descricao from vagas where id=?",String.class,id)).isEqualTo("Trabalho artístico válido");
    }
    @Test void nomePublicoNaoAceitaMarkupNaEdicao() throws Exception {
        mvc.perform(put("/api/usuarios/me").header("Authorization",token(1)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"<script>teste</script>\",\"telefone\":\"11900000001\",\"email\":\"a@rf18.invalid\"}"))
            .andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select nome from usuarios where id=1",String.class)).isEqualTo("Artista RF18");
    }
    @ParameterizedTest @ValueSource(strings={"titulo","cidade","tipoContrato","experiencia","requisitos","beneficios"})
    void outrosMetadadosPublicosNaoAceitamHtml(String campo) throws Exception {
        var p=publicacao();p.put(campo,"<img src=x onerror=teste>");
        mvc.perform(post("/api/vagas").header("Authorization",token(2)).contentType(MediaType.APPLICATION_JSON).content(p.toString()))
            .andExpect(status().isUnprocessableEntity());
        assertThat(quantidade("vagas")).isEqualTo(3);
    }
    @Test void arquivoComNomeAtivoNaoCriaRegistroNemBytes() throws Exception {
        mvc.perform(multipart("/api/portfolio/arquivos").file(new MockMultipartFile("arquivo","<img onerror=teste>.png","image/png",com.portifolio.validation.PortfolioFixtures.imagem("png")))
            .header("Authorization",token(1))).andExpect(status().isUnprocessableEntity());
        assertThat(quantidade("portfolio_arquivos")).isZero();
        try(var p=Files.walk(storage)) {assertThat(p.filter(Files::isRegularFile).count()).isZero();}
    }
    @Test void publicarRascunhoLegadoRevalidaBeneficiosAntesDeMudarStatus() throws Exception {
        db.update("update vagas set modelo_trabalho='PRESENCIAL',beneficios='<script>teste</script>' where id=12");
        mvc.perform(patch("/api/vagas/12/status").header("Authorization",token(2)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"acao\":\"PUBLICAR\"}")).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select status::text from vagas where id=12",String.class)).isEqualTo("RASCUNHO");
    }
}
