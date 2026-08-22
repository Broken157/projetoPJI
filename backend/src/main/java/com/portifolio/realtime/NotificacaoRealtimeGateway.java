package com.portifolio.realtime;

import com.portifolio.dto.NotificacaoResponse;

public interface NotificacaoRealtimeGateway {
    void entregar(Long usuarioId, String email, NotificacaoResponse notificacao);
}
