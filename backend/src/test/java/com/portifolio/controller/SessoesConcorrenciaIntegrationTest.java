package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.Usuario;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SessoesConcorrenciaIntegrationTest {
    private static final String SENHA = "SenhaAtual@2026";
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    private final ObjectMapper json = new ObjectMapper();

    @AfterEach void limpar() { jdbc.execute("TRUNCATE refresh_tokens, usuarios RESTART IDENTITY CASCADE"); }

    @Test void mesmoCookieConcorrenteTemExatamenteUmaRotacao() throws Exception {
        Usuario u = usuario(); MvcResult login = login(u); String antigo = cookie(login);
        List<MvcResult> resultados = sobDisputaReal(u, () -> refresh(antigo), () -> refresh(antigo));
        assertThat(resultados.stream().map(r -> r.getResponse().getStatus()).toList()).containsExactlyInAnyOrder(200, 401);
        assertThat(ativos(u)).isOne();
        MvcResult venceu = resultados.stream().filter(r -> r.getResponse().getStatus() == 200).findFirst().orElseThrow();
        assertThat(cookie(venceu)).isNotEqualTo(antigo);
        assertThat(refresh(antigo).getResponse().getStatus()).isEqualTo(401);
        assertThat(refresh(cookie(venceu)).getResponse().getStatus()).isEqualTo(200);
    }

    @Test void mesmoResetConcorrenteTrocaSenhaUmaUnicaVez() throws Exception {
        Usuario u = usuario(); String raw = "reset-concorrente-somente-fixture";
        u.setTokenRecuperacao(hash(raw)); u.setTokenExpiracao(LocalDateTime.now().plusHours(1)); usuarios.saveAndFlush(u);
        List<MvcResult> resultados = sobDisputaReal(u,
                () -> reset(raw, "Primeira@2026"), () -> reset(raw, "Segunda@2026"));
        assertThat(resultados.stream().map(r -> r.getResponse().getStatus()).toList()).containsExactlyInAnyOrder(200, 404);
        Usuario salvo = usuarios.findById(u.getId()).orElseThrow();
        assertThat(salvo.getTokenRecuperacao()).isNull(); assertThat(salvo.getTokenExpiracao()).isNull();
        assertThat(encoder.matches("Primeira@2026", salvo.getSenha()) ^ encoder.matches("Segunda@2026", salvo.getSenha())).isTrue();
        assertThat(reset(raw, "Terceira@2026").getResponse().getStatus()).isEqualTo(404);
    }

    @Test void logoutConcorrenteComRefreshNaoDeixaSessaoReutilizavel() throws Exception {
        Usuario u = usuario(); MvcResult a = login(u); String raw = cookie(a), access = bearer(a);
        List<MvcResult> resultados = sobDisputaReal(u,
                () -> mvc.perform(post("/api/auth/logout").header("Authorization", access).cookie(cookieRaw(raw))).andReturn(),
                () -> refresh(raw));
        assertThat(resultados.getFirst().getResponse().getStatus()).isEqualTo(204);
        assertThat(resultados.get(1).getResponse().getStatus()).isIn(200, 401);
        assertThat(ativos(u)).isZero();
        assertThat(refresh(raw).getResponse().getStatus()).isEqualTo(401);
        if (resultados.get(1).getResponse().getStatus() == 200)
            assertThat(refresh(cookie(resultados.get(1))).getResponse().getStatus()).isEqualTo(401);
        mvc.perform(get("/api/usuarios/me").header("Authorization", access)).andExpect(status().isUnauthorized());
    }

    @Test void senhaConcorrenteComRefreshRevogaQualquerRotacaoAnteriorAoCommit() throws Exception {
        Usuario u = usuario(); MvcResult a = login(u); String raw = cookie(a), access = bearer(a);
        List<MvcResult> resultados = sobDisputaReal(u,
                () -> mvc.perform(put("/api/auth/senha").header("Authorization", access).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("senhaAtual", SENHA, "novaSenha", "Nova@2026")))).andReturn(),
                () -> refresh(raw));
        assertThat(resultados.getFirst().getResponse().getStatus()).isEqualTo(204);
        assertThat(resultados.get(1).getResponse().getStatus()).isIn(200, 401);
        assertThat(encoder.matches("Nova@2026", usuarios.findById(u.getId()).orElseThrow().getSenha())).isTrue();
        assertThat(ativos(u)).isZero();
        assertThat(refresh(raw).getResponse().getStatus()).isEqualTo(401);
        mvc.perform(get("/api/usuarios/me").header("Authorization", access)).andExpect(status().isUnauthorized());
        if (resultados.get(1).getResponse().getStatus() == 200) {
            MvcResult rodou = resultados.get(1);
            assertThat(refresh(cookie(rodou)).getResponse().getStatus()).isEqualTo(401);
            mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(rodou))).andExpect(status().isUnauthorized());
        }
    }

    @Test void resetConcorrenteComRefreshRevogaSessaoAoFinal() throws Exception {
        Usuario u = usuario(); MvcResult a = login(u); String raw = cookie(a), reset = "reset-refresh-fixture";
        u.setTokenRecuperacao(hash(reset)); u.setTokenExpiracao(LocalDateTime.now().plusHours(1)); usuarios.saveAndFlush(u);
        List<MvcResult> resultados = sobDisputaReal(u, () -> reset(reset, "Nova@2026"), () -> refresh(raw));
        assertThat(resultados.getFirst().getResponse().getStatus()).isEqualTo(200);
        assertThat(resultados.get(1).getResponse().getStatus()).isIn(200, 401);
        assertThat(ativos(u)).isZero();
        if (resultados.get(1).getResponse().getStatus() == 200)
            assertThat(refresh(cookie(resultados.get(1))).getResponse().getStatus()).isEqualTo(401);
    }

    /** As duas operações efetivamente aguardam o mesmo lock no PostgreSQL antes da liberação. */
    private List<MvcResult> sobDisputaReal(Usuario u, Callable<MvcResult> primeira, Callable<MvcResult> segunda) throws Exception {
        CountDownLatch bloqueado = new CountDownLatch(1), liberar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(3)) {
            var holder = executor.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                usuarios.findByIdForUpdate(u.getId()).orElseThrow(); bloqueado.countDown();
                try { if (!liberar.await(20, TimeUnit.SECONDS)) throw new AssertionError("Barreira não liberada"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                return true;
            }));
            assertThat(bloqueado.await(10, TimeUnit.SECONDS)).isTrue();
            var a = executor.submit(primeira); var b = executor.submit(segunda);
            try {
                long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                int aguardando;
                do {
                    aguardando = jdbc.queryForObject("select count(*) from pg_stat_activity where datname=current_database() "
                            + "and wait_event_type='Lock' and query like '%usuarios%'", Integer.class);
                    if (aguardando < 2) Thread.sleep(25);
                } while (aguardando < 2 && System.nanoTime() < limite);
                assertThat(aguardando).as("duas transações aguardando lock real").isGreaterThanOrEqualTo(2);
            } finally { liberar.countDown(); }
            holder.get(10, TimeUnit.SECONDS);
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally { liberar.countDown(); }
    }

    private Usuario usuario() {
        Usuario u = com.portifolio.support.OfficialSchemaFixtures.usuario(); u.setSenha(encoder.encode(SENHA));
        u.setNome("Usuário de concorrência"); u.setEmail(u.getUsername() + "@concorrencia.test");
        u.setDataNascimento(java.time.LocalDate.of(1990, 1, 1)); u.setTelefone("11999999999");
        u.setTipoUsuario(com.portifolio.model.enums.TipoUsuario.ARTISTA);
        u.setDataCriacao(LocalDateTime.now()); u.setPerfilCompleto(false);
        u.setStatusConta(com.portifolio.model.enums.StatusConta.ATIVA); u.setEmailVerificado(true); return usuarios.saveAndFlush(u);
    }
    private MvcResult login(Usuario u) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", SENHA, "rememberMe", true))))
                .andExpect(status().isOk()).andReturn();
    }
    private MvcResult refresh(String raw) throws Exception {
        return mvc.perform(post("/api/auth/refresh").cookie(cookieRaw(raw))).andReturn();
    }
    private MvcResult reset(String raw, String senha) throws Exception {
        return mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", raw, "novaSenha", senha)))).andReturn();
    }
    private int ativos(Usuario u) {
        return jdbc.queryForObject("select count(*) from refresh_tokens where usuario_id=? and ativo", Integer.class, u.getId());
    }
    private String cookie(MvcResult r) { return r.getResponse().getHeader("Set-Cookie").split(";", 2)[0].substring("palco_refresh=".length()); }
    private Cookie cookieRaw(String raw) { return new Cookie("palco_refresh", raw); }
    private String bearer(MvcResult r) throws Exception { return "Bearer " + json.readTree(r.getResponse().getContentAsString()).get("token").textValue(); }
    private String hash(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
