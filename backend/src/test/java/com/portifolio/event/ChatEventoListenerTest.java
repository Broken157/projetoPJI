package com.portifolio.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.portifolio.dto.ChatEventoResponse;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.ChatEventoTipo;
import com.portifolio.realtime.ChatRealtimeGateway;
import com.portifolio.repository.UsuarioRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChatEventoListenerTest {

    @Test
    void falhaDoBrokerDepoisDoCommitEhIsolada() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ChatRealtimeGateway realtime = mock(ChatRealtimeGateway.class);
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setEmail("destino@rf24.test");
        ChatEventoResponse resposta = ChatEventoResponse.builder()
                .tipo(ChatEventoTipo.NOVA_MENSAGEM)
                .salaId(4L)
                .mensagemIds(List.of())
                .build();
        ChatEventoPosCommit evento = new ChatEventoPosCommit(Set.of(9L), resposta);
        when(usuarios.findAllById(Set.of(9L))).thenReturn(List.of(usuario));
        doThrow(new IllegalStateException("broker indisponivel"))
                .when(realtime).entregar(9L, "destino@rf24.test", resposta);
        ChatEventoListener listener = new ChatEventoListener(usuarios, realtime);

        assertThatCode(() -> listener.entregar(evento)).doesNotThrowAnyException();
    }
}
