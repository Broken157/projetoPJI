package com.portifolio.repository.projection;

import java.time.LocalDateTime;

public interface ChatSalaResumoProjection {
    Long getSalaId();
    Long getParticipanteId();
    String getParticipanteNome();
    String getParticipanteAvatar();
    String getUltimaMensagem();
    LocalDateTime getUltimaMensagemData();
    Long getNaoLidas();
}
