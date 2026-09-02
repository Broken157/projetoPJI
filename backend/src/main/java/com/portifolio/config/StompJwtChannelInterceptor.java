package com.portifolio.config;

import com.portifolio.security.JwtService;
import com.portifolio.security.UserDetailsServiceImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            autenticar(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new IllegalArgumentException("Conexao STOMP nao autenticada.");
            }
            if (!"/user/queue/notificacoes".equals(accessor.getDestination())
                    && !"/user/queue/chat".equals(accessor.getDestination())) {
                throw new IllegalArgumentException("Destino STOMP nao permitido.");
            }
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new IllegalArgumentException("Conexao STOMP nao autenticada.");
            }
            String destino = accessor.getDestination();
            if (destino == null
                    || !destino.matches("/app/chat/salas/[1-9][0-9]*/mensagens")) {
                throw new IllegalArgumentException("Destino STOMP nao permitido.");
            }
        }
        return message;
    }

    private void autenticar(StompHeaderAccessor accessor) {
        List<String> valores = accessor.getNativeHeader("Authorization");
        if (valores == null || valores.size() != 1
                || valores.getFirst() == null
                || !valores.getFirst().startsWith("Bearer ")) {
            throw new IllegalArgumentException(
                    "Authorization Bearer obrigatorio no STOMP CONNECT.");
        }
        String token = valores.getFirst().substring(7);
        if (!jwtService.tokenValido(token)) {
            throw new IllegalArgumentException("JWT invalido ou expirado no STOMP CONNECT.");
        }
        String email = jwtService.extrairEmail(token);
        Long usuarioId = jwtService.extrairUsuarioId(token);
        if (email == null || usuarioId == null || usuarioId < 1) {
            throw new IllegalArgumentException("JWT sem identidade valida no STOMP CONNECT.");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(email, usuarioId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()));
    }
}
