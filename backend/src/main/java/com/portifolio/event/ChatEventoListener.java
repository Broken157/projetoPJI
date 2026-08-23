package com.portifolio.event;

import com.portifolio.model.Usuario;
import com.portifolio.realtime.ChatRealtimeGateway;
import com.portifolio.repository.UsuarioRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventoListener {

    private final UsuarioRepository usuarioRepository;
    private final ChatRealtimeGateway realtimeGateway;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void entregar(ChatEventoPosCommit evento) {
        Map<Long, Usuario> usuarios = usuarioRepository.findAllById(evento.destinatarioIds()).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        for (Long destinatarioId : evento.destinatarioIds()) {
            Usuario usuario = usuarios.get(destinatarioId);
            if (usuario == null) {
                continue;
            }
            try {
                realtimeGateway.entregar(usuario.getId(), usuario.getEmail(), evento.evento());
            } catch (RuntimeException erro) {
                log.warn("Falha isolada na entrega em tempo real do chat na sala {}.",
                        evento.evento().getSalaId());
            }
        }
    }
}
