package com.portifolio.dto;

import com.portifolio.model.enums.ChatEventoTipo;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventoResponse {
    private ChatEventoTipo tipo;
    private Long salaId;
    private ChatMensagemResponse mensagem;
    private List<Long> mensagemIds;
}
