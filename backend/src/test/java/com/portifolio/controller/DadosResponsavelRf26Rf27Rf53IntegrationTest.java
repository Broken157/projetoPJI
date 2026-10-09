package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.dto.GuardianDecisionRequest;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.service.PasswordRecoveryEmailSender;
import com.portifolio.support.EmailVerificationTestConfig;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class DadosResponsavelRf26Rf27Rf53IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired EmailVerificationTestConfig.CapturingEmailVerificationSender emails;
    @Autowired EmailVerificationTestConfig.CapturingGuardianConsentSender convites;
    @MockitoBean PasswordRecoveryEmailSender recovery;
    private final ObjectMapper json = new ObjectMapper();
    private static final String SENHA = "Palco@2026";

    @AfterEach void limpar() { emails.limpar(); convites.limpar(); }

    @Test void rotasFinalisticasExigemJWT() throws Exception {
        mvc.perform(put("/api/usuarios/me/telefone").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("telefone", "11999999999")))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/responsavel")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "123", "09999999999", "abcdefghijk", "+1 1234567890", "11 99999-9999 x", "119999999999999999999"})
    void telefoneInvalidoNaoPersiste(String telefone) throws Exception {
        Conta a = conta(25, "ARTISTA"); String original = a.usuario().getTelefone();
        mvc.perform(put("/api/usuarios/me/telefone").header("Authorization", a.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("telefone", telefone))))
                .andExpect(status().isBadRequest());
        assertThat(relido(a).getTelefone()).isEqualTo(original);
        assertThat(relido(a).getResponsavelLegal()).isNull();
    }

    @ParameterizedTest @ValueSource(strings = {"11999999999", "1133334444", "(11) 99999-9999", "+55 (11) 99999-9999", "55 11 99999-9999"})
    void telefoneValidoUsaFormatoDoCadastroSemInventarVerificacao(String telefone) throws Exception {
        Conta a = conta(25, "ARTISTA");
        mvc.perform(put("/api/usuarios/me/telefone").header("Authorization", a.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("telefone", telefone))))
                .andExpect(status().isNoContent());
        assertThat(relido(a).getTelefone()).isEqualTo(telefone);
        assertThat(relido(a).getStatusConta()).isEqualTo(StatusConta.ATIVA);
        assertThat(relido(a).getResponsavelLegal()).isNull();
    }

    @Test void ownerDoTelefoneVemDoJWTNaoDoPayload() throws Exception {
        Conta a = conta(25, "ARTISTA"), b = conta(25, "ARTISTA");
        mvc.perform(put("/api/usuarios/me/telefone").header("Authorization", a.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("telefone", "11888887777", "usuarioId", b.usuario().getId()))))
                .andExpect(status().isNoContent());
        assertThat(relido(a).getTelefone()).isEqualTo("11888887777");
        assertThat(relido(b).getTelefone()).isEqualTo(b.usuario().getTelefone());
    }

    @ParameterizedTest @ValueSource(strings = {"me", "legado"})
    void putGenericoReutilizaValidacaoDoTelefone(String rota) throws Exception {
        Conta a = conta(25, "ARTISTA"); var p = dados(a, rota); p.put("telefone", "12345"); p.put("nome", "Não persistir");
        mvc.perform(put(rota(a, rota)).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(p))).andExpect(status().isBadRequest());
        assertThat(relido(a).getNome()).isEqualTo(a.usuario().getNome());
        assertThat(relido(a).getTelefone()).isEqualTo(a.usuario().getTelefone());
    }

    @ParameterizedTest @ValueSource(strings = {"me", "legado"})
    void trocaDiretaDeEmailNaoAlteraCredencialNemOutrosCampos(String rota) throws Exception {
        Conta a = conta(25, "ARTISTA"); String novo = "novo-" + UUID.randomUUID() + "@palco.test";
        var p = dados(a, rota); p.put("email", novo); p.put("nome", "Não persistir"); p.put("telefone", "11888887777");
        mvc.perform(put(rota(a, rota)).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(p))).andExpect(status().isUnprocessableEntity());
        assertThat(relido(a).getEmail()).isEqualTo(a.usuario().getEmail());
        assertThat(relido(a).getNome()).isEqualTo(a.usuario().getNome());
        assertThat(relido(a).getTelefone()).isEqualTo(a.usuario().getTelefone());
        assertThat(usuarios.findByEmail(novo)).isEmpty();
        login(a.usuario().getEmail()).andExpect(status().isOk());
        login(novo).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", novo)))).andExpect(status().isOk());
        verifyNoInteractions(recovery);
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", a.usuario().getEmail())))).andExpect(status().isOk());
        verify(recovery).enviarLinkRedefinicao(eq(a.usuario().getEmail()), anyString());
    }

    @ParameterizedTest @ValueSource(strings = {"me", "legado"})
    void emailDeTerceiroMesmoComCaixaDiferenteNaoEhAceito(String rota) throws Exception {
        Conta a = conta(25, "ARTISTA"), b = conta(25, "ARTISTA"); var p = dados(a, rota);
        p.put("email", b.usuario().getEmail().toUpperCase(java.util.Locale.ROOT));
        mvc.perform(put(rota(a, rota)).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(p))).andExpect(status().isConflict());
        assertThat(relido(a).getEmail()).isEqualTo(a.usuario().getEmail());
        assertThat(relido(b).getEmail()).isEqualTo(b.usuario().getEmail());
    }

    @Test void MesmoEmailEmCaixaDiferenteNaoReescreveSubjectConfiavel() throws Exception {
        Conta a = conta(25, "ARTISTA"); var p = dados(a, "me");
        p.put("email", a.usuario().getEmail().toUpperCase(java.util.Locale.ROOT));
        mvc.perform(put("/api/usuarios/me").header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(p))).andExpect(status().isOk());
        assertThat(relido(a).getEmail()).isEqualTo(a.usuario().getEmail());
        mvc.perform(get("/api/usuarios/me").header("Authorization", a.bearer())).andExpect(status().isOk());
    }

    @ParameterizedTest @ValueSource(strings = {"nomeResponsavel", "telefoneResponsavel", "emailResponsavel", "vinculoResponsavel"})
    void mudancaRelevanteDoResponsavelFicaContidaSemDestruirAutorizacao(String campo) throws Exception {
        Conta a = conta(16, "ARTISTA");
        for (String caminho : new String[]{"me", "legado"}) {
            var p = dados(a, caminho);
            p.put(campo, switch(campo) { case "telefoneResponsavel" -> "11888887777";
                case "emailResponsavel" -> "novo-responsavel@palco.test"; case "vinculoResponsavel" -> "Tutor";
                default -> "Pessoa substituta"; });
            p.put("nome", "Não persistir");
            mvc.perform(put(rota(a, caminho)).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                    .content(body(p))).andExpect(status().isUnprocessableEntity());
            var r = relido(a).getResponsavelLegal(); var antigo = a.usuario().getResponsavelLegal();
            assertThat(r.getNomeResponsavel()).isEqualTo(antigo.getNomeResponsavel());
            assertThat(r.getEmailResponsavel()).isEqualTo(antigo.getEmailResponsavel());
            assertThat(r.getTelefoneResponsavel()).isEqualTo(antigo.getTelefoneResponsavel());
            assertThat(r.getDataConsentimento()).isEqualTo(antigo.getDataConsentimento());
            assertThat(r.getConsentimentoRevogado()).isFalse();
            assertThat(relido(a).getNome()).isEqualTo(a.usuario().getNome());
        }
    }

    @Test void camposIguaisDoClienteLegadoNaoReautorizamNemReenviamConvite() throws Exception {
        Conta a = conta(16, "ARTISTA"); var r = a.usuario().getResponsavelLegal(); var p = dados(a, "me");
        p.put("nomeResponsavel", r.getNomeResponsavel()); p.put("telefoneResponsavel", r.getTelefoneResponsavel());
        p.put("emailResponsavel", r.getEmailResponsavel()); String antigo = convites.token(r.getEmailResponsavel());
        mvc.perform(put("/api/usuarios/me").header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(p))).andExpect(status().isOk());
        assertThat(relido(a).getResponsavelLegal().getDataConsentimento()).isEqualTo(r.getDataConsentimento());
        assertThat(convites.token(r.getEmailResponsavel())).isEqualTo(antigo);
    }

    @Test void consultaPrivadaNaoAceitaOwnerAlheioNemRetornaToken(CapturedOutput output) throws Exception {
        Conta a = conta(16, "ARTISTA"), b = conta(16, "ARTISTA"); var r = a.usuario().getResponsavelLegal();
        String resultado = mvc.perform(get("/api/usuarios/me/responsavel").param("usuarioId", b.usuario().getId().toString())
                .header("Authorization", a.bearer())).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.email").value(r.getEmailResponsavel()))
                .andExpect(jsonPath("$.token").doesNotExist()).andExpect(jsonPath("$.tokenConsentimento").doesNotExist())
                .andExpect(jsonPath("$.cpf").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThat(resultado).doesNotContain(b.usuario().getEmailResponsavel(), convites.token(r.getEmailResponsavel()));
        assertThat(output.getAll()).doesNotContain(r.getEmailResponsavel(), r.getTelefoneResponsavel(), convites.token(r.getEmailResponsavel()));
        var p = dados(b, "legado"); p.put("nomeResponsavel", "Ataque");
        mvc.perform(put(rota(b, "legado")).header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(p))).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/{id}", b.usuario().getId()).header("Authorization", a.bearer()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest @ValueSource(strings = {"ARTISTA", "CONTRATANTE"})
    void adultoNaoAdicionaResponsavelLegalPelasConfiguracoes(String tipo) throws Exception {
        Conta a = conta(25, tipo);
        mvc.perform(get("/api/usuarios/me/responsavel").header("Authorization", a.bearer()))
                .andExpect(status().isUnprocessableEntity());
        var p = dados(a, "me"); p.put("nomeResponsavel", "Outra pessoa");
        mvc.perform(put("/api/usuarios/me").header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(p))).andExpect(status().isUnprocessableEntity());
        assertThat(relido(a).getResponsavelLegal()).isNull();
    }

    @Test void estadoAtualPendenteBloqueiaJwtERefreshAntigosSemReabrirD15() throws Exception {
        Conta a = conta(16, "ARTISTA"); var u = relido(a); u.setStatusConta(StatusConta.PENDENTE_CONSENTIMENTO);
        usuarios.saveAndFlush(u); // DML do estado oficial: não simula implementação de revalidação bloqueada.
        mvc.perform(get("/api/usuarios/me").header("Authorization", a.bearer())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me/responsavel").header("Authorization", a.bearer())).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/usuarios/me/telefone").header("Authorization", a.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("telefone", "11888887777")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("palco_refresh", a.refresh())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.token").doesNotExist());
        login(u.getEmail()).andExpect(status().isForbidden());
    }

    @Test void responsavelEContatoPrivadoNaoAparecemNoPerfilOuDescoberta() throws Exception {
        Conta a = conta(16, "ARTISTA");
        for (String url : new String[]{"/api/perfis/publicos/ARTISTA/" + a.usuario().getId(), "/api/perfis/publicos?q=" + a.usuario().getUsername()}) {
            String resposta = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(resposta).doesNotContain(a.usuario().getEmail(), a.usuario().getTelefone(), a.usuario().getEmailResponsavel(),
                    "nomeResponsavel", "telefoneResponsavel", "vinculoResponsavel", "tokenConsentimento", "dataNascimento", "nivelExperiencia");
        }
    }

    @Test void tokenNaoApareceNoToStringDoDTO() {
        assertThat(new GuardianDecisionRequest("segredo-finalistico", GuardianDecisionRequest.Decision.AUTORIZAR).toString())
                .doesNotContain("segredo-finalistico");
    }

    @Test void sessaoDeANaoReenviaConviteDeB() throws Exception {
        Conta a = conta(25, "ARTISTA"), b = conta(16, "ARTISTA");
        String anterior = convites.token(b.usuario().getEmailResponsavel());
        mvc.perform(post("/api/auth/resend-guardian-invite").header("Authorization", a.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("email", b.usuario().getEmail()))))
                .andExpect(status().isForbidden());
        assertThat(convites.token(b.usuario().getEmailResponsavel())).isEqualTo(anterior);
    }

    private Conta conta(int idade, String tipo) throws Exception {
        var p = new HashMap<String,Object>(); com.portifolio.support.CadastroFixtures.identificar(p);
        String email = "dados-" + UUID.randomUUID() + "@palco.test", responsavel = "resp-" + UUID.randomUUID() + "@palco.test";
        p.put("nome", "Pessoa de configurações"); p.put("email", email); p.put("senha", SENHA);
        p.put("dataNascimento", LocalDate.now().minusYears(idade).toString()); p.put("telefone", "11999999999"); p.put("tipoUsuario", tipo);
        if (tipo.equals("ARTISTA")) { p.put("tipoPerfilArtistico", "ARTISTA_SOLO"); p.put("areaPrincipalId", 1); }
        else p.put("tipoPerfilContratante", "Pessoa Física");
        if (idade < 18) { p.put("nomeResponsavel", "Responsável privado");
            p.put("telefoneResponsavel", "119" + String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000_000)));
            p.put("emailResponsavel", responsavel); }
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(body(p))).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/confirm-email").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("token", emails.token(email)))))
                .andExpect(status().isOk());
        if (idade < 18) mvc.perform(post("/api/auth/guardian-decision").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("token", convites.token(responsavel), "decisao", "AUTORIZAR")))).andExpect(status().isOk());
        MvcResult sessao = login(email).andExpect(status().isOk()).andReturn();
        String raw = sessao.getResponse().getHeader("Set-Cookie").split(";", 2)[0].substring("palco_refresh=".length());
        return new Conta(usuarios.findByEmail(email).orElseThrow(), "Bearer " + json.readTree(sessao.getResponse().getContentAsString()).get("token").asText(), raw);
    }
    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", email, "senha", SENHA, "rememberMe", true))));
    }
    private Map<String,Object> dados(Conta a, String rota) {
        var p = new HashMap<String,Object>(); p.put("nome", a.usuario().getNome()); p.put("telefone", a.usuario().getTelefone()); p.put("email", a.usuario().getEmail());
        if (rota.equals("legado")) { p.put("dataNascimento", a.usuario().getDataNascimento().toString()); p.put("tipoUsuario", a.usuario().getTipoUsuario().name()); }
        return p;
    }
    private String rota(Conta a, String tipo) { return tipo.equals("me") ? "/api/usuarios/me" : "/api/usuarios/" + a.usuario().getId(); }
    private Usuario relido(Conta a) { return usuarios.findById(a.usuario().getId()).orElseThrow(); }
    private String body(Object p) throws Exception { return json.writeValueAsString(p); }
    private record Conta(Usuario usuario, String bearer, String refresh) {}
}
