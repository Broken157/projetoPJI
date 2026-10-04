package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.realtime.NotificacaoRealtimeGateway;
import com.portifolio.realtime.NotificacaoSseService;
import com.portifolio.repository.NotificacaoRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.service.ConviteVagaService;
import com.portifolio.support.OfficialPostgreSQLContainer;
import com.portifolio.support.OfficialSchemaFixtures;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ConviteVagaRf42IntegrationTest {
    @Container @ServiceConnection static PostgreSQLContainer<?> postgres = new OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired Clock clock;
    @Autowired Environment ambiente;
    @Autowired ConviteVagaService service;
    @Autowired PlatformTransactionManager transacoes;
    @MockitoSpyBean NotificacaoRepository notificacoes;
    @MockitoSpyBean NotificacaoRealtimeGateway realtime;
    @MockitoSpyBean SimpMessagingTemplate websocket;
    @MockitoSpyBean NotificacaoSseService sse;
    private final ObjectMapper mapper = new ObjectMapper();
    private long artista, colega, dono, outroDono, vaga;
    private String token;

    @BeforeEach void dados() throws Exception {
        limpar();
        artista = artista("artista", 30); colega = artista("colega", 30);
        dono = contratante("dono"); outroDono = contratante("outro_dono");
        vaga = vaga(dono, "ABERTA", null);
        membro(dono, artista);
        db.update("delete from notificacoes");
        clearInvocations(notificacoes, realtime, websocket, sse);
        token = bearer(dono);
    }

    @AfterEach void limpar() {
        SecurityContextHolder.clearContext();
        db.execute("truncate salas_chat, usuarios, funcoes, especializacoes restart identity cascade");
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "POST"})
    void anonimoETokenInvalidoNaoExecutam(String metodo) throws Exception {
        mvc.perform(request(metodo, artista, vaga)).andExpect(status().isUnauthorized());
        mvc.perform(request(metodo, artista, vaga).header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
        semConvite();
    }

    @ParameterizedTest @CsvSource({"ARTISTA,GET", "ARTISTA,POST", "ADMIN,GET", "ADMIN,POST", "MODERADOR,GET", "MODERADOR,POST"})
    void papelNaoContratanteNaoExecuta(String papel, String metodo) throws Exception {
        long id = usuario("papel", papel, 30);
        mvc.perform(request(metodo, artista, vaga).header("Authorization", bearer(id))).andExpect(status().isForbidden());
        semConvite();
    }

    @ParameterizedTest @ValueSource(strings = {"PENDENTE_VERIFICACAO_EMAIL", "PENDENTE_TIPO_PERFIL", "PENDENTE_CONSENTIMENTO", "BLOQUEADA"})
    void estadoPersistidoDoDonoInvalidaJwtAntigo(String estado) throws Exception {
        db.update("update usuarios set status_conta=?::status_conta_enum where id=?", estado, dono);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isUnauthorized());
        mvc.perform(listar(artista)).andExpect(status().isUnauthorized());
        semConvite();
    }

    @Test void papelAtualPrevaleceEIdsExtrasNaoConcedemAutoridade() throws Exception {
        String adulterado = "{\"vagaId\":" + vaga + ",\"confirmado\":true,\"contratanteId\":" + outroDono
                + ",\"ownerId\":" + outroDono + ",\"usuarioId\":" + colega + ",\"artistaId\":" + colega
                + ",\"tipo\":\"CANDIDATURA\",\"status\":\"ACEITA\"}";
        mvc.perform(enviar(artista, vaga).content(adulterado).param("contratanteId", "" + outroDono)).andExpect(status().isCreated());
        assertThat(registro()).containsEntry("usuario_destino_id", artista).containsEntry("link_contexto", "/vagas/" + vaga);
        db.update("update usuarios set tipo_usuario='ARTISTA' where id=?", dono);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isForbidden());
        assertThat(convites()).hasSize(1);
    }

    @ParameterizedTest @ValueSource(strings = {"PUBLICO", "SALVO", "OUTRO_BANCO", "CANDIDATURA"})
    void outraRelacaoNaoSubstituiMembershipProprio(String relacao) throws Exception {
        if (relacao.equals("SALVO")) mvc.perform(post("/api/salvos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipoAlvo\":\"PERFIL_ARTISTA\",\"alvoId\":" + colega + "}")).andExpect(status().isCreated());
        if (relacao.equals("OUTRO_BANCO")) membro(outroDono, colega);
        if (relacao.equals("CANDIDATURA")) candidatura(colega, "PENDENTE");
        var antes = db.queryForList("select * from banco_talentos order by contratante_id,artista_id");
        mvc.perform(enviar(colega, vaga)).andExpect(status().isNotFound());
        mvc.perform(listar(colega)).andExpect(status().isNotFound());
        assertThat(db.queryForList("select * from banco_talentos order by contratante_id,artista_id")).isEqualTo(antes);
        semConvite();
    }

    @ParameterizedTest @ValueSource(strings = {"PENDENTE_VERIFICACAO_EMAIL", "PENDENTE_TIPO_PERFIL", "PENDENTE_CONSENTIMENTO", "BLOQUEADA"})
    void artistaInaptoNaoRecebeNemApareceNaSelecao(String estado) throws Exception {
        db.update("update usuarios set status_conta=?::status_conta_enum where id=?", estado, artista);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isNotFound());
        mvc.perform(listar(artista)).andExpect(status().isNotFound());
        semConvite();
    }

    @Test void artistaComPapelAlteradoOuPerfilAusenteNaoRecebe() throws Exception {
        db.update("update usuarios set tipo_usuario='ADMIN' where id=?", artista);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isNotFound());
        db.update("update usuarios set tipo_usuario='ARTISTA' where id=?", artista);
        db.update("delete from perfis_artistas where usuario_id=?", artista);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isNotFound());
        semConvite();
    }

    @ParameterizedTest @ValueSource(strings = {"RASCUNHO", "PAUSADA", "ENCERRADA", "CANCELADA"})
    void vagaNaoAbertaNaoAceitaConvite(String estado) throws Exception {
        db.update("update vagas set status=?::status_vaga_enum where id=?", estado, vaga);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isUnprocessableEntity());
        assertThat(json(mvc.perform(listar(artista)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("content")).isEmpty();
        semConvite();
    }

    @ParameterizedTest @ValueSource(ints = {-1, 0})
    void prazoAtingidoSegueRf23SemEsperarScheduler(int dias) throws Exception {
        db.update("update vagas set data_limite_candidatura=? where id=?", LocalDate.now(clock).plusDays(dias), vaga);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isUnprocessableEntity());
        assertThat(json(mvc.perform(listar(artista)).andReturn().getResponse().getContentAsString()).path("totalElements").asLong()).isZero();
        assertThat(db.queryForObject("select status::text from vagas where id=?", String.class, vaga)).isEqualTo("ABERTA");
        semConvite();
    }

    @Test void vagaAlheiaEInexistenteRetornam404SemConvite() throws Exception {
        long alheia = vaga(outroDono, "ABERTA", null);
        mvc.perform(enviar(artista, alheia)).andExpect(status().isNotFound());
        mvc.perform(enviar(artista, 999999)).andExpect(status().isNotFound());
        mvc.perform(enviar(999999, vaga)).andExpect(status().isNotFound());
        semConvite();
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"vagaId\":1}", "{\"vagaId\":1,\"confirmado\":null}", "{\"vagaId\":0,\"confirmado\":true}", "{\"vagaId\":\"1 OR 1=1\",\"confirmado\":true}"})
    void payloadInvalidoRetorna400(String corpo) throws Exception {
        mvc.perform(enviar(artista, vaga).content(corpo)).andExpect(status().isBadRequest());
        semConvite();
    }

    @Test void confirmacaoFalseRetorna422() throws Exception {
        mvc.perform(enviar(artista, vaga).content("{\"vagaId\":" + vaga + ",\"confirmado\":false}")).andExpect(status().isUnprocessableEntity());
        semConvite();
    }

    @Test void listagemPropriaAbertaPaginadaOrdenadaERestritaNaoEscreve() throws Exception {
        var esperados = new ArrayList<Long>(); esperados.add(vaga);
        for (int i = 0; i < 4; i++) esperados.add(vaga(dono, "ABERTA", LocalDate.now(clock).plusDays(2)));
        for (String estado : List.of("RASCUNHO", "PAUSADA", "ENCERRADA", "CANCELADA")) vaga(dono, estado, null);
        vaga(outroDono, "ABERTA", null); vaga(dono, "ABERTA", LocalDate.now(clock));
        var obtidos = new ArrayList<Long>();
        for (int page = 0; page < 3; page++) {
            var resposta = json(mvc.perform(listar(artista).param("page", "" + page).param("size", "2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(resposta.path("totalElements").asLong()).isEqualTo(5);
            assertThat(resposta.path("hasMore").asBoolean()).isEqualTo(page < 2);
            resposta.path("content").forEach(v -> { campos(v, "id", "titulo"); obtidos.add(v.path("id").asLong()); });
            assertThat(resposta.toString()).doesNotContain("endereco", "email", "telefone", "responsavel", "cpf");
        }
        assertThat(obtidos).containsExactlyElementsOf(esperados);
        var vazia = json(mvc.perform(listar(artista).param("page", "8")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(vazia.path("content")).isEmpty();
        semConvite();
    }

    @ParameterizedTest @CsvSource({"-1,20", "0,0", "0,51"})
    void paginacaoInvalidaNaoListaNemEscreve(int page, int size) throws Exception {
        mvc.perform(listar(artista).param("page", "" + page).param("size", "" + size)).andExpect(status().isBadRequest());
        semConvite();
    }

    @Test void primeiraEmissaoPersisteTipoDestinoContextoEWhitelistNaCentral() throws Exception {
        var antes = db.queryForList("select * from banco_talentos");
        var tabelas = db.queryForList("select tablename from pg_tables where schemaname='public' order by tablename", String.class);
        var contagens = contagens(tabelas);
        var resposta = emitir(artista, vaga, 201);
        campos(resposta, "artistaId", "vagaId", "notificacaoId");
        assertThat(resposta.path("artistaId").asLong()).isEqualTo(artista);
        assertThat(resposta.path("vagaId").asLong()).isEqualTo(vaga);
        var registro = registro();
        assertThat(registro).containsEntry("usuario_destino_id", artista).containsEntry("tipo", "CONVITE")
                .containsEntry("link_contexto", "/vagas/" + vaga).containsEntry("lida", false);
        assertThat(registro.get("id")).isEqualTo(resposta.path("notificacaoId").asLong());
        assertThat(registro.get("mensagem_alerta")).isEqualTo("Um contratante convidou você para conhecer uma vaga.");
        assertThat(registro.get("data_criacao")).isNotNull();
        var central = central(artista).path("content"); assertThat(central).hasSize(1);
        privacidade(central.get(0));
        assertThat(central.get(0).path("tipo").asText()).isEqualTo("CONVITE");
        for (long isolado : List.of(colega, dono, outroDono)) assertThat(central(isolado).path("content")).isEmpty();
        mvc.perform(get("/api/vagas/" + vaga).header("Authorization", bearer(artista))).andExpect(status().isOk());
        for (String tabela : tabelas) assertThat(quantidade(tabela)).as(tabela).isEqualTo(contagens.get(tabela) + (tabela.equals("notificacoes") ? 1 : 0));
        assertThat(db.queryForList("select * from banco_talentos")).isEqualTo(antes);
        assertThat(notificacoes.findById(resposta.path("notificacaoId").asLong()).orElseThrow().getTipo()).isEqualTo(TipoNotificacao.CONVITE);
        verify(realtime, times(1)).entregar(eq(artista), anyString(), any());
    }

    @Test void lidaERepeticaoPreservamMesmoIdDataEstadoENaoEntregamDeNovo() throws Exception {
        var primeira = emitir(artista, vaga, 201); var antes = registro(); long id = primeira.path("notificacaoId").asLong();
        mvc.perform(patch("/api/notificacoes/" + id + "/lida").header("Authorization", bearer(dono))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/notificacoes/" + id + "/lida").header("Authorization", bearer(colega))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/notificacoes/" + id + "/lida").header("Authorization", bearer(artista))).andExpect(status().isNoContent());
        assertThat(emitir(artista, vaga, 200)).isEqualTo(primeira);
        assertThat(registro()).containsEntry("id", antes.get("id")).containsEntry("data_criacao", antes.get("data_criacao")).containsEntry("lida", true);
        assertThat(convites()).hasSize(1);
        verify(realtime, times(1)).entregar(eq(artista), anyString(), any());
    }

    @Test void vagasEArtistasDistintosTemIdentidadesSeparadasEMarcarTodasRespeitaDestino() throws Exception {
        membro(dono, colega); long segunda = vaga(dono, "ABERTA", null);
        emitir(artista, vaga, 201); emitir(artista, segunda, 201); emitir(colega, vaga, 201);
        mvc.perform(patch("/api/notificacoes/lidas").header("Authorization", bearer(artista))).andExpect(status().isNoContent());
        assertThat(db.queryForObject("select count(*) from notificacoes where tipo_notificacao='CONVITE' and usuario_destino_id=? and lida", Long.class, artista)).isEqualTo(2);
        assertThat(db.queryForObject("select lida from notificacoes where tipo_notificacao='CONVITE' and usuario_destino_id=?", Boolean.class, colega)).isFalse();
        assertThat(convites()).hasSize(3);
    }

    @RepeatedTest(3) void concorrenciaPostgresqlCriaUma201Uma200UmRegistroEUmaEntrega() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var barreira = new CyclicBarrier(2);
            var tarefa = (java.util.concurrent.Callable<Integer>) () -> { barreira.await(5, TimeUnit.SECONDS); return mvc.perform(enviar(artista, vaga)).andReturn().getResponse().getStatus(); };
            var a = pool.submit(tarefa); var b = pool.submit(tarefa);
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 200);
        }
        assertThat(convites()).hasSize(1); assertThat(quantidade("candidaturas")).isZero();
        verify(realtime, times(1)).entregar(eq(artista), anyString(), any());
    }

    @ParameterizedTest @ValueSource(strings = {"PENDENTE", "EM_ANALISE", "ACEITA", "REJEITADA", "RETIRADA", "CANCELADA_POR_VAGA"})
    void candidaturaExistenteNaoEMutadaNemConsumidaPorConvite(String estado) throws Exception {
        candidatura(artista, estado);
        var antes = db.queryForList("select * from candidaturas");
        emitir(artista, vaga, 201); emitir(artista, vaga, 200);
        assertThat(db.queryForList("select * from candidaturas")).isEqualTo(antes);
    }

    @Test void artistaDecideRf06NormalmenteEConviteNaoContornaLimite() throws Exception {
        emitir(artista, vaga, 201); assertThat(quantidade("candidaturas")).isZero();
        long primeira = candidatar(201);
        mvc.perform(delete("/api/candidaturas/" + primeira).header("Authorization", bearer(artista))).andExpect(status().isNoContent());
        emitir(artista, vaga, 200);
        long segunda = candidatar(201);
        mvc.perform(delete("/api/candidaturas/" + segunda).header("Authorization", bearer(artista))).andExpect(status().isNoContent());
        candidatar(409);
        assertThat(quantidade("candidaturas")).isEqualTo(2); assertThat(convites()).hasSize(1);
    }

    @Test void conviteNaoDispensaCompletudeRf06NemVagaAberta() throws Exception {
        emitir(artista, vaga, 201);
        db.update("update usuarios set perfil_completo=false where id=?", artista); candidatar(422);
        db.update("update usuarios set perfil_completo=true where id=?", artista);
        db.update("update vagas set status='PAUSADA' where id=?", vaga); candidatar(422);
        assertThat(quantidade("candidaturas")).isZero(); assertThat(convites()).hasSize(1);
    }

    @Test void consultasEAtualizacaoDeDisponibilidadeNaoEnviamConvite() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(listar(artista)).andExpect(status().isOk());
            mvc.perform(get("/api/talentos").header("Authorization", token)).andExpect(status().isOk());
            mvc.perform(get("/api/perfis/publicos/ARTISTA/" + artista)).andExpect(status().isOk());
        }
        mvc.perform(put("/api/perfis-artistas/" + artista).header("Authorization", bearer(artista))
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":" + artista
                        + ",\"disponivelOportunidades\":false,\"biografia\":\"atualizada\","
                        + "\"raioAtuacao\":\"LOCAL\",\"cidade\":\"São Paulo\",\"estado\":\"SP\"}"))
                .andExpect(status().isOk());
        mvc.perform(listar(artista)).andExpect(status().isOk()); semConvite();
    }

    @Test void afterCommitEntregaSomenteDepoisDaTransacaoPrincipal() {
        autenticado();
        try {
            new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
                assertThat(service.convidar(artista, vaga, true).criado()).isTrue();
                assertThat(convites()).hasSize(1); verify(realtime, never()).entregar(anyLong(), anyString(), any());
            });
        } finally { SecurityContextHolder.clearContext(); }
        var captura = ArgumentCaptor.forClass(NotificacaoResponse.class);
        verify(realtime, times(1)).entregar(eq(artista), anyString(), captura.capture());
        assertThat(captura.getValue().getTipo()).isEqualTo(TipoNotificacao.CONVITE);
        assertThat(captura.getValue().getLink()).isEqualTo("/vagas/" + vaga);
    }

    @Test void rollbackDepoisDoFlushNaoDeixaConviteNemEntregaERetentativaCria() throws Exception {
        autenticado();
        try {
            new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
                service.convidar(artista, vaga, true); assertThat(convites()).hasSize(1); tx.setRollbackOnly();
            });
        } finally { SecurityContextHolder.clearContext(); }
        semConvite(); emitir(artista, vaga, 201);
    }

    @Test void falhaDaPersistenciaNaoProduzConviteOuEntrega() throws Exception {
        doThrow(new DataIntegrityViolationException("falha injetada RF42")).when(notificacoes).saveAllAndFlush(any());
        mvc.perform(enviar(artista, vaga)).andExpect(status().isConflict()); semConvite();
        assertThat(quantidade("banco_talentos")).isEqualTo(1);
    }

    @ParameterizedTest @ValueSource(strings = {"WEBSOCKET", "SSE"})
    void transporteFalhaDepoisDoCommitSemPerderConvite(String transporte) throws Exception {
        if (transporte.equals("WEBSOCKET")) doThrow(new IllegalStateException("falha injetada WS")).when(websocket).convertAndSendToUser(anyString(), eq("/queue/notificacoes"), any());
        else doThrow(new IllegalStateException("falha injetada SSE")).when(sse).entregar(eq(artista), any());
        var criada = emitir(artista, vaga, 201); assertThat(emitir(artista, vaga, 200)).isEqualTo(criada);
        assertThat(convites()).hasSize(1); assertThat(central(artista).path("content")).hasSize(1);
        verify(websocket, times(1)).convertAndSendToUser(anyString(), eq("/queue/notificacoes"), any());
        verify(sse, times(1)).entregar(eq(artista), any());
    }

    @Test void falhaDoGatewayMantemRegistroConsultavel() throws Exception {
        doThrow(new IllegalStateException("falha injetada gateway")).when(realtime).entregar(eq(artista), anyString(), any());
        emitir(artista, vaga, 201); assertThat(central(artista).path("content")).hasSize(1); assertThat(convites()).hasSize(1);
    }

    @ParameterizedTest @ValueSource(ints = {14, 17})
    void menorAutorizadoRecebeComPrivacidadeEChatSomenteAposConvite(int idade) throws Exception {
        tornarMenor(artista, idade, true, false);
        var responsavel = db.queryForList("select * from responsaveis_legais");
        chat(dono, artista, 422); assertThat(quantidade("salas_chat")).isZero();
        emitir(artista, vaga, 201); privacidade(central(artista).path("content").get(0));
        chat(dono, artista, 201); assertThat(quantidade("salas_chat")).isEqualTo(1);
        assertThat(db.queryForList("select * from responsaveis_legais")).isEqualTo(responsavel);
        assertThat(quantidade("candidaturas")).isZero();
    }

    @ParameterizedTest @CsvSource({"false,false", "true,true"})
    void menorSemConsentimentoVigenteNaoRecebe(boolean consentiu, boolean revogado) throws Exception {
        tornarMenor(artista, 16, consentiu, revogado);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isNotFound());
        mvc.perform(listar(artista)).andExpect(status().isNotFound()); chat(dono, artista, 422); semConvite();
    }

    @ParameterizedTest @ValueSource(ints = {13, 16})
    void menorSemResponsavelOuAbaixoDaIdadeMinimaProtegido(int idade) throws Exception {
        db.update("update usuarios set data_nascimento=? where id=?", LocalDate.now(clock).minusYears(idade), artista);
        mvc.perform(enviar(artista, vaga)).andExpect(status().isNotFound()); chat(dono, artista, 422); semConvite();
    }

    @Test void conviteLidoHistoricoPermaneceInteracaoSomenteDoDonoCorreto() throws Exception {
        tornarMenor(artista, 16, true, false); tornarMenor(colega, 16, true, false);
        var criada = emitir(artista, vaga, 201);
        mvc.perform(patch("/api/notificacoes/lidas").header("Authorization", bearer(artista))).andExpect(status().isNoContent());
        db.update("update vagas set status='ENCERRADA' where id=?", vaga);
        chat(outroDono, artista, 422); chat(dono, colega, 422); chat(dono, artista, 201);
        assertThat(convites()).hasSize(1); assertThat(registro()).containsEntry("id", criada.path("notificacaoId").asLong()).containsEntry("lida", true);
    }

    @ParameterizedTest @ValueSource(strings = {"/mensagens", "/vagas/999999", "/vagas/1?outro=1", "https://example.test/vagas/1", "/vagas/1 OR 1=1"})
    void conviteGenericoOuContextoInvalidoNaoLiberaChat(String link) throws Exception {
        tornarMenor(artista, 16, true, false);
        db.update("insert into notificacoes(usuario_destino_id,tipo_notificacao,mensagem_alerta,link_contexto) values (?,'CONVITE','mensagem genérica',?)", artista, link);
        chat(dono, artista, 422); assertThat(quantidade("salas_chat")).isZero();
    }

    @Test void contextoDeVagaAlheiaOuRascunhoNaoLiberaChat() throws Exception {
        tornarMenor(artista, 16, true, false);
        long alheia = vaga(outroDono, "ABERTA", null);
        db.update("insert into notificacoes(usuario_destino_id,tipo_notificacao,mensagem_alerta,link_contexto) values (?,'CONVITE','contexto alheio',?)", artista, "/vagas/" + alheia);
        chat(dono, artista, 422);
        db.update("update vagas set status='RASCUNHO' where id=?", alheia);
        chat(outroDono, artista, 422);
    }

    @ParameterizedTest @ValueSource(strings = {"CONSENTIMENTO", "ARTISTA", "CONTRATANTE"})
    void revogacaoOuContaInaptaBloqueiaChatMesmoComConvite(String causa) throws Exception {
        tornarMenor(artista, 16, true, false); emitir(artista, vaga, 201);
        if (causa.equals("CONSENTIMENTO")) db.update("update responsaveis_legais set consentimento_revogado=true where usuario_id=?", artista);
        else db.update("update usuarios set status_conta='BLOQUEADA' where id=?", causa.equals("ARTISTA") ? artista : dono);
        chat(dono, artista, causa.equals("CONTRATANTE") ? 401 : 422);
        assertThat(quantidade("salas_chat")).isZero(); assertThat(convites()).hasSize(1);
    }

    @Test void schemaOficialValidateEnumConviteEApiNaoPermiteForjarNotificacao() throws Exception {
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(db.queryForList("select enumlabel from pg_enum e join pg_type t on t.oid=e.enumtypid where t.typname='tipo_notificacao_enum'", String.class)).contains("CONVITE");
        mvc.perform(post("/api/notificacoes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"CONVITE\",\"usuarioDestinoId\":" + artista + ",\"link\":\"/vagas/" + vaga + "\"}")).andExpect(status().isMethodNotAllowed());
        semConvite();
    }

    private MockHttpServletRequestBuilder request(String metodo, long alvo, long vagaId) {
        return metodo.equals("GET") ? get("/api/talentos/" + alvo + "/convites/vagas") : post("/api/talentos/" + alvo + "/convites")
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vagaId + ",\"confirmado\":true}");
    }
    private MockHttpServletRequestBuilder enviar(long alvo, long vagaId) { return request("POST", alvo, vagaId).header("Authorization", token); }
    private MockHttpServletRequestBuilder listar(long alvo) { return request("GET", alvo, vaga).header("Authorization", token); }
    private JsonNode emitir(long alvo, long vagaId, int codigo) throws Exception { return json(mvc.perform(enviar(alvo, vagaId)).andExpect(status().is(codigo)).andReturn().getResponse().getContentAsString()); }
    private JsonNode json(String texto) throws Exception { return mapper.readTree(texto); }
    private JsonNode central(long id) throws Exception { return json(mvc.perform(get("/api/notificacoes").header("Authorization", bearer(id))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()); }
    private void semConvite() { assertThat(convites()).isEmpty(); verify(realtime, never()).entregar(eq(artista), anyString(), any()); }
    private List<Map<String,Object>> convites() { return db.queryForList("select * from notificacoes where tipo_notificacao='CONVITE' order by id"); }
    private Map<String,Object> registro() { return db.queryForMap("select *,tipo_notificacao::text as tipo from notificacoes where tipo_notificacao='CONVITE'"); }
    private long quantidade(String tabela) { return db.queryForObject("select count(*) from " + tabela, Long.class); }
    private Map<String,Long> contagens(List<String> tabelas) { var r = new java.util.LinkedHashMap<String,Long>(); tabelas.forEach(t -> r.put(t, quantidade(t))); return r; }
    private void campos(JsonNode n, String... esperados) { var f = new ArrayList<String>(); n.fieldNames().forEachRemaining(f::add); assertThat(f).containsExactlyInAnyOrder(esperados); }
    private void privacidade(JsonNode n) { campos(n, "id", "tipo", "mensagem", "link", "lida", "data"); assertThat(n.toString()).doesNotContain("responsavel", "consentimento", "experiencia", "cpf", "telefone", "dataNascimento", "@rf42.test", "11988887777"); }
    private String bearer(long id) { return "Bearer " + jwt.gerarToken(usuarios.findById(id).orElseThrow()); }
    private void autenticado() { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("dono@rf42.test", null, List.of())); }
    private void chat(long autor, long destino, int codigo) throws Exception { mvc.perform(post("/api/chat/salas").header("Authorization", bearer(autor)).contentType(MediaType.APPLICATION_JSON).content("{\"usuarioDestinoId\":" + destino + "}")).andExpect(status().is(codigo)); }
    private long candidatar(int codigo) throws Exception { var r = mvc.perform(post("/api/candidaturas").header("Authorization", bearer(artista)).contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga + "}")).andExpect(status().is(codigo)).andReturn().getResponse(); return codigo == 201 ? json(r.getContentAsString()).path("id").asLong() : 0; }
    private void candidatura(long alvo, String estado) { db.update("insert into candidaturas(vaga_id,artista_id,status) values (?,?,?::status_candidatura_enum)", vaga, alvo, estado); }
    private void membro(long contratante, long alvo) throws Exception { mvc.perform(post("/api/talentos/contratantes/" + contratante + "/participacao").header("Authorization", bearer(alvo)).contentType(MediaType.APPLICATION_JSON).content("{\"confirmado\":true}")).andExpect(status().isCreated()); }
    private long usuario(String chave, String papel, int idade) { return db.queryForObject("insert into usuarios(nome,username,data_nascimento,telefone,email,senha,tipo_usuario,status_conta,email_verificado) values (?,?,?,'11999999999',?,'hash-ficticio',?::tipo_usuario_enum,'ATIVA',true) returning id", Long.class, chave, chave, LocalDate.now(clock).minusYears(idade), chave + "@rf42.test", papel); }
    private long artista(String chave, int idade) { long id = usuario(chave, "ARTISTA", idade); db.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,disponivel_oportunidades) values (?,'ARTISTA_SOLO',true)", id); OfficialSchemaFixtures.completarArtista(db, id); return id; }
    private long contratante(String chave) { long id = usuario(chave, "CONTRATANTE", 30); db.update("insert into perfis_contratantes(usuario_id,tipo_contratante) values (?,'PESSOA_FISICA')", id); return id; }
    private long vaga(long owner, String estado, LocalDate prazo) { return db.queryForObject("insert into vagas(contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,endereco_completo,tipo_contrato,abrangencia,status,data_limite_candidatura) values (?,1,'Vaga pública RF42','Descrição','', 'São Paulo','SP','endereco-privado','Freelance','LOCAL',?::status_vaga_enum,?) returning id", Long.class, owner, estado, prazo); }
    private void tornarMenor(long id, int idade, boolean consentiu, boolean revogado) { db.update("update usuarios set data_nascimento=? where id=?", LocalDate.now(clock).minusYears(idade), id); db.update("insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,data_consentimento,consentimento_revogado) values (?,'responsavel-privado','11988887777','responsavel@rf42.test',?,?)", id, consentiu ? LocalDateTime.now(clock) : null, revogado); }
}
