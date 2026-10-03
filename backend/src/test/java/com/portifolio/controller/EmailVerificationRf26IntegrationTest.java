package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.service.EmailVerificationService;
import com.portifolio.support.EmailVerificationTestConfig;
import com.portifolio.support.EmailVerificationTestConfig.CapturingEmailVerificationSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(EmailVerificationTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class EmailVerificationRf26IntegrationTest {
    private static final String SENHA = "Palco@2026";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CapturingEmailVerificationSender sender;
    @Autowired EmailVerificationTestConfig.CapturingGuardianConsentSender guardianSender;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired com.portifolio.security.JwtService jwtService;
    @Autowired com.portifolio.service.RefreshTokenService refreshTokenService;
    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    void limparSender() {
        sender.limpar();
        guardianSender.limpar();
    }

    @Test
    void cadastroAdultoGeraHashNaoDaSessaoEConfirmacaoAtivaUmaVez(CapturedOutput output) throws Exception {
        for (String tipo : new String[]{"ARTISTA", "CONTRATANTE"}) {
            String email = cadastrar(tipo, 25);
            Usuario pendente = usuarios.findByEmail(email).orElseThrow();
            String token = sender.token(email);
            assertThat(pendente.getStatusConta()).isEqualTo(StatusConta.PENDENTE_VERIFICACAO_EMAIL);
            assertThat(pendente.getEmailVerificado()).isFalse();
            assertThat(token).hasSize(43);
            assertThat(pendente.getTokenVerificacao()).isEqualTo(hash(token)).isNotEqualTo(token);
            assertThat(pendente.getUltimoReenvioVerificacao()).isNotNull();
            assertThat(passwordEncoder.matches(SENHA, pendente.getSenha())).isTrue();

            login(email).andExpect(status().isForbidden()).andExpect(jsonPath("$.token").doesNotExist());
            String jwtAnterior = jwtService.gerarToken(pendente);
            String refreshAnterior = refreshTokenService.gerarRefreshToken(pendente);
            mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + jwtAnterior))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("palco_refresh", refreshAnterior)))
                    .andExpect(status().isForbidden());

            MvcResult confirmado = confirmar(token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensagem").exists())
                    .andExpect(jsonPath("$.token").doesNotExist()).andReturn();
            Usuario ativo = usuarios.findById(pendente.getId()).orElseThrow();
            assertThat(ativo.getStatusConta()).isEqualTo(StatusConta.ATIVA);
            assertThat(ativo.getEmailVerificado()).isTrue();
            assertThat(ativo.getTokenVerificacao()).isNull();
            assertThat(passwordEncoder.matches(SENHA, ativo.getSenha())).isTrue();
            assertThat(confirmado.getResponse().getContentAsString()).doesNotContain(token, hash(token));
            confirmar(token).andExpect(status().isNotFound());
            login(email).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
            assertThat(output.getAll()).doesNotContain(token);
        }
    }

    @Test
    void artistaMenorVaiParaConsentimentoSemAcessoNormal() throws Exception {
        String email = cadastrar("ARTISTA", 16);
        String token = sender.token(email);
        confirmar(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value(
                        "E-mail confirmado. A conta aguarda o consentimento do responsável."));
        Usuario menor = usuarios.findByEmail(email).orElseThrow();
        assertThat(menor.getEmailVerificado()).isTrue();
        assertThat(menor.getStatusConta()).isEqualTo(StatusConta.PENDENTE_CONSENTIMENTO);
        login(email).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(menor)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contratanteMenorNaoPodeSerCadastradoNemAtivado() throws Exception {
        Map<String, Object> payload = cadastro("CONTRATANTE", 16);
        String email = (String) payload.get("email");
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.findByEmail(email)).isEmpty();
        assertThat(sender.token(email)).isNull();
    }

    @Test
    void tokenInvalidoExpiradoEBloqueadoNaoAtivam() throws Exception {
        String email = cadastrar("ARTISTA", 25);
        String token = sender.token(email);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        confirmar("token-inexistente").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        usuario.setUltimoReenvioVerificacao(LocalDateTime.now().minusHours(2));
        usuarios.saveAndFlush(usuario);
        confirmar(token).andExpect(status().isGone()).andExpect(jsonPath("$.status").value(410));
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getStatusConta())
                .isEqualTo(StatusConta.PENDENTE_VERIFICACAO_EMAIL);

        usuario.setUltimoReenvioVerificacao(LocalDateTime.now());
        usuario.setStatusConta(StatusConta.BLOQUEADA);
        usuarios.saveAndFlush(usuario);
        confirmar(token).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        Usuario bloqueado = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(bloqueado.getStatusConta()).isEqualTo(StatusConta.BLOQUEADA);
        assertThat(bloqueado.getEmailVerificado()).isFalse();
    }

    @Test
    void reenvioSubstituiTokenERespostaNaoEnumeraContas() throws Exception {
        String email = cadastrar("ARTISTA", 25);
        String primeiro = sender.token(email);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setUltimoReenvioVerificacao(LocalDateTime.now().minusMinutes(2));
        usuarios.saveAndFlush(usuario);

        String conhecido = reenviar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value(EmailVerificationService.MENSAGEM_REENVIO))
                .andReturn().getResponse().getContentAsString();
        String desconhecido = reenviar("ausente-" + UUID.randomUUID() + "@palco.test")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(conhecido).isEqualTo(desconhecido).doesNotContain(primeiro, email);
        String segundo = sender.token(email);
        assertThat(segundo).isNotEqualTo(primeiro);
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getTokenVerificacao())
                .isEqualTo(hash(segundo));
        confirmar(primeiro).andExpect(status().isNotFound());
        confirmar(segundo).andExpect(status().isOk());
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getTentativasVerificacaoEmail())
                .isEqualTo(1);
    }

    @Test
    void limitacaoDeReenvioIndependeDeExistenciaDaConta() throws Exception {
        String conhecido = cadastrar("ARTISTA", 25);
        String desconhecido = "ausente-" + UUID.randomUUID() + "@palco.test";
        reenviar(conhecido).andExpect(status().isOk());
        reenviar(desconhecido).andExpect(status().isOk());
        reenviar(conhecido).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
        reenviar(desconhecido).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void cotaPersistidaDeCincoReenviosNaoSubstituiToken() throws Exception {
        String email = cadastrar("ARTISTA", 25);
        String token = sender.token(email);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setTentativasVerificacaoEmail(5);
        usuario.setUltimoReenvioVerificacao(LocalDateTime.now().minusMinutes(2));
        usuarios.saveAndFlush(usuario);

        reenviar(email).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value(EmailVerificationService.MENSAGEM_REENVIO));
        Usuario depois = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(depois.getTokenVerificacao()).isEqualTo(hash(token));
        assertThat(depois.getTentativasVerificacaoEmail()).isEqualTo(5);
        confirmar(token).andExpect(status().isOk());
    }

    @Test
    void contaJaConfirmadaNaoRecebeNovoTokenNoReenvio() throws Exception {
        String email = cadastrar("ARTISTA", 25);
        String token = sender.token(email);
        confirmar(token).andExpect(status().isOk());
        String resposta = reenviar(email).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(resposta).contains(EmailVerificationService.MENSAGEM_REENVIO)
                .doesNotContain(email, token);
        assertThat(usuarios.findByEmail(email).orElseThrow().getTokenVerificacao()).isNull();
        assertThat(sender.token(email)).isEqualTo(token);
    }

    @Test
    void linkPublicoAbreFrontendSemExigirSessao() throws Exception {
        mvc.perform(get("/confirmar-email"))
                .andExpect(status().isOk());
    }

    @Test
    void falhaDeEntregaNoCadastroFazRollbackSemVazarToken(CapturedOutput output) throws Exception {
        sender.falhar(true);
        Map<String, Object> payload = cadastro("ARTISTA", 25);
        String email = (String) payload.get("email");
        MvcResult resultado = mvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isServiceUnavailable()).andReturn();
        assertThat(usuarios.findByEmail(email)).isEmpty();
        assertThat(sender.token(email)).isNotBlank();
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain(sender.token(email));
        assertThat(output.getAll()).doesNotContain(sender.token(email));
    }

    @Test
    void falhaDeReenvioPreservaLinkAnterior() throws Exception {
        String email = cadastrar("ARTISTA", 25);
        String primeiro = sender.token(email);
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        usuario.setUltimoReenvioVerificacao(LocalDateTime.now().minusMinutes(2));
        usuarios.saveAndFlush(usuario);
        sender.falhar(true);
        reenviar(email).andExpect(status().isOk());
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getTokenVerificacao())
                .isEqualTo(hash(primeiro));
        confirmar(primeiro).andExpect(status().isOk());
    }

    private String cadastrar(String tipo, int idade) throws Exception {
        Map<String, Object> payload = cadastro(tipo, idade);
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.token").doesNotExist());
        return (String) payload.get("email");
    }

    private Map<String, Object> cadastro(String tipo, int idade) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nome", "Pessoa RF26");
        com.portifolio.support.CadastroFixtures.identificar(payload);
        payload.put("dataNascimento", LocalDate.now().minusYears(idade).toString());
        payload.put("telefone", "11999999999");
        payload.put("email", "rf26-" + UUID.randomUUID() + "@palco.test");
        payload.put("senha", SENHA);
        payload.put("tipoUsuario", tipo);
        if (tipo.equals("ARTISTA")) {
            payload.put("tipoPerfilArtistico", "ARTISTA_SOLO");
            payload.put("areaPrincipalId", 1);
        } else payload.put("tipoPerfilContratante", "Pessoa Física");
        if (idade < 18) {
            payload.put("nomeResponsavel", "Responsável");
            payload.put("telefoneResponsavel", "11988887777");
            payload.put("emailResponsavel", "responsavel@palco.test");
        }
        return payload;
    }

    private org.springframework.test.web.servlet.ResultActions confirmar(String token) throws Exception {
        return mvc.perform(post("/api/auth/confirm-email").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("token", token))));
    }

    private org.springframework.test.web.servlet.ResultActions reenviar(String email) throws Exception {
        return mvc.perform(post("/api/auth/resend-confirmation").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "senha", SENHA, "rememberMe", false))));
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
