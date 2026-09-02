package com.portifolio.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.portifolio.security.JwtService;
import com.portifolio.security.UserDetailsServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class StompJwtChannelInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
    private final StompJwtChannelInterceptor interceptor =
            new StompJwtChannelInterceptor(jwtService, userDetailsService);

    @Test
    void connectValidoVinculaPrincipalAoEmailDoJwt() {
        when(jwtService.tokenValido("jwt-valido")).thenReturn(true);
        when(jwtService.extrairEmail("jwt-valido")).thenReturn("pessoa@rf23.test");
        when(jwtService.extrairUsuarioId("jwt-valido")).thenReturn(23L);
        UserDetails user = User.withUsername("pessoa@rf23.test")
                .password("x").authorities("ROLE_ARTISTA").build();
        when(userDetailsService.loadUserByUsername("pessoa@rf23.test", 23L)).thenReturn(user);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer jwt-valido");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("pessoa@rf23.test");
    }

    @Test
    void connectRejeitaJwtSemIdMesmoComEmailValido() {
        when(jwtService.tokenValido("jwt-sem-id")).thenReturn(true);
        when(jwtService.extrairEmail("jwt-sem-id")).thenReturn("pessoa@rf23.test");

        assertThatThrownBy(() -> interceptor.preSend(
                mensagem(StompCommand.CONNECT, "Bearer jwt-sem-id"),
                mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identidade valida");
    }

    @Test
    void connectSemAuthorizationEhRejeitado() {
        assertThatThrownBy(() -> interceptor.preSend(
                mensagem(StompCommand.CONNECT, null),
                mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authorization Bearer");
    }

    @Test
    void connectComJwtInvalidoOuExpiradoEhRejeitado() {
        when(jwtService.tokenValido("expirado")).thenReturn(false);
        assertThatThrownBy(() -> interceptor.preSend(
                mensagem(StompCommand.CONNECT, "Bearer expirado"),
                mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalido ou expirado");
    }

    @Test
    void subscribeAnonimoEhRejeitado() {
        assertThatThrownBy(() -> interceptor.preSend(
                mensagem(StompCommand.SUBSCRIBE, null),
                mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao autenticada");
    }

    @Test
    void usuarioAutenticadoPodeAssinarSomenteFilasPrivadasConhecidas() {
        assertThat(interceptor.preSend(
                mensagemAutenticada(StompCommand.SUBSCRIBE, "/user/queue/chat"),
                mock(org.springframework.messaging.MessageChannel.class))).isNotNull();
        assertThat(interceptor.preSend(
                mensagemAutenticada(StompCommand.SUBSCRIBE, "/user/queue/notificacoes"),
                mock(org.springframework.messaging.MessageChannel.class))).isNotNull();

        assertThatThrownBy(() -> interceptor.preSend(
                mensagemAutenticada(StompCommand.SUBSCRIBE, "/topic/chat"),
                mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao permitido");
    }

    @Test
    void sendAutenticadoAceitaSomenteEndpointChatComSalaNumerica() {
        assertThat(interceptor.preSend(
                mensagemAutenticada(StompCommand.SEND, "/app/chat/salas/42/mensagens"),
                mock(org.springframework.messaging.MessageChannel.class))).isNotNull();

        assertThatThrownBy(() -> interceptor.preSend(
                mensagemAutenticada(StompCommand.SEND, "/app/notificacoes/forjada"),
                mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao permitido");
    }

    private Message<byte[]> mensagem(StompCommand comando, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(comando);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> mensagemAutenticada(StompCommand comando, String destino) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(comando);
        accessor.setDestination(destino);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "pessoa@rf24.test", null, List.of()));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
