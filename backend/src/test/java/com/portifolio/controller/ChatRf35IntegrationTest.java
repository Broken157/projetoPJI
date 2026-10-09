package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.dto.ChatEventoResponse;
import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.*;
import com.portifolio.repository.*;
import com.portifolio.realtime.*;
import com.portifolio.security.JwtService;
import com.portifolio.service.ChatService;
import com.portifolio.validation.PortfolioFixtures;
import jakarta.persistence.EntityManagerFactory;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers @SpringBootTest @AutoConfigureMockMvc
class ChatRf35IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");
    static final Path STORAGE = Path.of("target-maven/database05/rf35-storage-" + UUID.randomUUID()).toAbsolutePath();
    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry) {
        registry.add("app.chat.storage-root", STORAGE::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired ChatService chat;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManagerFactory emf;
    @MockitoBean Clock clock;
    @MockitoSpyBean NotificacaoRepository notificacoes;
    @MockitoSpyBean NotificacaoRealtimeService gateway;
    @MockitoSpyBean ChatRealtimeService chatGateway;
    @MockitoSpyBean NotificacaoSseService sse;
    @MockitoSpyBean SimpMessagingTemplate messaging;
    final ObjectMapper mapper = new ObjectMapper();
    final ZoneId zone = ZoneId.of("America/Sao_Paulo");
    Instant agora;
    Usuario a, b, terceiro;
    long sala;

    @BeforeEach void preparar() throws Exception {
        jdbc.execute("TRUNCATE usuarios, salas_chat RESTART IDENTITY CASCADE");
        agora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        when(clock.getZone()).thenReturn(zone);
        when(clock.instant()).thenAnswer(i -> agora);
        a = usuario("autor", TipoUsuario.ARTISTA);
        b = usuario("destino", TipoUsuario.CONTRATANTE);
        terceiro = usuario("terceiro", TipoUsuario.ARTISTA);
        sala = chat.criarOuReutilizarSala(a.getEmail(), b.getId()).getSalaId();
        clearInvocations(gateway, chatGateway, sse, messaging);
    }
    @AfterEach void limpar() throws Exception {
        jdbc.execute("TRUNCATE usuarios, salas_chat RESTART IDENTITY CASCADE");
        if (Files.exists(STORAGE)) try (var paths = Files.walk(STORAGE)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    @ParameterizedTest @ValueSource(strings={"salas", "salas/1/mensagens", "mensagens/1/anexo", "nao-lidas/count"})
    void leiturasExigemJwtAtual(String caminho) throws Exception {
        mvc.perform(get("/api/chat/" + caminho)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/" + caminho).header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
    }
    @Test void mutacoesExigemJwt() throws Exception {
        for (var request : List.of(post("/api/chat/salas"), post(mensagens()), patch("/api/chat/mensagens/1"), delete("/api/chat/mensagens/1"), patch(lidas()), multipart(anexos()))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"fixture\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
    @Test void terceiroNaoAcessaSalaMensagemLeituraOuAnexo() throws Exception {
        long id = enviar(a, "Privada");
        mvc.perform(auth(get(mensagens()), terceiro)).andExpect(status().isNotFound());
        mvc.perform(auth(post(mensagens()), terceiro).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"intrusao\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(patch(lidas()), terceiro)).andExpect(status().isNotFound());
        mvc.perform(auth(patch(msg(id)), terceiro).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"alterada\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete(msg(id)), terceiro)).andExpect(status().isNotFound());
        long arquivo = upload(a, "png");
        mvc.perform(auth(get(download(arquivo)), terceiro)).andExpect(status().isNotFound());
        mvc.perform(auth(multipart(anexos()).file(file("png")), terceiro)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat", Long.class)).isEqualTo(2L);
    }
    @ParameterizedTest @EnumSource(value=TipoUsuario.class, names={"ADMIN","MODERADOR"})
    void papeisAdministrativosNaoSaoDuplaNemAtorDeChat(TipoUsuario papel) throws Exception {
        Usuario admin = usuario("admin", papel);
        mvc.perform(auth(post("/api/chat/salas"), a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioDestinoId\":" + admin.getId() + "}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(get("/api/chat/salas"), admin)).andExpect(status().isForbidden());
        jdbc.update("update usuarios set tipo_usuario=?::tipo_usuario_enum where id=?", papel.name(), b.getId());
        mvc.perform(auth(get(mensagens()), a)).andExpect(status().isUnprocessableEntity());
    }
    @Test void consultaNaoCriaSalaEReusoPreservaDupla() throws Exception {
        mvc.perform(auth(get("/api/chat/salas"), terceiro)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        assertThat(chat.criarOuReutilizarSala(b.getEmail(), a.getId()).getSalaId()).isEqualTo(sala);
        assertThat(jdbc.queryForObject("select count(*) from salas_chat", Long.class)).isOne();
    }
    @Test void contaBloqueadaETokenAntigoNaoMantemAcesso() throws Exception {
        String token = bearer(a);
        jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=?", a.getId());
        mvc.perform(get(mensagens()).header("Authorization", token)).andExpect(status().isUnauthorized());
        assertThatThrownBy(() -> chat.enviarMensagem(a.getEmail(), sala, "Nao permitida"))
                .isInstanceOf(com.portifolio.exception.ForbiddenException.class);
    }
    @Test void contaDestinoBloqueadaNaoRecebeNovasMensagens() throws Exception {
        jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=?", b.getId());
        mvc.perform(auth(post(mensagens()), a).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"teste\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test void autorJwtPrevaleceEContratoNaoExibeDadosPrivados() throws Exception {
        var node = json(mvc.perform(auth(post(mensagens()), a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\" Texto válido \",\"remetenteId\":" + b.getId() + ",\"salaId\":999}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(node.path("remetenteId").asLong()).isEqualTo(a.getId());
        assertThat(node.path("texto").asText()).isEqualTo("Texto válido");
        assertThat(node.toString()).doesNotContain(a.getEmail(), a.getUsername(), "senha", "responsavel", "textoOriginal");
        assertThat(jdbc.queryForMap("select usuario_destino_id,tipo_notificacao::text as tipo,link_contexto from notificacoes"))
                .containsEntry("usuario_destino_id", b.getId()).containsEntry("tipo", "MENSAGEM").containsEntry("link_contexto", "/mensagens?sala=" + sala);
    }
    @ParameterizedTest @ValueSource(strings={"", "   ", "Mensagem excluída pelo autor"})
    void textoInvalidoNaoPersiste(String texto) throws Exception {
        mvc.perform(auth(post(mensagens()), a).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("texto", texto))))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }
    @Test void textoMaiorQueLimiteNaoPersiste() throws Exception {
        mvc.perform(auth(post(mensagens()), a).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("texto", "a".repeat(4001)))))
                .andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(ints={0,899,900,901})
    void bordaTemporalDeterministicaPersistida(int segundos) throws Exception {
        long id = enviar(a, "Original");
        Object data = jdbc.queryForObject("select data_envio from mensagens_chat where id=?", Object.class, id);
        agora = agora.plusSeconds(segundos);
        var result = mvc.perform(auth(patch(msg(id)), a).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Editada\"}"));
        if (segundos > 900) {
            result.andExpect(status().isUnprocessableEntity());
            assertThat(jdbc.queryForObject("select editada from mensagens_chat where id=?", Boolean.class, id)).isFalse();
        } else {
            result.andExpect(status().isOk()).andExpect(jsonPath("$.editada").value(true));
            assertThat(jdbc.queryForObject("select data_edicao from mensagens_chat where id=?", java.sql.Timestamp.class, id).toLocalDateTime())
                    .isEqualTo(LocalDateTime.now(clock));
            mvc.perform(auth(get(mensagens()), b)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].editada").value(true));
            verify(chatGateway).entregar(eq(b.getId()), eq(b.getEmail()), argThat(e -> e.getTipo()==ChatEventoTipo.EDICAO && e.getMensagem().getEditada()));
        }
        assertThat(jdbc.queryForObject("select data_envio from mensagens_chat where id=?", Object.class, id)).isEqualTo(data);
        assertThat(jdbc.queryForObject("select remetente_id from mensagens_chat where id=?", Long.class, id)).isEqualTo(a.getId());
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
    }
    @ParameterizedTest @ValueSource(strings={"destino","terceiro"})
    void somenteAutorEditaEExclui(String ator) throws Exception {
        long id = enviar(a, "Original"); Usuario u = ator.equals("destino") ? b : terceiro;
        mvc.perform(auth(patch(msg(id)), u).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Editada\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete(msg(id)), u)).andExpect(status().isNotFound());
    }
    @Test void exclusaoSemPrazoMantemHistoricoSalaEDataENaoNotificaNovamente() throws Exception {
        long id = enviar(a, "Original"); agora = agora.plus(Duration.ofDays(3));
        mvc.perform(auth(delete(msg(id)), a)).andExpect(status().isNoContent());
        Object data = jdbc.queryForObject("select data_exclusao from mensagens_chat where id=?", Object.class, id);
        agora = agora.plusSeconds(60);
        mvc.perform(auth(delete(msg(id)), a)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select data_exclusao from mensagens_chat where id=?", Object.class, id)).isEqualTo(data);
        mvc.perform(auth(get(mensagens()), b)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].texto").value(ChatService.MENSAGEM_EXCLUIDA))
                .andExpect(jsonPath("$.content[0].excluida").value(true));
        mvc.perform(auth(get("/api/chat/salas"), b)).andExpect(jsonPath("$.content[0].ultimaMensagem").value(ChatService.MENSAGEM_EXCLUIDA));
        mvc.perform(auth(patch(msg(id)), a).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Outra\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("select count(*) from salas_chat", Long.class)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat", Long.class)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
    }
    @Test void denunciaPreviaPreservaEvidenciaPrivadaNaEdicaoENaExclusao() throws Exception {
        long id = upload(a, "pdf");
        jdbc.update("update mensagens_chat set texto_mensagem='Evidencia anterior' where id=?", id);
        jdbc.update("insert into reportes_usuario(denunciante_id,tipo_conteudo,conteudo_id,motivo_reporte) values (?,'MENSAGEM',?,'Evidencia fixture')", b.getId(), id);
        mvc.perform(auth(patch(msg(id)), a).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Modificada\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select texto_original from mensagens_chat where id=?", String.class, id)).isEqualTo("Evidencia anterior");
        String ref = referencia(id);
        mvc.perform(auth(delete(msg(id)), a)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select texto_mensagem from mensagens_chat where id=?", String.class, id)).isEqualTo("Modificada");
        assertThat(referencia(id)).isEqualTo(ref); assertThat(Files.exists(STORAGE.resolve(ref))).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from reportes_usuario", Long.class)).isOne();
        String historico = mvc.perform(auth(get(mensagens()), b)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(historico).contains(ChatService.MENSAGEM_EXCLUIDA).doesNotContain("Modificada", "Evidencia anterior", "textoOriginal", ref);
        mvc.perform(auth(get(download(id)), b)).andExpect(status().isNotFound());
    }
    @Test void leituraRecebidaEmLotesDe50NaoFalsificaAutorENaoDuplicaAlertas() throws Exception {
        long propria = enviar(b, "Minha");
        for (int i=0; i<53; i++) inserir(a.getId(), "Recebida " + i, LocalDateTime.now(clock));
        mvc.perform(auth(patch(lidas()), a)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat where remetente_id=? and lida=true", Long.class, a.getId())).isZero();
        mvc.perform(auth(patch(lidas()), b)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat where remetente_id=? and lida=true", Long.class, a.getId())).isEqualTo(53L);
        assertThat(jdbc.queryForObject("select lida from mensagens_chat where id=?", Boolean.class, propria)).isTrue();
        assertThat(jdbc.queryForObject("select lida from mensagens_chat where id=2", Boolean.class)).isTrue();
        mvc.perform(auth(patch(lidas()), b)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/chat/nao-lidas/count"), b)).andExpect(jsonPath("$.count").value(0));
        mvc.perform(auth(get(mensagens()), a)).andExpect(jsonPath("$.content[0].lida").value(true));
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
    }
    @Test void historicoTemOrdemEstavelLimitesEConsultasConstantes() throws Exception {
        for (int i=0;i<53;i++) inserir(a.getId(), "Empate " + i, LocalDateTime.now(clock));
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.setStatisticsEnabled(true); stats.clear();
        var page = json(mvc.perform(auth(get(mensagens()).param("size","50"), b)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(page.path("content").size()).isEqualTo(50);
        assertThat(page.path("content").get(0).path("id").asLong()).isEqualTo(53);
        assertThat(page.path("content").get(49).path("id").asLong()).isEqualTo(4);
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(9);
        stats.setStatisticsEnabled(false);
        mvc.perform(auth(get(mensagens()), b)).andExpect(jsonPath("$.size").value(20));
        mvc.perform(auth(get(mensagens()).param("size","50").param("page","1"), b)).andExpect(jsonPath("$.content.length()").value(3));
        mvc.perform(auth(get(mensagens()).param("size","51"), b)).andExpect(status().isBadRequest());
        mvc.perform(auth(get(mensagens()).param("page","-1"), b)).andExpect(status().isBadRequest());
    }
    @Test void mensagemENotificacaoCommitamJuntasERealtimeSoAposCommit() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            chat.enviarMensagem(a.getEmail(), sala, "Atomica");
            assertThat(jdbc.queryForObject("select count(*) from mensagens_chat", Long.class)).isOne();
            assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
            verifyNoInteractions(gateway, chatGateway);
        });
        verify(gateway).entregar(eq(b.getId()), eq(b.getEmail()), any(NotificacaoResponse.class));
        verify(chatGateway).entregar(eq(b.getId()), eq(b.getEmail()), any(ChatEventoResponse.class));
    }
    @Test void rollbackRetiraMensagemAlertaEArquivoSemRealtime() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            chat.enviarAnexo(a.getEmail(), sala, "Rollback", file("png"));
            assertThat(arquivos()).isOne(); tx.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
        assertThat(arquivos()).isZero(); verifyNoInteractions(gateway, chatGateway);
    }
    @Test void falhaPersistenciaNotificacaoDesfazMensagemEArquivo() throws Exception {
        doThrow(new DataIntegrityViolationException("falha fixture")).when(notificacoes).saveAllAndFlush(any());
        mvc.perform(auth(multipart(anexos()).file(file("png")), a)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat", Long.class)).isZero();
        assertThat(arquivos()).isZero(); verifyNoInteractions(gateway, chatGateway);
    }
    @ParameterizedTest @ValueSource(strings={"WS","SSE","CHAT"})
    void falhaTransporteNaoDesfazPersistencia(String canal) throws Exception {
        if (canal.equals("WS")) doThrow(new IllegalStateException("offline fixture")).when(messaging)
                .convertAndSendToUser(anyString(), eq("/queue/notificacoes"), any(Object.class));
        if (canal.equals("SSE")) doThrow(new IllegalStateException("offline fixture")).when(sse).entregar(anyLong(), any());
        if (canal.equals("CHAT")) doThrow(new IllegalStateException("offline fixture")).when(chatGateway).entregar(anyLong(), anyString(), any());
        enviar(a, "Persistencia resiste");
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat", Long.class)).isOne();
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
        verify(sse).entregar(eq(b.getId()), any());
        verify(messaging).convertAndSendToUser(eq(b.getEmail()), eq("/queue/notificacoes"), any(Object.class));
    }
    @ParameterizedTest @ValueSource(strings={"jpg","jpeg","png","pdf","mp3"})
    void anexoValidoPrivadoPreservaBytesESomenteReferenciaOpaca(String ext) throws Exception {
        long id = upload(a, ext);
        String ref = referencia(id);
        assertThat(ref).startsWith(sala + "/").doesNotContain("obra", "http", "..");
        for (Usuario participante : List.of(a,b)) {
            var response = mvc.perform(auth(get(download(id)), participante)).andExpect(status().isOk())
                    .andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(header().string("Content-Security-Policy","sandbox")).andReturn().getResponse();
            assertThat(response.getContentAsByteArray()).isEqualTo(bytes(ext));
            assertThat(response.getHeader("Content-Disposition")).contains("attachment", "anexo." + ext).doesNotContain("obra");
        }
        String history = mvc.perform(auth(get(mensagens()), b)).andReturn().getResponse().getContentAsString();
        assertThat(history).contains(download(id)).doesNotContain(ref, STORAGE.toString());
    }
    static Stream<Arguments> arquivosInvalidos() {
        return Stream.of(Arguments.of("obra.png","text/html",PortfolioFixtures.imagem("png")),
                Arguments.of("obra.exe","image/png",PortfolioFixtures.imagem("png")),
                Arguments.of("obra.png","image/png",new byte[5*1024*1024+1]),
                Arguments.of("obra.pdf","application/pdf",new byte[10*1024*1024+1]),
                Arguments.of("obra.mp3","audio/mpeg",new byte[20*1024*1024+1]),
                Arguments.of("obra.png","image/png",new byte[]{'M','Z',1,2}),
                Arguments.of("../obra.png","image/png",PortfolioFixtures.imagem("png")),
                Arguments.of("https://example.test/obra.pdf","application/pdf",PortfolioFixtures.pdf()),
                Arguments.of("obra.svg","image/svg+xml","<svg/>".getBytes()));
    }
    @ParameterizedTest @MethodSource("arquivosInvalidos")
    void anexoInvalidoNaoProduzArquivoNemMensagem(String nome, String mime, byte[] bytes) throws Exception {
        mvc.perform(auth(multipart(anexos()).file(new MockMultipartFile("arquivo",nome,mime,bytes)), a))
                .andExpect(status().isUnprocessableEntity());
        assertThat(arquivos()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat", Long.class)).isZero();
    }
    @Test void anexoAusenteRejeitadoETextoOpcionalComArquivoValido() throws Exception {
        mvc.perform(auth(multipart(anexos()), a)).andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(multipart(anexos()).file(file("pdf")).param("texto","Documento da proposta"), a))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.texto").value("Documento da proposta"));
    }
    @ParameterizedTest @ValueSource(strings={"../../fora.pdf", "https://example.test/a.pdf", "2/00000000-0000-0000-0000-000000000000.pdf", "1/00000000-0000-0000-0000-000000000000.exe"})
    void referenciaPersistidaManipuladaNuncaViraCaminhoLocal(String ref) throws Exception {
        long id = enviar(a,"Referencia"); jdbc.update("update mensagens_chat set url_anexo=? where id=?", ref,id);
        mvc.perform(auth(get(download(id)), b)).andExpect(status().isUnprocessableEntity());
        String history = mvc.perform(auth(get(mensagens()), b)).andReturn().getResponse().getContentAsString();
        assertThat(history).doesNotContain(ref);
    }
    @Test void exclusaoDeAnexoRemoveArquivoSomenteNoCommit() throws Exception {
        long id = upload(a,"png"); String ref=referencia(id);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            chat.excluirMensagem(a.getEmail(),id); assertThat(Files.exists(STORAGE.resolve(ref))).isTrue();
        });
        assertThat(Files.exists(STORAGE.resolve(ref))).isFalse(); assertThat(referencia(id)).isNull();
        mvc.perform(auth(get(download(id)), b)).andExpect(status().isNotFound());
    }
    @Test void rollbackDeExclusaoMantemAnexoETexto() throws Exception {
        long id=upload(a,"pdf"); String ref=referencia(id);
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {chat.excluirMensagem(a.getEmail(),id);tx.setRollbackOnly();});
        assertThat(Files.exists(STORAGE.resolve(ref))).isTrue(); assertThat(referencia(id)).isEqualTo(ref);
        mvc.perform(auth(get(download(id)), b)).andExpect(status().isOk());
    }
    @Test void fksOficiaisPermitemHistoricoAnonimoSemLiberarApiRf22() throws Exception {
        long id=upload(a,"png"); String token=bearer(a); String username=a.getUsername();
        // Exclusão de fixture SOMENTE no PostgreSQL Testcontainers; não implementa orquestração RF22.
        jdbc.update("delete from usuarios where id=?",a.getId());
        assertThat(jdbc.queryForObject("select remetente_id from mensagens_chat where id=?",Long.class,id)).isNull();
        String history=mvc.perform(auth(get(mensagens()),b)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].remetenteNome").value("Usuário Removido"))
                .andExpect(jsonPath("$.content[0].remetenteId").isEmpty()).andExpect(jsonPath("$.content[0].remetenteAvatar").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(history).doesNotContain(a.getEmail(),username,a.getNome(),"senha","cpf");
        mvc.perform(auth(get("/api/chat/salas"),b)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].participanteNome").value("Usuário Removido"))
                .andExpect(jsonPath("$.content[0].participanteId").isEmpty()).andExpect(jsonPath("$.content[0].participanteAvatar").isEmpty());
        mvc.perform(auth(get(download(id)),b)).andExpect(status().isOk());
        mvc.perform(auth(patch(lidas()),b)).andExpect(status().isNoContent());
        mvc.perform(get(mensagens()).header("Authorization",token)).andExpect(status().isUnauthorized());
        mvc.perform(auth(post(mensagens()),b).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Sem destinatario\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(delete("/api/usuarios/"+b.getId()),b)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat",Long.class)).isOne();
    }
    @ParameterizedTest @ValueSource(strings={"historico","envio","leitura","edicao","exclusao","anexo","download","reuso"})
    void consentimentoRevogadoRevalidadoEmSalaExistente(String operacao) throws Exception {
        long id=upload(a,"png"); tornarMenor(a, true);
        // A sala preexistente não substitui consentimento nem interação profissional.
        var request=switch(operacao) {
            case "historico" -> get(mensagens()); case "envio" -> post(mensagens()).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"contato\"}");
            case "leitura" -> patch(lidas()); case "edicao" -> patch(msg(id)).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"edicao\"}");
            case "exclusao" -> delete(msg(id)); case "anexo" -> multipart(anexos()).file(file("png")); case "download" -> get(download(id));
            default -> post("/api/chat/salas").contentType(MediaType.APPLICATION_JSON).content("{\"usuarioDestinoId\":"+a.getId()+"}");
        };
        mvc.perform(auth(request,b)).andExpect(status().isUnprocessableEntity());
    }
    @Test void salaExistenteNaoDispensaInteracaoProfissionalDoMenor() throws Exception {
        tornarMenor(a,false);
        mvc.perform(auth(get(mensagens()),b)).andExpect(status().isUnprocessableEntity());
        mvc.perform(auth(post(mensagens()),a).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Contato\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest @CsvSource({"TODAS,1", "LIDAS,0", "NAO_LIDAS,1"})
    void filtroRecebidasNaoDependeDaUltimaMensagemPropria(String filtro, int quantidade) throws Exception {
        enviar(b, "Recebida pendente");
        enviar(a, "Resposta ainda sem ler");
        mvc.perform(auth(get("/api/chat/salas").param("leitura", filtro), a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(quantidade))
                .andExpect(jsonPath("$.content.length()").value(quantidade));
        mvc.perform(auth(patch(lidas()), a)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/chat/salas").param("leitura", "LIDAS"), a))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].naoLidas").value(0));
        mvc.perform(auth(get("/api/chat/salas").param("leitura", "NAO_LIDAS"), a))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest @ValueSource(strings={"TODAS", "LIDAS"})
    void salaVaziaOuSomenteMensagemPropriaEhLida(String filtro) throws Exception {
        mvc.perform(auth(get("/api/chat/salas").param("leitura", filtro), a))
                .andExpect(jsonPath("$.totalElements").value(1));
        enviar(a, "Enviada");
        mvc.perform(auth(get("/api/chat/salas").param("leitura", filtro), a))
                .andExpect(jsonPath("$.content[0].naoLidas").value(0));
        mvc.perform(auth(get("/api/chat/nao-lidas/count"), a)).andExpect(jsonPath("$.count").value(0));
    }

    @ParameterizedTest @ValueSource(strings={"destino", "DESTINO", "  DeStInO  "})
    void buscaNomeCaseInsensitiveMantemEscopoDoJwt(String nome) throws Exception {
        mvc.perform(auth(get("/api/chat/salas").param("nome", nome), a))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].salaId").value(sala));
        mvc.perform(auth(get("/api/chat/salas").param("nome", nome), terceiro))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest @ValueSource(strings={"%", "_", "!", "\\"})
    void buscaTrataWildcardsComoTextoLiteral(String simbolo) throws Exception {
        jdbc.update("update usuarios set nome=? where id=?", "Nome " + simbolo + " literal", b.getId());
        Usuario normal = usuario("normal", TipoUsuario.CONTRATANTE);
        chat.criarOuReutilizarSala(a.getEmail(), normal.getId());
        mvc.perform(auth(get("/api/chat/salas").param("nome", simbolo), a))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].salaId").value(sala));
    }

    @ParameterizedTest @ValueSource(strings={"destino@rf35.test", "rf35_destino", "11999999999"})
    void buscaNaoIncluiEmailUsernameOuTelefone(String privado) throws Exception {
        mvc.perform(auth(get("/api/chat/salas").param("nome", privado), a))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest @CsvSource({"size,51", "size,0", "page,-1", "leitura,OUTRA"})
    void listaRejeitaLimiteOuFiltroInvalido(String parametro, String valor) throws Exception {
        mvc.perform(auth(get("/api/chat/salas").param(parametro, valor), a)).andExpect(status().isBadRequest());
    }

    @Test void filtrosCombinadosPaginamComContagemEOrdemEstavelSemNMaisUm() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i=0; i<22; i++) {
            Usuario alvo = usuario("empresa"+i, TipoUsuario.CONTRATANTE);
            long id = chat.criarOuReutilizarSala(a.getEmail(), alvo.getId()).getSalaId();
            jdbc.update("insert into mensagens_chat(sala_id,remetente_id,texto_mensagem,lida,data_envio) values (?,?,?,false,?)",
                    id, alvo.getId(), "Pendente", LocalDateTime.now(clock));
            ids.add(id);
        }
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true); stats.clear();
        JsonNode primeira;
        try {
            primeira = json(mvc.perform(auth(get("/api/chat/salas").param("nome","EMPRESA").param("leitura","NAO_LIDAS"), a))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(22)).andReturn().getResponse().getContentAsString());
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(4);
        } finally { stats.setStatisticsEnabled(false); }
        List<Long> obtidos = new ArrayList<>();
        primeira.path("content").forEach(item -> obtidos.add(item.path("salaId").asLong()));
        var segunda = json(mvc.perform(auth(get("/api/chat/salas").param("nome","empresa").param("leitura","NAO_LIDAS").param("page","1"), a))
                .andExpect(jsonPath("$.content.length()").value(2)).andReturn().getResponse().getContentAsString());
        segunda.path("content").forEach(item -> obtidos.add(item.path("salaId").asLong()));
        assertThat(obtidos).containsExactlyElementsOf(ids.reversed()).doesNotHaveDuplicates();
        mvc.perform(auth(get("/api/chat/salas").param("size","50"), a)).andExpect(jsonPath("$.size").value(50));
    }

    @Test void excluidaNaoContaComoRecebidaPendenteNemAlteraLeituraGlobal() throws Exception {
        long id=enviar(b,"Excluir");
        mvc.perform(auth(delete(msg(id)), b)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/chat/nao-lidas/count"), a)).andExpect(jsonPath("$.count").value(0));
        mvc.perform(auth(get("/api/chat/salas").param("leitura","NAO_LIDAS"), a))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(auth(get("/api/chat/salas").param("leitura","LIDAS"), a))
                .andExpect(jsonPath("$.content[0].naoLidas").value(0));
        mvc.perform(auth(patch(lidas()), a)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select lida from mensagens_chat where id=?",Boolean.class,id)).isFalse();
    }

    @Test void salaComMaisDeDoisNaoEntraEmListaContadorOuAutorizacaoDireta() throws Exception {
        inserir(b.getId(),"Coletiva invalida",LocalDateTime.now(clock));
        jdbc.update("insert into participantes_chat(sala_id,usuario_id) values (?,?)",sala,terceiro.getId());
        mvc.perform(auth(get("/api/chat/salas"), a)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(auth(get("/api/chat/nao-lidas/count"), a)).andExpect(jsonPath("$.count").value(0));
        mvc.perform(auth(get(mensagens()), a)).andExpect(status().isUnprocessableEntity());
    }

    @Test void moderacaoExistenteTambemPreservaOriginalEAnexo() throws Exception {
        long id=upload(a,"pdf");
        jdbc.update("update mensagens_chat set texto_mensagem='Conteudo sob analise' where id=?",id);
        jdbc.update("insert into moderacao_conteudo(tipo_conteudo,conteudo_id,autor_id) values ('MENSAGEM',?,?)",id,a.getId());
        mvc.perform(auth(patch(msg(id)),a).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Editada\"}"))
                .andExpect(status().isOk());
        String ref=referencia(id);
        mvc.perform(auth(delete(msg(id)),a)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select texto_original from mensagens_chat where id=?",String.class,id))
                .isEqualTo("Conteudo sob analise");
        assertThat(referencia(id)).isEqualTo(ref);
        assertThat(Files.exists(STORAGE.resolve(ref))).isTrue();
        mvc.perform(auth(get(download(id)),b)).andExpect(status().isNotFound());
        mvc.perform(auth(get(mensagens()),b)).andExpect(jsonPath("$.content[0].texto").value(ChatService.MENSAGEM_EXCLUIDA));
    }

    @Test void videoNaoEhAnexoAceito() throws Exception {
        mvc.perform(auth(multipart(anexos()).file(new MockMultipartFile("arquivo","video.mp4","video/mp4",new byte[]{0,0,0,1})),a))
                .andExpect(status().isUnprocessableEntity());
        assertThat(arquivos()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat",Long.class)).isZero();
    }

    @Test void chamadasDistintasComMesmoTextoSaoMensagensENotificacoesDistintas() throws Exception {
        long primeira=enviar(a,"Texto igual"); long segunda=enviar(a,"Texto igual");
        assertThat(segunda).isNotEqualTo(primeira);
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat",Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from notificacoes where usuario_destino_id=? and tipo_notificacao='MENSAGEM'",Long.class,b.getId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from notificacoes where usuario_destino_id=?",Long.class,a.getId())).isZero();
    }

    @Test void jwtAntigoNaoEnviaNemAbreConversaAposPendenciaDeConsentimento() throws Exception {
        String antigo=bearer(a);
        tornarMenor(a,false);
        jdbc.update("update usuarios set status_conta='PENDENTE_CONSENTIMENTO' where id=?",a.getId());
        mvc.perform(post(mensagens()).header("Authorization",antigo).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Nao permitida\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/chat/salas").header("Authorization",antigo).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioDestinoId\":"+b.getId()+"}")).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from mensagens_chat",Long.class)).isZero();
    }

    @Test void leituraConcorrenteEhIdempotenteComRecibosLimitados() throws Exception {
        for(int i=0;i<73;i++) inserir(a.getId(),"Pendente "+i,LocalDateTime.now(clock));
        var barreira = new java.util.concurrent.CyclicBarrier(2);
        try(var executores = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var chamadas = java.util.stream.IntStream.range(0,2).mapToObj(i -> executores.submit(() -> {
                barreira.await(10,java.util.concurrent.TimeUnit.SECONDS);
                return chat.marcarRecebidasComoLidas(b.getEmail(),sala);
            })).toList();
            int total=0;
            for(var chamada:chamadas) total+=chamada.get(30,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(total).isEqualTo(73);
        }
        assertThat(chat.contarNaoLidas(b.getEmail()).count()).isZero();
        var eventos=org.mockito.ArgumentCaptor.forClass(ChatEventoResponse.class);
        verify(chatGateway,atLeastOnce()).entregar(eq(a.getId()),eq(a.getEmail()),eventos.capture());
        assertThat(eventos.getAllValues()).allSatisfy(e -> assertThat(e.getMensagemIds()).hasSizeLessThanOrEqualTo(50));
    }

    @Test void edicoesConcorrentesPreservamAutoriaEEstadoFinalConsistente() throws Exception {
        long id=enviar(a,"Inicial");
        var barreira=new java.util.concurrent.CyclicBarrier(2);
        try(var executores=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var chamadas=List.of("Primeira","Segunda").stream().map(texto -> executores.submit(() -> {
                barreira.await(10,java.util.concurrent.TimeUnit.SECONDS);
                return chat.editarMensagem(a.getEmail(),id,texto).getTexto();
            })).toList();
            for(var chamada:chamadas) assertThat(chamada.get(30,java.util.concurrent.TimeUnit.SECONDS)).isIn("Primeira","Segunda");
        }
        assertThat(jdbc.queryForObject("select texto_mensagem from mensagens_chat where id=?",String.class,id)).isIn("Primeira","Segunda");
        assertThat(jdbc.queryForObject("select remetente_id from mensagens_chat where id=?",Long.class,id)).isEqualTo(a.getId());
        assertThat(jdbc.queryForObject("select editada from mensagens_chat where id=?",Boolean.class,id)).isTrue();
    }

    private void tornarMenor(Usuario usuario, boolean revogado) {
        jdbc.update("update usuarios set data_nascimento=? where id=?",LocalDate.now(clock).minusYears(16),usuario.getId());
        jdbc.update("insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,data_consentimento,consentimento_revogado) values (?,'Responsavel fixture','11988887777','responsavel@rf35.test',?,?)",
                usuario.getId(),LocalDateTime.now(clock),revogado);
    }
    private Usuario usuario(String nome, TipoUsuario tipo) {
        Usuario u=com.portifolio.support.OfficialSchemaFixtures.usuario();
        u.setNome(nome+" RF35");u.setEmail(nome+"@rf35.test");u.setUsername("rf35_"+nome);
        u.setTelefone("11999999999");u.setDataCriacao(LocalDateTime.now(clock));
        u.setDataNascimento(LocalDate.of(1990,1,1));u.setTipoUsuario(tipo);u.setStatusConta(StatusConta.ATIVA);
        u.setPerfilCompleto(true);u.setEmailVerificado(true);u.setSenha("{noop}fixture");u.setFotoPerfil("https://example.test/"+nome+".png");
        return usuarios.saveAndFlush(u);
    }
    private long inserir(Long autor,String texto,LocalDateTime data) {
        return jdbc.queryForObject("insert into mensagens_chat(sala_id,remetente_id,texto_mensagem,lida,data_envio) values (?,?,?,false,?) returning id",Long.class,sala,autor,texto,data);
    }
    private long enviar(Usuario autor,String texto) throws Exception {
        return json(mvc.perform(auth(post(mensagens()),autor).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("texto",texto))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
    }
    private long upload(Usuario autor,String ext) throws Exception {
        return json(mvc.perform(auth(multipart(anexos()).file(file(ext)),autor)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();
    }
    private byte[] bytes(String ext) {return ext.equals("pdf")?PortfolioFixtures.pdf():ext.equals("mp3")?PortfolioFixtures.mp3():PortfolioFixtures.imagem(ext.equals("jpeg")?"jpg":ext);}
    private MockMultipartFile file(String ext) {return new MockMultipartFile("arquivo","obra."+ext,ext.equals("pdf")?"application/pdf":ext.equals("mp3")?"audio/mpeg":ext.equals("png")?"image/png":"image/jpeg",bytes(ext));}
    private String referencia(long id) {return jdbc.queryForObject("select url_anexo from mensagens_chat where id=?",String.class,id);}
    private long arquivos() {try {if(!Files.exists(STORAGE))return 0;try(var files=Files.walk(STORAGE)){return files.filter(Files::isRegularFile).count();}}catch(Exception e){throw new IllegalStateException(e);}}
    private JsonNode json(String body) throws Exception {return mapper.readTree(body);}
    private String bearer(Usuario usuario){return "Bearer "+jwt.gerarToken(usuario);}
    private AbstractMockHttpServletRequestBuilder<?> auth(AbstractMockHttpServletRequestBuilder<?> request,Usuario usuario){request.header("Authorization",bearer(usuario));return request;}
    private String mensagens(){return "/api/chat/salas/"+sala+"/mensagens";}
    private String lidas(){return "/api/chat/salas/"+sala+"/lidas";}
    private String anexos(){return "/api/chat/salas/"+sala+"/anexos";}
    private String msg(long id){return "/api/chat/mensagens/"+id;}
    private String download(long id){return msg(id)+"/anexo";}
}
