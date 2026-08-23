package com.portifolio.event;

import com.portifolio.dto.ChatEventoResponse;
import java.util.Set;

public record ChatEventoPosCommit(
        Set<Long> destinatarioIds,
        ChatEventoResponse evento) {

    public ChatEventoPosCommit {
        destinatarioIds = Set.copyOf(destinatarioIds);
    }
}
