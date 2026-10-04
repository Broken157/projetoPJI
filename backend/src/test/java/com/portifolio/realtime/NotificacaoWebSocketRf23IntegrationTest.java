package com.portifolio.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Type;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacaoWebSocketRf23IntegrationTest {

    private static final String SECRET = requireTestSecret();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @LocalServerPort int port;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired JwtService jwtService;
    @Autowired NotificacaoRealtimeGateway realtimeGateway;
    @Autowired NotificacaoSseService sse;

    private static String requireTestSecret() {
        String secret = System.getProperty("JWT_SECRET", System.getenv("JWT_SECRET"));
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET deve ser fornecido externamente para o teste WebSocket");
        }
        return secret;
    }

    @AfterEach
    void limpar() {
        jdbcTemplate.execute("TRUNCATE notificacoes, usuarios RESTART IDENTITY CASCADE");
    }

    @ParameterizedTest @CsvSource({"CANDIDATURA,CANDIDATURA","BANCO,BANCO_DE_TALENTOS","CONVITE,CONVITE","MENSAGEM,MENSAGEM","SALVO_PERFIL,SALVO","SALVO_VAGA,SALVO","CANCELAMENTO,CANDIDATURA"})
    void jwtValidoEntregaNaFilaPrivadaCorretaEmMenosDeCincoSegundos(String produtor, TipoNotificacao tipo) throws Exception {
        Usuario destino = novoUsuario("destino-ws@rf23.test");
        Usuario isolado = novoUsuario("isolado-ws@rf23.test");
        WebSocketStompClient cliente = cliente();
        StompSession sessaoDestino = conectar(cliente, jwtService.gerarToken(destino));
        StompSession sessaoIsolada = conectar(cliente, jwtService.gerarToken(isolado));
        BlockingQueue<JsonNode> recebidasDestino = new LinkedBlockingQueue<>();
        BlockingQueue<JsonNode> recebidasIsolado = new LinkedBlockingQueue<>();
        sessaoDestino.subscribe("/user/queue/notificacoes", handler(recebidasDestino));
        sessaoIsolada.subscribe("/user/queue/notificacoes", handler(recebidasIsolado));
        aguardarEntregaPrivada(destino, recebidasDestino);
        aguardarEntregaPrivada(isolado, recebidasIsolado);

        NotificacaoResponse alerta = NotificacaoResponse.builder()
                .id(99L).tipo(tipo).mensagem("Entrega privada")
                .link("dashboard-contratante.html").lida(false).data(LocalDateTime.now()).build();
        long inicio = System.nanoTime();
        NotificacaoResponse emitida = produzirPorHttp(destino, produtor, alerta);
        JsonNode recebida = recebidasDestino.poll(5, TimeUnit.SECONDS);
        Duration duracao = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(recebida).isNotNull();
        assertThat(recebida.path("id").asLong()).isEqualTo(emitida.getId());
        assertThat(recebida.path("tipo").asText()).isEqualTo(tipo.name());
        assertThat(recebida.path("link").asText()).isEqualTo(emitida.getLink());
        assertThat(recebida.path("mensagem").asText()).isEqualTo(emitida.getMensagem());
        assertThat(duracao).isLessThan(Duration.ofSeconds(5));
        assertPrivacidade(recebida);
        System.out.println("RF36 WS produtor=" + produtor + " entregaMs=" + duracao.toMillis());
        assertThat(recebidasIsolado.poll(700, TimeUnit.MILLISECONDS)).isNull();
        sessaoDestino.disconnect();
        sessaoIsolada.disconnect();
        cliente.stop();
    }

    @Test
    void connectSemJwtComJwtInvalidoEComJwtExpiradoSaoRejeitados() {
        Usuario usuario = novoUsuario("seguranca-ws@rf23.test");
        WebSocketStompClient cliente = cliente();

        assertConexaoRejeitada(cliente, null);
        assertConexaoRejeitada(cliente, "jwt-invalido");
        assertConexaoRejeitada(cliente, tokenExpirado(usuario));
        cliente.stop();
    }

    @ParameterizedTest @CsvSource({"CANDIDATURA,CANDIDATURA","BANCO,BANCO_DE_TALENTOS","CONVITE,CONVITE","MENSAGEM,MENSAGEM","SALVO_PERFIL,SALVO","SALVO_VAGA,SALVO","CANCELAMENTO,CANDIDATURA"})
    void sseComAuthorizationEntregaSomenteAoUsuarioCorreto(String produtor, TipoNotificacao tipo) throws Exception {
        Usuario destino = novoUsuario("destino-sse@rf23.test");
        Usuario isolado = novoUsuario("isolado-sse@rf23.test");
        HttpClient clienteHttp = HttpClient.newHttpClient();
        HttpResponse<InputStream> respostaDestino = abrirSse(
                clienteHttp, jwtService.gerarToken(destino));
        HttpResponse<InputStream> respostaIsolada = abrirSse(
                clienteHttp, jwtService.gerarToken(isolado));
        assertThat(respostaDestino.statusCode()).isEqualTo(200);
        assertThat(respostaIsolada.statusCode()).isEqualTo(200);
        BufferedReader leitorDestino = new BufferedReader(
                new InputStreamReader(respostaDestino.body(), StandardCharsets.UTF_8));
        BufferedReader leitorIsolado = new BufferedReader(
                new InputStreamReader(respostaIsolada.body(), StandardCharsets.UTF_8));
        consumirEventoConectado(leitorDestino);
        consumirEventoConectado(leitorIsolado);

        NotificacaoResponse alerta = NotificacaoResponse.builder()
                .id(77L).tipo(tipo).mensagem("Fallback SSE")
                .link("dashboard-contratante.html").lida(false).data(LocalDateTime.now()).build();
        long inicio = System.nanoTime();
        CompletableFuture<String> leituraIsolada = CompletableFuture.supplyAsync(() -> lerDados(leitorIsolado));
        NotificacaoResponse emitida = produzirPorHttp(destino, produtor, alerta);
        String dados = CompletableFuture.supplyAsync(() -> lerDados(leitorDestino))
                .get(5, TimeUnit.SECONDS);

        JsonNode recebida = new ObjectMapper().readTree(dados);
        assertThat(recebida.path("id").asLong()).isEqualTo(emitida.getId());
        assertThat(recebida.path("tipo").asText()).isEqualTo(tipo.name());
        assertThat(recebida.path("link").asText()).isEqualTo(emitida.getLink());
        assertThat(recebida.path("mensagem").asText()).isEqualTo(emitida.getMensagem());
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(5));
        assertPrivacidade(recebida);
        realtimeGateway.entregar(isolado.getId(), isolado.getEmail(), NotificacaoResponse.builder()
                .id(-1L).tipo(tipo).mensagem("Sentinela de isolamento").link("/vagas/1").build());
        assertThat(new ObjectMapper().readTree(leituraIsolada.get(5, TimeUnit.SECONDS)).path("id").asLong()).isEqualTo(-1L);
        respostaDestino.body().close();
        respostaIsolada.body().close();
        // Uma escrita final permite ao servidor detectar os clientes fechados e
        // encerrar os SseEmitter sem prolongar o shutdown da suíte.
        realtimeGateway.entregar(destino.getId(), destino.getEmail(), emitida);
        realtimeGateway.entregar(isolado.getId(), isolado.getEmail(), emitida);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            sse.entregar(destino.getId(), emitida); sse.entregar(isolado.getId(), emitida);
            assertThat(sse.conexoesAtivas(destino.getId())).isZero();
            assertThat(sse.conexoesAtivas(isolado.getId())).isZero();
        });
    }

    // Transport readiness probe is not persisted and precedes the measured real producer.
    private void aguardarEntregaPrivada(Usuario usuario, BlockingQueue<JsonNode> fila) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        JsonNode recebido = null;
        while (recebido == null && System.nanoTime() < limite) {
            realtimeGateway.entregar(usuario.getId(), usuario.getEmail(), NotificacaoResponse.builder()
                    .id(-1L).tipo(TipoNotificacao.SALVO).mensagem("Prontidão").link("/vagas/1").build());
            recebido = fila.poll(50, TimeUnit.MILLISECONDS);
        }
        assertThat(recebido).isNotNull(); assertThat(recebido.path("id").asLong()).isEqualTo(-1L); fila.clear();
    }

    private void assertPrivacidade(JsonNode alerta) {
        java.util.Set<String> campos = new java.util.HashSet<>(); alerta.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder("id","tipo","mensagem","link","lida","data");
        assertThat(alerta.toString()).doesNotContain("@rf", "11999999999", "1990-01-01", "{noop}teste",
                "Corpo privado", "cpf", "cnpj", "responsavel", "experiencia", "token", "telefone", "senha");
        assertThat(alerta.path("link").asText()).startsWith("/").doesNotContain("://", "token=");
    }

    private NotificacaoResponse produzirPorHttp(Usuario destino, String produtor, NotificacaoResponse alerta) throws Exception {
        if (produtor.equals("CANDIDATURA") || produtor.startsWith("SALVO") || produtor.equals("CANCELAMENTO")) {
            return candidaturaSalvoOuCancelamento(destino, produtor);
        }
        return entregarOuParticipar(destino, alerta);
    }

    private NotificacaoResponse candidaturaSalvoOuCancelamento(Usuario destino, String produtor) throws Exception {
        Usuario ator = novoUsuario("ator-produtor@rf36.test");
        boolean perfil = produtor.equals("SALVO_PERFIL");
        Usuario contratante = produtor.equals("CANCELAMENTO") ? ator : destino;
        long vaga = 0;
        if (perfil) {
            jdbcTemplate.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico) values (?,'ARTISTA_SOLO')", destino.getId());
        } else {
            contratante.setTipoUsuario(TipoUsuario.CONTRATANTE); usuarioRepository.saveAndFlush(contratante);
            jdbcTemplate.update("insert into perfis_contratantes(usuario_id,tipo_contratante) values (?,'PESSOA_FISICA')", contratante.getId());
            vaga = jdbcTemplate.queryForObject("""
                    insert into vagas(contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,tipo_contrato,abrangencia,status)
                    values (?,1,'Vaga pública RF36','Descrição','','São Paulo','SP','Projeto','LOCAL','ABERTA') returning id
                    """, Long.class, contratante.getId());
        }
        String caminho, corpo, metodo = "POST";
        if (produtor.startsWith("SALVO")) {
            caminho = "/api/salvos"; corpo = "{\"tipoAlvo\":\""+(perfil?"PERFIL_ARTISTA":"VAGA")+"\",\"alvoId\":"+(perfil?destino.getId():vaga)+"}";
        } else if (produtor.equals("CANCELAMENTO")) {
            jdbcTemplate.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico) values (?,'ARTISTA_SOLO')", destino.getId());
            jdbcTemplate.update("insert into candidaturas(vaga_id,artista_id,mensagem_apresentacao,link_portfolio_candidatura,status,data_candidatura) values (?,?,'Corpo privado','https://portfolio.test','PENDENTE',current_timestamp)", vaga,destino.getId());
            caminho = "/api/vagas/"+vaga; corpo = "{\"confirmacao\":true,\"motivo\":\"Projeto encerrado\"}"; metodo = "DELETE";
        } else {
            jdbcTemplate.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,disponivel_oportunidades) values (?,'ARTISTA_SOLO',true)", ator.getId());
            com.portifolio.support.OfficialSchemaFixtures.completarArtista(jdbcTemplate, ator.getId());
            caminho = "/api/candidaturas"; corpo = "{\"vagaId\":"+vaga+",\"mensagemApresentacao\":\"Corpo privado\",\"linkPortfolioCandidatura\":\"https://portfolio.test\"}";
        }
        var resposta = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+caminho))
                .header("Authorization", "Bearer "+jwtService.gerarToken(ator)).header("Content-Type","application/json")
                .method(metodo,HttpRequest.BodyPublishers.ofString(corpo)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(resposta.statusCode()).isEqualTo(metodo.equals("DELETE") ? 204 : 201);
        var registro = jdbcTemplate.queryForMap("select id,tipo_notificacao::text as tipo,mensagem_alerta,link_contexto,usuario_destino_id from notificacoes");
        assertThat(registro).containsEntry("usuario_destino_id", destino.getId());
        String link = perfil ? "/perfis/ARTISTA/"+destino.getId() : "/vagas/"+vaga+(produtor.equals("CANDIDATURA") ? "/gerenciar" : "");
        assertThat(registro).containsEntry("link_contexto",link);
        return NotificacaoResponse.builder().id(((Number) registro.get("id")).longValue())
                .tipo(TipoNotificacao.valueOf((String)registro.get("tipo"))).mensagem((String)registro.get("mensagem_alerta"))
                .link((String)registro.get("link_contexto")).build();
    }

    @ParameterizedTest @ValueSource(strings={"INEXISTENTE","INAPTO"})
    void connectRejeitaIdentidadeAtualInexistenteOuInapta(String estado) {
        Usuario usuario = novoUsuario("estado-ws@rf36.test"); String token = jwtService.gerarToken(usuario);
        if (estado.equals("INEXISTENTE")) usuarioRepository.deleteById(usuario.getId());
        else jdbcTemplate.update("update usuarios set status_conta='BLOQUEADA' where id=?",usuario.getId());
        var cliente = cliente(); try { assertConexaoRejeitada(cliente,token); } finally { cliente.stop(); }
    }

    @ParameterizedTest @ValueSource(strings={"AUSENTE","INVALIDO","EXPIRADO","INEXISTENTE","INAPTO"})
    void connectInvalidoEnviaErrorEFechaWebSocketReal(String caso) throws Exception {
        Usuario usuario = novoUsuario("error-ws@rf36.test");
        String token = jwtService.gerarToken(usuario);
        if (caso.equals("INVALIDO")) token = "jwt-invalido";
        if (caso.equals("EXPIRADO")) token = tokenExpirado(usuario);
        if (caso.equals("INEXISTENTE")) usuarioRepository.deleteById(usuario.getId());
        if (caso.equals("INAPTO")) jdbcTemplate.update("update usuarios set status_conta='BLOQUEADA' where id=?",usuario.getId());
        QuadrosStomp quadros = new QuadrosStomp();
        try (HttpClient http = HttpClient.newHttpClient()) {
            var socket = http.newWebSocketBuilder().header("Origin","http://localhost:3000")
                    .buildAsync(URI.create("ws://localhost:"+port+"/ws"),quadros).get(5,TimeUnit.SECONDS);
            try {
                socket.sendText("CONNECT\naccept-version:1.2\nhost:localhost\n"
                        +(caso.equals("AUSENTE")?"":"Authorization:Bearer "+token+"\n")+"\n\u0000",true).get(5,TimeUnit.SECONDS);
                assertThat(quadros.erro.get(5,TimeUnit.SECONDS)).startsWith("ERROR");
                assertThat(quadros.fechamento.get(5,TimeUnit.SECONDS)).isNotNull();
                assertThat(quadros.conectado.isDone()).isFalse();
            } finally { socket.abort(); }
        }
    }

    @Test void assinaturaDeFilaPrivadaAlheiaEnviaErrorEFecha() throws Exception {
        Usuario usuario = novoUsuario("assinatura@rf36.test");
        Usuario outro = novoUsuario("alheio@rf36.test");
        QuadrosStomp quadros = new QuadrosStomp();
        try (HttpClient http = HttpClient.newHttpClient()) {
            var socket = http.newWebSocketBuilder().header("Origin","http://localhost:3000")
                    .buildAsync(URI.create("ws://localhost:"+port+"/ws"),quadros).get(5,TimeUnit.SECONDS);
            try {
                socket.sendText("CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer "+jwtService.gerarToken(usuario)+"\n\n\u0000",true).get(5,TimeUnit.SECONDS);
                assertThat(quadros.conectado.get(5,TimeUnit.SECONDS)).startsWith("CONNECTED");
                socket.sendText("SUBSCRIBE\nid:alheia\ndestination:/user/"+outro.getEmail()+"/queue/notificacoes\n\n\u0000",true).get(5,TimeUnit.SECONDS);
                assertThat(quadros.erro.get(5,TimeUnit.SECONDS)).startsWith("ERROR");
                assertThat(quadros.fechamento.get(5,TimeUnit.SECONDS)).isNotNull();
            } finally { socket.abort(); }
        }
    }

    private static class QuadrosStomp implements java.net.http.WebSocket.Listener {
        final CompletableFuture<String> conectado = new CompletableFuture<>();
        final CompletableFuture<String> erro = new CompletableFuture<>();
        final CompletableFuture<Integer> fechamento = new CompletableFuture<>();
        final StringBuilder buffer = new StringBuilder();
        @Override public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket socket, CharSequence texto, boolean ultimo) {
            buffer.append(texto);
            int fim;
            while ((fim = buffer.indexOf("\u0000")) >= 0) {
                String quadro = buffer.substring(0,fim).stripLeading(); buffer.delete(0,fim+1);
                if (quadro.startsWith("CONNECTED")) conectado.complete(quadro);
                if (quadro.startsWith("ERROR")) erro.complete(quadro);
            }
            socket.request(1); return null;
        }
        @Override public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket socket, int status, String motivo) {
            fechamento.complete(status); return null;
        }
        @Override public void onError(java.net.http.WebSocket socket, Throwable falha) {
            conectado.completeExceptionally(falha); erro.completeExceptionally(falha); fechamento.completeExceptionally(falha);
        }
    }

    private NotificacaoResponse entregarOuParticipar(Usuario destino, NotificacaoResponse alerta) throws Exception {
        if (alerta.getTipo() == TipoNotificacao.MENSAGEM) return mensagemPorHttp(destino);
        if (alerta.getTipo() == TipoNotificacao.CONVITE) {
            return convidarPorHttp(destino);
        }
        if (alerta.getTipo() != TipoNotificacao.BANCO_DE_TALENTOS) {
            realtimeGateway.entregar(destino.getId(), destino.getEmail(), alerta);
            return alerta;
        }
        destino.setTipoUsuario(TipoUsuario.CONTRATANTE);
        usuarioRepository.saveAndFlush(destino);
        jdbcTemplate.update("insert into perfis_contratantes(usuario_id,tipo_contratante) values (?,'PESSOA_FISICA')", destino.getId());
        Usuario artista = novoUsuario("artista-banco@rf36.test");
        jdbcTemplate.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,disponivel_oportunidades) values (?,'ARTISTA_SOLO',true)", artista.getId());
        com.portifolio.support.OfficialSchemaFixtures.completarArtista(jdbcTemplate, artista.getId());
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/api/talentos/contratantes/" + destino.getId() + "/participacao"))
                .header("Authorization", "Bearer " + jwtService.gerarToken(artista))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"confirmado\":true}"))
                .build();
        assertThat(HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode())
                .isEqualTo(201);
        assertThat(jdbcTemplate.queryForObject("select count(*) from banco_talentos where contratante_id=? and artista_id=?",
                Long.class, destino.getId(), artista.getId())).isEqualTo(1L);
        var registro = jdbcTemplate.queryForMap("select id,tipo_notificacao::text as tipo,mensagem_alerta,link_contexto from notificacoes");
        assertThat(registro).containsEntry("tipo", "BANCO_DE_TALENTOS")
                .containsEntry("link_contexto", "/perfis/ARTISTA/" + artista.getId());
        return NotificacaoResponse.builder().id(((Number) registro.get("id")).longValue())
                .tipo(TipoNotificacao.BANCO_DE_TALENTOS).mensagem((String) registro.get("mensagem_alerta"))
                .link((String) registro.get("link_contexto")).build();
    }

    private NotificacaoResponse mensagemPorHttp(Usuario artista) throws Exception {
        Usuario contratante = novoUsuario("contratante-chat@rf35.test");
        contratante.setTipoUsuario(TipoUsuario.CONTRATANTE);
        usuarioRepository.saveAndFlush(contratante);
        HttpClient cliente = HttpClient.newHttpClient();
        String autorizacao = "Bearer " + jwtService.gerarToken(contratante);
        var sala = cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/chat/salas"))
                .header("Authorization", autorizacao).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"usuarioDestinoId\":" + artista.getId() + "}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(sala.statusCode()).isEqualTo(201);
        long id = new ObjectMapper().readTree(sala.body()).path("salaId").asLong();
        var mensagem = cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/chat/salas/" + id + "/mensagens"))
                .header("Authorization", autorizacao).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"texto\":\"Contato profissional\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(mensagem.statusCode()).isEqualTo(201);
        var registro = jdbcTemplate.queryForMap("select id,tipo_notificacao::text as tipo,mensagem_alerta,link_contexto,usuario_destino_id from notificacoes");
        assertThat(registro).containsEntry("tipo", "MENSAGEM").containsEntry("usuario_destino_id", artista.getId())
                .containsEntry("link_contexto", "/mensagens?sala=" + id);
        return NotificacaoResponse.builder().id(((Number) registro.get("id")).longValue()).tipo(TipoNotificacao.MENSAGEM)
                .mensagem((String) registro.get("mensagem_alerta")).link((String) registro.get("link_contexto")).build();
    }

    @Test
    void handshakeNaoAceitaQueryMesmoComJwtValidoNoConnect() {
        Usuario usuario = novoUsuario("query@rf35.test");
        var cliente = cliente();
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtService.gerarToken(usuario));
        WebSocketHttpHeaders http = new WebSocketHttpHeaders();
        http.setOrigin("http://localhost:3000");
        try {
            assertThatThrownBy(() -> cliente.connectAsync("ws://localhost:" + port + "/ws?token=nao-e-credencial",
                    http, headers, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);
        } finally { cliente.stop(); }
    }

    private NotificacaoResponse convidarPorHttp(Usuario artista) throws Exception {
        jdbcTemplate.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,disponivel_oportunidades) values (?,'ARTISTA_SOLO',true)", artista.getId());
        com.portifolio.support.OfficialSchemaFixtures.completarArtista(jdbcTemplate, artista.getId());
        Usuario contratante = novoUsuario("contratante-convite@rf42.test");
        contratante.setTipoUsuario(TipoUsuario.CONTRATANTE);
        usuarioRepository.saveAndFlush(contratante);
        jdbcTemplate.update("insert into perfis_contratantes(usuario_id,tipo_contratante) values (?,'PESSOA_FISICA')", contratante.getId());
        HttpClient cliente = HttpClient.newHttpClient();
        HttpRequest participacao = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/api/talentos/contratantes/" + contratante.getId() + "/participacao"))
                .header("Authorization", "Bearer " + jwtService.gerarToken(artista))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"confirmado\":true}")).build();
        assertThat(cliente.send(participacao, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(201);
        long vagaId = jdbcTemplate.queryForObject("""
                insert into vagas(contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,tipo_contrato,abrangencia,status)
                values (?,1,'Vaga RF42','Descrição','','São Paulo','SP','Freelance','LOCAL','ABERTA') returning id
                """, Long.class, contratante.getId());
        HttpRequest convite = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/api/talentos/" + artista.getId() + "/convites"))
                .header("Authorization", "Bearer " + jwtService.gerarToken(contratante))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"vagaId\":" + vagaId + ",\"confirmado\":true}")).build();
        var resultado = cliente.send(convite, HttpResponse.BodyHandlers.ofString());
        assertThat(resultado.statusCode()).isEqualTo(201);
        var registro = jdbcTemplate.queryForMap("select id,tipo_notificacao::text as tipo,mensagem_alerta,link_contexto,usuario_destino_id from notificacoes where tipo_notificacao='CONVITE'");
        assertThat(registro).containsEntry("tipo", "CONVITE").containsEntry("usuario_destino_id", artista.getId())
                .containsEntry("link_contexto", "/vagas/" + vagaId);
        assertThat(new ObjectMapper().readTree(resultado.body()).path("notificacaoId").asLong())
                .isEqualTo(((Number) registro.get("id")).longValue());
        assertThat(jdbcTemplate.queryForObject("select count(*) from candidaturas", Long.class)).isZero();
        return NotificacaoResponse.builder().id(((Number) registro.get("id")).longValue())
                .tipo(TipoNotificacao.CONVITE).mensagem((String) registro.get("mensagem_alerta"))
                .link((String) registro.get("link_contexto")).build();
    }

    private void assertConexaoRejeitada(WebSocketStompClient cliente, String token) {
        assertThatThrownBy(() -> conectar(cliente, token))
                .isInstanceOfAny(ExecutionException.class, IllegalStateException.class);
    }

    private StompSession conectar(WebSocketStompClient cliente, String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.setOrigin("http://localhost:3000");
        return cliente.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        httpHeaders,
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private WebSocketStompClient cliente() {
        WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        cliente.setMessageConverter(converter);
        return cliente;
    }

    private HttpResponse<InputStream> abrirSse(HttpClient cliente, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/notificacoes/stream"))
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return cliente.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private void consumirEventoConectado(BufferedReader leitor) throws Exception {
        String linha;
        while ((linha = leitor.readLine()) != null && !linha.isBlank()) {
            // consome o evento inicial "conectado"
        }
    }

    private String lerDados(BufferedReader leitor) {
        try {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.startsWith("data:")) {
                    return linha.substring(5).trim();
                }
            }
            return "";
        } catch (Exception erro) {
            throw new IllegalStateException(erro);
        }
    }

    private StompFrameHandler handler(BlockingQueue<JsonNode> fila) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                fila.add((JsonNode) payload);
            }
        };
    }

    private Usuario novoUsuario(String email) {
        Usuario usuario = com.portifolio.support.OfficialSchemaFixtures.usuario();
        usuario.setNome("Pessoa WebSocket");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setTelefone("11999999999");
        usuario.setEmail(email);
        usuario.setSenha("{noop}teste");
        usuario.setTipoUsuario(TipoUsuario.ARTISTA);
        usuario.setPerfilCompleto(true);
        usuario.setStatusConta(com.portifolio.model.enums.StatusConta.ATIVA);
        usuario.setEmailVerificado(true);
        usuario.setDataCriacao(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private String tokenExpirado(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id",usuario.getId())
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
