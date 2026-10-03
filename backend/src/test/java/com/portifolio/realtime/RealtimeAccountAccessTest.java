package com.portifolio.realtime;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.portifolio.dto.ChatEventoResponse;
import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RealtimeAccountAccessTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SimpMessagingTemplate websocket = mock(SimpMessagingTemplate.class);
    private final NotificacaoSseService sse = mock(NotificacaoSseService.class);
    private final com.portifolio.security.GoogleAccountAccessPolicy accessPolicy =
            new com.portifolio.security.GoogleAccountAccessPolicy(
                    mock(com.portifolio.repository.PerfilArtistaRepository.class),
                    mock(com.portifolio.repository.PerfilContratanteRepository.class), java.time.Clock.systemUTC());

    @Test
    void notificacaoNaoChegaAConexoesAntigasDeContaNaoAtiva() {
        Usuario usuario = contaAtiva();
        NotificacaoResponse evento = mock(NotificacaoResponse.class);
        when(usuarios.findById(23L)).thenReturn(Optional.of(usuario));
        NotificacaoRealtimeService realtime = new NotificacaoRealtimeService(websocket, sse, usuarios, accessPolicy);

        realtime.entregar(23L, "pessoa@palco.test", evento);
        verify(websocket).convertAndSendToUser("pessoa@palco.test", "/queue/notificacoes", evento);
        verify(sse).entregar(23L, evento);
        clearInvocations(websocket, sse);

        for (StatusConta estado : StatusConta.values()) {
            if (estado == StatusConta.ATIVA) continue;
            usuario.setStatusConta(estado);
            realtime.entregar(23L, "pessoa@palco.test", evento);
        }
        verifyNoInteractions(websocket, sse);
    }

    @Test
    void chatNaoChegaAConexaoAntigaDeContaNaoAtiva() {
        Usuario usuario = contaAtiva();
        ChatEventoResponse evento = mock(ChatEventoResponse.class);
        when(usuarios.findById(23L)).thenReturn(Optional.of(usuario));
        ChatRealtimeService realtime = new ChatRealtimeService(websocket, usuarios, accessPolicy);

        realtime.entregar(23L, "pessoa@palco.test", evento);
        verify(websocket).convertAndSendToUser("pessoa@palco.test", "/queue/chat", evento);
        clearInvocations(websocket);

        usuario.setStatusConta(StatusConta.PENDENTE_CONSENTIMENTO);
        realtime.entregar(23L, "pessoa@palco.test", evento);
        verifyNoInteractions(websocket);
    }

    private Usuario contaAtiva() {
        Usuario usuario = new Usuario();
        usuario.setEmail("pessoa@palco.test");
        usuario.setStatusConta(StatusConta.ATIVA);
        usuario.setEmailVerificado(true);
        return usuario;
    }
}
