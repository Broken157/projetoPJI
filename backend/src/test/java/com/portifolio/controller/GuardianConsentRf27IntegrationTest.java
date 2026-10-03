package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.dto.GuardianDecisionRequest;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.repository.ResponsavelLegalRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.service.GuardianConsentService;
import com.portifolio.service.google.GoogleTokenClaims;
import com.portifolio.service.google.GoogleTokenVerifier;
import com.portifolio.support.EmailVerificationTestConfig;
import com.portifolio.support.EmailVerificationTestConfig.CapturingEmailVerificationSender;
import com.portifolio.support.EmailVerificationTestConfig.CapturingGuardianConsentSender;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import({EmailVerificationTestConfig.class, GuardianConsentRf27IntegrationTest.GoogleTestConfig.class})
class GuardianConsentRf27IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ResponsavelLegalRepository responsaveis;
    @Autowired com.portifolio.repository.PerfilArtistaRepository perfis;
    @Autowired GuardianConsentService consentimento;
    @Autowired CapturingEmailVerificationSender emailSender;
    @Autowired CapturingGuardianConsentSender guardianSender;
    @Autowired FakeGoogleVerifier googleVerifier;
    @Autowired com.portifolio.security.JwtService jwtService;
    @Autowired com.portifolio.service.RefreshTokenService refreshTokens;
    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach void limpar() { emailSender.limpar(); guardianSender.limpar(); googleVerifier.tokens.clear(); }

    @Test
    void convencionalSoAtivaDepoisDaDecisaoRealEPublicaSomenteDadosPermitidos() throws Exception {
        Conta conta = cadastrar(16);
        Usuario antes = usuarios.findByEmail(conta.email()).orElseThrow();
        assertThat(antes.getStatusConta()).isEqualTo(StatusConta.PENDENTE_VERIFICACAO_EMAIL);
        assertThat(guardianSender.token(conta.responsavel())).isNull();
        confirmarEmail(conta);
        Usuario pendente = usuarios.findByEmail(conta.email()).orElseThrow();
        assertThat(pendente.getEmailVerificado()).isTrue();
        assertThat(pendente.getStatusConta()).isEqualTo(StatusConta.PENDENTE_CONSENTIMENTO);
        String token = guardianSender.token(conta.responsavel());
        assertThat(token).hasSize(43);
        assertThat(pendente.getResponsavelLegal().getTokenConsentimento())
                .startsWith("v1:").doesNotContain(token, conta.email());
        mvc.perform(post("/api/auth/guardian-invite").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("token", token))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeArtista").value("Pessoa RF27"))
                .andExpect(jsonPath("$.emailResponsavel").doesNotExist());
        mvc.perform(get("/consentimento-responsavel")).andExpect(status().isOk());
        login(conta.email()).andExpect(status().isForbidden()).andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/api/perfis/publicos/ARTISTA/{id}", pendente.getId()))
                .andExpect(status().isNotFound());
        String jwtAntigo = jwtService.gerarToken(pendente);
        String refreshAntigo = refreshTokens.gerarRefreshToken(pendente);
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + jwtAntigo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("palco_refresh", refreshAntigo)))
                .andExpect(status().isForbidden());
        decidir(token, "AUTORIZAR").andExpect(status().isOk());
        Usuario ativo = usuarios.findById(pendente.getId()).orElseThrow();
        assertThat(ativo.getStatusConta()).isEqualTo(StatusConta.ATIVA);
        assertThat(ativo.getPerfilCompleto()).isFalse();
        assertThat(ativo.getResponsavelLegal().getDataConsentimento()).isNotNull();
        assertThat(ativo.getResponsavelLegal().getConsentimentoRevogado()).isFalse();
        assertThat(ativo.getResponsavelLegal().getTokenConsentimento()).isNull();
        decidir(token, "RECUSAR").andExpect(status().isNotFound());
        login(conta.email()).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/api/perfis/publicos/ARTISTA/{id}", ativo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(ativo.getId()))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andExpect(jsonPath("$.dataNascimento").doesNotExist())
                .andExpect(jsonPath("$.consentimento").doesNotExist())
                .andExpect(jsonPath("$.emailResponsavel").doesNotExist())
                .andExpect(jsonPath("$.experiencia").doesNotExist());
    }

    @Test
    void recusaRegistraDecisaoBloqueiaContaEImpedeReenvio() throws Exception {
        Conta conta = cadastrar(15);
        confirmarEmail(conta);
        String token = guardianSender.token(conta.responsavel());
        decidir(token, "RECUSAR").andExpect(status().isOk());
        Usuario recusado = usuarios.findByEmail(conta.email()).orElseThrow();
        assertThat(recusado.getStatusConta()).isEqualTo(StatusConta.BLOQUEADA);
        assertThat(recusado.getResponsavelLegal().getConsentimentoRevogado()).isTrue();
        assertThat(recusado.getResponsavelLegal().getDataConsentimento()).isNotNull();
        assertThat(recusado.getResponsavelLegal().getTokenConsentimento()).isNull();
        decidir(token, "AUTORIZAR").andExpect(status().isNotFound());
        reenviar(conta.email()).andExpect(status().isOk());
        assertThat(guardianSender.token(conta.responsavel())).isEqualTo(token);
        login(conta.email()).andExpect(status().isForbidden());
    }

    @Test
    void tokenInvalidoExpiradoReenvioEAntiEnumeracao() throws Exception {
        Conta conta = cadastrar(16);
        confirmarEmail(conta);
        String primeiro = guardianSender.token(conta.responsavel());
        decidir("token-inexistente", "AUTORIZAR").andExpect(status().isNotFound());
        ResponsavelLegal r = usuarios.findByEmail(conta.email()).orElseThrow().getResponsavelLegal();
        String[] partes = r.getTokenConsentimento().split(":");
        r.setTokenConsentimento(partes[0] + ":" + partes[1] + ":"
                + (Long.parseLong(partes[2]) - 90_000) + ":" + partes[3]);
        responsaveis.saveAndFlush(r);
        decidir(primeiro, "AUTORIZAR").andExpect(status().isGone());
        String conhecida = reenviar(conta.email()).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        String desconhecida = reenviar("ausente-" + UUID.randomUUID() + "@palco.test")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(conhecida).isEqualTo(desconhecida).doesNotContain(conta.email(), conta.responsavel());
        String segundo = guardianSender.token(conta.responsavel());
        assertThat(segundo).isNotEqualTo(primeiro);
        decidir(primeiro, "RECUSAR").andExpect(status().isNotFound());
        reenviar(conta.email()).andExpect(status().isTooManyRequests());
        decidir(segundo, "AUTORIZAR").andExpect(status().isOk());
    }

    @Test
    void falhaDeSmtpPreservaEmailConfirmadoEPermiteTentarDepois() throws Exception {
        Conta conta = cadastrar(16);
        guardianSender.falhar(true);
        confirmarEmail(conta);
        Usuario pendente = usuarios.findByEmail(conta.email()).orElseThrow();
        assertThat(pendente.getEmailVerificado()).isTrue();
        assertThat(pendente.getStatusConta()).isEqualTo(StatusConta.PENDENTE_CONSENTIMENTO);
        assertThat(pendente.getResponsavelLegal().getTokenConsentimento()).isNull();
        login(conta.email()).andExpect(status().isForbidden());
        guardianSender.falhar(false);
        reenviar(conta.email()).andExpect(status().isOk());
        assertThat(guardianSender.token(conta.responsavel())).isNotBlank();
    }

    @Test
    void emailIgualNoBackendEAdultoForaDoConsentimento() throws Exception {
        Map<String,Object> menor = payload(16);
        menor.put("emailResponsavel", menor.get("email"));
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(menor)))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.findByEmail((String) menor.get("email"))).isEmpty();
        Conta adulto = cadastrar(20);
        confirmarEmail(adulto);
        assertThat(usuarios.findByEmail(adulto.email()).orElseThrow().getStatusConta())
                .isEqualTo(StatusConta.ATIVA);
        assertThat(guardianSender.token(adulto.responsavel())).isNull();
    }

    @Test
    void googleMenorCompletoNaoRecebeSessaoAteAutorizacao() throws Exception {
        String idToken = UUID.randomUUID().toString();
        String email = "google-rf27-" + UUID.randomUUID() + "@palco.test";
        String guardian = "responsavel-" + UUID.randomUUID() + "@palco.test";
        googleVerifier.tokens.put(idToken, new GoogleTokenClaims("sub-" + idToken,email,"Pessoa Google",null));
        Map<String,Object> dados = new HashMap<>();
        dados.put("idToken", idToken);
        com.portifolio.support.CadastroFixtures.identificar(dados);
        dados.put("tipoUsuario", "ARTISTA");
        dados.put("dataNascimento", LocalDate.now().minusYears(16).toString());
        dados.put("telefone", "11999999999");
        dados.put("tipoPerfilArtistico", "ARTISTA_SOLO");
        dados.put("areaPrincipalId", 1);
        dados.put("nomeResponsavel", "Responsável Google");
        dados.put("telefoneResponsavel", "11988887777");
        dados.put("emailResponsavel", guardian);
        iniciarConclusaoGoogle(dados, idToken);
        mvc.perform(post("/api/auth/google/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(dados)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statusConta").value("PENDENTE_CONSENTIMENTO"))
                .andExpect(jsonPath("$.token").isEmpty());
        Usuario pendente = usuarios.findByEmail(email).orElseThrow();
        assertThat(pendente.getEmailVerificado()).isTrue();
        assertThat(pendente.getResponsavelLegal().getEmailResponsavel()).isEqualTo(guardian);
        guardianSender.limpar();
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idToken",idToken))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusConta").value("PENDENTE_CONSENTIMENTO"))
                .andExpect(jsonPath("$.token").isEmpty());
        assertThat(guardianSender.token(guardian)).isNull();
        String convite = pendente.getResponsavelLegal().getTokenConsentimento();
        // O convite não é recriado ao repetir o login Google.
        assertThat(usuarios.findByEmail(email).orElseThrow().getResponsavelLegal().getTokenConsentimento())
                .isEqualTo(convite);
        // Recupera o token bruto do primeiro envio por meio de um reenvio controlado.
        ResponsavelLegal r = pendente.getResponsavelLegal();
        String[] partes = r.getTokenConsentimento().split(":");
        r.setTokenConsentimento(partes[0] + ":" + partes[1] + ":"
                + (Long.parseLong(partes[2]) - 120) + ":" + partes[3]);
        responsaveis.saveAndFlush(r);
        reenviar(email).andExpect(status().isOk());
        decidir(guardianSender.token(guardian), "AUTORIZAR").andExpect(status().isOk());
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idToken",idToken))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTENTICADO"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void googleConcluiRf01SemPersistenciaProvisoriaAntesDoConvite() throws Exception {
        String idToken = UUID.randomUUID().toString();
        String email = "google-provisorio-" + UUID.randomUUID() + "@palco.test";
        String guardian = "responsavel-" + UUID.randomUUID() + "@palco.test";
        googleVerifier.tokens.put(idToken, new GoogleTokenClaims("sub-" + idToken,email,"Pessoa Google",null));
        Map<String,Object> dados = new HashMap<>();
        dados.put("idToken", idToken);
        dados.put("tipoUsuario", "ARTISTA");
        dados.put("dataNascimento", LocalDate.now().minusYears(16).toString());
        dados.put("telefone", "11999999999");
        iniciarConclusaoGoogle(dados, idToken);
        assertThat(usuarios.findByEmail(email)).isEmpty();
        assertThat(guardianSender.token(guardian)).isNull();
        com.portifolio.support.CadastroFixtures.identificar(dados);
        dados.put("tipoPerfilArtistico", "ARTISTA_SOLO");
        dados.put("areaPrincipalId", 1);
        dados.put("nomeResponsavel", "Responsável Google");
        dados.put("telefoneResponsavel", "11988887777");
        dados.put("emailResponsavel", guardian);
        mvc.perform(post("/api/auth/google/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(dados)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statusConta").value("PENDENTE_CONSENTIMENTO"))
                .andExpect(jsonPath("$.token").isEmpty());
        assertThat(guardianSender.token(guardian)).isNotBlank();
        decidir(guardianSender.token(guardian), "AUTORIZAR").andExpect(status().isOk());
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idToken",idToken))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void bloqueadaOuCadastroIncompletoNaoSaoAtivadosPeloLink() throws Exception {
        Conta conta = cadastrar(16);
        confirmarEmail(conta);
        String token = guardianSender.token(conta.responsavel());
        Usuario usuario = usuarios.findByEmail(conta.email()).orElseThrow();
        usuario.setStatusConta(StatusConta.BLOQUEADA);
        usuarios.saveAndFlush(usuario);
        decidir(token, "AUTORIZAR").andExpect(status().isConflict());
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getStatusConta())
                .isEqualTo(StatusConta.BLOQUEADA);
        usuario.setStatusConta(StatusConta.PENDENTE_CONSENTIMENTO);
        usuarios.saveAndFlush(usuario);
        perfis.deleteById(usuario.getId());
        decidir(token, "AUTORIZAR").andExpect(status().isConflict());
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getStatusConta())
                .isEqualTo(StatusConta.PENDENTE_CONSENTIMENTO);
    }

    @Test
    void duasDecisoesConcorrentesProduzemUmUnicoResultado() throws Exception {
        Conta conta = cadastrar(16);
        confirmarEmail(conta);
        String token = guardianSender.token(conta.responsavel());
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> autorizar = executor.submit(() -> decidirNaBarreira(inicio, token,
                    GuardianDecisionRequest.Decision.AUTORIZAR));
            Future<String> recusar = executor.submit(() -> decidirNaBarreira(inicio, token,
                    GuardianDecisionRequest.Decision.RECUSAR));
            inicio.countDown();
            String a = autorizar.get(20, TimeUnit.SECONDS);
            String b = recusar.get(20, TimeUnit.SECONDS);
            assertThat(java.util.List.of(a,b)).containsExactlyInAnyOrder("OK", "REPLAY");
            Usuario finalizado = usuarios.findByEmail(conta.email()).orElseThrow();
            assertThat(finalizado.getStatusConta()).isIn(StatusConta.ATIVA,StatusConta.BLOQUEADA);
            assertThat(finalizado.getResponsavelLegal().getTokenConsentimento()).isNull();
            assertThat(finalizado.getResponsavelLegal().getDataConsentimento()).isNotNull();
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    @Test
    void doisCliquesSimultaneosEmAutorizarConsomemOLinkUmaVez() throws Exception {
        Conta conta = cadastrar(16);
        confirmarEmail(conta);
        String token = guardianSender.token(conta.responsavel());
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> primeira = executor.submit(() -> decidirNaBarreira(inicio, token,
                    GuardianDecisionRequest.Decision.AUTORIZAR));
            Future<String> segunda = executor.submit(() -> decidirNaBarreira(inicio, token,
                    GuardianDecisionRequest.Decision.AUTORIZAR));
            inicio.countDown();
            assertThat(java.util.List.of(primeira.get(20, TimeUnit.SECONDS),
                    segunda.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "REPLAY");
            Usuario ativo = usuarios.findByEmail(conta.email()).orElseThrow();
            assertThat(ativo.getStatusConta()).isEqualTo(StatusConta.ATIVA);
            assertThat(ativo.getResponsavelLegal().getTokenConsentimento()).isNull();
        } finally { inicio.countDown(); executor.shutdownNow(); }
    }

    private String decidirNaBarreira(CountDownLatch inicio, String token,
            GuardianDecisionRequest.Decision decisao) throws Exception {
        inicio.await(10, TimeUnit.SECONDS);
        try { consentimento.decidir(new GuardianDecisionRequest(token, decisao)); return "OK"; }
        catch (ResourceNotFoundException ex) { return "REPLAY"; }
    }

    private Conta cadastrar(int idade) throws Exception {
        Map<String,Object> payload = payload(idade);
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated());
        return new Conta((String)payload.get("email"), (String)payload.getOrDefault("emailResponsavel", ""));
    }

    private void iniciarConclusaoGoogle(Map<String, Object> dados, String idToken) throws Exception {
        var resultado = mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("idToken", idToken))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isEmpty()).andReturn();
        dados.remove("idToken");
        dados.put("contexto", mapper.readTree(resultado.getResponse().getContentAsString()).get("contexto").asText());
    }

    private Map<String,Object> payload(int idade) {
        Map<String,Object> p = new HashMap<>();
        p.put("nome", "Pessoa RF27");
        com.portifolio.support.CadastroFixtures.identificar(p);
        p.put("dataNascimento", LocalDate.now().minusYears(idade).toString());
        p.put("telefone", "11999999999");
        p.put("email", "rf27-" + UUID.randomUUID() + "@palco.test");
        p.put("senha", "Palco@2026");
        p.put("tipoUsuario", "ARTISTA");
        p.put("tipoPerfilArtistico", "ARTISTA_SOLO");
        p.put("areaPrincipalId", 1);
        if (idade < 18) {
            p.put("nomeResponsavel", "Responsável RF27");
            p.put("telefoneResponsavel", "11988887777");
            p.put("emailResponsavel", "responsavel-" + UUID.randomUUID() + "@palco.test");
        }
        return p;
    }

    private void confirmarEmail(Conta conta) throws Exception {
        mvc.perform(post("/api/auth/confirm-email").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("token",emailSender.token(conta.email())))))
                .andExpect(status().isOk());
    }
    private org.springframework.test.web.servlet.ResultActions decidir(String token, String decisao) throws Exception {
        return mvc.perform(post("/api/auth/guardian-decision").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("token",token,"decisao",decisao))));
    }
    private org.springframework.test.web.servlet.ResultActions reenviar(String email) throws Exception {
        return mvc.perform(post("/api/auth/resend-guardian-invite").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email",email))));
    }
    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email",email,"senha","Palco@2026"))));
    }
    private String json(Object o) throws Exception { return mapper.writeValueAsString(o); }
    private record Conta(String email, String responsavel) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class GoogleTestConfig {
        @Bean @Primary FakeGoogleVerifier fakeGoogleVerifier() { return new FakeGoogleVerifier(); }
    }
    static class FakeGoogleVerifier implements GoogleTokenVerifier {
        private final Map<String,GoogleTokenClaims> tokens = new java.util.concurrent.ConcurrentHashMap<>();
        @Override public GoogleTokenClaims verificar(String idToken) {
            GoogleTokenClaims claims = tokens.get(idToken);
            if (claims == null) throw new com.portifolio.exception.UnauthorizedException(GoogleTokenVerifier.MENSAGEM_ERRO);
            return claims;
        }
    }
}
