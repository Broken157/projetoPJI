package com.portifolio.realtime;

import com.portifolio.dto.ChatEventoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRealtimeService implements ChatRealtimeGateway {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void entregar(Long usuarioId, String email, ChatEventoResponse evento) {
        messagingTemplate.convertAndSendToUser(email, "/queue/chat", evento);
    }
}
