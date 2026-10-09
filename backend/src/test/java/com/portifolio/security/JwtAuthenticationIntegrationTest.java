package com.portifolio.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest {

    private static final String SENHA = "SenhaAuth123!";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void limparBanco() {
        jdbcTemplate.execute("TRUNCATE refresh_tokens, usuarios RESTART IDENTITY CASCADE");
    }

    @Test
    void trocaDiretaContidaEJwtAntigoNaoApontaParaOutraContaAposMudancaPersistida() throws Exception {
        Usuario usuario = criarUsuario("email-antigo@fix-auth.test", TipoUsuario.CONTRATANTE);
        String tokenAntigo = login(usuario.getEmail());

        mockMvc.perform(put("/api/usuarios/me")
                        .header("Authorization", bearer(tokenAntigo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "nome", usuario.getNome(),
                                "telefone", usuario.getTelefone(),
                                "email", "email-novo@fix-auth.test"))))
                .andExpect(status().isUnprocessableEntity());

        assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getEmail()).isEqualTo(usuario.getEmail());
        mockMvc.perform(get("/api/usuarios/me").header("Authorization", bearer(tokenAntigo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(usuario.getEmail()));

        // DML preserva a regressão de ID+subject/reuso após alteração persistida;
        // não simula entrega de troca protegida bloqueada por falta de email pendente.
        jdbcTemplate.update("update usuarios set email=? where id=?", "email-novo@fix-auth.test", usuario.getId());

        MvcResult resultado = mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", bearer(tokenAntigo)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .doesNotContain("UsernameNotFoundException", "email-antigo@fix-auth.test", "trace");

        criarUsuario("email-antigo@fix-auth.test", TipoUsuario.ARTISTA);
        MvcResult resultadoAposReuso = mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", bearer(tokenAntigo)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(resultadoAposReuso.getResponse().getContentAsString())
                .doesNotContain("UsernameNotFoundException", "email-antigo@fix-auth.test", "trace");
    }

    @Test
    void endpointPrivadoSemTokenOuComTokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoMantemAcessoNormal() throws Exception {
        Usuario usuario = criarUsuario("token-valido@fix-auth.test", TipoUsuario.CONTRATANTE);

        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", bearer(login(usuario.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()))
                .andExpect(jsonPath("$.email").value(usuario.getEmail()));
    }

    @Test
    void usuarioAutenticadoSemPermissaoMantem403() throws Exception {
        Usuario artista = criarUsuario("artista-sem-permissao@fix-auth.test", TipoUsuario.ARTISTA);

        mockMvc.perform(post("/api/vagas")
                        .header("Authorization", bearer(login(artista.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "titulo", "Vaga restrita a contratante",
                                "descricao", "Descrição válida",
                                "requisitos", "Requisitos válidos",
                                "areaId", 1,
                                "abrangencia", "LOCAL",
                                "formaRemuneracao", "A_COMBINAR",
                                "cidade", "São Paulo",
                                "estado", "SP",
                                "modeloTrabalho", "REMOTO",
                                "tipoContrato", "Freelance"))))
                .andExpect(status().isForbidden());
    }

    private Usuario criarUsuario(String email, TipoUsuario tipoUsuario) {
        Usuario usuario = com.portifolio.support.OfficialSchemaFixtures.usuario();
        usuario.setNome("Pessoa FIX-AUTH-01");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setTelefone("11999999999");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setPerfilCompleto(false);
        usuario.setStatusConta(StatusConta.ATIVA);
        usuario.setEmailVerificado(true);
        usuario.setDataCriacao(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private String login(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "senha", SENHA,
                                "rememberMe", false))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode resposta = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return resposta.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }
}
