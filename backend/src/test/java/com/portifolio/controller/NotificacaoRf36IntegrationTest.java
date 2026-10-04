package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.event.NotificacaoEvento;
import com.portifolio.event.NotificacaoPersistida;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.realtime.NotificacaoRealtimeGateway;
import com.portifolio.realtime.NotificacaoSseService;
import com.portifolio.repository.NotificacaoRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.service.NotificacaoPersistenceService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class NotificacaoRf36IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired ApplicationEventPublisher eventos;
    @Autowired NotificacaoPersistenceService persistence;
    @Autowired PlatformTransactionManager txManager;
    @MockitoSpyBean NotificacaoRepository notificacoes;
    @MockitoSpyBean NotificacaoRealtimeGateway realtime;
    @MockitoSpyBean NotificacaoSseService sse;
    @MockitoSpyBean SimpMessagingTemplate ws;
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    Usuario ator, dono, artista, terceiro;

    @BeforeEach void preparar() {
        jdbc.execute("truncate usuarios restart identity cascade");
        jdbc.execute("""
                insert into usuarios(id,username,nome,data_nascimento,telefone,email,senha,tipo_usuario,status_conta,perfil_completo)
                values (1,'rf36_ator','Ator','1990-01-01','11900000001','ator@rf36.test','hash-privado','CONTRATANTE','ATIVA',true),
                (2,'rf36_dono','Dono','1990-01-01','11900000002','dono@rf36.test','hash-privado','CONTRATANTE','ATIVA',true),
                (3,'rf36_artista','Artista','1990-01-01','11900000003','artista@rf36.test','hash-privado','ARTISTA','ATIVA',true),
                (4,'rf36_terceiro','Terceiro','1990-01-01','11900000004','terceiro@rf36.test','hash-privado','ARTISTA','ATIVA',true)
                """);
        jdbc.execute("insert into perfis_contratantes(usuario_id,tipo_contratante) values (1,'PESSOA_FISICA'),(2,'PESSOA_FISICA')");
        jdbc.execute("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,disponivel_oportunidades) values (3,'ARTISTA_SOLO',true),(4,'ARTISTA_SOLO',true)");
        jdbc.execute("""
                insert into vagas(id,contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,tipo_contrato,abrangencia,status)
                values (10,2,1,'Vaga pública','Descrição','Requisitos','São Paulo','SP','Projeto','LOCAL','ABERTA')
                """);
        ator = usuarios.findById(1L).orElseThrow(); dono = usuarios.findById(2L).orElseThrow();
        artista = usuarios.findById(3L).orElseThrow(); terceiro = usuarios.findById(4L).orElseThrow();
        clearInvocations(realtime, sse, ws, notificacoes);
    }

    @AfterEach void limpar() { jdbc.execute("truncate usuarios restart identity cascade"); }
    String bearer(Usuario u) { return "Bearer " + jwt.gerarToken(u); }
    long quantidade() { return jdbc.queryForObject("select count(*) from notificacoes", Long.class); }
    NotificacaoEvento evento() {
        return new NotificacaoEvento(Set.of(2L), TipoNotificacao.SALVO, "Sua vaga foi salva.", "/vagas/10");
    }
    TransactionTemplate tx() { return new TransactionTemplate(txManager); }
    long inserir(long destino, TipoNotificacao tipo, boolean lida) {
        return jdbc.queryForObject("""
                insert into notificacoes(usuario_destino_id,tipo_notificacao,mensagem_alerta,link_contexto,lida,data_criacao)
                values (?,?, 'Alerta mínimo','/vagas/10',?,timestamp '2026-10-04 12:00:00') returning id
                """, Long.class, destino, tipo.name(), lida);
    }
    JsonNode central(Usuario u) throws Exception {
        return json.readTree(mvc.perform(get("/api/notificacoes").header("Authorization", bearer(u)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    void privado(JsonNode alerta) {
        Set<String> campos = new HashSet<>(); alerta.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder("id", "tipo", "mensagem", "link", "lida", "data");
        assertThat(alerta.toString()).doesNotContain("ator@rf36.test", "dono@rf36.test", "artista@rf36.test",
                "terceiro@rf36.test", "11900000001", "11900000002", "hash-privado", "1990-01-01",
                "cpf", "cnpj", "responsavel", "telefone", "senha", "token", "experiencia", "Corpo privado");
        assertThat(alerta.path("link").asText()).startsWith("/").doesNotContain("://", "token=");
    }

    @ParameterizedTest @EnumSource(TipoNotificacao.class)
    void tiposReaisLinksEWhitelistPreservados(TipoNotificacao tipo) throws Exception {
        long id = inserir(2, tipo, false); inserir(4, tipo, false);
        JsonNode pagina = central(dono);
        assertThat(pagina.path("totalElements").asLong()).isEqualTo(1);
        JsonNode alerta = pagina.path("content").get(0);
        assertThat(alerta.path("id").asLong()).isEqualTo(id);
        assertThat(alerta.path("tipo").asText()).isEqualTo(tipo.name());
        assertThat(alerta.path("mensagem").asText()).isEqualTo("Alerta mínimo");
        assertThat(alerta.path("link").asText()).isEqualTo("/vagas/10"); privado(alerta);
    }

    @Test void empateDeDataTemOrdemPorIdEPaginaSemTerceiro() throws Exception {
        long primeiro = inserir(2, TipoNotificacao.SALVO, true);
        long segundo = inserir(2, TipoNotificacao.CONVITE, false);
        inserir(4, TipoNotificacao.MENSAGEM, false);
        mvc.perform(get("/api/notificacoes?page=0&size=1").header("Authorization", bearer(dono)))
                .andExpect(jsonPath("$.content[0].id").value(segundo)).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/notificacoes?page=1&size=1").header("Authorization", bearer(dono)))
                .andExpect(jsonPath("$.content[0].id").value(primeiro)).andExpect(jsonPath("$.content[0].lida").value(true));
    }

    @ParameterizedTest @ValueSource(strings={"", "Bearer inválido", "Bearer expirado", "Bearer inapto"})
    void credenciaisInvalidasNaoListamNemAbremSse(String autorizacao) throws Exception {
        if (autorizacao.endsWith("expirado")) {
            String segredo = System.getProperty("JWT_SECRET", System.getenv("JWT_SECRET"));
            autorizacao = "Bearer " + Jwts.builder().subject(dono.getEmail()).claim("id", dono.getId())
                    .expiration(new Date(System.currentTimeMillis() - 60_000))
                    .signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8))).compact();
        } else if (autorizacao.endsWith("inapto")) {
            autorizacao = bearer(dono); jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=2");
        }
        for (String caminho : List.of("/api/notificacoes", "/api/notificacoes/stream", "/api/notificacoes/nao-lidas/count")) {
            mvc.perform(get(caminho).header("Authorization", autorizacao)).andExpect(status().isUnauthorized());
        }
    }

    @Test void usuarioRemovidoNaoAutenticaNemRecebeRealtime() throws Exception {
        String token = bearer(terceiro); jdbc.update("delete from usuarios where id=4");
        mvc.perform(get("/api/notificacoes").header("Authorization", token)).andExpect(status().isUnauthorized());
        realtime.entregar(4L, terceiro.getEmail(), NotificacaoResponse.builder().id(1L).build());
        verifyNoInteractions(sse, ws);
    }

    @Test void usuarioInaptoNaoRecebeRealtime() {
        jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=2");
        realtime.entregar(2L, dono.getEmail(), NotificacaoResponse.builder().id(1L).build());
        verifyNoInteractions(sse, ws);
    }

    @ParameterizedTest @ValueSource(strings={"?token=nao-e-segredo", "?access_token=nao-e-segredo"})
    void streamRejeitaQueryMesmoComHeaderValido(String query) throws Exception {
        mvc.perform(get("/api/notificacoes/stream" + query).header("Authorization", bearer(dono)))
                .andExpect(status().isBadRequest()).andExpect(request().asyncNotStarted());
        verify(sse, never()).conectar(anyLong());
    }

    @Test void naoExisteCriacaoArbitrariaDeNotificacao() throws Exception {
        mvc.perform(post("/api/notificacoes").header("Authorization", bearer(ator)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioDestino\":2,\"tipo\":\"MENSAGEM\",\"mensagem\":\"Forjada\",\"linkContexto\":\"https://invalid.test\",\"ator\":3}"))
                .andExpect(status().isMethodNotAllowed());
        assertThat(quantidade()).isZero(); verifyNoInteractions(realtime);
    }

    @ParameterizedTest @ValueSource(ints={0,1,5})
    void lerTodasEhIdempotenteSomenteDoJwt(int numero) throws Exception {
        for (int i=0;i<numero;i++) inserir(2, TipoNotificacao.SALVO, i % 2 == 0);
        inserir(4, TipoNotificacao.SALVO, false);
        for (int vez=0;vez<2;vez++) mvc.perform(patch("/api/notificacoes/lidas?usuarioId=4")
                .header("Authorization", bearer(dono))).andExpect(status().isNoContent());
        mvc.perform(get("/api/notificacoes/nao-lidas/count").header("Authorization", bearer(dono)))
                .andExpect(jsonPath("$.count").value(0));
        assertThat(jdbc.queryForObject("select count(*) from notificacoes where usuario_destino_id=4 and lida=false", Long.class)).isOne();
        assertThat(quantidade()).isEqualTo(numero+1); verifyNoInteractions(realtime);
    }

    @Test void lidaPropriaReduzCountEInexistenteOuAlheiaNaoVaza() throws Exception {
        long minha = inserir(2, TipoNotificacao.SALVO, false); long alheia = inserir(4, TipoNotificacao.SALVO, false);
        for (int i=0;i<2;i++) mvc.perform(patch("/api/notificacoes/{id}/lida", minha).header("Authorization", bearer(dono)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notificacoes/nao-lidas/count").header("Authorization", bearer(dono)))
                .andExpect(jsonPath("$.count").value(0));
        var x = mvc.perform(patch("/api/notificacoes/{id}/lida", alheia).header("Authorization", bearer(dono)))
                .andExpect(status().isNotFound()).andReturn().getResponse();
        var y = mvc.perform(patch("/api/notificacoes/999999/lida").header("Authorization", bearer(dono)))
                .andExpect(status().isNotFound()).andReturn().getResponse();
        JsonNode mensagem = json.readTree(x.getContentAsString()).path("mensagem");
        assertThat(mensagem.isTextual()).isTrue();
        assertThat(mensagem).isEqualTo(json.readTree(y.getContentAsString()).path("mensagem"));
        assertThat(jdbc.queryForObject("select lida from notificacoes where id=?", Boolean.class, alheia)).isFalse();
        assertThat(quantidade()).isEqualTo(2); verifyNoInteractions(realtime);
    }

    @Test void persistenciaPrecedeEntregaQueOcorreSomenteDepoisDoCommit() {
        tx().executeWithoutResult(status -> {
            eventos.publishEvent(evento()); assertThat(quantidade()).isOne(); verifyNoInteractions(realtime);
        });
        verify(realtime, times(1)).entregar(eq(2L), eq(dono.getEmail()), any());
        assertThat(quantidade()).isOne();
    }

    @Test void rollbackDoProdutorRemoveNotificacaoENaoEntrega() {
        tx().executeWithoutResult(status -> { eventos.publishEvent(evento()); status.setRollbackOnly(); });
        assertThat(quantidade()).isZero(); verifyNoInteractions(realtime);
    }

    @Test void eventoSemTransacaoNaoViraSucessoSilencioso() {
        assertThatThrownBy(() -> eventos.publishEvent(evento())).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> persistence.persistirNaTransacaoAtual(evento())).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(quantidade()).isZero(); verifyNoInteractions(realtime);
    }

    @Test void mesmoObjetoNoMesmoFluxoNaoMultiplicaMasTextoIgualEmOutraOperacaoEhValido() {
        NotificacaoEvento unico = evento();
        tx().executeWithoutResult(status -> { eventos.publishEvent(unico); eventos.publishEvent(unico); });
        assertThat(quantidade()).isOne(); verify(realtime, times(1)).entregar(eq(2L), anyString(), any());
        tx().executeWithoutResult(status -> { eventos.publishEvent(evento()); eventos.publishEvent(evento()); });
        assertThat(quantidade()).isEqualTo(3); verify(realtime, times(3)).entregar(eq(2L), anyString(), any());
    }

    @Test void reentregarPersistidaNaoCriaOutraLinha() {
        tx().executeWithoutResult(status -> {
            var persistida = persistence.persistirNaTransacaoAtual(evento()).getFirst();
            eventos.publishEvent(persistida); eventos.publishEvent(persistida);
        });
        assertThat(quantidade()).isOne();
        centralDisponivelAposEntrega();
    }
    void centralDisponivelAposEntrega() {
        assertThat(jdbc.queryForObject("select lida from notificacoes", Boolean.class)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings={"WS","SSE","AMBOS","GATEWAY"})
    void falhaDoTransportePreservaDominioNotificacaoECentral(String falha) throws Exception {
        if (falha.equals("WS") || falha.equals("AMBOS")) doThrow(new IllegalStateException("broker indisponivel"))
                .when(ws).convertAndSendToUser(anyString(), eq("/queue/notificacoes"), any(Object.class));
        if (falha.equals("SSE") || falha.equals("AMBOS")) doThrow(new IllegalStateException("stream indisponivel"))
                .when(sse).entregar(anyLong(), any());
        if (falha.equals("GATEWAY")) doThrow(new IllegalStateException("transportes indisponiveis"))
                .when(realtime).entregar(anyLong(), anyString(), any());
        salvar("VAGA",10).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from itens_salvos", Long.class)).isOne();
        assertThat(quantidade()).isOne(); privado(central(dono).path("content").get(0));
        if (falha.equals("WS")) verify(sse).entregar(eq(2L), any());
        if (falha.equals("SSE")) verify(ws).convertAndSendToUser(eq(dono.getEmail()), eq("/queue/notificacoes"), any(Object.class));
    }

    ResultActions salvar(String tipo, long alvo) throws Exception {
        return mvc.perform(post("/api/salvos").header("Authorization", bearer(ator)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAlvo\":\""+tipo+"\",\"alvoId\":"+alvo+",\"usuarioDestino\":4,\"ator\":4}"));
    }
    @ParameterizedTest @CsvSource({"PERFIL_ARTISTA,3,/perfis/ARTISTA/3", "VAGA,10,/vagas/10"})
    void salvoRealUmaVezGetRemoverNaoAlertamEAtorNaoVaza(String tipo,long alvo,String link) throws Exception {
        salvar(tipo,alvo).andExpect(status().isCreated()); salvar(tipo,alvo).andExpect(status().isOk());
        long destino = tipo.equals("VAGA") ? 2 : 3;
        Usuario u = destino == 2 ? dono : artista;
        mvc.perform(get("/api/salvos").header("Authorization", bearer(ator))).andExpect(status().isOk());
        mvc.perform(get("/api/salvos/estado").param("tipoAlvo",tipo).param("alvoId",""+alvo)
                .header("Authorization", bearer(ator))).andExpect(status().isOk());
        mvc.perform(delete("/api/salvos/{tipo}/{alvo}",tipo,alvo).header("Authorization", bearer(ator))).andExpect(status().isNoContent());
        assertThat(quantidade()).isOne(); verify(realtime, times(1)).entregar(eq(destino), eq(u.getEmail()), any());
        JsonNode alerta = central(u).path("content").get(0); privado(alerta);
        assertThat(alerta.path("tipo").asText()).isEqualTo("SALVO");
        assertThat(alerta.path("link").asText()).isEqualTo(link);
        assertThat(central(terceiro).path("content")).isEmpty();
    }

    @ParameterizedTest @CsvSource({"PERFIL_ARTISTA,3", "VAGA,10"})
    void falhaDePersistenciaDaNotificacaoDesfazSalvoENaoEntrega(String tipo,long alvo) throws Exception {
        doThrow(new DataIntegrityViolationException("Falha simulada")) .when(notificacoes).saveAllAndFlush(any());
        salvar(tipo,alvo).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from itens_salvos", Long.class)).isZero();
        assertThat(quantidade()).isZero(); verifyNoInteractions(realtime);
    }

    @Test void obraIndisponivelNaoInventaProdutorOuNotificacao() throws Exception {
        salvar("OBRA",10).andExpect(status().isBadRequest());
        assertThat(quantidade()).isZero(); verifyNoInteractions(realtime);
    }
}
