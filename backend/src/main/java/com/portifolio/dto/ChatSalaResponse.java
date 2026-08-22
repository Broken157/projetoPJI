package com.portifolio.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSalaResponse {
    private Long salaId;
    private Long participanteId;
    private String participanteNome;
    private String participanteAvatar;
    private String ultimaMensagem;
    private LocalDateTime ultimaMensagemData;
    private long naoLidas;
}
