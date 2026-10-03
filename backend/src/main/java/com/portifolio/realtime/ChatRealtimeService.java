package com.portifolio.realtime;

import com.portifolio.dto.ChatEventoResponse;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.GoogleAccountAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRealtimeService implements ChatRealtimeGateway {

    private final SimpMessagingTemplate messagingTemplate;
    private final UsuarioRepository usuarioRepository;
    private final GoogleAccountAccessPolicy accountAccessPolicy;

    @Override
    public void entregar(Long usuarioId, String email, ChatEventoResponse evento) {
        if (usuarioId == null || email == null || usuarioRepository.findById(usuarioId)
                .filter(usuario -> email.equals(usuario.getEmail()))
                .filter(accountAccessPolicy::acessoNormalPermitido).isEmpty()) {
            return;
        }
        messagingTemplate.convertAndSendToUser(email, "/queue/chat", evento);
    }
}
