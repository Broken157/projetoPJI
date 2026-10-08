package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.portifolio.event.AvisoResponsavelCandidaturaEvento;
import com.portifolio.event.AvisoResponsavelCandidaturaListener;
import com.portifolio.model.*;
import com.portifolio.model.enums.*;
import com.portifolio.repository.*;
import com.portifolio.security.JwtService;
import com.portifolio.service.GuardianApplicationNoticeSender;
import com.portifolio.service.GuardianApplicationNoticeService;
import com.portifolio.support.OfficialSchemaFixtures;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AvisoResponsavelRf44IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilArtistaRepository artistas;
    @Autowired PerfilContratanteRepository contratantes;
    @Autowired ResponsavelLegalRepository responsaveis;
    @Autowired CandidaturaRepository candidaturas;
    @Autowired VagaRepository vagas;
    @Autowired JwtService jwt;
    @Autowired PlatformTransactionManager manager;
    @Autowired ApplicationEventPublisher eventos;
    @Autowired org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter mvcAdapter;
    @Autowired GuardianApplicationNoticeService avisos;
    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory emf;
    @Autowired com.portifolio.security.MenorAutorizadoPolicy menores;
    @Autowired jakarta.validation.Validator validator;
    @Autowired @Qualifier("avisosResponsavelExecutor") ThreadPoolTaskExecutor executor;
    @MockitoBean GuardianApplicationNoticeSender sender;

    private final List<Envio> envios = new CopyOnWriteArrayList<>();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger serviceLogger = (Logger) LoggerFactory.getLogger(GuardianApplicationNoticeService.class);
    private final Logger listenerLogger = (Logger) LoggerFactory.getLogger(AvisoResponsavelCandidaturaListener.class);
    private PerfilContratante dono;
    private Vaga vaga;

    @BeforeEach void preparar() {
        jdbc.execute("TRUNCATE candidaturas, vagas, perfis_artistas, perfis_contratantes, usuarios RESTART IDENTITY CASCADE");
        logs.list = new CopyOnWriteArrayList<>();
        logs.start();
        serviceLogger.addAppender(logs);
        listenerLogger.addAppender(logs);
        doAnswer(invocacao -> {
            envios.add(new Envio(invocacao.getArgument(0), invocacao.getArgument(1),
                    TransactionSynchronizationManager.isActualTransactionActive(),
                    TransactionSynchronizationManager.hasResource(dataSource)
                            || TransactionSynchronizationManager.hasResource(emf),
                    candidaturas.count()));
            return null;
        }).when(sender).enviarAviso(any(), any());
        var usuario = usuario("dono@rf44.test", TipoUsuario.CONTRATANTE, 35);
        dono = new PerfilContratante();
        dono.setUsuario(usuario);
        dono.setTipoPerfil("PESSOA_FISICA");
        dono.setNomeEmpresa("Contratante RF44");
        dono = contratantes.saveAndFlush(dono);
        vaga = new Vaga();
        vaga.setContratante(dono);
        vaga.setArea(OfficialSchemaFixtures.area());
        vaga.setAbrangencia(Abrangencia.LOCAL);
        vaga.setTitulo("Oportunidade RF44");
        vaga.setDescricao("Contexto público da vaga");
        vaga.setRequisitos("Requisitos públicos");
        vaga.setValorMinimo(new BigDecimal("1000"));
        vaga.setValorMaximo(new BigDecimal("1000"));
        vaga.setFormaRemuneracao(FormaRemuneracao.POR_EVENTO);
        vaga.setCidade("São Paulo");
        vaga.setEstado("SP");
        vaga.setModeloTrabalho(ModeloTrabalho.REMOTO);
        vaga.setTipoContrato("Freelance");
        vaga.setStatus(StatusVaga.ABERTA);
        vaga.setDataPublicacao(LocalDateTime.now());
        vaga = vagas.saveAndFlush(vaga);
    }

    @AfterEach void limpar() {
        aguardar();
        serviceLogger.detachAppender(logs);
        listenerLogger.detachAppender(logs);
        logs.stop();
        jdbc.execute("TRUNCATE candidaturas, vagas, perfis_artistas, perfis_contratantes, usuarios RESTART IDENTITY CASCADE");
    }

    @ParameterizedTest @ValueSource(ints = {14, 15, 17})
    void menorAutorizadoAvisaResponsavelPersistidoSemAceitarDestinatarioCliente(int idade) throws Exception {
        var artista = artista(idade, true);
        var outro = artista("outro@rf44.test", 16, true);
        responsaveis.findAll().stream().filter(r -> r.getUsuario().getId().equals(outro.getUsuarioId()))
                .forEach(r -> {r.setEmailResponsavel("outro.responsavel@rf44.test");responsaveis.saveAndFlush(r);});
        String resposta = mvc.perform(post("/api/candidaturas").header("Authorization", bearer(artista))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmacao\":true,\"vagaId\":" + vaga.getId()
                                + ",\"emailResponsavel\":\"arbitrario@rf44.test\",\"responsavelId\":999}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        aguardar();
        assertThat(envios).hasSize(1);
        assertThat(envios.getFirst()).isEqualTo(new Envio("responsavel.certo@rf44.test", vaga.getId(), false, false, 1));
        assertThat(resposta).doesNotContain("responsavel", "consentimento", "Responsável Privado RF44", "token");
        assertThat(jdbc.queryForList("select usuario_destino_id from notificacoes", Long.class))
                .containsExactly(dono.getUsuarioId());
        assertThat(jdbc.queryForObject("select mensagem_alerta from notificacoes limit 1", String.class))
                .doesNotContain("responsavel", "Privado", "11987654321", "token");
    }

    @Test void envioSoIniciaAposCommitEEncerraLeituraAntesDoSender() {
        var artista = artista(16, true);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            assertThat(criar(artista, vaga.getId())).isEqualTo(201);
            verifyNoInteractions(sender);
            assertThat(executor.getThreadPoolExecutor().getQueue()).isEmpty();
        });
        aguardar();
        assertThat(envios).hasSize(1);
        assertThat(envios.getFirst().transacaoAtiva()).isFalse();
        assertThat(envios.getFirst().recursoVinculado()).isFalse();
        assertThat(envios.getFirst().candidaturasPersistidas()).isOne();
    }

    @Test void rollbackNaoAvisaNemPersisteCandidaturaOuRf36() {
        var artista = artista(16, true);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            assertThat(criar(artista, vaga.getId())).isEqualTo(201);
            verifyNoInteractions(sender);
            tx.setRollbackOnly();
        });
        aguardar();
        assertThat(candidaturas.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
        verifyNoInteractions(sender);
        assertThat(logs.list).isEmpty();
    }

    @ParameterizedTest @ValueSource(ints = {18, 40})
    void adultoComResponsavelHistoricoNaoGeraAviso(int idade) {
        var artista = artista(idade, true);
        assertThat(criar(artista, vaga.getId())).isEqualTo(201);
        aguardar();
        verifyNoInteractions(sender);
        assertThat(logs.list).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"ausente", "pendente", "recusado"})
    void contaAtivaInconsistenteSemConsentimentoNaoCandidata(String caso) {
        var artista = artista(16, !caso.equals("ausente"));
        if (caso.equals("pendente")) jdbc.update("update responsaveis_legais set data_consentimento=null");
        if (caso.equals("recusado")) jdbc.update("update responsaveis_legais set consentimento_revogado=true");
        assertThat(criar(artista, vaga.getId())).isEqualTo(403);
        semEfeitos();
    }

    @ParameterizedTest @ValueSource(ints = {0, 13})
    void abaixoDe14NaoCandidataMesmoComConsentimentoInconsistente(int idade) {
        assertThat(criar(artista(idade, true), vaga.getId())).isEqualTo(403);
        semEfeitos();
    }

    @ParameterizedTest @EnumSource(value = StatusConta.class, names = {"ATIVA"}, mode = EnumSource.Mode.EXCLUDE)
    void contaInaptaComJwtAntigoNaoCandidata(StatusConta estado) {
        var artista = artista(16, true);
        jdbc.update("update usuarios set status_conta=? where id=?", estado.name(), artista.getUsuarioId());
        assertThat(criar(artista, vaga.getId())).isEqualTo(401);
        semEfeitos();
    }

    @ParameterizedTest @EnumSource(value = StatusVaga.class, names = {"ABERTA"}, mode = EnumSource.Mode.EXCLUDE)
    void vagaNaoAbertaNaoGeraAviso(StatusVaga estado) {
        vaga.setStatus(estado);
        vagas.saveAndFlush(vaga);
        assertThat(criar(artista(16, true), vaga.getId())).isEqualTo(422);
        semEfeitos();
    }

    @Test void vagaInexistenteNaoGeraAviso() {
        assertThat(criar(artista(16, true), 999999L)).isEqualTo(404);
        semEfeitos();
    }

    @Test void perfilIncompletoNaoGeraAviso() {
        var artista = artista(16, true);
        jdbc.update("update usuarios set perfil_completo=false where id=?", artista.getUsuarioId());
        assertThat(criar(artista, vaga.getId())).isEqualTo(422);
        semEfeitos();
    }

    @Test void recandidaturaTemNovoAvisoMasDuplicidadeETerceiraTentativaNao() throws Exception {
        var artista = artista(16, true);
        assertThat(criar(artista, vaga.getId())).isEqualTo(201);
        aguardar();
        assertThat(criar(artista, vaga.getId())).isEqualTo(409);
        var primeira = candidaturas.findAll().getFirst();
        retirar(artista, primeira.getId());
        assertThat(criar(artista, vaga.getId())).isEqualTo(201);
        aguardar();
        var segunda = candidaturas.findByVagaIdAndArtistaUsuarioIdOrderByIdDesc(vaga.getId(), artista.getUsuarioId()).getFirst();
        assertThat(segunda.getId()).isNotEqualTo(primeira.getId());
        retirar(artista, segunda.getId());
        assertThat(criar(artista, vaga.getId())).isEqualTo(409);
        aguardar();
        assertThat(envios).hasSize(2);
        assertThat(candidaturas.count()).isEqualTo(2);
        verify(sender, times(2)).enviarAviso("responsavel.certo@rf44.test", vaga.getId());
    }

    @Test void getListagemENavegacaoERetiradaNaoCriamAvisoAdicional() throws Exception {
        var artista = artista(16, true);
        assertThat(criar(artista, vaga.getId())).isEqualTo(201);
        aguardar();
        Long id = candidaturas.findAll().getFirst().getId();
        mvc.perform(get("/api/candidaturas").header("Authorization", bearer(artista))).andExpect(status().isOk());
        mvc.perform(get("/api/candidaturas/{id}", id).header("Authorization", bearer(artista))).andExpect(status().isOk());
        mvc.perform(get("/api/vagas/{id}", vaga.getId())).andExpect(status().isOk());
        retirar(artista, id);
        aguardar();
        assertThat(envios).hasSize(1);
    }

    @Test void concorrenciaGeraUmAvisoParaUmaCandidaturaPersistida() throws Exception {
        var artista = artista(16, true);
        var prontas = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);
        try (var clientes = Executors.newFixedThreadPool(2)) {
            var resultados = java.util.stream.IntStream.range(0, 2).mapToObj(i -> clientes.submit(() -> {
                prontas.countDown();
                inicio.await();
                return criar(artista, vaga.getId());
            })).toList();
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            assertThat(List.of(resultados.get(0).get(30, TimeUnit.SECONDS), resultados.get(1).get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        aguardar();
        assertThat(candidaturas.count()).isOne();
        assertThat(envios).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
    }

    @Test void smtpFalhaSemAlterarAcaoNemRf36ELogNaoContemPayload() {
        var artista = artista(16, true);
        doThrow(new IllegalStateException("responsavel.certo@rf44.test Nome Privado 11987654321 token-secreto-rf44"))
                .when(sender).enviarAviso(any(), any());
        assertThat(criar(artista, vaga.getId())).isEqualTo(201);
        aguardar();
        assertThat(candidaturas.findAll()).singleElement().extracting(Candidatura::getStatus)
                .isEqualTo(StatusCandidatura.PENDENTE);
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
        verify(sender, times(1)).enviarAviso("responsavel.certo@rf44.test", vaga.getId());
        assertThat(logs.list.stream().map(ILoggingEvent::getFormattedMessage))
                .anyMatch(m -> m.contains("resultado=INICIO"))
                .anyMatch(m -> m.contains("resultado=FALHA categoria=IllegalStateException"))
                .allMatch(m -> !m.contains("@") && !m.contains("Privado") && !m.contains("11987654321") && !m.contains("token-secreto"));
        assertThat(logs.list).allMatch(e -> e.getThrowableProxy() == null && e.getTimeStamp() > 0);
    }

    @ParameterizedTest @ValueSource(strings = {"invalido", "", "menor@rf44.test", "contato@rf44.test\r\nBcc:x@rf44.test"})
    void contatoPersistidoInvalidoNaoUsaFallbackENaoReverteAcao(String email) {
        var artista = artista(16, true);
        jdbc.update("update responsaveis_legais set email_responsavel=?", email);
        assertThat(criar(artista, vaga.getId())).isEqualTo(201);
        aguardar();
        assertThat(candidaturas.count()).isOne();
        verifyNoInteractions(sender);
        assertThat(logs.list.stream().map(ILoggingEvent::getFormattedMessage))
                .anyMatch(m -> m.contains("resultado=FALHA categoria=ContextoInvalidoException"));
    }

    @Test void destinatarioPersistidoEhResolvidoDepoisDaAcao() {
        var artista = artista(16, true);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            assertThat(criar(artista, vaga.getId())).isEqualTo(201);
            jdbc.update("update responsaveis_legais set email_responsavel='atualizado@rf44.test'");
            verifyNoInteractions(sender);
        });
        aguardar();
        assertThat(envios).singleElement().extracting(Envio::email).isEqualTo("atualizado@rf44.test");
    }

    @ParameterizedTest @ValueSource(strings = {"candidatura", "artista", "vaga", "adulto", "recusado", "responsavel", "abaixo14", "bloqueado", "nulo", "negativo"})
    void eventoInconsistenteNaoEnviaCegamente(String caso) {
        var artista = artista(16, true);
        var candidatura = candidaturaPersistida(artista);
        Long candidaturaId = candidatura.getId(), artistaId = artista.getUsuarioId(), vagaId = vaga.getId();
        switch (caso) {
            case "candidatura" -> candidaturaId = 999999L;
            case "artista" -> artistaId = dono.getUsuarioId();
            case "vaga" -> vagaId = 999999L;
            case "adulto" -> jdbc.update("update usuarios set data_nascimento=? where id=?", LocalDate.now().minusYears(18), artistaId);
            case "recusado" -> jdbc.update("update responsaveis_legais set consentimento_revogado=true");
            case "responsavel" -> jdbc.update("update responsaveis_legais set usuario_id=null");
            case "abaixo14" -> jdbc.update("update usuarios set data_nascimento=? where id=?", LocalDate.now().minusYears(13), artistaId);
            case "bloqueado" -> jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=?", artistaId);
            case "nulo" -> candidaturaId = null;
            case "negativo" -> candidaturaId = -1L;
            default -> throw new IllegalArgumentException(caso);
        }
        var evento = new AvisoResponsavelCandidaturaEvento(candidaturaId, artistaId, vagaId);
        new TransactionTemplate(manager).executeWithoutResult(tx -> eventos.publishEvent(evento));
        aguardar();
        verifyNoInteractions(sender);
        assertThat(candidaturas.count()).isOne();
        assertThat(logs.list.stream().map(ILoggingEvent::getFormattedMessage))
                .anyMatch(m -> m.contains("categoria=ContextoInvalidoException"));
    }

    @Test void mesmaInstanciaDeEventoSoAgendaUmaTentativa() {
        var artista = artista(16, true);
        var candidatura = candidaturaPersistida(artista);
        var evento = new AvisoResponsavelCandidaturaEvento(candidatura.getId(), artista.getUsuarioId(), vaga.getId());
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            eventos.publishEvent(evento);
            eventos.publishEvent(evento);
        });
        aguardar();
        assertThat(envios).hasSize(1);
        assertThat(candidaturas.count()).isOne();
    }

    @Test void eventoSemTransacaoNaoEnviaPorFallback() {
        var artista = artista(16, true);
        var candidatura = candidaturaPersistida(artista);
        eventos.publishEvent(new AvisoResponsavelCandidaturaEvento(candidatura.getId(), artista.getUsuarioId(), vaga.getId()));
        aguardar();
        verifyNoInteractions(sender);
        assertThat(logs.list).isEmpty();
    }

    @Test void smtpNaoConfiguradoRegistraFalhaTecnicaSemAlterarCandidatura() {
        var artista = artista(16, true);
        var candidatura = candidaturaPersistida(artista);
        var beansVazios = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        var avisos = new GuardianApplicationNoticeService(candidaturas, usuarios, menores, validator,
                beansVazios.getBeanProvider(GuardianApplicationNoticeSender.class), manager);
        avisos.avisar(new AvisoResponsavelCandidaturaEvento(candidatura.getId(), artista.getUsuarioId(), vaga.getId()));
        assertThat(candidaturas.count()).isOne();
        verifyNoInteractions(sender);
        assertThat(logs.list.stream().map(ILoggingEvent::getFormattedMessage))
                .anyMatch(m -> m.contains("resultado=FALHA categoria=SMTP_INDISPONIVEL"));
    }

    @Test void repeticaoSomenteDoEnvioNaoRecriaAcaoOuNotificacao() {
        var artista = artista(16, true);
        assertThat(criar(artista, vaga.getId())).isEqualTo(201);
        aguardar();
        var candidatura = candidaturas.findAll().getFirst();
        avisos.avisar(new AvisoResponsavelCandidaturaEvento(candidatura.getId(), artista.getUsuarioId(), vaga.getId()));
        assertThat(envios).hasSize(2);
        assertThat(candidaturas.findAll()).singleElement().extracting(Candidatura::getId).isEqualTo(candidatura.getId());
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isOne();
    }

    @Test void executorRf44NaoSubstituiExecutorDoSpringMvc() {
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(mvcAdapter, "taskExecutor"))
                .isInstanceOf(org.springframework.core.task.AsyncTaskExecutor.class).isNotSameAs(executor);
    }

    @Test void smtpLentoNaoRetemResponseNemConexaoDaLeitura() throws Exception {
        var artista = artista(16, true);
        var entrou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var transacao = new java.util.concurrent.atomic.AtomicBoolean(true);
        doAnswer(i -> {
            transacao.set(TransactionSynchronizationManager.isActualTransactionActive()
                    || TransactionSynchronizationManager.hasResource(dataSource)
                    || TransactionSynchronizationManager.hasResource(emf));
            entrou.countDown();
            liberar.await(10, TimeUnit.SECONDS);
            return null;
        }).when(sender).enviarAviso(any(), any());
        try (var cliente = Executors.newSingleThreadExecutor()) {
            try {
                assertThat(cliente.submit(() -> criar(artista, vaga.getId())).get(5, TimeUnit.SECONDS)).isEqualTo(201);
                assertThat(entrou.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(transacao).isFalse();
                assertThat(candidaturas.count()).isOne();
            } finally { liberar.countDown(); }
        }
    }

    @Test void schemaEControllersNaoOferecemProdutorDePublicacaoRf16OuRf40() {
        assertThat(PortfolioController.class.getDeclaredMethods()).noneMatch(m -> m.getName().equalsIgnoreCase("publicar"));
        assertThat(jdbc.queryForList("select table_name from information_schema.tables where table_schema='public'", String.class))
                .contains("portfolio_arquivos", "embeds_externos")
                .doesNotContain("projetos_portfolio", "publicacoes", "publicacoes_perfil");
        assertThat(jdbc.queryForList("select column_name from information_schema.columns where table_name='portfolio_arquivos'", String.class))
                .doesNotContain("status", "projeto_id", "capa", "titulo");
        verifyNoInteractions(sender);
    }

    private void aguardar() {
        org.awaitility.Awaitility.await().pollInterval(Duration.ofMillis(20))
                .during(Duration.ofMillis(100)).atMost(Duration.ofSeconds(10)).until(() ->
                executor.getActiveCount() == 0 && executor.getThreadPoolExecutor().getQueue().isEmpty());
    }

    private void semEfeitos() {
        aguardar();
        assertThat(candidaturas.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
        verifyNoInteractions(sender);
    }

    private Usuario usuario(String email, TipoUsuario tipo, int idade) {
        var u = OfficialSchemaFixtures.usuario();
        u.setNome("Nome Privado RF44");
        u.setDataNascimento(LocalDate.now().minusYears(idade));
        u.setTelefone("11987654321");
        u.setEmail(email);
        u.setSenha("{noop}senha-ficticia");
        u.setTipoUsuario(tipo);
        u.setStatusConta(StatusConta.ATIVA);
        u.setEmailVerificado(true);
        return usuarios.saveAndFlush(u);
    }

    private PerfilArtista artista(int idade, boolean autorizado) { return artista("menor@rf44.test", idade, autorizado); }

    private PerfilArtista artista(String email, int idade, boolean autorizado) {
        var u = usuario(email, TipoUsuario.ARTISTA, idade);
        var p = new PerfilArtista();
        p.setUsuario(u);
        p.setTipoPerfilArtistico(TipoPerfilArtistico.ARTISTA_SOLO);
        p.setRaioAtuacao(Abrangencia.LOCAL);
        p = artistas.saveAndFlush(p);
        OfficialSchemaFixtures.completarArtista(jdbc, p.getUsuarioId());
        if (autorizado) {
            var r = new ResponsavelLegal();
            r.setUsuario(u);
            r.setNomeResponsavel("Responsável Privado RF44");
            r.setTelefoneResponsavel("11987654321");
            r.setEmailResponsavel("responsavel.certo@rf44.test");
            r.setDataConsentimento(LocalDateTime.now().minusDays(1));
            r.setConsentimentoRevogado(false);
            r.setTokenConsentimento("token-secreto-rf44");
            responsaveis.saveAndFlush(r);
        }
        return p;
    }

    private int criar(PerfilArtista artista, Long vagaId) {
        try {
            return mvc.perform(post("/api/candidaturas").header("Authorization", bearer(artista))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"confirmacao\":true,\"vagaId\":" + vagaId + "}"))
                    .andReturn().getResponse().getStatus();
        } catch (Exception erro) { throw new IllegalStateException(erro); }
    }

    private void retirar(PerfilArtista artista, Long id) throws Exception {
        mvc.perform(delete("/api/candidaturas/{id}", id).header("Authorization", bearer(artista)))
                .andExpect(status().isNoContent());
    }

    private Candidatura candidaturaPersistida(PerfilArtista artista) {
        var c = new Candidatura();
        c.setArtista(artista);
        c.setVaga(vaga);
        c.setStatus(StatusCandidatura.PENDENTE);
        c.setDataCandidatura(LocalDateTime.now());
        return candidaturas.saveAndFlush(c);
    }

    private String bearer(PerfilArtista artista) { return "Bearer " + jwt.gerarToken(artista.getUsuario()); }
    private record Envio(String email, Long vagaId, boolean transacaoAtiva, boolean recursoVinculado, long candidaturasPersistidas) { }
}
