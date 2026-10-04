package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.*;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.service.*;
import com.portifolio.validation.PortfolioFixtures;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers @SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @AutoConfigureMockMvc
class ExclusaoContaRf22IntegrationTest {
    @Container @ServiceConnection static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");
    static final Path ROOT = Path.of("target-maven/database05/rf22-storage-"+UUID.randomUUID()).toAbsolutePath();
    static final String SENHA = "Legada1!"; // Confirmação não impõe a política de criação de senha nova.
    static final String HASH = new BCryptPasswordEncoder().encode(SENHA);
    @DynamicPropertySource static void roots(DynamicPropertyRegistry r) {
        r.add("app.portfolio.storage-root", () -> ROOT.resolve("portfolio").toString());
        r.add("app.chat.storage-root", () -> ROOT.resolve("chat").toString());
    }
    @LocalServerPort int port;
    @Autowired MockMvc mvc;
    @MockitoSpyBean JdbcTemplate jdbc;
    @MockitoSpyBean PortfolioStorageService portfolio;
    @MockitoSpyBean ChatAnexoStorage chatStorage;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired RefreshTokenService refresh;
    @Autowired ChatService chat;
    @Autowired com.portifolio.config.StompJwtChannelInterceptor interceptor;
    @Autowired org.springframework.core.env.Environment environment;
    final ObjectMapper json = new ObjectMapper();
    Usuario artista, contratante;

