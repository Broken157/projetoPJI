package com.portifolio.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.realtime.NotificacaoRealtimeGateway;
import com.portifolio.service.NotificacaoPersistenceService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NotificacaoEventoListenerTest {

    @Test
    void falhaDoRealtimeNaoDesfazNemPropagaAposPersistencia() {
        NotificacaoPersistenceService persistence = mock(NotificacaoPersistenceService.class);
        NotificacaoRealtimeGateway realtime = mock(NotificacaoRealtimeGateway.class);
        NotificacaoResponse resposta = NotificacaoResponse.builder()
                .id(7L).tipo(TipoNotificacao.CANDIDATURA).mensagem("Alerta")
                .link("dashboard-contratante.html").lida(false).data(LocalDateTime.now()).build();
        NotificacaoEvento evento = new NotificacaoEvento(
                Set.of(3L), TipoNotificacao.CANDIDATURA, "Alerta", "dashboard-contratante.html");
        when(persistence.persistir(evento)).thenReturn(List.of(
                new NotificacaoPersistida(3L, "destino@rf23.test", resposta)));
        doThrow(new IllegalStateException("broker indisponivel"))
                .when(realtime).entregar(3L, "destino@rf23.test", resposta);
        NotificacaoEventoListener listener = new NotificacaoEventoListener(persistence, realtime);

        assertThatCode(() -> listener.processar(evento)).doesNotThrowAnyException();
    }
}
