package com.portifolio.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.portifolio.service.GuardianApplicationNoticeService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;

class AvisoResponsavelCandidaturaListenerTest {
    private final GuardianApplicationNoticeService avisos = mock(GuardianApplicationNoticeService.class);
    private final TaskExecutor executor = mock(TaskExecutor.class);
    private final AvisoResponsavelCandidaturaListener listener = new AvisoResponsavelCandidaturaListener(avisos, executor);

    @Test void callbackAgendaSemEnviarSincronamente() {
        var evento = new AvisoResponsavelCandidaturaEvento(1L, 2L, 3L);
        listener.avisar(evento);
        var tarefa = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(tarefa.capture());
        verifyNoInteractions(avisos);
        tarefa.getValue().run();
        verify(avisos).avisar(evento);
    }

    @Test void repeticaoDaMesmaInstanciaNaoAgendaSpam() {
        var evento = new AvisoResponsavelCandidaturaEvento(1L, 2L, 3L);
        listener.avisar(evento);
        listener.avisar(evento);
        verify(executor, times(1)).execute(any());
    }

    @Test void novaCandidaturaPermaneceNovaAcaoMesmoArtistaEVaga() {
        listener.avisar(new AvisoResponsavelCandidaturaEvento(1L, 2L, 3L));
        listener.avisar(new AvisoResponsavelCandidaturaEvento(4L, 2L, 3L));
        verify(executor, times(2)).execute(any());
    }

    @Test void callbacksConcorrentesDaMesmaInstanciaAgendamUmaTentativa() throws Exception {
        var evento = new AvisoResponsavelCandidaturaEvento(1L, 2L, 3L);
        var inicio = new CountDownLatch(1);
        try (var tarefas = Executors.newFixedThreadPool(4)) {
            var resultados = java.util.stream.IntStream.range(0, 8).mapToObj(i -> tarefas.submit(() -> {
                inicio.await();
                listener.avisar(evento);
                return null;
            })).toList();
            inicio.countDown();
            for (var resultado : resultados) resultado.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        verify(executor, times(1)).execute(any());
    }

    @Test void rejeicaoDoExecutorEhIsoladaELogadaSemPayload() {
        Logger logger = (Logger) LoggerFactory.getLogger(AvisoResponsavelCandidaturaListener.class);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);
        try {
            doThrow(new RejectedExecutionException("responsavel@teste.com token-secreto"))
                    .when(executor).execute(any());
            listener.avisar(new AvisoResponsavelCandidaturaEvento(1L, 2L, 3L));
            verifyNoInteractions(avisos);
            assertThat(logs.list).singleElement().satisfies(e -> {
                assertThat(e.getFormattedMessage()).contains("candidaturaId=1", "resultado=FALHA_AGENDAMENTO", "RejectedExecutionException")
                        .doesNotContain("@", "token-secreto");
                assertThat(e.getThrowableProxy()).isNull();
            });
        } finally { logger.detachAppender(logs); logs.stop(); }
    }

    @Test void eventoNuloNaoAgenda() {
        listener.avisar(null);
        verifyNoInteractions(executor, avisos);
    }
}
