package com.portifolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class VagaGerenciamentoRf31IntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PerfilContratanteRepository perfilContratanteRepository;
    @Autowired PerfilArtistaRepository perfilArtistaRepository;
    @Autowired VagaRepository vagaRepository;
    @Autowired CandidaturaRepository candidaturaRepository;
    @Autowired JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void limparBanco() {
        jdbcTemplate.execute("TRUNCATE candidaturas, vagas, perfis_artistas, "
                + "perfis_contratantes, usuarios RESTART IDENTITY CASCADE");
    }

    @Test
    void visitanteNaoPodeGerenciarStatus() throws Exception {
        Vaga vaga = novaVaga(novoContratante("visitante@rf31.test"), StatusVaga.ABERTA);

        gerenciarSemToken(vaga, "SUSPENDER")
                .andExpect(status().isUnauthorized());

        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ABERTA);
    }

    @Test
    void artistaNaoPodeGerenciarStatus() throws Exception {
        Vaga vaga = novaVaga(novoContratante("dono-artista@rf31.test"), StatusVaga.ABERTA);
        Usuario artista = novoArtista("artista@rf31.test").getUsuario();

        gerenciar(vaga, artista, "SUSPENDER")
                .andExpect(status().isForbidden());

        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ABERTA);
    }

    @ParameterizedTest
    @CsvSource({
            "SUSPENDER, ABERTA",
            "REABRIR, PAUSADA",
            "ENCERRAR, ABERTA"
    })
    void outroContratanteNaoPodeExecutarNenhumaAcaoRf31(
            String acao, StatusVaga statusInicial) throws Exception {
        PerfilContratante dono = novoContratante("dono-idor-" + acao + "@rf31.test");
        PerfilContratante invasor = novoContratante("invasor-idor-" + acao + "@rf31.test");
        Vaga vaga = novaVaga(dono, statusInicial);

        gerenciar(vaga, invasor.getUsuario(), acao)
                .andExpect(status().isForbidden());

        assertThat(statusPersistido(vaga)).isEqualTo(statusInicial);
    }

    @Test
    void vagaInexistenteDeveRetornar404() throws Exception {
        PerfilContratante contratante = novoContratante("inexistente@rf31.test");

        mockMvc.perform(patch("/api/vagas/{id}/status", 999999)
                        .header("Authorization", bearer(contratante.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\":\"SUSPENDER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void proprietarioDeveSuspenderVagaAberta() throws Exception {
        PerfilContratante dono = novoContratante("suspender@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);

        gerenciar(vaga, dono.getUsuario(), "SUSPENDER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vaga.getId()))
                .andExpect(jsonPath("$.status").value("PAUSADA"))
                .andExpect(jsonPath("$.propriaDoContratante").value(true));

        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.PAUSADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, names = {"PAUSADA", "ENCERRADA", "CANCELADA"})
    void suspensaoDeEstadoInvalidoDeveRetornar422(StatusVaga estado) throws Exception {
        PerfilContratante dono = novoContratante("suspender-" + estado + "@rf31.test");
        Vaga vaga = novaVaga(dono, estado);

        gerenciar(vaga, dono.getUsuario(), "SUSPENDER")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("SUSPENDER")))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString(estado.name())));

        assertThat(statusPersistido(vaga)).isEqualTo(estado);
    }

    @Test
    void proprietarioDeveReabrirVagaPausada() throws Exception {
        PerfilContratante dono = novoContratante("reabrir@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.PAUSADA);

        gerenciar(vaga, dono.getUsuario(), "REABRIR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"));

        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ABERTA);
    }

    @Test
    void encerradaSemPrazoPodeReabrirSemInventarData() throws Exception {
        PerfilContratante dono = novoContratante("sem-prazo@rf23.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ENCERRADA);
        gerenciar(vaga, dono.getUsuario(), "REABRIR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"));
        assertThat(vagaRepository.findById(vaga.getId()).orElseThrow().getDataLimiteCandidatura())
                .isNull();
    }

    @Test
    void encerradaComPrazoVencidoExigeNovoPrazoFuturo() throws Exception {
        PerfilContratante dono = novoContratante("prazo-vencido@rf23.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ENCERRADA);
        vaga.setDataLimiteCandidatura(LocalDate.now().minusDays(1));
        vagaRepository.saveAndFlush(vaga);
        gerenciar(vaga, dono.getUsuario(), "REABRIR")
                .andExpect(status().isUnprocessableEntity());
        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ENCERRADA);
        gerenciarComPrazo(vaga, dono.getUsuario(), LocalDate.now())
                .andExpect(status().isUnprocessableEntity());
        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ENCERRADA);
        LocalDate futuro = LocalDate.now().plusDays(10);
        gerenciarComPrazo(vaga, dono.getUsuario(), futuro)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"));
        assertThat(vagaRepository.findById(vaga.getId()).orElseThrow().getDataLimiteCandidatura())
                .isEqualTo(futuro);
    }

    @Test
    void outroContratanteNaoReabreEncerrada() throws Exception {
        PerfilContratante dono = novoContratante("dono-encerrada@rf23.test");
        PerfilContratante outro = novoContratante("outro-encerrada@rf23.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ENCERRADA);
        gerenciar(vaga, outro.getUsuario(), "REABRIR")
                .andExpect(status().isForbidden());
        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ENCERRADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, names = {"ABERTA", "RASCUNHO", "CANCELADA"})
    void reaberturaDeEstadoInvalidoDeveRetornar422(StatusVaga estado) throws Exception {
        PerfilContratante dono = novoContratante("reabrir-" + estado + "@rf31.test");
        Vaga vaga = novaVaga(dono, estado);

        gerenciar(vaga, dono.getUsuario(), "REABRIR")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("REABRIR")))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString(estado.name())));

        assertThat(statusPersistido(vaga)).isEqualTo(estado);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, names = {"ABERTA"})
    void proprietarioDeveEncerrarVagaAberta(StatusVaga estado) throws Exception {
        PerfilContratante dono = novoContratante("encerrar-" + estado + "@rf31.test");
        Vaga vaga = novaVaga(dono, estado);

        gerenciar(vaga, dono.getUsuario(), "ENCERRAR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"));

        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ENCERRADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, names = {"PAUSADA", "ENCERRADA", "CANCELADA"})
    void encerramentoDeEstadoFinalDeveRetornar422(StatusVaga estado) throws Exception {
        PerfilContratante dono = novoContratante("final-" + estado + "@rf31.test");
        Vaga vaga = novaVaga(dono, estado);

        gerenciar(vaga, dono.getUsuario(), "ENCERRAR")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("Ações permitidas")));

        assertThat(statusPersistido(vaga)).isEqualTo(estado);
    }

    @Test
    void acaoCancelarNaoPertenceAoRf31EDeveRetornar400() throws Exception {
        PerfilContratante dono = novoContratante("cancelar@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);

        gerenciar(vaga, dono.getUsuario(), "CANCELAR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("Ação de gerenciamento inválida")));

        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ABERTA);
    }

    @Test
    void acaoAusenteDeveRetornar400() throws Exception {
        PerfilContratante dono = novoContratante("acao-ausente@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);

        mockMvc.perform(patch("/api/vagas/{id}/status", vaga.getId())
                        .header("Authorization", bearer(dono.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(statusPersistido(vaga)).isEqualTo(StatusVaga.ABERTA);
    }

    @Test
    void candidaturasDevemPermanecerIguaisAoSuspenderReabrirEEncerrar() throws Exception {
        PerfilContratante dono = novoContratante("historico@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        Candidatura pendente = novaCandidatura(
                vaga, novoArtista("pendente@rf31.test"), StatusCandidatura.PENDENTE);
        Candidatura emAnalise = novaCandidatura(
                vaga, novoArtista("analise@rf31.test"), StatusCandidatura.EM_ANALISE);
        Map<Long, StatusCandidatura> esperado = new LinkedHashMap<>();
        esperado.put(pendente.getId(), StatusCandidatura.PENDENTE);
        esperado.put(emAnalise.getId(), StatusCandidatura.EM_ANALISE);

        gerenciar(vaga, dono.getUsuario(), "SUSPENDER").andExpect(status().isOk());
        assertThat(candidaturasPersistidas(vaga)).isEqualTo(esperado);

        gerenciar(vaga, dono.getUsuario(), "REABRIR").andExpect(status().isOk());
        assertThat(candidaturasPersistidas(vaga)).isEqualTo(esperado);

        gerenciar(vaga, dono.getUsuario(), "ENCERRAR").andExpect(status().isOk());
        assertThat(candidaturasPersistidas(vaga)).isEqualTo(esperado);
        assertThat(candidaturasPersistidas(vaga).values())
                .doesNotContain(StatusCandidatura.CANCELADA_POR_VAGA);
    }

    @Test
    void rf06DeveBloquearPausadaPermitirReabertaEBloquearEncerrada() throws Exception {
        PerfilContratante dono = novoContratante("rf06@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        PerfilArtista primeiro = novoArtista("primeiro-rf06@rf31.test");
        PerfilArtista segundo = novoArtista("segundo-rf06@rf31.test");

        gerenciar(vaga, dono.getUsuario(), "SUSPENDER").andExpect(status().isOk());
        candidatar(vaga, primeiro).andExpect(status().isUnprocessableEntity());

        gerenciar(vaga, dono.getUsuario(), "REABRIR").andExpect(status().isOk());
        candidatar(vaga, primeiro)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVA"));

        gerenciar(vaga, dono.getUsuario(), "ENCERRAR").andExpect(status().isOk());
        candidatar(vaga, segundo).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rf03DeveRefletirSuspensaoReaberturaEEncerramentoNoFeed() throws Exception {
        PerfilContratante dono = novoContratante("feed@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);

        assertThat(idsDoFeed()).contains(vaga.getId());

        gerenciar(vaga, dono.getUsuario(), "SUSPENDER").andExpect(status().isOk());
        assertThat(idsDoFeed()).doesNotContain(vaga.getId());

        gerenciar(vaga, dono.getUsuario(), "REABRIR").andExpect(status().isOk());
        assertThat(idsDoFeed()).contains(vaga.getId());

        gerenciar(vaga, dono.getUsuario(), "ENCERRAR").andExpect(status().isOk());
        assertThat(idsDoFeed()).doesNotContain(vaga.getId());
    }

    @Test
    void rf05DevePreservarAcessoDoDonoEDoCandidatoSemExporParaTerceiro() throws Exception {
        PerfilContratante dono = novoContratante("detalhes@rf31.test");
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        PerfilArtista candidato = novoArtista("candidato-rf05@rf31.test");
        PerfilArtista terceiro = novoArtista("terceiro-rf05@rf31.test");
        novaCandidatura(vaga, candidato, StatusCandidatura.PENDENTE);

        gerenciar(vaga, dono.getUsuario(), "SUSPENDER").andExpect(status().isOk());
        detalhar(vaga, dono.getUsuario()).andExpect(status().isOk());
        detalhar(vaga, candidato.getUsuario()).andExpect(status().isOk());
        detalhar(vaga, terceiro.getUsuario()).andExpect(status().isNotFound());

        gerenciar(vaga, dono.getUsuario(), "ENCERRAR").andExpect(status().isUnprocessableEntity());
        gerenciar(vaga, dono.getUsuario(), "REABRIR").andExpect(status().isOk());
        gerenciar(vaga, dono.getUsuario(), "ENCERRAR").andExpect(status().isOk());
        detalhar(vaga, dono.getUsuario())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
        detalhar(vaga, candidato.getUsuario()).andExpect(status().isOk());
        detalhar(vaga, terceiro.getUsuario()).andExpect(status().isNotFound());
    }

    private ResultActions gerenciar(Vaga vaga, Usuario usuario, String acao) throws Exception {
        return mockMvc.perform(patch("/api/vagas/{id}/status", vaga.getId())
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("acao", acao))));
    }

    private ResultActions gerenciarComPrazo(Vaga vaga, Usuario usuario, LocalDate prazo) throws Exception {
        return mockMvc.perform(patch("/api/vagas/{id}/status", vaga.getId())
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "acao", "REABRIR", "dataLimiteCandidatura", prazo.toString()))));
    }

    private ResultActions gerenciarSemToken(Vaga vaga, String acao) throws Exception {
        return mockMvc.perform(patch("/api/vagas/{id}/status", vaga.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("acao", acao))));
    }

    private ResultActions detalhar(Vaga vaga, Usuario usuario) throws Exception {
        return mockMvc.perform(get("/api/vagas/{id}", vaga.getId())
                .header("Authorization", bearer(usuario)));
    }

    private ResultActions candidatar(Vaga vaga, PerfilArtista artista) throws Exception {
        String corpo = """
                {
                  "vagaId": %d,
                  "confirmacao": true,
                  "artistaId": %d,
                  "mensagemApresentacao": "Tenho interesse nesta oportunidade.",
                  "linkPortfolioCandidatura": "https://portfolio.example/candidatura"
                }
                """.formatted(vaga.getId(), artista.getUsuarioId());
        return mockMvc.perform(post("/api/candidaturas")
                .header("Authorization", bearer(artista.getUsuario()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private Set<Long> idsDoFeed() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/vagas"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(
                resultado.getResponse().getContentAsString()).get("content");
        return java.util.stream.StreamSupport.stream(content.spliterator(), false)
                .map(item -> item.get("id").asLong())
                .collect(Collectors.toSet());
    }

    private StatusVaga statusPersistido(Vaga vaga) {
        return vagaRepository.findById(vaga.getId()).orElseThrow().getStatus();
    }

    private Map<Long, StatusCandidatura> candidaturasPersistidas(Vaga vaga) {
        return candidaturaRepository.findByVagaId(vaga.getId()).stream()
                .collect(Collectors.toMap(
                        Candidatura::getId,
                        Candidatura::getStatus,
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new));
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private PerfilContratante novoContratante(String email) {
        Usuario usuario = novoUsuario(email, TipoUsuario.CONTRATANTE, false);
        PerfilContratante perfil = new PerfilContratante();
        perfil.setTipoPerfil("PESSOA_FISICA");
        perfil.setUsuario(usuario);
        perfil.setNomeEmpresa("Empresa RF31");
        return perfilContratanteRepository.save(perfil);
    }

    private PerfilArtista novoArtista(String email) {
        Usuario usuario = novoUsuario(email, TipoUsuario.ARTISTA, false);
        PerfilArtista perfil = new PerfilArtista();
        perfil.setTipoPerfilArtistico(com.portifolio.model.enums.TipoPerfilArtistico.ARTISTA_SOLO);
        perfil.setRaioAtuacao(com.portifolio.model.enums.Abrangencia.LOCAL);
        perfil.setUsuario(usuario);
        perfil.setBiografia("Biografia profissional");
        perfil.setLocalizacao("São Paulo, SP");
        perfil.setUrlPortfolio("https://portfolio.example");
        perfil.setUltimaAtualizacao(LocalDateTime.now());
        perfil = perfilArtistaRepository.saveAndFlush(perfil);
        perfil.setUsuario(usuario);
        com.portifolio.support.OfficialSchemaFixtures.completarArtista(
                jdbcTemplate, perfil.getUsuarioId());
        perfil.getUsuario().setPerfilCompleto(true);
        return perfil;
    }

    private Usuario novoUsuario(String email, TipoUsuario tipo, boolean perfilCompleto) {
        Usuario usuario = com.portifolio.support.OfficialSchemaFixtures.usuario();
        usuario.setNome(tipo == TipoUsuario.ARTISTA ? "Artista RF31" : "Contratante RF31");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setTelefone("11999999999");
        usuario.setEmail(email);
        usuario.setSenha("hash-teste");
        usuario.setTipoUsuario(tipo);
        usuario.setPerfilCompleto(perfilCompleto);
        usuario.setStatusConta(com.portifolio.model.enums.StatusConta.ATIVA);
        usuario.setEmailVerificado(true);
        usuario.setDataCriacao(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private Vaga novaVaga(PerfilContratante contratante, StatusVaga status) {
        Vaga vaga = new Vaga();
        vaga.setArea(com.portifolio.support.OfficialSchemaFixtures.area());
        vaga.setAbrangencia(com.portifolio.model.enums.Abrangencia.LOCAL);
        vaga.setContratante(contratante);
        vaga.setTitulo("Vaga RF31");
        vaga.setDescricao("Descrição da vaga");
        vaga.setRequisitos("Requisitos profissionais");
        vaga.setValorMinimo(new BigDecimal("2500.00"));
        vaga.setValorMaximo(new BigDecimal("2500.00"));
        vaga.setFormaRemuneracao(com.portifolio.model.enums.FormaRemuneracao.POR_EVENTO);
        vaga.setCidade("Campinas");
        vaga.setEstado("SP");
        vaga.setModeloTrabalho(ModeloTrabalho.HIBRIDO);
        vaga.setTipoContrato("Freelance");
        vaga.setExperiencia("SEM_EXPERIENCIA");
        vaga.setStatus(status);
        vaga.setDataPublicacao(LocalDateTime.now());
        return vagaRepository.save(vaga);
    }

    private Candidatura novaCandidatura(
            Vaga vaga, PerfilArtista artista, StatusCandidatura status) {
        Candidatura candidatura = new Candidatura();
        candidatura.setVaga(vaga);
        candidatura.setArtista(artista);
        candidatura.setMensagemApresentacao("Mensagem profissional");
        candidatura.setLinkPortfolioCandidatura("https://portfolio.example/candidatura");
        candidatura.setStatus(status);
        candidatura.setDataCandidatura(LocalDateTime.now());
        return candidaturaRepository.save(candidatura);
    }
}
