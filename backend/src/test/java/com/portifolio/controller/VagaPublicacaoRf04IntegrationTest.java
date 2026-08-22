package com.portifolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Tag;
import com.portifolio.model.Usuario;
import com.portifolio.model.Vaga;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.TagRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.security.JwtService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class VagaPublicacaoRf04IntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withInitScript("db/schema-test.sql")
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PerfilContratanteRepository perfilContratanteRepository;
    @Autowired TagRepository tagRepository;
    @Autowired VagaRepository vagaRepository;
    @Autowired JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void limparBanco() {
        jdbcTemplate.execute("TRUNCATE vagas, tags, perfis_artistas, "
                + "perfis_contratantes, usuarios RESTART IDENTITY CASCADE");
    }

    @Test
    void semJwtDeveRetornar401() throws Exception {
        mockMvc.perform(post("/api/vagas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payloadValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void artistaAutenticadoDeveRetornar403() throws Exception {
        Usuario artista = criarUsuario("artista-rf04@teste.com", TipoUsuario.ARTISTA);

        publicar(artista, payloadValido())
                .andExpect(status().isForbidden());

        assertThat(vagaRepository.count()).isZero();
    }

    @Test
    void devePublicarParaContratanteDoJwtIgnorarCamposControladosENormalizar() throws Exception {
        Usuario autenticado = criarUsuario("dono-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(autenticado, "Empresa Autenticada");
        Usuario terceiro = criarUsuario("terceiro-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(terceiro, "Empresa Terceira");
        Tag tag1 = criarTag("Fotografia");
        Tag tag2 = criarTag("Eventos");

        ObjectNode payload = payloadValido();
        payload.put("contratanteId", terceiro.getId());
        payload.put("id", 999999);
        payload.put("status", "CANCELADA");
        payload.put("dataPublicacao", "2000-01-01T00:00:00");
        payload.put("titulo", "  Fotógrafo de evento  ");
        payload.put("estado", "sp");
        payload.put("enderecoCompleto", "   ");
        payload.putArray("tagIds").add(tag1.getId()).add(tag2.getId());

        MvcResult resultado = publicar(autenticado, payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contratanteId").value(autenticado.getId()))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.titulo").value("Fotógrafo de evento"))
                .andExpect(jsonPath("$.estado").value("SP"))
                .andExpect(jsonPath("$.propriaDoContratante").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andReturn();

        JsonNode resposta = objectMapper.readTree(resultado.getResponse().getContentAsString());
        Long vagaId = resposta.get("id").asLong();
        Vaga persistida = vagaRepository.findByIdIn(List.of(vagaId)).getFirst();
        assertThat(persistida.getId()).isNotEqualTo(999999L);
        assertThat(persistida.getContratante().getUsuarioId()).isEqualTo(autenticado.getId());
        assertThat(persistida.getStatus()).isEqualTo(StatusVaga.ABERTA);
        assertThat(persistida.getDataPublicacao()).isAfter(LocalDateTime.now().minusMinutes(1));
        assertThat(persistida.getEnderecoCompleto()).isNull();
        assertThat(persistida.getTags()).extracting(Tag::getId)
                .containsExactlyInAnyOrder(tag1.getId(), tag2.getId());
    }

    @Test
    void contratanteIdLegadoPodeSerOmitido() throws Exception {
        Usuario contratante = criarUsuario("sem-id-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(contratante, "Empresa sem ID no payload");
        ObjectNode payload = payloadValido();
        payload.remove("contratanteId");

        publicar(contratante, payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contratanteId").value(contratante.getId()));
    }

    @ParameterizedTest(name = "campo obrigatório inválido: {0}")
    @MethodSource("camposObrigatoriosInvalidos")
    void camposObrigatoriosDevemSerValidadosNoServidor(String campo, boolean usarNull) throws Exception {
        Usuario contratante = criarUsuario(campo + "@rf04.teste", TipoUsuario.CONTRATANTE);
        criarPerfil(contratante, "Empresa validação");
        ObjectNode payload = payloadValido();
        if (usarNull) {
            payload.putNull(campo);
        } else {
            payload.put(campo, "   ");
        }

        publicar(contratante, payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."));

        assertThat(vagaRepository.count()).isZero();
    }

    static Stream<Arguments> camposObrigatoriosInvalidos() {
        return Stream.of(
                Arguments.of("titulo", false),
                Arguments.of("descricao", false),
                Arguments.of("requisitos", false),
                Arguments.of("remuneraValor", true),
                Arguments.of("formaPagamento", false),
                Arguments.of("cidade", false),
                Arguments.of("estado", false),
                Arguments.of("modeloTrabalho", true),
                Arguments.of("tipoContrato", false));
    }

    @Test
    void remuneracaoZeroDeveSerAceitaENegativaRejeitada() throws Exception {
        Usuario contratante = criarUsuario("remuneracao-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(contratante, "Empresa remuneração");
        ObjectNode zero = payloadValido();
        zero.put("remuneraValor", 0);

        publicar(contratante, zero)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remuneraValor").value(0));

        ObjectNode negativa = payloadValido();
        negativa.put("remuneraValor", -0.01);
        publicar(contratante, negativa)
                .andExpect(status().isBadRequest());

        assertThat(vagaRepository.count()).isEqualTo(1);
    }

    @Test
    void modeloInvalidoEUfNaoAlfabeticaDevemRetornar400() throws Exception {
        Usuario contratante = criarUsuario("enums-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(contratante, "Empresa enums");
        ObjectNode modeloInvalido = payloadValido();
        modeloInvalido.put("modeloTrabalho", "VIRTUAL");
        publicar(contratante, modeloInvalido)
                .andExpect(status().isBadRequest());

        ObjectNode estadoInvalido = payloadValido();
        estadoInvalido.put("estado", "1X");
        publicar(contratante, estadoInvalido)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."));

        assertThat(vagaRepository.count()).isZero();
    }

    @Test
    void tagInexistenteDeveRetornar404EFazerRollbackCompleto() throws Exception {
        Usuario contratante = criarUsuario("rollback-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(contratante, "Empresa rollback");
        Tag existente = criarTag("Tag existente");
        ObjectNode payload = payloadValido();
        payload.putArray("tagIds").add(existente.getId()).add(999999);

        publicar(contratante, payload)
                .andExpect(status().isNotFound());

        assertThat(vagaRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tags_vaga", Long.class)).isZero();
    }

    @Test
    void idDeTagNaoPositivoDeveRetornar400SemPersistir() throws Exception {
        Usuario contratante = criarUsuario("tag-invalida-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(contratante, "Empresa tag inválida");
        ObjectNode payload = payloadValido();
        payload.putArray("tagIds").add(0);

        publicar(contratante, payload)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."));

        assertThat(vagaRepository.count()).isZero();
    }

    @Test
    void tagsAusentesOuVaziasDevemContinuarOpcionais() throws Exception {
        Usuario contratante = criarUsuario("tags-opcionais-rf04@teste.com", TipoUsuario.CONTRATANTE);
        criarPerfil(contratante, "Empresa tags opcionais");
        ObjectNode ausentes = payloadValido();
        ausentes.remove("tagIds");
        publicar(contratante, ausentes)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tagIds.length()").value(0));

        ObjectNode vazias = payloadValido();
        vazias.putArray("tagIds");
        publicar(contratante, vazias)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tagIds.length()").value(0));

        assertThat(vagaRepository.count()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM tags_vaga", Long.class)).isZero();
    }

    @Test
    void contratanteSemPerfilDeveRetornar404SemCriacaoParcial() throws Exception {
        Usuario contratante = criarUsuario("sem-perfil-rf04@teste.com", TipoUsuario.CONTRATANTE);

        publicar(contratante, payloadValido())
                .andExpect(status().isNotFound());

        assertThat(vagaRepository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions publicar(
            Usuario usuario, ObjectNode payload) throws Exception {
        return mockMvc.perform(post("/api/vagas")
                .header("Authorization", "Bearer " + jwtService.gerarToken(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(payload)));
    }

    private ObjectNode payloadValido() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("contratanteId", 123456);
        payload.put("titulo", "Fotógrafo de evento");
        payload.put("descricao", "Cobertura completa do evento");
        payload.put("requisitos", "Portfólio atualizado");
        payload.put("remuneraValor", 1500.50);
        payload.put("formaPagamento", "Pix");
        payload.put("cidade", "São Paulo");
        payload.put("estado", "SP");
        payload.put("enderecoCompleto", "Rua das Artes, 10");
        payload.put("beneficios", "Transporte");
        payload.put("modeloTrabalho", "PRESENCIAL");
        payload.put("tipoContrato", "Freelance");
        payload.putArray("tagIds");
        return payload;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Usuario criarUsuario(String email, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário RF04");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setTelefone("11999999999");
        usuario.setEmail(email);
        usuario.setSenha("{noop}senha-teste");
        usuario.setTipoUsuario(tipo);
        usuario.setPerfilCompleto(false);
        usuario.setDataCriacao(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private PerfilContratante criarPerfil(Usuario usuario, String nomeEmpresa) {
        PerfilContratante perfil = new PerfilContratante();
        perfil.setUsuario(usuario);
        perfil.setNomeEmpresa(nomeEmpresa);
        return perfilContratanteRepository.save(perfil);
    }

    private Tag criarTag(String nome) {
        Tag tag = new Tag();
        tag.setNome(nome);
        return tagRepository.save(tag);
    }
}
