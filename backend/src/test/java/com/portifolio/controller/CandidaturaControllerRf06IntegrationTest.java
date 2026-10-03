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
import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.Usuario;
import com.portifolio.model.Vaga;
import com.portifolio.model.enums.ModeloTrabalho;
import com.portifolio.model.enums.StatusCandidatura;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.ResponsavelLegalRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PerfilArtistaRepository perfilArtistaRepository;
    @Autowired PerfilContratanteRepository perfilContratanteRepository;
    @Autowired ResponsavelLegalRepository responsavelLegalRepository;
    @Autowired VagaRepository vagaRepository;
    @MockitoSpyBean CandidaturaRepository candidaturaRepository;
    @MockitoSpyBean com.portifolio.service.NotificacaoPersistenceService notificacaoPersistenceService;
    @MockitoBean com.portifolio.service.GuardianApplicationNoticeSender guardianNoticeSender;
    @MockitoBean com.portifolio.realtime.NotificacaoRealtimeGateway realtimeGateway;
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
                        .content(corpoCriacao(vaga.getId(), "ACEITA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.artistaId").value(artista.getUsuarioId()))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        Candidatura salva = candidaturaRepository.findAll().getFirst();
        assertThat(salva.getDataCandidatura()).isBetween(inicio, LocalDateTime.now().plusSeconds(1));
        assertThat(jdbcTemplate.queryForList("select usuario_destino_id from notificacoes", Long.class))
                .containsExactly(contratante.getUsuarioId());
    }

    @Test
    void candidaturaSemMensagemELinkEhValida() throws Exception {
        PerfilContratante dono = novoContratante("dono-opcional@teste.com");
        PerfilArtista artista = novoArtista("artista-opcional@teste.com", true);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        mockMvc.perform(post("/api/candidaturas").header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagemApresentacao").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.linkPortfolioCandidatura").value(org.hamcrest.Matchers.nullValue()));
        assertThat(candidaturaRepository.count()).isOne();
    }

    @ParameterizedTest
    @ValueSource(ints = {14, 17})
    void menorAutorizadoGeraAvisoPosCommitSemExporResponsavel(int idade) throws Exception {
        PerfilContratante dono = novoContratante("dono-menor@teste.com");
        PerfilArtista artista = novoMenorAutorizado(idade);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailResponsavel").doesNotExist());
        verify(guardianNoticeSender).enviarAviso("responsavel@teste.com", vaga.getId());
        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isConflict());
        verify(guardianNoticeSender, org.mockito.Mockito.times(1))
                .enviarAviso("responsavel@teste.com", vaga.getId());
    }

    @Test
    void falhaDeEmailNaoDesfazCandidaturaNemNotificacaoDoContratante() throws Exception {
        PerfilContratante dono = novoContratante("dono-email-falha@teste.com");
        PerfilArtista artista = novoMenorAutorizado(16);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        doThrow(new IllegalStateException("SMTP sintético indisponível"))
                .when(guardianNoticeSender).enviarAviso("responsavel@teste.com", vaga.getId());

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"));
        assertThat(candidaturaRepository.count()).isOne();
        assertThat(jdbcTemplate.queryForList("select usuario_destino_id from notificacoes", Long.class))
                .containsExactly(dono.getUsuarioId());
        verify(guardianNoticeSender).enviarAviso("responsavel@teste.com", vaga.getId());
    }

    @Test
    void falhaRealtimeNaoDesfazCandidaturaENotificacaoFicaRecuperavel() throws Exception {
        PerfilContratante dono = novoContratante("dono-realtime-falha@teste.com");
        PerfilArtista artista = novoArtista("artista-realtime-falha@teste.com", true);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        doThrow(new IllegalStateException("Transporte sintético indisponível"))
                .when(realtimeGateway).entregar(any(), any(), any());

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isCreated());
        assertThat(candidaturaRepository.count()).isOne();
        assertThat(jdbcTemplate.queryForList("select usuario_destino_id from notificacoes", Long.class))
                .containsExactly(dono.getUsuarioId());
        verify(realtimeGateway).entregar(org.mockito.ArgumentMatchers.eq(dono.getUsuarioId()),
                org.mockito.ArgumentMatchers.eq(dono.getUsuario().getEmail()), any());
        mockMvc.perform(get("/api/notificacoes").header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("CANDIDATURA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"perfil", "pausada", "prazo", "ativa", "limite"})
    void tentativaBloqueadaNaoNotificaContratanteNemResponsavel(String motivo) throws Exception {
        PerfilContratante dono = novoContratante("dono-bloqueio@teste.com");
        PerfilArtista artista = novoMenorAutorizado(16);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        int esperado = 422;
        switch (motivo) {
            case "perfil" -> jdbcTemplate.update("update usuarios set perfil_completo = false where id = ?",
                    artista.getUsuarioId());
            case "pausada" -> {
                vaga.setStatus(StatusVaga.PAUSADA);
                vagaRepository.saveAndFlush(vaga);
            }
            case "prazo" -> {
                vaga.setDataLimiteCandidatura(LocalDate.now().minusDays(1));
                vagaRepository.saveAndFlush(vaga);
            }
            case "ativa" -> {
                novaCandidatura(vaga, artista, StatusCandidatura.EM_ANALISE);
                esperado = 409;
            }
            case "limite" -> {
                novaCandidatura(vaga, artista, StatusCandidatura.RETIRADA);
                novaCandidatura(vaga, artista, StatusCandidatura.RETIRADA);
                esperado = 409;
            }
            default -> throw new IllegalArgumentException(motivo);
        }
        long historicoInicial = candidaturaRepository.count();

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().is(esperado));
        assertThat(candidaturaRepository.count()).isEqualTo(historicoInicial);
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
        verify(guardianNoticeSender, never()).enviarAviso(any(), any());
        verify(realtimeGateway, never()).entregar(any(), any(), any());
    }

    @Test
    void perfilCompletoNaoRevalidaFotoPortfolioOuRaioNoRf06() throws Exception {
        PerfilContratante dono = novoContratante("dono-perfil-flag@teste.com");
        PerfilArtista artista = novoArtista("artista-perfil-flag@teste.com", true);
        jdbcTemplate.update("update perfis_artistas set url_portfolio = null, raio_atuacao = null where usuario_id = ?",
                artista.getUsuarioId());
        // O RF06 consome a flag; o cálculo de completude pertence ao RF08 e ao trigger oficial.
        jdbcTemplate.update("update usuarios set perfil_completo = true, foto_perfil_url = null where id = ?",
                artista.getUsuarioId());
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(ints = {18, 40})
    void adultoNaoGeraAvisoAoResponsavelMesmoComRegistroHistorico(int idade) throws Exception {
        PerfilContratante dono = novoContratante("dono-adulto@teste.com");
        PerfilArtista artista = novoMenorAutorizado(idade);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isCreated());
        verify(guardianNoticeSender, never()).enviarAviso(any(), any());
    }

    @Test
    void candidaturaAnonimaContinuaExigindoAutenticacao() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-anonimo@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-anonimo@teste.com", true);

        mockMvc.perform(post("/api/candidaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), null)))
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
                        .content(corpoCriacao(vaga.getId(), null)))
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
                        .content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void vagaInexistenteRetorna404() throws Exception {
        PerfilArtista artista = novoArtista("artista-vaga-inexistente@teste.com", true);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(999999L, null)))
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
                        .content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(
                        "A vaga não aceita candidaturas porque está com status " + statusVaga + "."));
    }

    static Stream<StatusVaga> statusDeVagaIndisponiveis() {
        return Stream.of(StatusVaga.RASCUNHO, StatusVaga.PAUSADA,
                StatusVaga.ENCERRADA, StatusVaga.CANCELADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"PENDENTE", "EM_ANALISE"})
    void candidaturaDuplicadaRetorna409(StatusCandidatura ativo) throws Exception {
        PerfilContratante contratante = novoContratante("contratante-duplicada@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-duplicada@teste.com", true);
        novaCandidatura(vaga, artista, ativo);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Você já possui uma candidatura ativa para esta vaga."));
        assertThat(candidaturaRepository.count()).isOne();
    }

    @Test
    void camposDeIdentidadeAdulteradosNaoRepresentamOutroArtista() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-identidade@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista autenticado = novoArtista("artista-autenticado-rf06@teste.com", true);
        PerfilArtista outro = novoArtista("outro-artista-rf06@teste.com", true);

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(autenticado.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vagaId":%d,"artistaId":%d,"usuarioId":%d,
                                "mensagemApresentacao":"Tenho interesse nesta oportunidade.",
                                "linkPortfolioCandidatura":"https://exemplo.com/portfolio"}
                                """.formatted(
                                vaga.getId(), outro.getUsuarioId(), outro.getUsuarioId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.artistaId").value(autenticado.getUsuarioId()))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        Candidatura salva = candidaturaRepository.findAll().getFirst();
        assertThat(salva.getArtista().getUsuarioId()).isEqualTo(autenticado.getUsuarioId());
    }

    @Test
    void validacaoDeTamanhoImpedeMensagemELinkInvalidos() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-limites@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-limites@teste.com", true);
        String corpo = """
                {"vagaId":%d,"mensagemApresentacao":"%s","linkPortfolioCandidatura":"%s"}
                """.formatted(vaga.getId(), "x".repeat(2001), "x".repeat(256));

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.length()").value(2));
    }

    @Test
    void aceitaLimitesExatosDeMensagemELink() throws Exception {
        PerfilContratante contratante = novoContratante("contratante-limites-exatos@teste.com");
        Vaga vaga = novaVaga(contratante, StatusVaga.ABERTA);
        PerfilArtista artista = novoArtista("artista-limites-exatos@teste.com", true);
        String corpo = """
                {"vagaId":%d,"mensagemApresentacao":"%s","linkPortfolioCandidatura":"%s"}
                """.formatted(vaga.getId(), "x".repeat(2000), "x".repeat(255));

        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.artistaId").value(artista.getUsuarioId()))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
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
    void contratanteProprietarioNaoCriaMaisEstadosFormais() throws Exception {
        PerfilContratante dono = novoContratante("dono-analise@teste.com");
        PerfilArtista artista = novoArtista("artista-analise@teste.com", true);
        Candidatura candidatura = novaCandidatura(
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);

        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.EM_ANALISE)
                .andExpect(status().isUnprocessableEntity());
        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.ACEITA)
                .andExpect(status().isUnprocessableEntity());
        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.REJEITADA)
                .andExpect(status().isUnprocessableEntity());
        assertThat(candidaturaRepository.findById(candidatura.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusCandidatura.PENDENTE);
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

        atualizarStatus(candidatura, artista.getUsuario(), StatusCandidatura.ACEITA)
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
        pendente.setStatus(StatusCandidatura.REJEITADA);
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
                novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.ACEITA);

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

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"PENDENTE", "EM_ANALISE"})
    void retiradaPreservaCamposNotificaDonoEPermiteRecandidatura(StatusCandidatura inicial) throws Exception {
        PerfilContratante dono = novoContratante("dono-retirada-ciclo@teste.com");
        PerfilArtista artista = novoArtista("artista-retirada-ciclo@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, StatusVaga.ABERTA), artista, inicial);
        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isNoContent());
        Candidatura salva = candidaturaRepository.findById(candidatura.getId()).orElseThrow();
        assertThat(salva.getStatus()).isEqualTo(StatusCandidatura.RETIRADA);
        assertThat(salva.getMensagemApresentacao()).isEqualTo(candidatura.getMensagemApresentacao());
        assertThat(salva.getLinkPortfolioCandidatura()).isEqualTo(candidatura.getLinkPortfolioCandidatura());
        assertThat(jdbcTemplate.queryForObject("select status::text from candidaturas where id = ?", String.class, salva.getId())).isEqualTo("RETIRADA");
        assertThat(jdbcTemplate.queryForList("select usuario_destino_id from notificacoes", Long.class)).containsExactly(dono.getUsuarioId());
        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/candidaturas").header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(candidatura.getVaga().getId(), null)))
                .andExpect(status().isCreated());
        assertThat(candidaturaRepository.count()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isEqualTo(2L);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"EM_ANALISE", "ACEITA", "REJEITADA"})
    void estadosLegadosPermanecemLegiveisMasNaoSaoCriadosNovamente(StatusCandidatura destino) throws Exception {
        PerfilContratante dono = novoContratante("dono-notificacao@teste.com");
        PerfilArtista artista = novoArtista("artista-notificacao@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, StatusVaga.ABERTA), artista, destino);
        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.ACEITA)
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbcTemplate.queryForObject("select status::text from candidaturas where id = ?", String.class, candidatura.getId())).isEqualTo(destino.getDatabaseValue());
        mockMvc.perform(get("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(destino.name()));
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }

    @Test
    void retiradaViaPutTambemNotificaSemAlterarConteudo() throws Exception {
        PerfilContratante dono = novoContratante("dono-put@teste.com");
        PerfilArtista artista = novoArtista("artista-put@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);
        atualizarStatus(candidatura, artista.getUsuario(), StatusCandidatura.RETIRADA)
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensagemApresentacao").value(candidatura.getMensagemApresentacao()));
        assertThat(jdbcTemplate.queryForList("select usuario_destino_id from notificacoes", Long.class)).containsExactly(dono.getUsuarioId());
    }

    @Test
    void contratanteNaoRetiraEOperacoesInexistentesRetornam404() throws Exception {
        PerfilContratante dono = novoContratante("dono-inexistente@teste.com");
        PerfilArtista artista = novoArtista("artista-inexistente@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);
        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId()).header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/candidaturas/999999").header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isNotFound());
        candidatura.setId(999999L);
        atualizarStatus(candidatura, dono.getUsuario(), StatusCandidatura.EM_ANALISE).andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }

    @Test
    void concorrenciaSubstituiPremissaDeUniqueGlobalEPermiteUmaAtiva() throws Exception {
        PerfilContratante dono = novoContratante("dono-constraint@teste.com");
        PerfilArtista artista = novoArtista("artista-constraint@teste.com", true);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        // O pacote oficial não contém UNIQUE global nem índice único para esse par.
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from pg_index
                 where indrelid = 'candidaturas'::regclass and indisunique and not indisprimary
                """, Long.class)).isZero();
        var prontas = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarefas = java.util.stream.IntStream.range(0, 2).mapToObj(ignorado ->
                    executor.submit(() -> {
                        prontas.countDown();
                        inicio.await();
                        return mockMvc.perform(post("/api/candidaturas")
                                        .header("Authorization", bearer(artista.getUsuario()))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpoCriacao(vaga.getId(), null)))
                                .andReturn().getResponse().getStatus();
                    })).toList();
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            assertThat(List.of(tarefas.get(0).get(30, TimeUnit.SECONDS),
                    tarefas.get(1).get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(candidaturaRepository.count()).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from candidaturas where vaga_id = ? and artista_id = ?
                  and status in ('PENDENTE', 'EM_ANALISE')
                """, Long.class, vaga.getId(), artista.getUsuarioId())).isOne();
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isEqualTo(1L);
    }

    @Test
    void duasRetiradasPreservamHistoricoETerceiraTentativaAtingeLimite() throws Exception {
        PerfilContratante dono = novoContratante("dono-ciclo@teste.com");
        PerfilArtista artista = novoArtista("artista-ciclo@teste.com", true);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        var primeiraResposta = mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isCreated()).andReturn();
        Long primeiraId = ((Number) com.jayway.jsonpath.JsonPath.read(
                primeiraResposta.getResponse().getContentAsString(), "$.id")).longValue();
        LocalDateTime primeiraData = candidaturaRepository.findById(primeiraId).orElseThrow().getDataCandidatura();
        mockMvc.perform(delete("/api/candidaturas/{id}", primeiraId)
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isNoContent());
        var segundaResposta = mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isCreated()).andReturn();
        Long segundaId = ((Number) com.jayway.jsonpath.JsonPath.read(
                segundaResposta.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(segundaId).isNotEqualTo(primeiraId);
        Candidatura segunda = candidaturaRepository.findById(segundaId).orElseThrow();
        assertThat(segunda.getStatus()).isEqualTo(StatusCandidatura.PENDENTE);
        assertThat(segunda.getDataCandidatura()).isAfter(primeiraData);
        mockMvc.perform(get("/api/vagas/{id}", vaga.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.minhaCandidaturaId").value(segundaId));
        assertThat(candidaturaRepository.findById(primeiraId).orElseThrow().getStatus())
                .isEqualTo(StatusCandidatura.RETIRADA);
        assertThat(candidaturaRepository.findById(primeiraId).orElseThrow().getDataCandidatura())
                .isEqualTo(primeiraData);
        mockMvc.perform(delete("/api/candidaturas/{id}", segundaId)
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/candidaturas")
                        .header("Authorization", bearer(artista.getUsuario()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("Limite de recandidatura atingido")));
        assertThat(candidaturaRepository.findByVagaId(vaga.getId())).hasSize(2)
                .allSatisfy(c -> assertThat(c.getStatus()).isEqualTo(StatusCandidatura.RETIRADA));
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class))
                .isEqualTo(4L);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, names = {"ABERTA", "PAUSADA"})
    void retiradaEmVagaAbertaOuPausadaEhPermitida(StatusVaga statusVaga) throws Exception {
        PerfilContratante dono = novoContratante("dono-retirada-" + statusVaga + "@teste.com");
        PerfilArtista artista = novoArtista("artista-retirada-" + statusVaga + "@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, statusVaga), artista, StatusCandidatura.PENDENTE);
        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isNoContent());
        assertThat(candidaturaRepository.findById(candidatura.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusCandidatura.RETIRADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, names = {"ENCERRADA", "CANCELADA"})
    void retiradaEmVagaFinalizadaRetorna422(StatusVaga statusVaga) throws Exception {
        PerfilContratante dono = novoContratante("dono-final-" + statusVaga + "@teste.com");
        PerfilArtista artista = novoArtista("artista-final-" + statusVaga + "@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, statusVaga), artista, StatusCandidatura.PENDENTE);
        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isUnprocessableEntity());
        assertThat(candidaturaRepository.findById(candidatura.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusCandidatura.PENDENTE);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class,
            names = {"RETIRADA", "CANCELADA_POR_VAGA", "ACEITA", "REJEITADA", "BLOQUEADA"})
    void retiradaNaoAlteraEstadoTerminalNemNotifica(StatusCandidatura terminal) throws Exception {
        PerfilContratante dono = novoContratante("dono-terminal@teste.com");
        PerfilArtista artista = novoArtista("artista-terminal@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, StatusVaga.ABERTA), artista, terminal);
        LocalDateTime dataOriginal = candidaturaRepository.findById(candidatura.getId())
                .orElseThrow().getDataCandidatura();
        mockMvc.perform(delete("/api/candidaturas/{id}", candidatura.getId())
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isUnprocessableEntity());
        Candidatura persistida = candidaturaRepository.findById(candidatura.getId()).orElseThrow();
        assertThat(persistida.getStatus()).isEqualTo(terminal);
        assertThat(persistida.getDataCandidatura()).isEqualTo(dataOriginal);
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }

    @Test
    void recandidaturasConcorrentesPreservamPrimeiraETotalDeDuasTentativas() throws Exception {
        PerfilContratante dono = novoContratante("dono-corrida-segunda@teste.com");
        PerfilArtista artista = novoArtista("artista-corrida-segunda@teste.com", true);
        Vaga vaga = novaVaga(dono, StatusVaga.ABERTA);
        Candidatura primeira = novaCandidatura(vaga, artista, StatusCandidatura.RETIRADA);
        var prontas = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tarefas = java.util.stream.IntStream.range(0, 2).mapToObj(ignorado ->
                    executor.submit(() -> {
                        prontas.countDown();
                        inicio.await();
                        return mockMvc.perform(post("/api/candidaturas")
                                        .header("Authorization", bearer(artista.getUsuario()))
                                        .contentType(MediaType.APPLICATION_JSON).content(corpoCriacao(vaga.getId(), null)))
                                .andReturn().getResponse().getStatus();
                    })).toList();
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            assertThat(List.of(tarefas.get(0).get(30, TimeUnit.SECONDS),
                    tarefas.get(1).get(30, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(candidaturaRepository.findByVagaId(vaga.getId())).hasSize(2);
        assertThat(candidaturaRepository.findById(primeira.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusCandidatura.RETIRADA);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from candidaturas where vaga_id = ? and artista_id = ?
                  and status in ('PENDENTE', 'EM_ANALISE')
                """, Long.class, vaga.getId(), artista.getUsuarioId())).isOne();
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
    }

    @Test
    void falhaDeNotificacaoDesfazRetiradaSemAlterarSchema() throws Exception {
        PerfilContratante dono = novoContratante("dono-rollback@teste.com");
        PerfilArtista artista = novoArtista("artista-rollback@teste.com", true);
        Candidatura candidatura = novaCandidatura(novaVaga(dono, StatusVaga.ABERTA), artista, StatusCandidatura.PENDENTE);
        com.portifolio.service.NotificacaoPersistenceService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(notificacaoPersistenceService);
        doThrow(new IllegalArgumentException("Falha sintética de persistência")).when(alvo).persistirNaTransacaoAtual(any());
        atualizarStatus(candidatura, artista.getUsuario(), StatusCandidatura.RETIRADA).andExpect(status().isBadRequest());
        assertThat(candidaturaRepository.findById(candidatura.getId()).orElseThrow().getStatus()).isEqualTo(StatusCandidatura.PENDENTE);
        assertThat(jdbcTemplate.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }

    private Usuario novoUsuario(String email, TipoUsuario tipo) {
        Usuario usuario = com.portifolio.support.OfficialSchemaFixtures.usuario();
        usuario.setNome("Usuário RF06");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setTelefone("11999999999");
        usuario.setEmail(email);
        usuario.setSenha("{noop}senha-teste");
        usuario.setTipoUsuario(tipo);
        usuario.setPerfilCompleto(false);
        usuario.setStatusConta(com.portifolio.model.enums.StatusConta.ATIVA);
        usuario.setEmailVerificado(true);
        usuario.setDataCriacao(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private PerfilContratante novoContratante(String email) {
        PerfilContratante perfil = new PerfilContratante();
        perfil.setTipoPerfil("PESSOA_FISICA");
        perfil.setUsuario(novoUsuario(email, TipoUsuario.CONTRATANTE));
        perfil.setNomeEmpresa("Empresa RF06");
        return perfilContratanteRepository.save(perfil);
    }

    private PerfilArtista novoMenorAutorizado(int idade) {
        PerfilArtista artista = novoArtista("menor-rf44@teste.com", true);
        artista.getUsuario().setDataNascimento(LocalDate.now().minusYears(idade));
        usuarioRepository.saveAndFlush(artista.getUsuario());
        ResponsavelLegal responsavel = new ResponsavelLegal();
        responsavel.setUsuario(artista.getUsuario());
        responsavel.setNomeResponsavel("Responsável RF44");
        responsavel.setTelefoneResponsavel("11988887777");
        responsavel.setEmailResponsavel("responsavel@teste.com");
        responsavel.setDataConsentimento(LocalDateTime.now().minusDays(1));
        responsavel.setConsentimentoRevogado(false);
        responsavelLegalRepository.saveAndFlush(responsavel);
        return artista;
    }

    private PerfilArtista novoArtista(String email, boolean completo) {
        Usuario usuario = novoUsuario(email, TipoUsuario.ARTISTA);
        PerfilArtista perfil = new PerfilArtista();
        perfil.setTipoPerfilArtistico(com.portifolio.model.enums.TipoPerfilArtistico.ARTISTA_SOLO);
        perfil.setRaioAtuacao(com.portifolio.model.enums.Abrangencia.LOCAL);
        perfil.setUsuario(usuario);
        perfil.setBiografia("Biografia RF06");
        perfil = perfilArtistaRepository.saveAndFlush(perfil);
        perfil.setUsuario(usuario);
        if (completo) {
            com.portifolio.support.OfficialSchemaFixtures.completarArtista(
                    jdbcTemplate, perfil.getUsuarioId());
            perfil.getUsuario().setPerfilCompleto(true);
        }
        return perfil;
    }

    private Vaga novaVaga(PerfilContratante contratante, StatusVaga status) {
        Vaga vaga = new Vaga();
        vaga.setArea(com.portifolio.support.OfficialSchemaFixtures.area());
        vaga.setAbrangencia(com.portifolio.model.enums.Abrangencia.LOCAL);
        vaga.setContratante(contratante);
        vaga.setTitulo("Vaga RF06 " + status);
        vaga.setDescricao("Descrição da vaga");
        vaga.setRequisitos("Requisitos da vaga");
        vaga.setValorMinimo(new BigDecimal("1000.00"));
        vaga.setValorMaximo(new BigDecimal("1000.00"));
        vaga.setFormaRemuneracao(com.portifolio.model.enums.FormaRemuneracao.POR_EVENTO);
        vaga.setCidade("São Paulo");
        vaga.setEstado("SP");
        vaga.setModeloTrabalho(ModeloTrabalho.REMOTO);
        vaga.setTipoContrato("Freelance");
        vaga.setStatus(status);
        vaga.setDataPublicacao(LocalDateTime.now());
        vaga.setFuncoes(new HashSet<>());
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

    private String corpoCriacao(Long vagaId, String status) {
        String campoStatus = status == null ? "" : ",\"status\":\"" + status + "\"";
        return """
                {"vagaId":%d,"mensagemApresentacao":"Tenho interesse nesta oportunidade.",
                "linkPortfolioCandidatura":"https://exemplo.com/portfolio"%s}
                """.formatted(vagaId, campoStatus);
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
