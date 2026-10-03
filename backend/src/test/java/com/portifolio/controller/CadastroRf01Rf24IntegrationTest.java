package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.Usuario;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.service.google.GoogleTokenClaims;
import com.portifolio.support.CadastroFixtures;
import com.portifolio.support.EmailVerificationTestConfig;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = "app.vagas.auto-close.enabled=false")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({EmailVerificationTestConfig.class, GoogleAuthRf24HardeningIntegrationTest.GoogleVerifierTestConfig.class})
class CadastroRf01Rf24IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean UsuarioRepository usuarios;
    @Autowired GoogleAuthRf24HardeningIntegrationTest.FakeGoogleTokenVerifier google;
    @Autowired EmailVerificationTestConfig.CapturingEmailVerificationSender emailSender;
    @Autowired EmailVerificationTestConfig.CapturingGuardianConsentSender guardianSender;
    @MockitoSpyBean PerfilArtistaRepository artistas;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void limpar() {
        reset(artistas);
        reset(usuarios);
        google.limpar();
        emailSender.limpar();
        guardianSender.limpar();
        jdbc.execute("truncate table usuarios restart identity cascade");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "@diogo", "com espaco", "diogo-silva", "diogó", "abc\n", "1234567890123456789012345678901"})
    void usernameInvalidoNaoPersiste(String username) throws Exception {
        var dados = convencional();
        dados.put("username", username);
        enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void usernameAusenteOuNuloEObrigatorioNosDoisFluxos() throws Exception {
        var dados = convencional();
        dados.remove("username");
        enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
        dados.put("username", null);
        enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
        var conclusao = conclusao(false, null);
        conclusao.remove("username");
        enviar("/api/auth/google/cadastro", conclusao).andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void usernamePreservaCaixaELimitesEUnicidadeDoDatabase04() throws Exception {
        for (String username : new String[]{"D", "Diogo", "diogo", "Ab_" + "x".repeat(27), "._"}) {
            var dados = convencional();
            dados.put("username", username);
            enviar("/api/auth/cadastro", dados).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(username));
            assertThat(usuarios.findByEmail((String) dados.get("email")).orElseThrow().getUsername()).isEqualTo(username);
        }
        var repetido = convencional();
        repetido.put("username", "Diogo");
        enviar("/api/auth/cadastro", repetido).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Este username ja esta em uso."));
        assertThat(usuarios.count()).isEqualTo(5);
    }

    @Test
    void usernameNaoAutenticaESenhaNaoSofreTrim() throws Exception {
        var dados = convencional();
        dados.put("senha", " Palco@2026 ");
        enviar("/api/auth/cadastro", dados).andExpect(status().isCreated());
        enviar("/api/auth/confirm-email", Map.of("token", emailSender.token((String) dados.get("email"))))
                .andExpect(status().isOk());
        enviar("/api/auth/login", Map.of("email", dados.get("username"), "senha", dados.get("senha")))
                .andExpect(status().isBadRequest());
        enviar("/api/auth/login", Map.of("email", dados.get("email"), "senha", "Palco@2026"))
                .andExpect(status().isUnauthorized());
        enviar("/api/auth/login", Map.of("email", dados.get("email"), "senha", dados.get("senha")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cpf", "telefone", "dataNascimento", "tipoUsuario", "tipoPerfilArtistico", "areaPrincipalId"})
    void camposRf01ObrigatoriosNosDoisFluxos(String campo) throws Exception {
        var local = convencional();
        local.remove(campo);
        enviar("/api/auth/cadastro", local).andExpect(status().isBadRequest());
        var googleDados = conclusao(false, null);
        googleDados.remove(campo);
        enviar("/api/auth/google/cadastro", googleDados).andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void documentosInvalidosRepetidosOuDeFormatoInvalidoNaoPersistem() throws Exception {
        for (String cpf : new String[]{"11111111111", "12345678900", "123", "abc12345678900"}) {
            var dados = convencional();
            dados.put("cpf", cpf);
            enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
        }
        for (String cnpj : new String[]{"11111111111111", "12345678901234", "abc"}) {
            var dados = convencional();
            dados.put("cnpj", cnpj);
            enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
        }
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void cpfFormatadoEPuroSaoMesmoDocumentoPrivado() throws Exception {
        var dados = convencional();
        String cpf = (String) dados.get("cpf");
        dados.put("cpf", cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9));
        String body = enviar("/api/auth/cadastro", dados).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("cpf", "cnpj", "senha", "Responsavel", cpf, "Palco@2026", "$2");
        assertThat(usuarios.findByEmail((String) dados.get("email")).orElseThrow().getCpf()).isEqualTo(cpf);
        var repetido = convencional();
        repetido.put("cpf", cpf);
        enviar("/api/auth/cadastro", repetido).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "11999abc9999", "00000000000", "123456789012345678901"})
    void telefoneInvalidoNaoPersiste(String telefone) throws Exception {
        var dados = convencional();
        dados.put("telefone", telefone);
        enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void contratantePessoaFisicaOuEntidadeExigeDocumentoENomeAplicavel() throws Exception {
        for (String subtipo : new String[]{"PESSOA_FISICA", "SETOR_PUBLICO", "SETOR_PRIVADO", "ONG"}) {
            var dados = convencional();
            dados.put("tipoUsuario", "CONTRATANTE");
            dados.put("tipoPerfilContratante", subtipo);
            if (!subtipo.equals("PESSOA_FISICA")) {
                dados.remove("cpf");
                dados.put("cnpj", CadastroFixtures.cnpj());
                enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
                dados.put("nomeEntidade", "Entidade de teste");
            }
            enviar("/api/auth/cadastro", dados).andExpect(status().isCreated());
            Long usuarioId = usuarios.findByEmail((String) dados.get("email")).orElseThrow().getId();
            assertThat(jdbc.queryForObject("select tipo_contratante::text from perfis_contratantes where usuario_id=?", String.class, usuarioId))
                    .isEqualTo(subtipo);
            if (!subtipo.equals("PESSOA_FISICA")) {
                assertThat(jdbc.queryForObject("select nome_empresa from perfis_contratantes where usuario_id=?", String.class, usuarioId))
                        .isEqualTo("Entidade de teste");
            }
        }
    }

    @Test
    void artistaEmpresarialExigeAdultoECnpjUnico() throws Exception {
        for (String subtipo : new String[]{"ESTUDIO", "PRODUTORA_EMPRESA"}) {
            var dados = convencional();
            dados.put("tipoPerfilArtistico", subtipo);
            enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
            dados.remove("cpf");
            dados.put("cnpj", CadastroFixtures.cnpj());
            enviar("/api/auth/cadastro", dados).andExpect(status().isCreated());
            var repetido = convencional();
            repetido.put("tipoPerfilArtistico", subtipo);
            repetido.put("cnpj", dados.get("cnpj"));
            enviar("/api/auth/cadastro", repetido).andExpect(status().isConflict());
            repetido.put("cnpj", CadastroFixtures.cnpj());
            menor(repetido, 17);
            enviar("/api/auth/cadastro", repetido).andExpect(status().isBadRequest());
        }
    }

    @Test
    void limitesDeIdadeEPrivacidadeDoResponsavel() throws Exception {
        for (int idade : new int[]{13, 14, 17, 18}) {
            var dados = convencional();
            menor(dados, idade);
            var resultado = enviar("/api/auth/cadastro", dados).andExpect(status().is(idade < 14 ? 400 : 201));
            if (idade >= 14) {
                assertThat(resultado.andReturn().getResponse().getContentAsString())
                        .doesNotContain("Responsavel", "responsavel@palco.test", "11988887777", (String) dados.get("cpf"));
            }
            var contratante = convencional();
            contratante.put("tipoUsuario", "CONTRATANTE");
            contratante.put("tipoPerfilContratante", "PESSOA_FISICA");
            contratante.put("dataNascimento", LocalDate.now().minusYears(idade).toString());
            enviar("/api/auth/cadastro", contratante).andExpect(status().is(idade < 18 ? 400 : 201));
        }
    }

    @Test
    void emailDoResponsavelNaoPodeSerDoMenorMesmoComCaixaDiferente() throws Exception {
        var dados = convencional();
        menor(dados, 16);
        dados.put("emailResponsavel", ((String) dados.get("email")).toUpperCase(java.util.Locale.ROOT));
        enviar("/api/auth/cadastro", dados).andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void duasTentativasComMesmoUsernameProduzem201E409SemSqlNoCliente() throws Exception {
        var primeira = convencional();
        var segunda = convencional();
        segunda.put("username", primeira.get("username"));
        concorrentes("/api/auth/cadastro", primeira, segunda);
        assertThat(usuarios.count()).isOne();
        assertThat(artistas.count()).isOne();
    }

    @Test
    void constraintRealProtegeUsernameMesmoQuandoPreCheckNaoDetectaDuplicidade() throws Exception {
        var primeira = convencional();
        primeira.put("username", "username_ocupado");
        enviar("/api/auth/cadastro", primeira).andExpect(status().isCreated());
        doReturn(false).when(usuarios).existsByUsername("username_ocupado");
        var segunda = convencional();
        segunda.put("username", "username_ocupado");
        String resposta = enviar("/api/auth/cadastro", segunda).andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();
        assertThat(resposta).doesNotContain("23505", "constraint", "SQL", "usuarios_", (String) segunda.get("cpf"));
        assertThat(usuarios.count()).isOne();
        assertThat(artistas.count()).isOne();
        assertThat(emailSender.token((String) segunda.get("email"))).isNull();
    }

    @Test
    void googleVerificadoConcluiRf26LocalSemExigirRf08NemDuplicarConta() throws Exception {
        var dados = convencional();
        enviar("/api/auth/cadastro", dados).andExpect(status().isCreated());
        Usuario antes = usuarios.findByEmail((String) dados.get("email")).orElseThrow();
        google.aceitar("vincular-local", new GoogleTokenClaims("sub-local", antes.getEmail(), "Nome Google", null));
        var resultado = enviar("/api/auth/google", Map.of("idToken", "vincular-local"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTENTICADO"))
                .andExpect(jsonPath("$.id").value(antes.getId())).andExpect(jsonPath("$.perfilCompleto").value(false))
                .andReturn();
        Usuario depois = usuarios.findById(antes.getId()).orElseThrow();
        assertThat(depois.getSenha()).isEqualTo(antes.getSenha());
        assertThat(depois.getNome()).isEqualTo(antes.getNome());
        assertThat(depois.getEmailVerificado()).isTrue();
        assertThat(depois.getTokenVerificacao()).isNull();
        assertThat(usuarios.count()).isOne();
        String token = mapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void subExistenteComOutroEmailNaoConfirmaEmailDaContaLocal() throws Exception {
        var dados = convencional();
        enviar("/api/auth/cadastro", dados).andExpect(status().isCreated());
        Usuario usuario = usuarios.findByEmail((String) dados.get("email")).orElseThrow();
        usuario.setGoogleId("sub-vinculado");
        usuarios.saveAndFlush(usuario);
        google.aceitar("email-diferente", new GoogleTokenClaims("sub-vinculado", "outro@palco.test", "Pessoa", null));
        enviar("/api/auth/google", Map.of("idToken", "email-diferente")).andExpect(status().isUnauthorized());
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getEmailVerificado()).isFalse();
        assertThat(usuarios.count()).isOne();
    }

    @Test
    void googleAdultoCompletoSemSenhaSemRf26SemRaioComAvatarEJwtUtilizavel() throws Exception {
        var dados = conclusao(false, "https://img.example/avatar");
        MvcResult resultado = enviar("/api/auth/google/cadastro", dados).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AUTENTICADO"))
                .andExpect(jsonPath("$.statusConta").value("ATIVA"))
                .andExpect(jsonPath("$.avatarUrl").value("https://img.example/avatar"))
                .andExpect(jsonPath("$.perfilCompleto").value(false))
                .andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
        Usuario usuario = usuarios.findAll().getFirst();
        assertThat(usuario.getSenha()).isNull();
        assertThat(usuario.getUsername()).isEqualTo(dados.get("username"));
        assertThat(usuario.getEmailVerificado()).isTrue();
        assertThat(usuario.getTokenVerificacao()).isNull();
        assertThat(emailSender.token(usuario.getEmail())).isNull();
        assertThat(artistas.findById(usuario.getId()).orElseThrow().getRaioAtuacao()).isNull();
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens", Integer.class)).isZero();
        String token = mapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("cpf", "senha", (String) dados.get("cpf"));
    }

    @Test
    void googleSemFotoNaoBloqueiaEContratanteComRememberMeUsaCookieRf25() throws Exception {
        var dados = conclusao(true, null);
        dados.put("tipoUsuario", "CONTRATANTE");
        dados.put("tipoPerfilContratante", "SETOR_PRIVADO");
        dados.put("cnpj", CadastroFixtures.cnpj());
        dados.put("nomeEntidade", "Entidade Google");
        var resultado = enviar("/api/auth/google/cadastro", dados).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AUTENTICADO"))
                .andExpect(jsonPath("$.avatarUrl").isNotEmpty()).andReturn();
        assertThat(usuarios.findAll().getFirst().getFotoPerfil()).isNull();
        assertThat(resultado.getResponse().getHeader("Set-Cookie")).contains("HttpOnly", "Secure", "SameSite=Strict");
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens", Integer.class)).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "googleId", "sub", "emailVerified", "nome", "foto", "senha", "statusConta"})
    void conclusaoRejeitaIdentidadeOuEstadoEnviadosPeloCliente(String campo) throws Exception {
        var dados = conclusao(false, null);
        dados.put(campo, "identidade-adulterada");
        enviar("/api/auth/google/cadastro", dados).andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void contextoAdulteradoNaoValidaENaoServeComoSessao() throws Exception {
        var dados = conclusao(false, null);
        String contexto = (String) dados.get("contexto");
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + contexto)).andExpect(status().isUnauthorized());
        String[] partes = contexto.split("\\.");
        var claims = mapper.readTree(Base64.getUrlDecoder().decode(partes[1]));
        ((com.fasterxml.jackson.databind.node.ObjectNode) claims).put("email", "outra@palco.test");
        partes[1] = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(claims));
        dados.put("contexto", String.join(".", partes));
        enviar("/api/auth/google/cadastro", dados).andExpect(status().isUnauthorized());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void conclusaoGoogleUnicaMesmoComDoisContextosConcorrentesEReplay() throws Exception {
        String credencial = UUID.randomUUID().toString();
        google.aceitar(credencial, new GoogleTokenClaims("sub-" + credencial, "concorrente@palco.test", "Pessoa Google", null));
        var primeira = CadastroFixtures.artista();
        primeira.put("contexto", iniciar(credencial, false));
        var segunda = CadastroFixtures.artista();
        segunda.put("contexto", iniciar(credencial, false));
        concorrentes("/api/auth/google/cadastro", primeira, segunda);
        enviar("/api/auth/google/cadastro", primeira).andExpect(status().isConflict());
        enviar("/api/auth/google/cadastro", segunda).andExpect(status().isConflict());
        assertThat(usuarios.count()).isOne();
        assertThat(artistas.count()).isOne();
    }

    @Test
    void falhaNoPerfilReverteUsuarioGoogleEPermiteNovaTentativaDoContexto() throws Exception {
        var dados = conclusao(false, null);
        doThrow(new DataIntegrityViolationException("Falha controlada de perfil"))
                .when(artistas).saveAndFlush(any(PerfilArtista.class));
        enviar("/api/auth/google/cadastro", dados).andExpect(status().isConflict());
        assertThat(usuarios.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from perfil_artista_area", Integer.class)).isZero();
        reset(artistas);
        enviar("/api/auth/google/cadastro", dados).andExpect(status().isCreated());
    }

    @Test
    void googleMenorSemResponsavelOuComEmailIgualNaoPersiste() throws Exception {
        var dados = conclusao(false, null);
        dados.put("dataNascimento", LocalDate.now().minusYears(16).toString());
        enviar("/api/auth/google/cadastro", dados).andExpect(status().isBadRequest());
        menor(dados, 16);
        String[] partes = ((String) dados.get("contexto")).split("\\.");
        String email = mapper.readTree(Base64.getUrlDecoder().decode(partes[1])).get("email").asText();
        dados.put("emailResponsavel", email.toUpperCase(java.util.Locale.ROOT));
        enviar("/api/auth/google/cadastro", dados).andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void emailGoogleInvalidoRetorna401Generico() throws Exception {
        google.aceitar("email-invalido", new GoogleTokenClaims("sub-invalido", "invalido", "Pessoa", null));
        enviar("/api/auth/google", Map.of("idToken", "email-invalido")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(com.portifolio.service.google.GoogleTokenVerifier.MENSAGEM_ERRO));
        assertThat(usuarios.count()).isZero();
    }

    private Map<String, Object> convencional() {
        var dados = CadastroFixtures.artista();
        dados.put("nome", "Pessoa RF01");
        dados.put("email", "cadastro-" + UUID.randomUUID() + "@palco.test");
        dados.put("senha", "Palco@2026");
        return dados;
    }

    private Map<String, Object> conclusao(boolean rememberMe, String foto) throws Exception {
        String credencial = UUID.randomUUID().toString();
        google.aceitar(credencial, new GoogleTokenClaims("sub-" + credencial, "google-" + credencial + "@palco.test", "Pessoa Google", foto));
        var dados = CadastroFixtures.artista();
        dados.put("contexto", iniciar(credencial, rememberMe));
        return dados;
    }

    private String iniciar(String credencial, boolean rememberMe) throws Exception {
        long antes = usuarios.count();
        var resultado = enviar("/api/auth/google", Map.of("idToken", credencial, "rememberMe", rememberMe))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isEmpty())
                .andExpect(jsonPath("$.id").isEmpty()).andExpect(jsonPath("$.contextoExpiraEmSegundos").value(600))
                .andReturn();
        assertThat(usuarios.count()).isEqualTo(antes);
        return mapper.readTree(resultado.getResponse().getContentAsString()).get("contexto").asText();
    }

    private void menor(Map<String, Object> dados, int idade) {
        dados.put("dataNascimento", LocalDate.now().minusYears(idade).toString());
        dados.put("nomeResponsavel", "Responsavel Legal");
        dados.put("telefoneResponsavel", "11988887777");
        dados.put("emailResponsavel", "responsavel@palco.test");
    }

    private ResultActions enviar(String rota, Map<String, Object> dados) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dados)));
    }

    private void concorrentes(String rota, Map<String, Object> primeira, Map<String, Object> segunda) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var resultadoA = executor.submit(() -> { inicio.await(); return enviar(rota, primeira).andReturn(); });
            var resultadoB = executor.submit(() -> { inicio.await(); return enviar(rota, segunda).andReturn(); });
            inicio.countDown();
            var respostaA = resultadoA.get(30, TimeUnit.SECONDS).getResponse();
            var respostaB = resultadoB.get(30, TimeUnit.SECONDS).getResponse();
            assertThat(new int[]{respostaA.getStatus(), respostaB.getStatus()}).containsExactlyInAnyOrder(201, 409);
            String conflito = respostaA.getStatus() == 409 ? respostaA.getContentAsString() : respostaB.getContentAsString();
            assertThat(conflito).doesNotContain("23505", "constraint", "SQL", "usuarios_", "org.postgresql");
        }
    }
}
