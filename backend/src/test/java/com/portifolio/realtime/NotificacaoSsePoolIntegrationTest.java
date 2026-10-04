package com.portifolio.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.zaxxer.hikari.HikariDataSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.connection-timeout=1000",
        "spring.jpa.open-in-view=true",
        "spring.lifecycle.timeout-per-shutdown-phase=2s"
})
class NotificacaoSsePoolIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @LocalServerPort int port;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired DataSource dataSource;
    @Autowired NotificacaoSseService sse;

    @Test
    void seisStreamsNaoRetemPoolDeDuasConexoesEMantemEntregaPrivada() throws Exception {
        Usuario dono = usuario("pool-dono@rf36.test");
        Usuario outro = usuario("pool-outro@rf36.test");
        String token = jwt.gerarToken(dono);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        List<InputStream> streams = new ArrayList<>();
        List<BufferedReader> readers = new ArrayList<>();
        NotificacaoResponse alerta = NotificacaoResponse.builder().id(901L)
                .tipo(TipoNotificacao.MENSAGEM).mensagem("Evento privado RF36")
                .link("/mensagens?sala=1").lida(false).data(LocalDateTime.now()).build();
        try {
            for (int i = 0; i < 6; i++) {
                String bearer = i == 5 ? jwt.gerarToken(outro) : token;
                HttpResponse<InputStream> response = client.send(request("/api/notificacoes/stream", bearer)
                        .header("Accept", "text/event-stream").build(), HttpResponse.BodyHandlers.ofInputStream());
                streams.add(response.body());
                assertThat(response.statusCode()).as("stream %s com pool máximo 2", i + 1).isEqualTo(200);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
                readers.add(reader);
                assertThat(evento(reader)).contains("event:conectado", "data:ok");
            }
            HttpResponse<String> count = client.send(request("/api/notificacoes/nao-lidas/count", token).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(count.statusCode()).isEqualTo(200);
            assertThat(count.body()).contains("\"count\":0");
            HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (pool.getHikariPoolMXBean().getActiveConnections() != 0 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
            assertThat(sse.conexoesAtivas(dono.getId())).isEqualTo(5);
            assertThat(sse.conexoesAtivas(outro.getId())).isEqualTo(1);

            sse.entregar(dono.getId(), alerta);
            for (int i = 0; i < 5; i++) {
                assertThat(evento(readers.get(i))).contains("event:notificacao", "\"id\":901", "Evento privado RF36");
            }
            // The other stream receives only its own sentinel, never the owner's event.
            NotificacaoResponse sentinel = NotificacaoResponse.builder().id(902L)
                    .tipo(TipoNotificacao.MENSAGEM).mensagem("Somente outro usuário")
                    .lida(false).data(LocalDateTime.now()).build();
            sse.entregar(outro.getId(), sentinel);
            assertThat(evento(readers.get(5))).contains("\"id\":902").doesNotContain("901", "Evento privado RF36");
        } finally {
            for (InputStream stream : streams) stream.close();
            client.shutdownNow();
            // Let the existing emitter detect closed clients without waiting its 30-minute timeout.
            sse.entregar(dono.getId(), alerta);
            sse.entregar(outro.getId(), alerta);
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                sse.entregar(dono.getId(), alerta); sse.entregar(outro.getId(), alerta);
                assertThat(sse.conexoesAtivas(dono.getId())).isZero();
                assertThat(sse.conexoesAtivas(outro.getId())).isZero();
            });
        }
    }

    private HttpRequest.Builder request(String path, String token) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + token).GET();
    }

    private String evento(BufferedReader reader) throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder event = new StringBuilder();
            try {
                String line;
                while ((line = reader.readLine()) != null && !line.isBlank()) event.append(line).append('\n');
                return event.toString();
            } catch (Exception error) { throw new IllegalStateException(error); }
        }).get(5, TimeUnit.SECONDS);
    }

    private Usuario usuario(String email) {
        Usuario user = com.portifolio.support.OfficialSchemaFixtures.usuario();
        user.setNome("Usuário sintético RF36");
        user.setDataNascimento(LocalDate.of(1990, 1, 1));
        user.setTelefone("11999999999");
        user.setEmail(email);
        user.setSenha("{noop}teste");
        user.setTipoUsuario(TipoUsuario.ARTISTA);
        user.setPerfilCompleto(true);
        user.setStatusConta(com.portifolio.model.enums.StatusConta.ATIVA);
        user.setEmailVerificado(true);
        user.setDataCriacao(LocalDateTime.now());
        return usuarios.save(user);
    }
}
