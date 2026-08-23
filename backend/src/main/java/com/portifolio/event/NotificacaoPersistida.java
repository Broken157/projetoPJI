package com.portifolio.event;

import com.portifolio.dto.NotificacaoResponse;

public record NotificacaoPersistida(
        Long usuarioId,
        String email,
        NotificacaoResponse notificacao) {
}
