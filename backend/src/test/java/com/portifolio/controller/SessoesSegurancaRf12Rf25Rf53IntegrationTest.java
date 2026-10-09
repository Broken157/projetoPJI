package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.RefreshToken;
import com.portifolio.model.Usuario;
import com.portifolio.repository.RefreshTokenRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.security.SessionCookiePolicy;
import com.portifolio.validation.PasswordPolicy;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SessoesSegurancaRf12Rf25Rf53IntegrationTest {
    private static final String SENHA = "SenhaAtual@2026";
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @MockitoSpyBean RefreshTokenRepository sessoes;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    @AfterEach void limpar() {
        reset(sessoes);
        jdbc.execute("TRUNCATE refresh_tokens, usuarios RESTART IDENTITY CASCADE");
    }

    @Test void rememberFalseEDefaultNaoCriamSessaoPersistente() throws Exception {
        Usuario u = usuario();
        for (Map<String, ?> payload : java.util.List.of(
                Map.of("email", u.getEmail(), "senha", SENHA),
                Map.of("email", u.getEmail(), "senha", SENHA, "rememberMe", false))) {
            MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(payload))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
            assertThat(result.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
        }
        assertThat(sessoes.count()).isZero();
    }

    @Test void rememberTruePersisteHashECookieComTodosOsAtributos() throws Exception {
        Usuario u = usuario();
        MvcResult result = login(u, true);
        String raw = cookie(result);
        RefreshToken rt = sessoes.findAll().getFirst();
        assertThat(rt.getTokenHash()).isEqualTo(hash(raw)).matches("[0-9a-f]{64}").isNotEqualTo(raw);
        assertThat(rt.getExpiracao()).isBetween(LocalDateTime.now().plusDays(29), LocalDateTime.now().plusDays(30));
        assertThat(rt.getAtivo()).isTrue();
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth", "Max-Age=2592000");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(raw, rt.getTokenHash(), "refreshToken");
    }

    @Test void doisDispositivosCoexistemELogoutEncerraSomenteAtual() throws Exception {
        Usuario u = usuario();
        MvcResult a = login(u, true), b = login(u, true);
        assertThat(sessoes.findAll()).hasSize(2).allMatch(rt -> rt.getAtivo());
        MvcResult out = mvc.perform(post("/api/auth/logout").cookie(refreshCookie(a))
                .header("Authorization", bearer(a))).andExpect(status().isNoContent()).andReturn();
        assertCookieApagado(out);
        assertThat(out.getResponse().getContentAsString()).isEmpty();
        protegida(a, 401);
        protegida(b, 200);
        refresh(cookie(a)).andExpect(status().isUnauthorized());
        refresh(cookie(b)).andExpect(status().isOk());
        assertThat(sessoes.findAll()).filteredOn(rt -> rt.getAtivo()).hasSize(1);
    }

    @Test void logoutAccessOnlyRevogaJWTESegueIdempotenteSemCriarRefresh() throws Exception {
        MvcResult a = login(usuario(), false);
        protegida(a, 200);
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            MvcResult result = mvc.perform(post("/api/auth/logout").header("Authorization", bearer(a)))
                    .andExpect(status().isNoContent()).andReturn();
            assertCookieApagado(result);
        }
        protegida(a, 401);
        assertThat(sessoes.count()).isZero();
    }

    @Test void rotacaoSubstituiSegredoMantemIDSessaoEJamaisAceitaAnterior() throws Exception {
        MvcResult a = login(usuario(), true);
        Long sessionId = sessoes.findAll().getFirst().getId();
        LocalDateTime criadaEm = sessoes.findAll().getFirst().getDataCriacao();
        MvcResult b = refresh(cookie(a)).andExpect(status().isOk()).andReturn();
        assertThat(cookie(b)).isNotEqualTo(cookie(a));
        RefreshToken atual = sessoes.findById(sessionId).orElseThrow();
        assertThat(atual.getTokenHash()).isEqualTo(hash(cookie(b))).isNotEqualTo(hash(cookie(a)));
        assertThat(atual.getDataCriacao()).isEqualTo(criadaEm);
        assertThat(atual.getUltimoUso()).isNotNull();
        assertThat(sessoes.count()).isOne();
        assertThat(sessoes.findByTokenHash(hash(cookie(a)))).isEmpty();
        assertThat(b.getResponse().getContentAsString()).doesNotContain(cookie(b), atual.getTokenHash(), "refreshToken");
        refresh(cookie(a)).andExpect(status().isUnauthorized());
        refresh(cookie(b)).andExpect(status().isOk());
    }

    @ParameterizedTest @ValueSource(strings = {"EXPIRADO", "REVOGADO", "ALEATORIO", "AUSENTE", "ORFAO"})
    void refreshInvalidoRetorna401GenericoELimpaCookie(String caso) throws Exception {
        MvcResult login = login(usuario(), true);
        RefreshToken rt = sessoes.findAll().getFirst();
        if (caso.equals("EXPIRADO")) rt.setExpiracao(LocalDateTime.now().minusSeconds(1));
        if (caso.equals("REVOGADO")) rt.setAtivo(false);
        if (caso.equals("ORFAO")) rt.setUsuario(null);
        sessoes.saveAndFlush(rt);
        String raw = caso.equals("ALEATORIO") ? "segredo-aleatorio" : caso.equals("AUSENTE") ? null : cookie(login);
        MvcResult result = refresh(raw).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Sessão expirada. Entre novamente."))
                .andExpect(jsonPath("$.token").doesNotExist()).andReturn();
        assertCookieApagado(result);
        assertThat(sessoes.count()).isOne();
    }

    @ParameterizedTest @ValueSource(strings = {"BODY", "QUERY", "AUTHORIZATION"})
    void refreshNaoAceitaFallbackForaDoCookie(String canal) throws Exception {
        MvcResult a = login(usuario(), true);
        var req = post("/api/auth/refresh");
        if (canal.equals("BODY")) req.contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", cookie(a))));
        if (canal.equals("QUERY")) req.queryParam("refreshToken", cookie(a));
        if (canal.equals("AUTHORIZATION")) req.header("Authorization", "Bearer " + cookie(a));
        MvcResult result = mvc.perform(req).andExpect(status().isUnauthorized()).andReturn();
        assertCookieApagado(result);
        refresh(cookie(a)).andExpect(status().isOk());
    }

    @Test void origemExternaNaoConsomeRefreshNemEncerraSessao() throws Exception {
        MvcResult a = login(usuario(), true);
        mvc.perform(post("/api/auth/refresh").header("Origin", "https://fora.example")
                .cookie(refreshCookie(a))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").header("Origin", "https://fora.example")
                .cookie(refreshCookie(a))).andExpect(status().isForbidden());
        refresh(cookie(a)).andExpect(status().isOk());
    }

    @Test void listaSomentePropriasAtivasNaoExpiradasSemSegredosOuDispositivosInventados() throws Exception {
        Usuario a = usuario(), b = usuario();
        MvcResult a1 = login(a, true), a2 = login(a, true);
        MvcResult terceiro = login(b, true);
        MvcResult expirada = login(a, true), revogada = login(a, true);
        RefreshToken e = sessoes.findByTokenHash(hash(cookie(expirada))).orElseThrow();
        e.setExpiracao(LocalDateTime.now().minusSeconds(1)); sessoes.saveAndFlush(e);
        RefreshToken r = sessoes.findByTokenHash(hash(cookie(revogada))).orElseThrow();
        r.setAtivo(false); sessoes.saveAndFlush(r);
        MvcResult lista = mvc.perform(get("/api/auth/sessoes").header("Authorization", bearer(a1))
                .cookie(refreshCookie(a1)).queryParam("usuarioId", b.getId().toString()))
                .andExpect(status().isOk()).andReturn();
        JsonNode rows = corpo(lista);
        assertThat(rows.size()).isEqualTo(2);
        assertThat(rows.findValues("atual")).containsExactlyInAnyOrder(json.readTree("true"), json.readTree("false"));
        assertThat(lista.getResponse().getContentAsString()).doesNotContain(cookie(a1), cookie(a2), cookie(terceiro),
                "tokenHash", "refreshToken", "usuarioId", "email", "dispositivo", "ip", "navegador");
        for (JsonNode row : rows) {
            assertThat(row.size()).isEqualTo(5);
            assertThat(row.has("sessionId") && row.has("criadaEm") && row.has("expiraEm")).isTrue();
            assertThat(sessoes.findById(row.get("sessionId").longValue()).orElseThrow().getUsuario().getId()).isEqualTo(a.getId());
        }
    }

    @Test void listaAccessOnlyNaoInventaRegistroEIgnoraCookieDeTerceiro() throws Exception {
        Usuario a = usuario(), b = usuario();
        MvcResult access = login(a, false), outro = login(b, true);
        mvc.perform(get("/api/auth/sessoes").header("Authorization", bearer(access)).cookie(refreshCookie(outro)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(sessoes.count()).isOne();
    }

    @Test void revogaSessaoRemotaPreservaCookieESessaoAtual() throws Exception {
        Usuario a = usuario(); MvcResult atual = login(a, true), remoto = login(a, true);
        Long alvo = sessoes.findByTokenHash(hash(cookie(remoto))).orElseThrow().getId();
        MvcResult result = mvc.perform(delete("/api/auth/sessoes/{id}", alvo)
                .header("Authorization", bearer(atual)).cookie(refreshCookie(atual)))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
        protegida(atual, 200); protegida(remoto, 401);
        refresh(cookie(remoto)).andExpect(status().isUnauthorized());
        refresh(cookie(atual)).andExpect(status().isOk());
    }

    @Test void revogaSessaoAtualLimpaCookieENegaProximaChamada() throws Exception {
        MvcResult a = login(usuario(), true);
        Long alvo = sessoes.findAll().getFirst().getId();
        MvcResult result = mvc.perform(delete("/api/auth/sessoes/{id}", alvo).header("Authorization", bearer(a))
                .cookie(refreshCookie(a))).andExpect(status().isNoContent()).andReturn();
        assertCookieApagado(result); protegida(a, 401);
        refresh(cookie(a)).andExpect(status().isUnauthorized());
    }

    @Test void idorRejeitaSessaoAlheiaSemAlterarNenhuma() throws Exception {
        MvcResult a = login(usuario(), true), b = login(usuario(), true);
        Long alheia = sessoes.findByTokenHash(hash(cookie(b))).orElseThrow().getId();
        mvc.perform(delete("/api/auth/sessoes/{id}", alheia).header("Authorization", bearer(a))
                .cookie(refreshCookie(a))).andExpect(status().isNotFound());
        assertThat(sessoes.findAll()).allMatch(rt -> rt.getAtivo());
        protegida(a, 200); protegida(b, 200);
    }

    @Test void todasRevogaSomenteTitularIncluindoAccessOnlyELimpaCookie() throws Exception {
        Usuario a = usuario(), b = usuario();
        MvcResult atual = login(a, true), remoto = login(a, true), access = login(a, false), outra = login(b, true);
        MvcResult result = mvc.perform(delete("/api/auth/sessoes").header("Authorization", bearer(access))
                .cookie(refreshCookie(atual)).queryParam("usuarioId", b.getId().toString()))
                .andExpect(status().isNoContent()).andReturn();
        assertCookieApagado(result);
        protegida(atual, 401); protegida(remoto, 401); protegida(access, 401); protegida(outra, 200);
        refresh(cookie(atual)).andExpect(status().isUnauthorized());
        refresh(cookie(remoto)).andExpect(status().isUnauthorized());
        refresh(cookie(outra)).andExpect(status().isOk());
    }

    @Test void endpointsSegurancaExigemJWT() throws Exception {
        mvc.perform(get("/api/auth/sessoes")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/auth/sessoes/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/auth/sessoes")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"A\",\"novaSenha\":\"B\"}")).andExpect(status().isUnauthorized());
    }

    @Test void senhaAtualIncorretaNaoMudaHashNemRevoga() throws Exception {
        Usuario u = usuario(); MvcResult a = login(u, true);
        alterar(a, "Incorreta@2026", "Nova@2026").andExpect(status().isForbidden());
        assertThat(usuarios.findById(u.getId()).orElseThrow().getSenha()).isEqualTo(u.getSenha());
        protegida(a, 200); refresh(cookie(a)).andExpect(status().isOk());
    }

    @ParameterizedTest @NullSource
    @ValueSource(strings = {"Aa1!aaa", "Aa1!aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "aa1!aaaa", "AA1!AAAA", "Aa!!aaaa", "Aa12aaaa", "Aa1 aaaa", "Aa1\taaaa"})
    void novaSenhaInvalidaNaoAlteraCredencialOuSessoes(String nova) throws Exception {
        Usuario u = usuario(); MvcResult a = login(u, true), b = login(u, false);
        alterar(a, SENHA, nova).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(PasswordPolicy.MESSAGE));
        assertThat(usuarios.findById(u.getId()).orElseThrow().getSenha()).isEqualTo(u.getSenha());
        protegida(a, 200); protegida(b, 200);
        refresh(cookie(a)).andExpect(status().isOk());
    }

    @Test void senhaValidaMantemEspacosRevogaTodosSomenteDoTitularEIgnoraOwnerEnviado() throws Exception {
        Usuario u = usuario(), outra = usuario();
        MvcResult a = login(u, true), b = login(u, true), access = login(u, false), terceiro = login(outra, true);
        String nova = "  Nova Senha@2026  ";
        MvcResult result = mvc.perform(put("/api/auth/senha").header("Authorization", bearer(access))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "senhaAtual", SENHA, "novaSenha", nova, "usuarioId", outra.getId(), "email", outra.getEmail()))))
                .andExpect(status().isNoContent()).andReturn();
        assertCookieApagado(result);
        String hash = usuarios.findById(u.getId()).orElseThrow().getSenha();
        assertThat(hash).startsWith("$2").isNotEqualTo(nova);
        assertThat(encoder.matches(nova, hash)).isTrue();
        assertThat(encoder.matches(nova.trim(), hash)).isFalse();
        assertThat(usuarios.findById(outra.getId()).orElseThrow().getSenha()).isEqualTo(outra.getSenha());
        protegida(a, 401); protegida(b, 401); protegida(access, 401); protegida(terceiro, 200);
        refresh(cookie(a)).andExpect(status().isUnauthorized()); refresh(cookie(b)).andExpect(status().isUnauthorized());
        MvcResult novo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", nova))))
                .andExpect(status().isOk()).andReturn();
        protegida(novo, 200);
    }

    @Test void googleOnlyNaoCriaPrimeiraSenhaPorJWT() throws Exception {
        Usuario u = usuario();
        jdbc.update("insert into perfis_artistas(usuario_id, tipo_perfil_artistico) values (?, 'ARTISTA_SOLO')", u.getId());
        com.portifolio.support.OfficialSchemaFixtures.completarArtista(jdbc, u.getId());
        u = usuarios.findById(u.getId()).orElseThrow();
        u.setGoogleId("google-" + u.getId()); u.setSenha(null); usuarios.saveAndFlush(u);
        String token = jwt.gerarToken(u);
        mvc.perform(put("/api/auth/senha").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"senhaAtual\":\"Qualquer@2026\",\"novaSenha\":\"Nova@2026\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(usuarios.findById(u.getId()).orElseThrow().getSenha()).isNull();
        assertThat(jwt.tokenValido(token)).isTrue();
    }

    @Test void falhaDeRevogacaoFazRollbackDeSenhaESessoesESemBlacklist() throws Exception {
        Usuario u = usuario(); MvcResult a = login(u, true), access = login(u, false);
        doThrow(new org.springframework.dao.DataIntegrityViolationException("Falha simulada sanitizada"))
                .when(sessoes).invalidarTodosPorUsuario(u.getId());
        alterar(access, SENHA, "Nova@2026").andExpect(status().isConflict());
        assertThat(usuarios.findById(u.getId()).orElseThrow().getSenha()).isEqualTo(u.getSenha());
        protegida(a, 200); protegida(access, 200);
        assertThat(sessoes.findAll()).allMatch(rt -> rt.getAtivo());
    }

    @Test void falhaDeRotacaoNaoConsomeSegredoAnterior() throws Exception {
        MvcResult a = login(usuario(), true);
        doThrow(new org.springframework.dao.DataIntegrityViolationException("Falha simulada sanitizada"))
                .when(sessoes).saveAndFlush(any(RefreshToken.class));
        refresh(cookie(a)).andExpect(status().isConflict());
        assertThat(sessoes.findAll().getFirst().getTokenHash()).isEqualTo(hash(cookie(a)));
        reset(sessoes);
        refresh(cookie(a)).andExpect(status().isOk());
    }

    @Test void respostasELogsNaoExibemSegredos(CapturedOutput output) throws Exception {
        Usuario u = usuario(); MvcResult a = login(u, true);
        String raw = cookie(a), access = corpo(a).get("token").textValue(), tokenHash = hash(raw);
        MvcResult lista = mvc.perform(get("/api/auth/sessoes").header("Authorization", bearer(a))
                .cookie(refreshCookie(a))).andExpect(status().isOk()).andReturn();
        String nova = "Nova@2026";
        MvcResult senha = alterar(a, SENHA, nova).andExpect(status().isNoContent()).andReturn();
        String novoHash = usuarios.findById(u.getId()).orElseThrow().getSenha();
        assertThat(lista.getResponse().getContentAsString() + senha.getResponse().getContentAsString())
                .doesNotContain(raw, access, tokenHash, SENHA, nova, u.getSenha(), novoHash);
        assertThat(output.getAll()).doesNotContain(raw, access, tokenHash, SENHA, nova, u.getSenha(), novoHash);
    }

    private Usuario usuario() {
        Usuario u = com.portifolio.support.OfficialSchemaFixtures.usuario();
        u.setNome("Usuário de sessões"); u.setEmail(u.getUsername() + "@sessoes.test");
        u.setDataNascimento(java.time.LocalDate.of(1990, 1, 1)); u.setTelefone("11999999999");
        u.setTipoUsuario(com.portifolio.model.enums.TipoUsuario.ARTISTA);
        u.setDataCriacao(LocalDateTime.now()); u.setPerfilCompleto(false);
        u.setSenha(encoder.encode(SENHA));
        u.setStatusConta(com.portifolio.model.enums.StatusConta.ATIVA); u.setEmailVerificado(true);
        return usuarios.saveAndFlush(u);
    }
    private MvcResult login(Usuario u, boolean remember) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", SENHA, "rememberMe", remember))))
                .andExpect(status().isOk()).andReturn();
    }
    private org.springframework.test.web.servlet.ResultActions alterar(MvcResult a, String atual, String nova) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(); body.put("senhaAtual", atual); body.put("novaSenha", nova);
        return mvc.perform(put("/api/auth/senha").header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    private org.springframework.test.web.servlet.ResultActions refresh(String raw) throws Exception {
        var req = post("/api/auth/refresh"); if (raw != null) req.cookie(new Cookie(SessionCookiePolicy.NAME, raw));
        return mvc.perform(req);
    }
    private String cookie(MvcResult r) { return r.getResponse().getHeader("Set-Cookie").split(";", 2)[0].substring("palco_refresh=".length()); }
    private Cookie refreshCookie(MvcResult r) { return new Cookie(SessionCookiePolicy.NAME, cookie(r)); }
    private JsonNode corpo(MvcResult r) throws Exception { return json.readTree(r.getResponse().getContentAsString()); }
    private String bearer(MvcResult r) throws Exception { return "Bearer " + corpo(r).get("token").textValue(); }
    private void protegida(MvcResult r, int status) throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(r))).andExpect(status().is(status));
    }
    private void assertCookieApagado(MvcResult r) {
        assertThat(r.getResponse().getHeader("Set-Cookie"))
                .contains("palco_refresh=;", "Max-Age=0", "HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth");
    }
    private String hash(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
