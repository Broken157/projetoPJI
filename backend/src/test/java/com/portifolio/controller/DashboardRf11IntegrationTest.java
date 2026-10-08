package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portifolio.model.Candidatura;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Funcao;
import com.portifolio.model.Usuario;
import com.portifolio.model.Vaga;
import com.portifolio.model.enums.ModeloTrabalho;
import com.portifolio.model.enums.StatusCandidatura;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.FuncaoRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.security.JwtService;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DashboardRf11IntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PerfilArtistaRepository perfilArtistaRepository;
    @Autowired PerfilContratanteRepository perfilContratanteRepository;
    @Autowired FuncaoRepository funcaoRepository;
    @Autowired VagaRepository vagaRepository;
    @Autowired CandidaturaRepository candidaturaRepository;
    @Autowired JwtService jwtService;
    @Autowired EntityManagerFactory entityManagerFactory;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate namedJdbc;

    @AfterEach
    void limparBanco() {
        jdbcTemplate.execute("""
                TRUNCATE candidaturas, vagas, funcoes, perfis_artistas,
                         perfis_contratantes, usuarios RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void endpointExigeJwtValido() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void artistaRecebeSomenteVagasAbertasComFuncoesCoincidentesOrdenadasPorCompatibilidade() throws Exception {
        Funcao teatro = criarFuncao("Teatro");
        Funcao danca = criarFuncao("Danca");
        Funcao musica = criarFuncao("Musica");
        PerfilArtista artista = criarArtista(
                "artista@rf11.test", true, LocalDate.of(1990, 1, 1), teatro, danca);

        PerfilContratante contratante = criarContratante("contratante@rf11.test", true);
        criarVaga(contratante, "Uma coincidencia", StatusVaga.ABERTA, teatro);
        criarVaga(contratante, "Duas coincidencias", StatusVaga.ABERTA, teatro, danca);
        criarVaga(contratante, "Pausada", StatusVaga.PAUSADA, teatro, danca);
        criarVaga(contratante, "Encerrada", StatusVaga.ENCERRADA, teatro, danca);
        criarVaga(contratante, "Cancelada", StatusVaga.CANCELADA, teatro, danca);
        criarVaga(contratante, "Sem coincidencia", StatusVaga.ABERTA, musica);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoUsuario").value("ARTISTA"))
                .andExpect(jsonPath("$.vagasRecomendadas.content.length()").value(3))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].titulo").value("Duas coincidencias"))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].quantidadeFuncoesCoincidentes").value(2))
                .andExpect(jsonPath("$.vagasRecomendadas.content[1].titulo").value("Uma coincidencia"))
                .andExpect(jsonPath("$.vagasRecomendadas.content[2].quantidadeFuncoesCoincidentes").value(0))
                .andExpect(jsonPath("$.candidaturasRecentes").doesNotExist())
                .andExpect(jsonPath("$.talentosSugeridos").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist())
                .andExpect(jsonPath("$.dataNascimento").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void artistaSemFuncoesNaoRecebeSugestaoAleatoriaEEnxergaPerfilIncompleto() throws Exception {
        PerfilArtista artista = criarArtista("sem-funcoes@rf11.test", false, LocalDate.of(1992, 2, 2));
        jdbcTemplate.update("delete from perfil_artista_area where perfil_artista_id=?", artista.getUsuarioId());
        PerfilContratante contratante = criarContratante("publicador@rf11.test", true);
        criarVaga(contratante, "Vaga qualquer", StatusVaga.ABERTA, criarFuncao("Cinema"));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilCompleto").value(false))
                .andExpect(jsonPath("$.vagasRecomendadas.content.length()").value(0))
                .andExpect(jsonPath("$.vagasRecomendadas.totalElements").value(0))
                .andExpect(jsonPath("$.vagasRecomendadas.hasMore").value(false));
    }

    @Test
    void perfilIncompletoNaoBloqueiaDashboardNemVisualizacaoDeRecomendacoes() throws Exception {
        PerfilArtista artista = criarArtista("incompleto-com-funcao@rf11.test", false, LocalDate.of(1992, 2, 2));
        Funcao funcao = criarFuncao("Teatro incompleto");
        com.portifolio.support.OfficialSchemaFixtures.funcoes(artista, new HashSet<>(Set.of(funcao)));
        perfilArtistaRepository.save(artista);
        PerfilContratante contratante = criarContratante("publicador-incompleto@rf11.test", true);
        criarVaga(contratante, "Vaga visivel", StatusVaga.ABERTA, funcao);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilCompleto").value(false))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].titulo").value("Vaga visivel"));
    }

    @Test
    void identidadeVemDoJwtEMesmoParametroDeOutroUsuarioNaoProduzIdor() throws Exception {
        PerfilArtista autenticado = criarArtista("autenticado@rf11.test", true, LocalDate.of(1990, 1, 1));
        PerfilArtista outro = criarArtista("outro@rf11.test", false, LocalDate.of(1990, 1, 1));

        mockMvc.perform(get("/api/dashboard")
                        .param("usuarioId", outro.getUsuarioId().toString())
                        .header("Authorization", bearer(autenticado.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeExibicao").value(autenticado.getUsuario().getNome()))
                .andExpect(jsonPath("$.perfilCompleto").value(true));
    }

    @Test
    void sizeEhLimitadoEntreUmECinquenta() throws Exception {
        PerfilArtista artista = criarArtista("size@rf11.test", true, LocalDate.of(1990, 1, 1));
        String token = bearer(artista.getUsuario());

        mockMvc.perform(get("/api/dashboard").param("size", "0").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("size deve estar entre 1 e 50."));
        mockMvc.perform(get("/api/dashboard").param("size", "51").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contratanteVeCandidaturasRecentesSomenteDasPropriasVagasAtivas() throws Exception {
        PerfilContratante dono = criarContratante("dono@rf11.test", true);
        PerfilContratante terceiro = criarContratante("terceiro@rf11.test", true);
        PerfilArtista artista = criarArtista("candidato@rf11.test", true, LocalDate.of(1990, 1, 1));
        Vaga aberta = criarVaga(dono, "Aberta propria", StatusVaga.ABERTA);
        Vaga pausada = criarVaga(dono, "Pausada propria", StatusVaga.PAUSADA);
        Vaga encerrada = criarVaga(dono, "Encerrada propria", StatusVaga.ENCERRADA);
        Vaga alheia = criarVaga(terceiro, "Aberta alheia", StatusVaga.ABERTA);
        criarCandidatura(aberta, artista, LocalDateTime.now().minusHours(2));
        criarCandidatura(pausada, artista, LocalDateTime.now().minusHours(1));
        criarCandidatura(encerrada, artista, LocalDateTime.now());
        criarCandidatura(alheia, artista, LocalDateTime.now().plusHours(1));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoUsuario").value("CONTRATANTE"))
                .andExpect(jsonPath("$.candidaturasRecentes.content.length()").value(2))
                .andExpect(jsonPath("$.candidaturasRecentes.content[0].tituloVaga").value("Pausada propria"))
                .andExpect(jsonPath("$.candidaturasRecentes.content[1].tituloVaga").value("Aberta propria"))
                .andExpect(jsonPath("$.vagasRecomendadas").doesNotExist())
                .andExpect(jsonPath("$.candidaturasRecentes.content[0].mensagemApresentacao").doesNotExist())
                .andExpect(jsonPath("$.candidaturasRecentes.content[0].linkPortfolioCandidatura").doesNotExist());
    }

    @Test
    void talentosSugeridosUsamBancoProprioSemReativarPremissaGlobalRf13() throws Exception {
        PerfilContratante dono = criarContratante("matching@rf11.test", true);
        Funcao teatro = criarFuncao("Teatro RF11");
        Funcao cinema = criarFuncao("Cinema RF11");
        Funcao circo = criarFuncao("Circo RF11");
        criarVaga(dono, "Vaga ativa", StatusVaga.PAUSADA, teatro, cinema);
        criarVaga(dono, "Vaga cancelada", StatusVaga.CANCELADA, circo);
        PerfilArtista compativel = criarArtista("compativel@rf11.test", true, LocalDate.of(1990, 1, 1), teatro, cinema);
        criarArtista("incompleto@rf11.test", false, LocalDate.of(1990, 1, 1), teatro, cinema);
        criarArtista("menor@rf11.test", true, LocalDate.now().minusYears(17), teatro, cinema);
        PerfilArtista outroMembro = criarArtista("funcao-cancelada@rf11.test", true, LocalDate.of(1990, 1, 1), circo);
        jdbcTemplate.update("insert into banco_talentos(contratante_id,artista_id) values (?,?),(?,?)",
                dono.getUsuarioId(), compativel.getUsuarioId(), dono.getUsuarioId(), outroMembro.getUsuarioId());

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.talentosSugeridos.content.length()").value(2))
                .andExpect(jsonPath("$.talentosSugeridos.content[0].artistaId").value(compativel.getUsuarioId()))
                .andExpect(jsonPath("$.talentosSugeridos.content[0].quantidadeFuncoesCoincidentes").doesNotExist())
                .andExpect(jsonPath("$.talentosSugeridos.content[1].quantidadeFuncoesCoincidentes").doesNotExist())
                .andExpect(jsonPath("$.talentosSugeridos.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.talentosSugeridos.content[0].dataNascimento").doesNotExist());
    }

    @Test
    void contratanteSemVagaAtivaComFuncoesNaoRecebeTalentoAleatorio() throws Exception {
        PerfilContratante dono = criarContratante("sem-contexto@rf11.test", true);
        Funcao funcao = criarFuncao("Sem contexto");
        criarVaga(dono, "Encerrada", StatusVaga.ENCERRADA, funcao);
        criarArtista("artista-aleatorio@rf11.test", true, LocalDate.of(1990, 1, 1), funcao);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.talentosSugeridos.content.length()").value(0))
                .andExpect(jsonPath("$.talentosSugeridos.totalElements").value(0));
    }

    @Test
    void talentosOrdenamPorCoincidenciaDepoisPorAtualizacaoMaisRecente() throws Exception {
        PerfilContratante dono = criarContratante("ordem-talentos@rf11.test", true);
        Funcao teatro = criarFuncao("Teatro ordem");
        Funcao musica = criarFuncao("Musica ordem");
        criarVaga(dono, "Contexto", StatusVaga.ABERTA, teatro, musica);
        PerfilArtista duasFuncoesAntigo = criarArtista(
                "duas-antigo@rf11.test", true, LocalDate.of(1990, 1, 1), teatro, musica);
        PerfilArtista duasFuncoesNovo = criarArtista(
                "duas-novo@rf11.test", true, LocalDate.of(1990, 1, 1), teatro, musica);
        PerfilArtista umaFuncao = criarArtista(
                "uma@rf11.test", true, LocalDate.of(1990, 1, 1), teatro);
        jdbcTemplate.update("insert into banco_talentos(contratante_id,artista_id) values (?,?),(?,?),(?,?)",
                dono.getUsuarioId(), duasFuncoesAntigo.getUsuarioId(), dono.getUsuarioId(), duasFuncoesNovo.getUsuarioId(),
                dono.getUsuarioId(), umaFuncao.getUsuarioId());
        jdbcTemplate.update("update perfis_artistas set ultima_atualizacao=? where usuario_id=?",
                LocalDateTime.now().minusDays(2), duasFuncoesAntigo.getUsuarioId());
        jdbcTemplate.update("update perfis_artistas set ultima_atualizacao=? where usuario_id=?",
                LocalDateTime.now(), duasFuncoesNovo.getUsuarioId());
        jdbcTemplate.update("update perfis_artistas set ultima_atualizacao=? where usuario_id=?",
                LocalDateTime.now().plusDays(1), umaFuncao.getUsuarioId());

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.talentosSugeridos.content[0].artistaId").value(duasFuncoesNovo.getUsuarioId()))
                .andExpect(jsonPath("$.talentosSugeridos.content[1].artistaId").value(duasFuncoesAntigo.getUsuarioId()))
                .andExpect(jsonPath("$.talentosSugeridos.content[2].artistaId").value(umaFuncao.getUsuarioId()));
    }

    @Test
    void notificacoesRf23EMensagensRf24EstaoDisponiveis() throws Exception {
        PerfilArtista artista = criarArtista("modulos@rf11.test", true, LocalDate.of(1990, 1, 1));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacoes.disponivel").value(true))
                .andExpect(jsonPath("$.notificacoes.mensagem").value(
                        org.hamcrest.Matchers.containsString("tempo real")))
                .andExpect(jsonPath("$.notificacoes.quantidade").doesNotExist())
                .andExpect(jsonPath("$.mensagens.disponivel").value(true))
                .andExpect(jsonPath("$.mensagens.mensagem").value(
                        org.hamcrest.Matchers.containsString("privacidade")))
                .andExpect(jsonPath("$.mensagens.quantidadeNaoLidas").value(0));
    }

    @Test
    void paginacaoInformaTotalEHasMore() throws Exception {
        Funcao funcao = criarFuncao("Paginada");
        PerfilArtista artista = criarArtista(
                "paginacao@rf11.test", true, LocalDate.of(1990, 1, 1), funcao);
        PerfilContratante contratante = criarContratante("paginador@rf11.test", true);
        IntStream.range(0, 3).forEach(i -> criarVaga(contratante, "Vaga " + i, StatusVaga.ABERTA, funcao));

        mockMvc.perform(get("/api/dashboard").param("size", "2")
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vagasRecomendadas.content.length()").value(2))
                .andExpect(jsonPath("$.vagasRecomendadas.totalElements").value(3))
                .andExpect(jsonPath("$.vagasRecomendadas.hasMore").value(true));
    }

    @Test
    void vinteRecomendacoesNaoDisparamNMaisUm() throws Exception {
        Funcao funcao = criarFuncao("Escalavel");
        PerfilArtista artista = criarArtista(
                "nmaisum@rf11.test", true, LocalDate.of(1990, 1, 1), funcao);
        PerfilContratante contratante = criarContratante("volume@rf11.test", true);
        IntStream.range(0, 20).forEach(i -> criarVaga(contratante, "Vaga volume " + i, StatusVaga.ABERTA, funcao));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/dashboard").param("size", "20")
                        .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vagasRecomendadas.content.length()").value(5))
                .andExpect(jsonPath("$.vagasRecomendadas.totalElements").value(20));

        long cinco = statistics.getPrepareStatementCount();
        long jdbcCinco = chamadasJdbc();
        org.mockito.Mockito.clearInvocations(namedJdbc);
        statistics.clear();
        painel(artista.getUsuario(), 1);
        long uma = statistics.getPrepareStatementCount();
        long jdbcUma = chamadasJdbc();
        System.out.println("RF11 consultas artista: preview1="+uma+" preview5="+cinco+" JDBC1="+jdbcUma+" JDBC5="+jdbcCinco);
        // Ambos os tamanhos têm mais resultados: COUNT não é omitido pelo Pageable.
        assertThat(cinco).isLessThanOrEqualTo(uma);
        assertThat(jdbcCinco).isEqualTo(jdbcUma);
    }

    @Test
    void vinteCandidaturasEVinteTalentosNaoDisparamNMaisUm() throws Exception {
        PerfilContratante dono = criarContratante("volume-contratante@rf11.test", true);
        Funcao funcao = criarFuncao("Volume contratante");
        Vaga vaga = criarVaga(dono, "Vaga para vinte", StatusVaga.ABERTA, funcao);
        IntStream.range(0, 20).forEach(i -> {
            PerfilArtista artista = criarArtista(
                    "volume-artista-" + i + "@rf11.test",
                    true,
                    LocalDate.of(1990, 1, 1),
                    funcao);
            criarCandidatura(vaga, artista, LocalDateTime.now().minusMinutes(i));
            jdbcTemplate.update("insert into banco_talentos(contratante_id,artista_id) values (?,?)",
                    dono.getUsuarioId(), artista.getUsuarioId());
        });
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        org.mockito.Mockito.clearInvocations(namedJdbc);

        mockMvc.perform(get("/api/dashboard").param("size", "20")
                        .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidaturasRecentes.content.length()").value(5))
                .andExpect(jsonPath("$.talentosSugeridos.content.length()").value(5))
                .andExpect(jsonPath("$.candidaturasRecentes.totalElements").value(20))
                .andExpect(jsonPath("$.talentosSugeridos.totalElements").value(20));

        long cinco = statistics.getPrepareStatementCount();
        long jdbcCinco = chamadasJdbc();
        org.mockito.Mockito.clearInvocations(namedJdbc);
        statistics.clear();
        painel(dono.getUsuario(), 1);
        long uma = statistics.getPrepareStatementCount();
        long jdbcUma = chamadasJdbc();
        System.out.println("RF11 consultas contratante: preview1="+uma+" preview5="+cinco+" JDBC1="+jdbcUma+" JDBC5="+jdbcCinco);
        // Page de uma vaga dispensa COUNT para size=5, mas pode contar para size=1.
        assertThat(cinco).isLessThanOrEqualTo(uma);
        assertThat(jdbcCinco).isEqualTo(jdbcUma);
    }

@org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = com.portifolio.model.enums.StatusConta.class,
            mode = org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE, names = "ATIVA")
    void tokenAntigoNaoLiberaContaInapta(com.portifolio.model.enums.StatusConta estado) throws Exception {
        var artista = criarArtista("estado@rf11.test", true, LocalDate.of(1990, 1, 1));
        String token = bearer(artista.getUsuario());
        jdbcTemplate.update("update usuarios set status_conta=? where id=?", estado.name(), artista.getUsuarioId());
        mockMvc.perform(get("/api/dashboard").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = TipoUsuario.class, names = {"ADMIN", "MODERADOR"})
    void equipeNaoRecebeFallbackContratante(TipoUsuario tipo) throws Exception {
        var u = criarUsuario("equipe@rf11.test", tipo, false, LocalDate.of(1990, 1, 1));
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(u))).andExpect(status().isForbidden());
    }

    @Test void tokenExpiradoRevogadoEUsuarioRemovidoNaoAcessam() throws Exception {
        var artista = criarArtista("token@rf11.test", true, LocalDate.of(1990, 1, 1));
        Long expiracao = (Long) org.springframework.test.util.ReflectionTestUtils.getField(jwtService, "expiration");
        String expirado;
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
            expirado = bearer(artista.getUsuario());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(jwtService, "expiration", expiracao);
        }
        mockMvc.perform(get("/api/dashboard").header("Authorization", expirado)).andExpect(status().isUnauthorized());
        String revogado = bearer(artista.getUsuario());
        jwtService.revogar(revogado.substring(7));
        mockMvc.perform(get("/api/dashboard").header("Authorization", revogado)).andExpect(status().isUnauthorized());
        String antigo = bearer(artista.getUsuario());
        jdbcTemplate.update("delete from usuarios where id=?", artista.getUsuarioId());
        mockMvc.perform(get("/api/dashboard").header("Authorization", antigo)).andExpect(status().isUnauthorized());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {13, 14, 17})
    void idadeProtegidaSemAutorizacaoNaoAcessa(int idade) throws Exception {
        var a = criarArtista("menor-sem@rf11.test", true, LocalDate.now().minusYears(idade));
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(a.getUsuario())))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {14, 17})
    void menorAutorizadoTemPainelMinimoEConsentimentoAtual(int idade) throws Exception {
        var a = criarArtista("menor-autorizado@rf11.test", true, LocalDate.now().minusYears(idade));
        autorizar(a.getUsuarioId());
        String token = bearer(a.getUsuario());
        var result = mockMvc.perform(get("/api/dashboard").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfilCompleto").value(true)).andReturn();
        verificarPrivacidade(result.getResponse().getContentAsString());
        jdbcTemplate.update("update responsaveis_legais set consentimento_revogado=true where usuario_id=?", a.getUsuarioId());
        mockMvc.perform(get("/api/dashboard").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = StatusVaga.class,
            mode = org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE, names = "ABERTA")
    void recomendacaoNaoIncluiOutrosEstados(StatusVaga estado) throws Exception {
        var f = criarFuncao("Estado recomendação");
        var a = criarArtista("estado-vaga@rf11.test", true, LocalDate.of(1990, 1, 1), f);
        var c = criarContratante("dono-estado@rf11.test", true);
        criarVaga(c, "Fora do feed", estado, f);
        criarVaga(c, "Elegível", StatusVaga.ABERTA, f);
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(a.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vagasRecomendadas.totalElements").value(1))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].titulo").value("Elegível"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {-1, 0, 1})
    void recomendacaoRespeitaBordaDoPrazoSemAlterarEstado(int dias) throws Exception {
        var f = criarFuncao("Prazo");
        var a = criarArtista("prazo@rf11.test", true, LocalDate.of(1990, 1, 1), f);
        var c = criarContratante("dono-prazo@rf11.test", true);
        var v = criarVaga(c, "Prazo controlado", StatusVaga.ABERTA, f);
        jdbcTemplate.update("update vagas set data_limite_candidatura=? where id=?", LocalDate.now().plusDays(dias), v.getId());
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(a.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vagasRecomendadas.totalElements").value(dias > 0 ? 1 : 0));
        assertThat(jdbcTemplate.queryForObject("select status::text from vagas where id=?", String.class, v.getId())).isEqualTo("ABERTA");
    }

    @Test void hierarquiaAreasSecundariasEspecializacoesEExplicacaoReal() throws Exception {
        var f1 = criarFuncao("Primária");
        var a = criarArtista("hierarquia@rf11.test", true, LocalDate.of(1990, 1, 1), f1);
        var c = criarContratante("hierarquia-dono@rf11.test", true);
        Long f2id = jdbcTemplate.queryForObject("insert into funcoes(area_id,nome) values(2,'Secundária') returning id", Long.class);
        jdbcTemplate.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal,nivel_experiencia) values(?,2,false,'INICIANTE')", a.getUsuarioId());
        jdbcTemplate.update("insert into perfil_artista_funcao(perfil_artista_id,area_id,funcao_id) values(?,2,?)", a.getUsuarioId(), f2id);
        Long spec = jdbcTemplate.queryForObject("insert into especializacoes(nome) values('Especialização secundária') returning id", Long.class);
        jdbcTemplate.update("insert into funcao_especializacao(funcao_id,especializacao_id) values(?,?)", f2id, spec);
        jdbcTemplate.update("insert into perfil_artista_especializacao(perfil_artista_id,area_id,especializacao_id) values(?,2,?)", a.getUsuarioId(), spec);
        var principal = criarVaga(c, "Principal", StatusVaga.ABERTA, f1);
        var secundaria = criarVaga(c, "Secundária especializada", StatusVaga.ABERTA);
        jdbcTemplate.update("update vagas set area_id=2 where id=?", secundaria.getId());
        jdbcTemplate.update("insert into vaga_funcao(vaga_id,funcao_id) values(?,?)", secundaria.getId(), f2id);
        jdbcTemplate.update("insert into vaga_especializacao(vaga_id,especializacao_id) values(?,?)", secundaria.getId(), spec);
        var outra = criarVaga(c, "Área incompatível", StatusVaga.ABERTA);
        jdbcTemplate.update("update vagas set area_id=3 where id=?", outra.getId());
        // Mesmo timestamp: especialização refina o empate de funções; principal não ganha peso.
        jdbcTemplate.update("update vagas set data_publicacao='2026-01-01'");
        var first = mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(a.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vagasRecomendadas.totalElements").value(2))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].id").value(secundaria.getId()))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].areaId").value(2))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].quantidadeFuncoesCoincidentes").value(1))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].quantidadeEspecializacoesCoincidentes").value(1))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].motivoRecomendacao").value(
                        org.hamcrest.Matchers.containsString("1 especialização")))
                .andExpect(jsonPath("$.vagasRecomendadas.content[1].id").value(principal.getId())).andReturn();
        var second = painel(a.getUsuario(), 5);
        assertThat(second).isEqualTo(first.getResponse().getContentAsString());
        verificarPrivacidade(second);
    }

    @Test void funcaoPrecedeEspecializacaoEDesempateUsaIdSemEngajamento() throws Exception {
        var f1 = criarFuncao("Função 1"); var f2 = criarFuncao("Função 2");
        var a = criarArtista("ordem@rf11.test", true, LocalDate.of(1990, 1, 1), f1, f2);
        var c = criarContratante("ordem-dono@rf11.test", true);
        Long spec = jdbcTemplate.queryForObject("select especializacao_id from perfil_artista_especializacao where perfil_artista_id=? limit 1", Long.class, a.getUsuarioId());
        var especializada = criarVaga(c, "Uma especializada", StatusVaga.ABERTA, f1);
        jdbcTemplate.update("insert into vaga_especializacao(vaga_id,especializacao_id) values(?,?)", especializada.getId(), spec);
        var duas = criarVaga(c, "Duas funções", StatusVaga.ABERTA, f1, f2);
        var empate = criarVaga(c, "Empate mais novo ID", StatusVaga.ABERTA, f1, f2);
        jdbcTemplate.update("update vagas set data_publicacao='2026-01-01'");
        String antes = painel(a.getUsuario(), 5);
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(a.getUsuario())))
                .andExpect(jsonPath("$.vagasRecomendadas.content[0].id").value(empate.getId()))
                .andExpect(jsonPath("$.vagasRecomendadas.content[1].id").value(duas.getId()))
                .andExpect(jsonPath("$.vagasRecomendadas.content[2].id").value(especializada.getId()));
        // RF19 é independente: salvar a vaga de menor relevância não altera matching.
        jdbcTemplate.update("insert into itens_salvos(usuario_id,tipo_alvo,alvo_id) values(?,'VAGA',?)", a.getUsuarioId(), especializada.getId());
        assertThat(painel(a.getUsuario(), 5)).isEqualTo(antes);
        assertThat(antes).doesNotContain("score", "medalha", "seguidores", "engajamento", "87%");
    }

    @Test void minhasCandidaturasSaoPropriasLimitadasComStatusReal() throws Exception {
        var a = criarArtista("apps@rf11.test", true, LocalDate.of(1990, 1, 1));
        var outro = criarArtista("apps-outro@rf11.test", true, LocalDate.of(1990, 1, 1));
        var c = criarContratante("apps-dono@rf11.test", true);
        for (int i=0; i<7; i++) criarCandidatura(criarVaga(c, "Minha "+i, StatusVaga.ABERTA), a, LocalDateTime.now().plusMinutes(i));
        criarCandidatura(criarVaga(c, "Alheia", StatusVaga.ABERTA), outro, LocalDateTime.now().plusDays(1));
        var result = mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(a.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.minhasCandidaturas.content.length()").value(5))
                .andExpect(jsonPath("$.minhasCandidaturas.totalElements").value(7))
                .andExpect(jsonPath("$.minhasCandidaturas.hasMore").value(true))
                .andExpect(jsonPath("$.minhasCandidaturas.content[0].tituloVaga").value("Minha 6"))
                .andExpect(jsonPath("$.minhasCandidaturas.content[0].status").value("PENDENTE")).andReturn();
        var minhas = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString())
                .path("minhasCandidaturas").toString();
        assertThat(minhas).doesNotContain("Alheia", "ACEITA", "REJEITADA", "Mensagem privada");
        verificarPrivacidade(result.getResponse().getContentAsString());
    }

    @Test void incompletoSinalizaMasNaoConcedePermissaoRf06() throws Exception {
        var f = criarFuncao("Incompleto RF06");
        var a = criarArtista("incompleto-rf06@rf11.test", false, LocalDate.of(1990, 1, 1), f);
        var c = criarContratante("dono-rf06@rf11.test", true);
        var v = criarVaga(c, "Candidatar", StatusVaga.ABERTA, f);
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(a.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfilIncompleto").value(true));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/candidaturas")
                .header("Authorization", bearer(a.getUsuario())).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"confirmacao\":true,\"vagaId\":"+v.getId()+",\"mensagemApresentacao\":\"Interesse profissional\",\"linkPortfolioCandidatura\":\"https://example.org/p\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(candidaturaRepository.count()).isZero();
    }

    @Test void contratanteResumoEstadosEPreviaSomenteDoDonoSemTransicaoDePrazo() throws Exception {
        var c = criarContratante("estados-dono@rf11.test", true);
        var outro = criarContratante("estados-outro@rf11.test", true);
        for (var s : StatusVaga.values()) criarVaga(c, "Própria "+s, s);
        criarVaga(outro, "Alheia", StatusVaga.ABERTA);
        jdbcTemplate.update("update vagas set data_limite_candidatura=? where contratante_id=? and status='ABERTA'", LocalDate.now(), c.getUsuarioId());
        var result = mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(c.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.minhasVagas.totalElements").value(5))
                .andExpect(jsonPath("$.minhasVagas.content.length()").value(5))
                .andExpect(jsonPath("$.vagasPorStatus.ABERTA").value(1))
                .andExpect(jsonPath("$.vagasPorStatus.RASCUNHO").value(1))
                .andExpect(jsonPath("$.vagasPorStatus.PAUSADA").value(1))
                .andExpect(jsonPath("$.vagasPorStatus.ENCERRADA").value(1))
                .andExpect(jsonPath("$.vagasPorStatus.CANCELADA").value(1)).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("Alheia");
        assertThat(jdbcTemplate.queryForObject("select count(*) from vagas where contratante_id=? and status='ABERTA'", Long.class, c.getUsuarioId())).isEqualTo(1);
    }

    @Test void recentesUsamVigenciaRf45SemRetiradasDuplicacaoNemUsuarioRemovido() throws Exception {
        var c = criarContratante("vigencia-dono@rf11.test", true);
        var a = criarArtista("vigencia@rf11.test", true, LocalDate.of(1990, 1, 1));
        var menor = criarArtista("vigencia-menor@rf11.test", true, LocalDate.now().minusYears(17));
        autorizar(menor.getUsuarioId());
        var v = criarVaga(c, "Vaga vigente", StatusVaga.ABERTA);
        var antiga = criarCandidatura(v, a, LocalDateTime.now().minusDays(1));
        jdbcTemplate.update("update candidaturas set status='RETIRADA' where id=?", antiga.getId());
        var atual = criarCandidatura(v, a, LocalDateTime.now());
        criarCandidatura(v, menor, LocalDateTime.now().plusMinutes(1));
        var result = mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(c.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.candidaturasRecentes.totalElements").value(2))
                .andExpect(jsonPath("$.candidaturasRecentes.content[1].id").value(atual.getId())).andReturn();
        verificarPrivacidade(result.getResponse().getContentAsString());
        jdbcTemplate.update("update candidaturas set status='RETIRADA' where id=?", atual.getId());
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(c.getUsuario())))
                .andExpect(jsonPath("$.candidaturasRecentes.totalElements").value(1));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = TipoUsuario.class, names = {"ARTISTA", "CONTRATANTE"})
    void mensagensNotificacoesReaisPropriasSemLeituraOuEfeitosColaterais(TipoUsuario tipo) throws Exception {
        Usuario u = tipo == TipoUsuario.ARTISTA ? criarArtista("counts@rf11.test", true, LocalDate.of(1990,1,1)).getUsuario()
                : criarContratante("counts@rf11.test", true).getUsuario();
        var outro = criarArtista("counts-outro@rf11.test", true, LocalDate.of(1990,1,1)).getUsuario();
        Long sala = jdbcTemplate.queryForObject("insert into salas_chat default values returning id", Long.class);
        Long alheia = jdbcTemplate.queryForObject("insert into salas_chat default values returning id", Long.class);
        jdbcTemplate.update("insert into participantes_chat(sala_id,usuario_id) values(?,?),(?,?),(?,?)", sala,u.getId(),sala,outro.getId(),alheia,outro.getId());
        jdbcTemplate.update("insert into mensagens_chat(sala_id,remetente_id,texto_mensagem,lida) values(?,?,?,false),(?,?,?,false),(?,?,?,true),(?,?,?,false)",
                sala,outro.getId(),"recebida",sala,u.getId(),"própria",sala,outro.getId(),"lida",alheia,outro.getId(),"alheia");
        jdbcTemplate.update("insert into mensagens_chat(sala_id,remetente_id,texto_mensagem,texto_original,url_anexo,lida,excluida) values(?,null,'Mensagem excluída','Evidência restrita','/storage/privado',false,true)", sala);
        jdbcTemplate.update("insert into notificacoes(usuario_destino_id,tipo_notificacao,mensagem_alerta,link_contexto,lida) values(?,'MENSAGEM','Recebida','/mensagens',false),(?,'MENSAGEM','Já lida','/mensagens',true),(?,'MENSAGEM','Alheia','/mensagens',false)",u.getId(),u.getId(),outro.getId());
        var antes = snapshot();
        var result = mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(u)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensagens.quantidadeNaoLidas").value(2))
                .andExpect(jsonPath("$.notificacoes.quantidadeNaoLidas").value(1)).andReturn();
        assertThat(snapshot()).isEqualTo(antes);
        verificarPrivacidade(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getContentAsString()).doesNotContain("Evidência restrita", "/storage/privado", "recebida", "Alheia");
    }

    @Test void bancoProprioCountSemContextoNuncaCriaSalaOuMembership() throws Exception {
        var c = criarContratante("banco-count@rf11.test", true);
        var outro = criarContratante("banco-outro@rf11.test", true);
        var a = criarArtista("banco-artista@rf11.test", true, LocalDate.of(1990,1,1));
        var b = criarArtista("banco-alheio@rf11.test", true, LocalDate.of(1990,1,1));
        jdbcTemplate.update("insert into banco_talentos(contratante_id,artista_id) values(?,?),(?,?)",c.getUsuarioId(),a.getUsuarioId(),outro.getUsuarioId(),b.getUsuarioId());
        var antes = snapshot();
        mockMvc.perform(get("/api/dashboard").param("contratanteId",outro.getUsuarioId().toString())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"usuarioId\":"+outro.getUsuarioId()+"}")
                .header("Authorization", bearer(c.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeBancoTalentos").value(1))
                .andExpect(jsonPath("$.talentosSugeridos.content.length()").value(0));
        assertThat(snapshot()).isEqualTo(antes);
    }

    @Test void bancoPreviewRf17IsoladoEMenorSemExperienciaPrivada() throws Exception {
        var f = criarFuncao("Banco menor");
        var dono = criarContratante("banco-preview@rf11.test", true);
        var outro = criarContratante("banco-preview-outro@rf11.test", true);
        criarVaga(dono,"Contexto próprio",StatusVaga.ABERTA,f);
        var menor = criarArtista("banco-menor@rf11.test",true,LocalDate.now().minusYears(17),f);
        autorizar(menor.getUsuarioId());
        var alheio = criarArtista("membro-alheio@rf11.test",true,LocalDate.of(1990,1,1),f);
        criarArtista("publico-fora-banco@rf11.test",true,LocalDate.of(1990,1,1),f);
        jdbcTemplate.update("insert into banco_talentos(contratante_id,artista_id) values(?,?),(?,?)",
                dono.getUsuarioId(),menor.getUsuarioId(),outro.getUsuarioId(),alheio.getUsuarioId());
        var antes = snapshot();
        var result = mockMvc.perform(get("/api/dashboard").header("Authorization",bearer(dono.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeBancoTalentos").value(1))
                .andExpect(jsonPath("$.talentosSugeridos.totalElements").value(1))
                .andExpect(jsonPath("$.talentosSugeridos.content[0].artistaId").value(menor.getUsuarioId())).andReturn();
        verificarPrivacidade(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getContentAsString()).doesNotContain("membro-alheio","publico-fora-banco","INICIANTE");
        assertThat(snapshot()).isEqualTo(antes);
    }

    private long chamadasJdbc() {
        return org.mockito.Mockito.mockingDetails(namedJdbc).getInvocations().stream()
                .filter(i -> i.getMethod().getName().startsWith("query")).count();
    }

    private String painel(Usuario u, int size) throws Exception {
        return mockMvc.perform(get("/api/dashboard").param("size",String.valueOf(size)).header("Authorization",bearer(u)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void autorizar(Long artista) {
        jdbcTemplate.update("insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,data_consentimento) values(?,'Responsável privado','11900009999','guardiao@rf11.invalid',current_timestamp)", artista);
    }

    private java.util.Map<String,String> snapshot() {
        var resultado = new java.util.LinkedHashMap<String,String>();
        for (String tabela : new String[]{"usuarios","perfis_artistas","perfil_artista_area","vagas","candidaturas",
                "salas_chat","participantes_chat","mensagens_chat","notificacoes","banco_talentos"}) {
            resultado.put(tabela, jdbcTemplate.queryForObject("select coalesce(jsonb_agg(to_jsonb(t) order by to_jsonb(t)::text),'[]'::jsonb)::text from "+tabela+" t", String.class));
        }
        return resultado;
    }

    private void verificarPrivacidade(String response) throws Exception {
        var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        var proibidos = Set.of("cpf","cnpj","telefone","email","dataNascimento","responsavel","responsavelLegal",
                "consentimento","senha","refreshToken","nivelExperiencia","textoOriginal","texto_original","urlAnexo","storagePath");
        var pendentes = new java.util.ArrayDeque<com.fasterxml.jackson.databind.JsonNode>(); pendentes.add(root);
        while(!pendentes.isEmpty()) {
            var node = pendentes.remove();
            if(node.isObject()) node.fieldNames().forEachRemaining(key -> assertThat(proibidos).doesNotContain(key));
            node.elements().forEachRemaining(pendentes::add);
        }
        assertThat(response).doesNotContain("guardiao@rf11.invalid","Responsável privado","11900009999","hash-privado");
    }

    private Usuario criarUsuario(String email, TipoUsuario tipo, boolean completo, LocalDate nascimento) {
        Usuario usuario = com.portifolio.support.OfficialSchemaFixtures.usuario();
        usuario.setNome("Pessoa " + email.substring(0, email.indexOf('@')));
        usuario.setDataNascimento(nascimento);
        usuario.setTelefone("11999999999");
        usuario.setEmail(email);
        usuario.setSenha("hash-privado");
        usuario.setTipoUsuario(tipo);
        usuario.setStatusConta(com.portifolio.model.enums.StatusConta.ATIVA);
        usuario.setPerfilCompleto(false);
        usuario.setDataCriacao(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private PerfilArtista criarArtista(String email, boolean completo, LocalDate nascimento, Funcao... funcoes) {
        Usuario usuario = criarUsuario(email, TipoUsuario.ARTISTA, completo, nascimento);
        PerfilArtista perfil = new PerfilArtista();
        perfil.setTipoPerfilArtistico(com.portifolio.model.enums.TipoPerfilArtistico.ARTISTA_SOLO);
        perfil.setRaioAtuacao(com.portifolio.model.enums.Abrangencia.LOCAL);
        perfil.setUsuario(usuario);
        perfil.setBiografia("Biografia publica");
        perfil.setLocalizacao("Sao Paulo - SP");
        perfil.setUrlPortfolio("https://portfolio.example/" + email);
        com.portifolio.support.OfficialSchemaFixtures.funcoes(perfil, new HashSet<>(Set.of(funcoes)));
        perfil = perfilArtistaRepository.saveAndFlush(perfil);
        perfil.setUsuario(usuario);
        if (completo) {
            if (funcoes.length == 0) {
                com.portifolio.support.OfficialSchemaFixtures.completarArtista(
                        jdbcTemplate, perfil.getUsuarioId());
            } else {
                com.portifolio.support.OfficialSchemaFixtures.completarArtista(
                        jdbcTemplate, perfil.getUsuarioId(), funcoes[0].getId());
            }
            perfil.getUsuario().setPerfilCompleto(true);
        }
        return perfil;
    }

    private PerfilContratante criarContratante(String email, boolean completo) {
        Usuario usuario = criarUsuario(email, TipoUsuario.CONTRATANTE, completo, LocalDate.of(1985, 1, 1));
        PerfilContratante perfil = new PerfilContratante();
        perfil.setTipoPerfil("PESSOA_FISICA");
        perfil.setUsuario(usuario);
        perfil.setNomeEmpresa("Empresa " + email.substring(0, email.indexOf('@')));
        perfil.setBiografia("Biografia pública do contratante");
        perfil.setLocalizacao("São Paulo, SP");
        perfil = perfilContratanteRepository.saveAndFlush(perfil);
        perfil.setUsuario(usuario);
        if (completo) {
            com.portifolio.support.OfficialSchemaFixtures.completarContratante(
                    jdbcTemplate, perfil.getUsuarioId());
            perfil.getUsuario().setPerfilCompleto(true);
        }
        return perfil;
    }

    private Funcao criarFuncao(String nome) {
        Funcao funcao = new Funcao();
        funcao.setArea(com.portifolio.support.OfficialSchemaFixtures.area());
        funcao.setNome(nome);
        return funcaoRepository.save(funcao);
    }

    private Vaga criarVaga(PerfilContratante contratante, String titulo, StatusVaga status, Funcao... funcoes) {
        Vaga vaga = new Vaga();
        vaga.setArea(com.portifolio.support.OfficialSchemaFixtures.area());
        vaga.setAbrangencia(com.portifolio.model.enums.Abrangencia.LOCAL);
        vaga.setContratante(contratante);
        vaga.setTitulo(titulo);
        vaga.setDescricao("Descricao da vaga");
        vaga.setRequisitos("Requisitos da vaga");
        vaga.setValorMinimo(new BigDecimal("1000.00"));
        vaga.setValorMaximo(new BigDecimal("1000.00"));
        vaga.setFormaRemuneracao(com.portifolio.model.enums.FormaRemuneracao.POR_EVENTO);
        vaga.setCidade("Sao Paulo");
        vaga.setEstado("SP");
        vaga.setModeloTrabalho(ModeloTrabalho.PRESENCIAL);
        vaga.setTipoContrato("Freelancer");
        vaga.setStatus(status);
        vaga.setDataPublicacao(LocalDateTime.now());
        vaga.setFuncoes(new HashSet<>(Set.of(funcoes)));
        return vagaRepository.save(vaga);
    }

    private Candidatura criarCandidatura(Vaga vaga, PerfilArtista artista, LocalDateTime data) {
        Candidatura candidatura = new Candidatura();
        candidatura.setVaga(vaga);
        candidatura.setArtista(artista);
        candidatura.setMensagemApresentacao("Mensagem privada");
        candidatura.setLinkPortfolioCandidatura("https://privado.example/portfolio");
        candidatura.setStatus(StatusCandidatura.PENDENTE);
        candidatura.setDataCandidatura(data);
        return candidaturaRepository.save(candidatura);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
