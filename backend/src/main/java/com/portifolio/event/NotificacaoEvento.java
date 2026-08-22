package com.portifolio.event;

import com.portifolio.model.enums.TipoNotificacao;
import java.util.Set;

public record NotificacaoEvento(
        Set<Long> destinatarioIds,
        TipoNotificacao tipo,
        String mensagem,
        String link) {

    public NotificacaoEvento {
        destinatarioIds = Set.copyOf(destinatarioIds);
    }
}
