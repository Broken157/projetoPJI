package com.portifolio.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.realtime.NotificacaoRealtimeGateway;
import com.portifolio.service.NotificacaoPersistenceService;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificacaoEventoListenerTest {

    @Test
    void falhaDoRealtimeNaoDesfazNemPropagaAposPersistencia() {
        NotificacaoPersistenceService persistence = mock(NotificacaoPersistenceService.class);
        NotificacaoRealtimeGateway realtime = mock(NotificacaoRealtimeGateway.class);
        NotificacaoResponse resposta = NotificacaoResponse.builder()
                .id(7L).tipo(TipoNotificacao.CANDIDATURA).mensagem("Alerta")
                .link("/vagas/1/gerenciar").lida(false).data(LocalDateTime.now()).build();
        NotificacaoPersistida persistida = new NotificacaoPersistida(3L, "destino@rf23.test", resposta);
        doThrow(new IllegalStateException("broker indisponivel"))
                .when(realtime).entregar(3L, "destino@rf23.test", resposta);
        NotificacaoEventoListener listener = new NotificacaoEventoListener(persistence, realtime,
                mock(ApplicationEventPublisher.class));

        assertThatCode(() -> listener.entregar(persistida)).doesNotThrowAnyException();
        org.mockito.Mockito.verify(realtime).entregar(3L, "destino@rf23.test", resposta);
        org.mockito.Mockito.verifyNoInteractions(persistence);
    }
}
