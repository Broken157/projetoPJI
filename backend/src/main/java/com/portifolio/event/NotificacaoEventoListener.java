package com.portifolio.event;

import com.portifolio.realtime.NotificacaoRealtimeGateway;
import com.portifolio.service.NotificacaoPersistenceService;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoEventoListener {

    private final NotificacaoPersistenceService persistenceService;
    private final NotificacaoRealtimeGateway realtimeGateway;
    private final ApplicationEventPublisher eventos;

    @EventListener
    public void processar(NotificacaoEvento evento) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalTransactionStateException("Notificacao exige a transacao do produtor.");
        }
        EventosProcessados processados = TransactionSynchronizationManager.getSynchronizations().stream()
                .filter(EventosProcessados.class::isInstance)
                .map(EventosProcessados.class::cast)
                .findFirst().orElseGet(() -> {
                    EventosProcessados registro = new EventosProcessados();
                    TransactionSynchronizationManager.registerSynchronization(registro);
                    return registro;
                });
        if (!processados.eventos.add(evento)) {
            return;
        }
        persistenceService.persistirNaTransacaoAtual(evento).forEach(eventos::publishEvent);
    }

    // Identity is scoped to this transaction; equal messages from distinct operations remain valid.
    private static final class EventosProcessados implements TransactionSynchronization {
        private final Set<NotificacaoEvento> eventos = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void entregar(NotificacaoPersistida persistida) {
        try {
            realtimeGateway.entregar(
                    persistida.usuarioId(),
                    persistida.email(),
                    persistida.notificacao());
        } catch (RuntimeException erro) {
            log.warn("Falha isolada na entrega em tempo real da notificacao {}.",
                    persistida.notificacao().getId());
        }
    }
}
