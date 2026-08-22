package com.portifolio.realtime;

import com.portifolio.dto.ChatEventoResponse;

public interface ChatRealtimeGateway {
    void entregar(Long usuarioId, String email, ChatEventoResponse evento);
}
