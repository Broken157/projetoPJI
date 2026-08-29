package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portifolio.model.Candidatura;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Usuario;
import com.portifolio.model.Vaga;
import com.portifolio.model.enums.ModeloTrabalho;
import com.portifolio.model.enums.StatusCandidatura;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CandidaturaControllerRf06IntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withInitScript("db/schema-test.sql")
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PerfilArtistaRepository perfilArtistaRepository;
    @Autowired PerfilContratanteRepository perfilContratanteRepository;
    @Autowired VagaRepository vagaRepository;
    @Autowired CandidaturaRepository candidaturaRepository;
    @Autowired JwtService jwtService;

    @AfterEach
    void limparBanco() {
        jdbcTemplate.execute(
                "TRUNCATE candidaturas, vagas, perfis_artistas, perfis_contratantes, usuarios RESTART IDENTITY CASCADE");
    }

    @Test
    void artistaCompletoCriaCandidaturaPendenteComDataReal() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-criacao@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-criacao@teste.com", true);
        LocalDateTime inicio = LocalDateTime.now().minusSeconds(1);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), artista.getUsuarioId(), "APROVADO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.artistaId").value(artista.getUsuarioId()))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        Candidatura salva = candidaturaRepository.findAll().getFirst();
        assertThat(salva.getDataCandidatura()).isBetween(inicio, LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void candidaturaAnonimaContinuaExigindoAutenticacao() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-anonimo@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-anonimo@teste.com", true);

        mockMvc.perform(post("/api/candidaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), artista.getUsuarioId(), null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilIncompletoRetorna422() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-incompleto@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-incompleto@teste.com", false);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), artista.getUsuarioId(), null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Complete seu perfil antes de se candidatar."));
    }

    @Test
    void contratanteNaoPodeCriarCandidatura() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-nao-artista@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(contratante.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), contratante.getUsuarioId(), null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void vagaInexistenteRetorna404() throws Exception {
        PerfilArtista artista = novoArtista("artista-vaga-inexistente@teste.com", true);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(999999L, artista.getUsuarioId(), null)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("statusDeVagaIndisponiveis")
    void vagaNaoAbertaRetorna422(StatusVaga statusVaga) throws Exception {
        PerfilContratante contratante = novoContratante("contratante-" + statusVaga + "@teste.com");
        Vaga vaga = novaVaga(contratante, statusVaga);
        PerfilArtista artista = novoArtista("artista-" + statusVaga + "@teste.com", true);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), artista.getUsuarioId(), null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "A vaga não aceita candidaturas porque está com status " + statusVaga + "."));
    }

    static Stream<StatusVaga> statusDeVagaIndisponiveis() {
        return Stream.of(StatusVaga.PAUSADA, StatusVaga.ENCERRADA, StatusVaga.CANCELADA);
    }

    @Test
    void candidaturaDuplicadaRetorna409() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-duplicada@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-duplicada@teste.com", true);
        novaCandidatura(vaga, artista, StatusCandidatura.PENDENTE);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), artista.getUsuarioId(), null)))
                .andExpect(status().isConflict());
    }

    @Test
    void artistaNaoPodeUsarOutroArtistaId() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-identidade@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista autenticado = novoArtista("artista-autenticado-rf06@teste.com", true);
        PerfilArtista outro = novoArtista("outro-artista-rf06@teste.com", true);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(autenticado.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), outro.getUsuarioId(), null)))
                .andExpect(status().isForbidden());

        assertThat(candidaturaRepository.count()).isZero();
    }

    @Test
    void validacaoDeTamanhoImpedeMensagemELinkInvalidos() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-limites@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-limites@teste.com", true);
        String corpo = """
                {"vagaId":%d,"artistaId":%d,"mensagemApresentacao":"%s","linkPortfolioCandidatura":"%s"}
                """.formatted(vaga.getId(), artista.getUsuarioId(), "x".repeat(2001), "x".repeat(256));

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.length()").value(2));
    }

    @Test
    void artistaListaEDetalhaSomentePropriasCandidaturas() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-consulta-artista@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-consulta@teste.com", true);
        PerfilArtista outro = novoArtista("outro-consulta@teste.com", true);
        Candidatura propria = novaCandidatura(vaga, artista, StatusCandidatura.PENDENTE);
        Candidatura alheia = novaCandidatura(vaga, outro, StatusCandidatura.PENDENTE);

        mockMvc.perform(get("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propria.getId()));
        mockMvc.perform(get("/api/candidaturas/{id}", propria.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/candidaturas/{id}", alheia.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isNotFound());
    }

    @Test
    void contratanteListaEDetalhaSomenteCandidaturasDasPropriasVagas() throws Exception {
        PerfilContratante dono = novoContratante("dono-consulta@teste.com");
        PerfilContratante outroContratante = novoContratante("outro-consulta-contratante@teste.com");
        PerfilArtista artista = novoArtista("artista-consulta-contratante@teste.com", true);
        Candidatura propria = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);
        Candidatura alheia = novaCandidatura(
                novaVaga(outroContratante, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);

        mockMvc.perform(get("/api/candidaturas/minhas-vagas")
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propria.getId()));
        mockMvc.perform(get("/api/candidaturas/{id}", propria.getId())
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/candidaturas/{id}", alheia.getId())
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isNotFound());
    }

    @Test
    void contratanteProprietarioRegistraAnaliseEResultado() throws Exception {
        PerfilContratante dono = novoContratante("dono-analise@teste.com");
        PerfilArtista artista = novoArtista("artista-analise@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);

        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.EM_ANALISE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));
        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.APROVADO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void outroContratanteNaoAlteraCandidatura() throws Exception {
        PerfilContratante dono = novoContratante("dono-bloqueio@teste.com");
        PerfilContratante outro = novoContratante("outro-bloqueio@teste.com");
        PerfilArtista artista = novoArtista("artista-bloqueio@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);

        atualizarStatus(candidatura, outro.getUsuario(), StatusCandidatura.EM_ANALISE)
                .andExpect(status().isForbidden());
        assertThat(candidaturaRepository.findById(candidatura.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusCandidatura.PENDENTE);
    }

    @Test
    void artistaNaoPodeAprovarOuRejeitar() throws Exception {
        PerfilContratante dono = novoContratante("dono-artista-status@teste.com");
        PerfilArtista artista = novoArtista("artista-status@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);

        atualizarStatus(candidatura, artista.getUsuario(), StatusCandidatura.APROVADO)
                .andExpect(status().isForbidden());
    }

    @Test
    void transicaoInvalidaEReversaoDeTerminalRetornam422() throws Exception {
        PerfilContratante dono = novoContratante("dono-transicao@teste.com");
        PerfilArtista artista = novoArtista("artista-transicao@teste.com", true);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        Candidatura pendente = novaCandidatura(vaga, artista, StatusCandidatura.PENDENTE);

        atualizarStatus(pendente, dono.getUsuario(), StatusCandidatura.RETIRADA)
                .andExpect(status().isUnprocessableEntity());
        pendente.setStatus(StatusCandidatura.REJEITADO);
        candidaturaRepository.save(pendente);
        atualizarStatus(pendente, dono.getUsuario(), StatusCandidatura.EM_ANALISE)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void artistaRetiraPropriaCandidaturaSemExclusaoFisica() throws Exception {
        PerfilContratante dono = novoContratante("dono-retirada@teste.com");
        PerfilArtista artista = novoArtista("artista-retirada@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.EM_ANALISE);

        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isNoContent());

        assertThat(candidaturaRepository.findById(candidatura.getId())).isPresent()
                .get().extracting(Candidatura::getStatus).isEqualTo(StatusCandidatura.RETIRADA);
    }

    @Test
    void outroArtistaNaoRetiraCandidaturaAlheia() throws Exception {
        PerfilContratante dono = novoContratante("dono-retirada-alheia@teste.com");
        PerfilArtista artista = novoArtista("artista-dono-retirada@teste.com", true);
        PerfilArtista outro = novoArtista("outro-artista-retirada@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);

        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(outro.getUsuario())))
                .andExpect(status().isNotFound());
    }

    @Test
    void candidaturaAprovadaNaoPodeSerRetirada() throws Exception {
        PerfilContratante dono = novoContratante("dono-aprovada@teste.com");
        PerfilArtista artista = novoArtista("artista-aprovada@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.APROVADO);

        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void canceladaPorVagaNaoPodeSerAlteradaManualmente() throws Exception {
        PerfilContratante dono = novoContratante("dono-cancelada@teste.com");
        PerfilArtista artista = novoArtista("artista-cancelada@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.CANCELADA), artista, StatusCandidatura.CANCELADA_POR_VAGA);

        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.EM_ANALISE)
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isUnprocessableEntity());
    }

    private Usuario novoUsuario(String email, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário RF06");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setTelefone("11999999999");
        usuario.setEmail(email);
        usuario.setSenha("{noop}senha-teste");
        usuario.setTipoUsuario(tipo);
        usuario.setPerfilCompleto(false);
        usuario.setDataCriacao(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private PerfilContratante novoContratante(String email) {
        PerfilContratante perfil = new PerfilContratante();
        perfil.setUsuario(novoUsuario(email, TipoUsuario.CONTRATANTE));
        perfil.setNomeEmpresa("Empresa RF06");
        return perfilContratanteRepository.save(perfil);
    }

    private PerfilArtista novoArtista(String email, boolean completo) {
        Usuario usuario = novoUsuario(email, TipoUsuario.ARTISTA);
        usuario.setPerfilCompleto(completo);
        usuarioRepository.save(usuario);
        PerfilArtista perfil = new PerfilArtista();
        perfil.setUsuario(usuario);
        perfil.setBiografia("Biografia RF06");
        return perfilArtistaRepository.save(perfil);
    }

    private Vaga novaVaga(PerfilContratante contratante, StatusVaga status) {
        Vaga vaga = new Vaga();
        vaga.setContratante(contratante);
        vaga.setTitulo("Vaga RF06 " + status);
        vaga.setDescricao("Descrição da vaga");
        vaga.setRequisitos("Requisitos da vaga");
        vaga.setRemuneraValor(new BigDecimal("1000.00"));
        vaga.setFormaPagamento("Pix");
        vaga.setCidade("São Paulo");
        vaga.setEstado("SP");
        vaga.setModeloTrabalho(ModeloTrabalho.REMOTO);
        vaga.setTipoContrato("Freelance");
        vaga.setStatus(status);
        vaga.setDataPublicacao(LocalDateTime.now());
        vaga.setTags(new HashSet<>());
        return vagaRepository.save(vaga);
    }

    private Candidatura novaCandidatura(
            Vaga vaga, PerfilArtista artista, StatusCandidatura status) {
        Candidatura candidatura = new Candidatura();
        candidatura.setVaga(vaga);
        candidatura.setArtista(artista);
        candidatura.setMensagemApresentacao("Tenho interesse nesta oportunidade.");
        candidatura.setLinkPortfolioCandidatura("https://exemplo.com/portfolio");
        candidatura.setStatus(status);
        candidatura.setDataCandidatura(LocalDateTime.now());
        return candidaturaRepository.save(candidatura);
    }

    private String corpoCriacao(Long vagaId, Long artistaId, String status) {
        String campoStatus = status == null ? "" : ",\"status\":\"" + status + "\"";
        return """
                {"vagaId":%d,"artistaId":%d,"mensagemApresentacao":"Tenho interesse nesta oportunidade.",
                "linkPortfolioCandidatura":"https://exemplo.com/portfolio"%s}
                """.formatted(vagaId, artistaId, campoStatus);
    }

    private org.springframework.test.web.servlet.ResultActions atualizarStatus(
            Candidatura candidatura, Usuario ator, StatusCandidatura status) throws Exception {
        String corpo = """
                {"vagaId":%d,"artistaId":%d,"mensagemApresentacao":"ignorada",
                "linkPortfolioCandidatura":"https://ignorado.example","status":"%s"}
                """.formatted(
                candidatura.getVaga().getId(), candidatura.getArtista().getUsuarioId(), status);
        return mockMvc.perform(put("/api/candidaturas/{id}", candidatura.getId())
                .header("Authorization", bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