    @BeforeEach void preparar() {
        reset(jdbc, portfolio, chatStorage);
        limparBanco();
        artista = usuario("ArtistaRF22", TipoUsuario.ARTISTA);
        contratante = usuario("ContratanteRF22", TipoUsuario.CONTRATANTE);
    }
    @AfterEach void limpar() throws Exception {
        reset(jdbc, portfolio, chatStorage);
        limparBanco();
        if (Files.exists(ROOT)) try (var paths=Files.walk(ROOT)) {
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
    void limparBanco() {
        // DML apenas no banco descartável. Conserva o ID 0 criado pelo init/seed oficial.
        jdbc.execute("TRUNCATE salas_chat,vagas,denuncias_plagio,reportes_usuario,moderacao_conteudo,comunidades,editais,galerias_virtuais,portfolio_arquivos,embeds_externos,agenda_artista,conquistas_desbloqueadas,historico_medalhas,ranking_top_da_semana,log_exclusoes_lgpd,refresh_tokens,responsaveis_legais,itens_salvos,visualizacoes_perfil,banco_talentos,autodeclaracoes,perfil_artista_area CASCADE");
        jdbc.update("delete from usuarios where id<>0");
    }

    @ParameterizedTest @ValueSource(strings={"", "Bearer invalido"})
    void titularidadeExigeJwt(String token) throws Exception {
        var antes=snapshot();
        mvc.perform(delete("/api/usuarios/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(body(SENHA))).andExpect(status().isUnauthorized());
        assertThat(snapshot()).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"", "incorreta"})
    void confirmacaoInvalidaNaoAlteraNada(String senha) throws Exception {
        refresh.gerarRefreshToken(artista);
        var antes=snapshot();
        var result=mvc.perform(excluir(artista, senha));
        result.andExpect(senha.isEmpty()?status().isBadRequest():status().isForbidden());
        assertThat(snapshot()).isEqualTo(antes);
    }
    @Test void corpoAusenteNaoExclui() throws Exception {
        var antes=snapshot();
        mvc.perform(delete("/api/usuarios/me").header("Authorization",bearer(artista))).andExpect(status().isBadRequest());
        assertThat(snapshot()).isEqualTo(antes);
    }
    @Test void idsExtrasNaoMudamTitularEComprovanteNaoGuardaMotivoPii() throws Exception {
        var response=node(mvc.perform(excluir(artista,SENHA).content(json.writeValueAsString(Map.of(
                "senhaAtual",SENHA,"usuarioId",contratante.getId(),"id",contratante.getId(),"confirmado",true,
                "motivo","email privado fixture@test.invalid CPF 00000000000"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(usuarios.findById(artista.getId())).isEmpty();
        assertThat(usuarios.findById(contratante.getId())).isPresent();
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.path("comprovanteHash").asText()).matches("[0-9a-f]{64}");
        assertThat(response.path("dataExclusao").asText()).isNotBlank();
        assertThat(response.path("status").asText()).isEqualTo("CONCLUIDA");
        var log=jdbc.queryForMap("select motivo_opcional,comprovante_hash,data_exclusao from log_exclusoes_lgpd");
        assertThat(log.get("motivo_opcional")).isNull();
        assertThat(log.get("comprovante_hash")).isEqualTo(response.path("comprovanteHash").asText());
        assertThat(log.get("data_exclusao").toString().replace(' ','T')).startsWith(response.path("dataExclusao").asText().substring(0,19));
        assertThat(response.toString()).doesNotContain(artista.getEmail(),artista.getUsername(),SENHA,"CPF","motivo");
        semOrfaos();
    }
    @ParameterizedTest @EnumSource(value=TipoUsuario.class,names={"ADMIN","MODERADOR"})
    void administrativoSoExcluiPropriaConta(TipoUsuario tipo) throws Exception {
        Usuario admin=usuario("AdminRF22",tipo);
        mvc.perform(delete("/api/usuarios/"+artista.getId()).header("Authorization",bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(body(SENHA))).andExpect(status().isForbidden());
        mvc.perform(excluir(admin,SENHA).content(json.writeValueAsString(Map.of("senhaAtual",SENHA,"usuarioId",artista.getId()))))
                .andExpect(status().isOk());
        assertThat(usuarios.findById(artista.getId())).isPresent();
        assertThat(usuarios.findById(admin.getId())).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"", "Legada1!"})
    void googleOnlyFalhaFechadoSemReaproveitarOutroToken(String senha) throws Exception {
        jdbc.update("update usuarios set senha=null,google_id='rf22-google-fixture',token_recuperacao='outra-finalidade' where id=?",artista.getId());
        var antes=snapshot();
        mvc.perform(excluir(artista,senha).content(json.writeValueAsString(Map.of("senhaAtual",senha,"codigoConfirmacao","outra-finalidade","confirmado",true))))
                .andExpect(status().isUnprocessableEntity());
        assertThat(snapshot()).isEqualTo(antes);
    }
    @Test void googleComSenhaLocalPodeConfirmar() throws Exception {
        jdbc.update("update usuarios set google_id='rf22-google-local' where id=?",artista.getId());
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
    }
    @Test void sessoesRevogadasEWsRealNaoReconecta() throws Exception {
        String token=bearer(artista), raw=refresh.gerarRefreshToken(artista);
        refresh.gerarRefreshToken(artista);
        refresh.gerarRefreshToken(contratante);
        var connect=StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setNativeHeader("Authorization",token); connect.setSessionAttributes(new HashMap<>());connect.setLeaveMutable(true);
        var connected=interceptor.preSend(MessageBuilder.createMessage(new byte[0],connect.getMessageHeaders()),null);
        assertThat(connected).isNotNull();
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        assertThat(count("refresh_tokens","usuario_id",artista.getId())).isZero();
        assertThat(count("refresh_tokens","usuario_id",contratante.getId())).isOne();
        mvc.perform(get("/api/usuarios/me").header("Authorization",token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("palco_refresh",raw))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",artista.getEmail(),"senha",SENHA)))).andExpect(status().isUnauthorized());
        for(StompCommand command:List.of(StompCommand.SUBSCRIBE,StompCommand.SEND)) {
            var old=StompHeaderAccessor.create(command);old.setUser(connect.getUser());old.setSessionAttributes(connect.getSessionAttributes());
            old.setDestination(command==StompCommand.SUBSCRIBE?"/user/queue/chat":"/app/chat/salas/1/mensagens");old.setLeaveMutable(true);
            assertThatThrownBy(() -> interceptor.preSend(MessageBuilder.createMessage(new byte[0],old.getMessageHeaders()),null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        var client=new WebSocketStompClient(new StandardWebSocketClient());
        try {
            var headers=new StompHeaders();headers.add("Authorization",token);
            assertThatThrownBy(() -> client.connectAsync("ws://localhost:"+port+"/ws",new WebSocketHttpHeaders(),headers,new StompSessionHandlerAdapter(){}).get(5,TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);
        } finally {client.stop();}
    }
    @Test void dadosPessoaisEliminadosIncluindoMenorETaxonomia() throws Exception {
        jdbc.update("update usuarios set data_nascimento=?,cpf='00000000001',foto_perfil_url='https://example.test/avatar' where id=?",LocalDate.now().minusYears(16),artista.getId());
        jdbc.update("insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,data_consentimento) values(?,'Responsável fictício','00000000002','responsavel@example.test',current_timestamp)",artista.getId());
        jdbc.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal,nivel_experiencia) values(?,4,true,'EXPERIENTE')",artista.getId());
        jdbc.update("insert into perfil_artista_funcao(perfil_artista_id,area_id,funcao_id) values(?,4,1)",artista.getId());
        jdbc.update("insert into perfil_artista_especializacao(perfil_artista_id,area_id,especializacao_id) values(?,4,1)",artista.getId());
        jdbc.update("insert into autodeclaracoes(usuario_id,categoria,exibicao_publica) values(?,'PCD',false)",artista.getId());
        jdbc.update("insert into visualizacoes_perfil(perfil_visitado_id) values(?)",artista.getId());
        jdbc.update("insert into embeds_externos(artista_id,plataforma,url_original,codigo_iframe,tipo_midia) values(?,'YOUTUBE','https://youtube.com/watch?v=rf22','fixture','VIDEO')",artista.getId());
        jdbc.update("insert into agenda_artista(artista_id,titulo_compromisso,tipo_compromisso,data_hora_inicio,data_hora_fim,localizacao_logistica) values(?,'Fixture','Teste',current_timestamp,current_timestamp+interval '1 hour','Privado')",artista.getId());
        jdbc.update("insert into conquistas_desbloqueadas(artista_id,nome_conquista,descricao_conquista) values(?,'Fixture','Fixture')",artista.getId());
        jdbc.update("insert into historico_medalhas(artista_id,nivel_novo) values(?,1)",artista.getId());
        jdbc.update("insert into ranking_top_da_semana(artista_id,score_semanal,data_inicio_ciclo,data_fim_ciclo,posicao_ranking) values(?,1,current_date,current_date,1)",artista.getId());
        mvc.perform(get("/api/perfis/publicos/ARTISTA/"+artista.getId())).andExpect(status().isOk());
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        for(String table:List.of("responsaveis_legais","autodeclaracoes","perfil_artista_area","perfil_artista_funcao","perfil_artista_especializacao","agenda_artista","conquistas_desbloqueadas","historico_medalhas","ranking_top_da_semana","embeds_externos","visualizacoes_perfil"))
            assertThat(jdbc.queryForObject("select count(*) from "+table,Long.class)).as(table).isZero();
        assertThat(usuarios.findById(artista.getId())).isEmpty();
        mvc.perform(get("/api/perfis/publicos/ARTISTA/"+artista.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfis/publicos").param("q","@"+artista.getUsername())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        semOrfaos();
    }
    @Test void relacoesDescartaveisECatalogosPreservados() throws Exception {
        var catalogo=jdbc.queryForList("select nome from areas_artisticas order by id",String.class);
        long arquivo=portfolioArquivo(artista), comunidade=comunidade(contratante);
        jdbc.update("insert into membros_comunidade(comunidade_id,usuario_id) values(?,?)",comunidade,artista.getId());
        jdbc.update("insert into banco_talentos(contratante_id,artista_id) values(?,?)",contratante.getId(),artista.getId());
        long galeria=jdbc.queryForObject("insert into galerias_virtuais(dono_id,titulo,descricao,categoria,tipo_galeria) values(?,'Fixture','Fixture','Fixture','INDIVIDUAL') returning id",Long.class,contratante.getId());
        jdbc.update("insert into itens_galeria(galeria_id,arquivo_id) values(?,?)",galeria,arquivo);
        jdbc.update("insert into interacoes_galeria(usuario_id,arquivo_id,comentario) values(?,?,'Fixture pessoal')",contratante.getId(),arquivo);
        for(var alvo:List.of(Map.entry("PERFIL_ARTISTA",artista.getId()),Map.entry("OBRA",arquivo)))
            jdbc.update("insert into itens_salvos(usuario_id,tipo_alvo,alvo_id) values(?,?::tipo_alvo_salvo_enum,?)",contratante.getId(),alvo.getKey(),alvo.getValue());
        jdbc.update("insert into notificacoes(usuario_destino_id,tipo_notificacao,mensagem_alerta,link_contexto) values(?,'BANCO_DE_TALENTOS','Nome antigo','/perfis/ARTISTA/'||?)",contratante.getId(),artista.getId());
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        for(String table:List.of("portfolio_arquivos","banco_talentos","membros_comunidade","itens_galeria","interacoes_galeria","itens_salvos","notificacoes"))
            assertThat(jdbc.queryForObject("select count(*) from "+table,Long.class)).as(table).isZero();
        assertThat(jdbc.queryForObject("select count(*) from galerias_virtuais",Long.class)).isOne();
        assertThat(jdbc.queryForList("select nome from areas_artisticas order by id",String.class)).isEqualTo(catalogo);
        semOrfaos();
    }
    @ParameterizedTest @EnumSource(StatusCandidatura.class)
    void candidaturasPreservadasSemIdentidadeApresentacaoOuLink(StatusCandidatura estado) throws Exception {
        long vaga=vaga(contratante,"ABERTA"), anterior=candidatura(vaga,artista,"RETIRADA");
        long candidatura=candidatura(vaga,artista,estado.name());
        var data=jdbc.queryForObject("select data_candidatura from candidaturas where id=?",Object.class,candidatura);
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        var c=jdbc.queryForMap("select artista_id,mensagem_apresentacao,link_portfolio_candidatura,status::text as status,data_candidatura from candidaturas where id=?",candidatura);
        assertThat(c).containsEntry("artista_id",0L).containsEntry("status",estado.name()).containsEntry("data_candidatura",data);
        assertThat(c.get("mensagem_apresentacao")).isNull();assertThat(c.get("link_portfolio_candidatura")).isNull();
        assertThat(jdbc.queryForObject("select count(*) from candidaturas where id in(?,?)",Long.class,candidatura,anterior)).isEqualTo(2);
        mvc.perform(get("/api/vagas/"+vaga+"/candidaturas").header("Authorization",bearer(contratante)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertThat(count("vagas","id",vaga)).isOne();semOrfaos();
    }
    @ParameterizedTest @ValueSource(strings={"ABERTA","PAUSADA","ENCERRADA","CANCELADA"})
    void vagasPublicadasPreservamTerceirosEPerdemPii(String estado) throws Exception {
        long v=vaga(contratante,estado), c=candidatura(v,artista,"PENDENTE");
        jdbc.update("insert into log_vagas_canceladas(vaga_id,cancelado_por_id,motivo) values(?,?,'PII em motivo fixture')",v,contratante.getId());
        jdbc.update("insert into fotos_vaga(vaga_id,ordem,url) values(?,1,'https://example.test/foto')",v);
        jdbc.update("insert into notificacoes(usuario_destino_id,tipo_notificacao,mensagem_alerta,link_contexto) values(?,'CONVITE','Nome antigo','/vagas/'||?)",artista.getId(),v);
        mvc.perform(excluir(contratante,SENHA)).andExpect(status().isOk());
        var row=jdbc.queryForMap("select contratante_id,status::text as status,titulo,endereco_completo from vagas where id=?",v);
        assertThat(row).containsEntry("contratante_id",0L).containsEntry("status",estado.equals("CANCELADA")?"CANCELADA":"ENCERRADA");
        assertThat(row.get("endereco_completo")).isNull();
        assertThat(count("candidaturas","id",c)).isOne();
        assertThat(jdbc.queryForMap("select cancelado_por_id,motivo from log_vagas_canceladas")).containsEntry("cancelado_por_id",0L).containsEntry("motivo",null);
        assertThat(jdbc.queryForObject("select count(*) from fotos_vaga",Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notificacoes",Long.class)).isZero();
        mvc.perform(get("/api/vagas/"+v).header("Authorization",bearer(artista))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(contratante.getEmail()))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Endereço privado"))));
        semOrfaos();
    }
    @Test void rascunhoSemHistoricoRemovido() throws Exception {
        long v=vaga(contratante,"RASCUNHO");
        mvc.perform(excluir(contratante,SENHA)).andExpect(status().isOk());assertThat(count("vagas","id",v)).isZero();
    }
    @Test void rascunhoComHistoricoRecusadoSemMutacao() throws Exception {
        long v=vaga(contratante,"RASCUNHO");candidatura(v,artista,"RETIRADA");var antes=snapshot();
        mvc.perform(excluir(contratante,SENHA)).andExpect(status().isUnprocessableEntity());assertThat(snapshot()).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"sem_estado","log","reporte","moderacao"})
    void vagaLegadaOuRascunhoComEvidenciaNaoSofreCascade(String caso) throws Exception {
        long v=vaga(contratante,caso.equals("sem_estado")?"ABERTA":"RASCUNHO");
        if(caso.equals("sem_estado")) {candidatura(v,artista,"PENDENTE");jdbc.update("update vagas set status=null where id=?",v);}
        if(caso.equals("log")) jdbc.update("insert into log_vagas_canceladas(vaga_id,cancelado_por_id,motivo) values(?,?,'Registro necessário')",v,contratante.getId());
        if(caso.equals("reporte")) jdbc.update("insert into reportes_usuario(denunciante_id,tipo_conteudo,conteudo_id,motivo_reporte) values(?,'VAGA',?,'Registro necessário')",artista.getId(),v);
        if(caso.equals("moderacao")) jdbc.update("insert into moderacao_conteudo(tipo_conteudo,conteudo_id,autor_id) values('VAGA',?,?)",v,contratante.getId());
        var antes=snapshot();mvc.perform(excluir(contratante,SENHA)).andExpect(status().isUnprocessableEntity());assertThat(snapshot()).isEqualTo(antes);
    }
    @Test void denunciasPreservadasNosDoisPapeisEModeradorDesvinculado() throws Exception {
        long v=vaga(contratante,"ABERTA");
        jdbc.update("insert into denuncias_plagio(denunciante_id,perfil_denunciado_id,tipo_violacao,descricao_detalhada) values(?,?,'OUTRO','Evidência fixture'),(?,?,'OUTRO','Evidência de terceiro')",artista.getId(),contratante.getId(),contratante.getId(),artista.getId());
        jdbc.update("insert into reportes_usuario(denunciante_id,tipo_conteudo,conteudo_id,motivo_reporte) values(?,'VAGA',?,'Fixture')",artista.getId(),v);
        jdbc.update("insert into moderacao_conteudo(tipo_conteudo,conteudo_id,autor_id,moderador_id) values('VAGA',?,?,?)",v,artista.getId(),artista.getId());
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from denuncias_plagio",Long.class)).isEqualTo(2);
        assertThat(count("denuncias_plagio","denunciante_id",0)).isOne();assertThat(count("denuncias_plagio","perfil_denunciado_id",0)).isOne();
        assertThat(count("reportes_usuario","denunciante_id",0)).isOne();
        assertThat(jdbc.queryForMap("select autor_id,moderador_id from moderacao_conteudo")).containsEntry("autor_id",0L).containsEntry("moderador_id",null);
        mvc.perform(get("/api/denuncias").header("Authorization",bearer(contratante))).andExpect(status().isOk());semOrfaos();
    }
    @Test void chatRealAnonimoMantemHistoricoERemoveAnexoPessoal() throws Exception {
        long sala=chat.criarOuReutilizarSala(artista.getEmail(),contratante.getId()).getSalaId();
        long texto=chat.enviarMensagem(artista.getEmail(),sala,"Histórico profissional necessário").getId();
        long anexo=upload(sala), msgOutro=chat.enviarMensagem(contratante.getEmail(),sala,"Resposta do terceiro").getId();
        String ref=jdbc.queryForObject("select url_anexo from mensagens_chat where id=?",String.class,anexo);
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        var history=node(mvc.perform(get("/api/chat/salas/"+sala+"/mensagens").header("Authorization",bearer(contratante)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(history.path("totalElements").asLong()).isEqualTo(3);
        for(JsonNode m:history.path("content")) if(m.path("id").asLong()!=msgOutro) {
            assertThat(m.path("remetenteNome").asText()).isEqualTo("Usuário Removido");
            assertThat(m.path("remetenteId").isNull()).isTrue();assertThat(m.path("remetenteAvatar").isNull()).isTrue();
            assertThat(m.path("urlAnexo").isNull()).isTrue();
        }
        assertThat(history.toString()).contains("Histórico profissional necessário").doesNotContain(artista.getEmail(),artista.getUsername());
        assertThat(count("participantes_chat","usuario_id",artista.getId())).isZero();
        assertThat(count("salas_chat","id",sala)).isOne();assertThat(count("mensagens_chat","id",texto)).isOne();
        assertThat(Files.exists(ROOT.resolve("chat").resolve(ref))).isFalse();
        mvc.perform(get("/api/chat/mensagens/"+anexo+"/anexo").header("Authorization",bearer(contratante))).andExpect(status().isNotFound());
        mvc.perform(post("/api/chat/salas/"+sala+"/mensagens").header("Authorization",bearer(contratante))
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Sem destinatário\"}")).andExpect(status().isUnprocessableEntity());
        semOrfaos();
    }
    @Test void anexoDenunciadoPermaneceRestritoNaExclusaoReal() throws Exception {
        long sala=chat.criarOuReutilizarSala(artista.getEmail(),contratante.getId()).getSalaId(), msg=upload(sala);
        String ref=jdbc.queryForObject("select url_anexo from mensagens_chat where id=?",String.class,msg);
        jdbc.update("insert into reportes_usuario(denunciante_id,tipo_conteudo,conteudo_id,motivo_reporte) values(?,'MENSAGEM',?,'Evidência prévia')",contratante.getId(),msg);
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        assertThat(Files.readAllBytes(ROOT.resolve("chat").resolve(ref))).isEqualTo(PortfolioFixtures.imagem("png"));
        assertThat(count("reportes_usuario","conteudo_id",msg)).isOne();
        assertThat(jdbc.queryForMap("select remetente_id,excluida,url_anexo from mensagens_chat where id=?",msg)).containsEntry("remetente_id",null).containsEntry("excluida",true).containsEntry("url_anexo",ref);
        mvc.perform(get("/api/chat/mensagens/"+msg+"/anexo").header("Authorization",bearer(contratante))).andExpect(status().isNotFound());
        mvc.perform(get("/api/chat/salas/"+sala+"/mensagens").header("Authorization",bearer(contratante)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].urlAnexo").isEmpty()).andExpect(jsonPath("$.content[0].texto").value("Mensagem excluída pelo autor"));
    }
    @Test void salaSemParticipanteOuEvidenciaRemovidaSemAfetarOutras() throws Exception {
        long sala=chat.criarOuReutilizarSala(artista.getEmail(),contratante.getId()).getSalaId();
        chat.enviarMensagem(artista.getEmail(),sala,"Fixture");
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk());
        mvc.perform(excluir(contratante,SENHA)).andExpect(status().isOk());
        assertThat(count("salas_chat","id",sala)).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"comunidade","edital","portfolio","galeria","interacao","avatar"})
    void dominioSemPoliticaOuStorageOuEvidenciaRecusado(String dominio) throws Exception {
        if(dominio.equals("comunidade")) comunidade(artista);
        if(dominio.equals("edital")) jdbc.update("insert into editais(publicador_id,titulo,descricao,url_arquivo_oficial,data_inicio_inscricao,data_fim_inscricao,data_resultado) values(?,'Fixture','Fixture','https://example.test/edital',current_date,current_date,current_date)",artista.getId());
        if(dominio.equals("portfolio")) {
            portfolioArquivo(artista);
            jdbc.update("insert into denuncias_plagio(denunciante_id,perfil_denunciado_id,tipo_violacao,descricao_detalhada) values(?,?,'OUTRO','Evidência necessária')",contratante.getId(),artista.getId());
        }
        if(dominio.equals("galeria")) {
            long g=jdbc.queryForObject("insert into galerias_virtuais(dono_id,titulo,descricao,categoria,tipo_galeria) values(?,'Fixture','Fixture','Fixture','INDIVIDUAL') returning id",Long.class,artista.getId());
            jdbc.update("insert into reportes_usuario(denunciante_id,tipo_conteudo,conteudo_id,motivo_reporte) values(?,'GALERIA',?,'Evidência necessária')",contratante.getId(),g);
        }
        if(dominio.equals("interacao")) {
            Usuario terceiro=usuario("TerceiroRF22",TipoUsuario.ARTISTA);long arquivo=portfolioArquivo(terceiro);
            long g=jdbc.queryForObject("insert into galerias_virtuais(dono_id,titulo,descricao,categoria,tipo_galeria) values(?,'Fixture','Fixture','Fixture','INDIVIDUAL') returning id",Long.class,contratante.getId());
            jdbc.update("insert into itens_galeria(galeria_id,arquivo_id) values(?,?)",g,arquivo);
            jdbc.update("insert into interacoes_galeria(usuario_id,arquivo_id,comentario) values(?,?,'Comentário sob análise')",artista.getId(),arquivo);
            jdbc.update("insert into moderacao_conteudo(tipo_conteudo,conteudo_id,autor_id) values('GALERIA',?,?)",g,contratante.getId());
        }
        if(dominio.equals("avatar")) jdbc.update("update usuarios set foto_perfil_url='../fora-da-raiz.png' where id=?",artista.getId());
        var antes=snapshot();mvc.perform(excluir(artista,SENHA)).andExpect(status().isUnprocessableEntity());assertThat(snapshot()).isEqualTo(antes);
    }
    @Test void semContaReservadaOficialNaoReparaEstrutura() throws Exception {
        jdbc.update("delete from perfis_contratantes where usuario_id=0");
        try {
            var antes=snapshot();mvc.perform(excluir(artista,SENHA)).andExpect(status().isUnprocessableEntity());assertThat(snapshot()).isEqualTo(antes);
        } finally {jdbc.update("insert into perfis_contratantes(usuario_id,nome_empresa,tipo_contratante) values(0,'Entidade Removida','PESSOA_FISICA')");}
    }
    @Test void contaReservadaNaoAutenticaNemPublicaPerfil() throws Exception {
        var u=usuarios.findById(0L).orElseThrow();
        mvc.perform(excluir(u,SENHA)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfis/publicos/ARTISTA/0")).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfis/publicos").param("q","@usuario.removido")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @ParameterizedTest @ValueSource(strings={"update candidaturas", "delete from portfolio_arquivos", "delete from usuarios"})
    void falhaEmTresEtapasReverteTodoBancoEConservaArquivo(String prefixo) throws Exception {
        long v=vaga(contratante,"ABERTA");candidatura(v,artista,"PENDENTE");portfolioArquivo(artista);refresh.gerarRefreshToken(artista);
        var antes=snapshot();
        doAnswer(i -> {if(((String)i.getArgument(0)).startsWith(prefixo)) throw new DataIntegrityViolationException("Falha controlada RF22");return i.callRealMethod();})
                .when(jdbc).update(anyString(),any(Object[].class));
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isConflict()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))));
        assertThat(snapshot()).isEqualTo(antes);
        mvc.perform(get("/api/perfis/publicos/ARTISTA/"+artista.getId())).andExpect(status().isOk());
        try(var files=Files.walk(ROOT.resolve("portfolio"))) {assertThat(files.filter(Files::isRegularFile).count()).isOne();}
        verify(portfolio,never()).limparUpload(anyLong(),anyString());
    }
    @Test void arquivosSomenteAposCommitEFalhaFisicaNaoMenteSobreRollback() throws Exception {
        portfolioArquivo(artista);
        doAnswer(i -> {assertThat(count("usuarios","id",artista.getId())).isZero();throw new com.portifolio.exception.PortfolioOperationException(new java.io.IOException("Falha controlada"));})
                .when(portfolio).limparUpload(eq(artista.getId()),anyString());
        mvc.perform(excluir(artista,SENHA)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA_COM_LIMPEZA_PENDENTE"));
        assertThat(count("usuarios","id",artista.getId())).isZero();assertThat(jdbc.queryForObject("select count(*) from log_exclusoes_lgpd",Long.class)).isOne();
        try(var files=Files.walk(ROOT.resolve("portfolio"))) {assertThat(files.filter(Files::isRegularFile).count()).isOne();}
    }
    @Test void duasSolicitacoesConcorrentesGeramUmComprovanteSem500() throws Exception {
        String token=bearer(artista);var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task=() -> {gate.await();return mvc.perform(delete("/api/usuarios/me").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(body(SENHA))).andReturn().getResponse().getStatus();};
            var a=pool.submit(task);var b=pool.submit(task);gate.countDown();var statuses=List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200,401);assertThat(count("usuarios","id",artista.getId())).isZero();
            assertThat(jdbc.queryForObject("select count(*) from log_exclusoes_lgpd",Long.class)).isOne();
            mvc.perform(delete("/api/usuarios/me").header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(body(SENHA))).andExpect(status().isUnauthorized());
        } finally {pool.shutdownNow();}
    }
    @Test void integridadeDatabase05ValidateEFksReais() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(jdbc.queryForObject("select current_database()",String.class)).isEqualTo("palco_test_manu05");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE'",Long.class)).isEqualTo(43);
        assertThat(jdbc.queryForObject("select count(*) from pg_constraint where contype='f' and connamespace='public'::regnamespace",Long.class)).isEqualTo(57);
        assertThat(jdbc.queryForObject("select is_nullable from information_schema.columns where table_name='candidaturas' and column_name='artista_id'",String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject("select pg_get_constraintdef(oid) from pg_constraint where conname='mensagens_chat_remetente_id_fkey'",String.class)).endsWith("ON DELETE SET NULL");
        semOrfaos();
    }

    Usuario usuario(String nome,TipoUsuario tipo) {
        var u=new Usuario();u.setNome(nome);u.setUsername(nome);u.setEmail(nome+"@rf22.test");u.setSenha(HASH);u.setTelefone("00000000000");
        u.setDataNascimento(LocalDate.now().minusYears(25));u.setTipoUsuario(tipo);u.setStatusConta(StatusConta.ATIVA);u.setPerfilCompleto(true);u.setEmailVerificado(true);
        u=usuarios.saveAndFlush(u);
        if(tipo==TipoUsuario.ARTISTA) jdbc.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,disponivel_oportunidades) values(?,'ARTISTA_SOLO',true)",u.getId());
        if(tipo==TipoUsuario.CONTRATANTE) jdbc.update("insert into perfis_contratantes(usuario_id,nome_empresa,tipo_contratante) values(?,'Empresa fixture','PESSOA_FISICA')",u.getId());
        return u;
    }
    String bearer(Usuario u) {return "Bearer "+jwt.gerarToken(u);}
    String body(String senha) throws Exception {return json.writeValueAsString(Map.of("senhaAtual",senha));}
    MockHttpServletRequestBuilder excluir(Usuario u,String senha) throws Exception {return delete("/api/usuarios/me").header("Authorization",bearer(u)).contentType(MediaType.APPLICATION_JSON).content(body(senha));}
    JsonNode node(String body) throws Exception {return json.readTree(body);}
    long count(String table,String coluna,long id) {return jdbc.queryForObject("select count(*) from "+table+" where "+coluna+"=?",Long.class,id);}
    long vaga(Usuario dono,String estado) {return jdbc.queryForObject("""
            insert into vagas(contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,endereco_completo,tipo_contrato,abrangencia,status)
            values(?,4,'Contato pessoal fixture','Descrição pessoal fixture','Fixture','Cidade','SP','Endereço privado','Fixture','LOCAL',?::status_vaga_enum) returning id
            """,Long.class,dono.getId(),estado);}
    long candidatura(long vaga,Usuario a,String estado) {return jdbc.queryForObject("insert into candidaturas(vaga_id,artista_id,mensagem_apresentacao,link_portfolio_candidatura,status) values(?,?,'PII apresentação','https://example.test/portfolio',?::status_candidatura_enum) returning id",Long.class,vaga,a.getId(),estado);}
    long comunidade(Usuario dono) {return jdbc.queryForObject("insert into comunidades(criador_id,nome,descricao,categoria_artistica) values(?,'Comunidade fixture','Fixture','Fixture') returning id",Long.class,dono.getId());}
    long portfolioArquivo(Usuario a) {
        String ref=portfolio.novaReferencia(a.getId(),"png");byte[] bytes=PortfolioFixtures.imagem("png");portfolio.gravar(a.getId(),ref,bytes);
        return jdbc.queryForObject("insert into portfolio_arquivos(artista_id,url_arquivo,nome_original,tamanho_bytes,tipo_mime) values(?,?,'Pessoal fixture.png',?,'image/png') returning id",Long.class,a.getId(),ref,bytes.length);
    }
    long upload(long sala) throws Exception {
        return node(mvc.perform(multipart("/api/chat/salas/"+sala+"/anexos").file(new MockMultipartFile("arquivo","fixture.png","image/png",PortfolioFixtures.imagem("png")))
                .header("Authorization",bearer(artista))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
    }
    Map<String,List<String>> snapshot() {
        var result=new TreeMap<String,List<String>>();
        for(String table:jdbc.queryForList("select tablename from pg_tables where schemaname='public' order by tablename",String.class)) {
            assertThat(table).matches("[a-z_]+");result.put(table,jdbc.queryForList("select to_jsonb(t)::text from "+table+" t order by 1",String.class));
        } return result;
    }
    void semOrfaos() {
        assertThat(jdbc.queryForObject("select count(*) from pg_constraint where contype='f' and not convalidated and connamespace='public'::regnamespace",Long.class)).isZero();
        var fks=jdbc.queryForList("""
                select c.conrelid::regclass::text as origem,c.confrelid::regclass::text as alvo,
                  string_agg('o.'||quote_ident(a.attname)||' = p.'||quote_ident(b.attname),' and ' order by k.ord) as ligacao,
                  string_agg('o.'||quote_ident(a.attname)||' is not null',' and ' order by k.ord) as nao_nulo
                from pg_constraint c join lateral unnest(c.conkey,c.confkey) with ordinality k(oa,pa,ord) on true
                  join pg_attribute a on a.attrelid=c.conrelid and a.attnum=k.oa
                  join pg_attribute b on b.attrelid=c.confrelid and b.attnum=k.pa
                where c.contype='f' and c.connamespace='public'::regnamespace group by c.oid
                """);
        assertThat(fks).hasSize(57);
        for(var fk:fks) assertThat(jdbc.queryForObject("select count(*) from "+fk.get("origem")+" o where "+fk.get("nao_nulo")+" and not exists(select 1 from "+fk.get("alvo")+" p where "+fk.get("ligacao")+")",Long.class)).as(fk.get("origem").toString()).isZero();
    }
}
