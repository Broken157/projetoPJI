package com.portifolio.realtime;

import com.portifolio.dto.NotificacaoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoRealtimeService implements NotificacaoRealtimeGateway {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacaoSseService sseService;

    @Override
    public void entregar(Long usuarioId, String email, NotificacaoResponse notificacao) {
        try {
            messagingTemplate.convertAndSendToUser(
                    email, "/queue/notificacoes", notificacao);
        } catch (RuntimeException erro) {
            log.warn("Falha isolada na entrega WebSocket da notificacao {}.",
                    notificacao.getId());
        }
        try {
            sseService.entregar(usuarioId, notificacao);
        } catch (RuntimeException erro) {
            log.warn("Falha isolada na entrega SSE da notificacao {}.",
                    notificacao.getId());
        }
    }
}
