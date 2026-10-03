package com.portifolio.event;

import com.portifolio.service.GuardianApplicationNoticeSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvisoResponsavelCandidaturaListener {
    private final ObjectProvider<GuardianApplicationNoticeSender> senderProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void avisar(AvisoResponsavelCandidaturaEvento evento) {
        try {
            GuardianApplicationNoticeSender sender = senderProvider.getIfAvailable();
            if (sender == null) {
                log.warn("Aviso RF44 não enviado: SMTP indisponível para vaga {}.", evento.vagaId());
                return;
            }
            sender.enviarAviso(evento.emailResponsavel(), evento.vagaId());
            log.info("Aviso RF44 de candidatura enviado para vaga {}.", evento.vagaId());
        } catch (RuntimeException erro) {
            log.warn("Falha no aviso RF44 para candidatura de vaga {}: {}",
                    evento.vagaId(), erro.getClass().getSimpleName());
        }
    }
}
