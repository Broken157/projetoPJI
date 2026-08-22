package com.portifolio.controller;

import com.portifolio.dto.ChatMensagemRequest;
import com.portifolio.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/salas/{salaId}/mensagens")
    public void enviar(
            @DestinationVariable Long salaId,
            @Valid ChatMensagemRequest request,
            Principal principal) {
        chatService.enviarMensagem(principal.getName(), salaId, request.texto());
    }
}
