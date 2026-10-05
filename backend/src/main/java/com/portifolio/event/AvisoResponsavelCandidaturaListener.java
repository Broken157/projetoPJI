package com.portifolio.event;

import com.portifolio.service.GuardianApplicationNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class AvisoResponsavelCandidaturaListener {
    private final GuardianApplicationNoticeService avisos;
    private final TaskExecutor executor;

    public AvisoResponsavelCandidaturaListener(GuardianApplicationNoticeService avisos,
            @Qualifier("avisosResponsavelExecutor") TaskExecutor executor) {
        this.avisos = avisos;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void avisar(AvisoResponsavelCandidaturaEvento evento) {
        if (evento == null || !evento.iniciarTentativa()) return;
        try {
            executor.execute(() -> avisos.avisar(evento));
        } catch (RuntimeException erro) {
            log.warn("RF44 evento=CANDIDATURA candidaturaId={} resultado=FALHA_AGENDAMENTO categoria={}",
                    evento.candidaturaId(), erro.getClass().getSimpleName());
        }
    }
}
