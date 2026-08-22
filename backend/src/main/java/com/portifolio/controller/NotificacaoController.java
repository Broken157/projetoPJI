package com.portifolio.controller;

import com.portifolio.dto.NotificacaoNaoLidaCountResponse;
import com.portifolio.dto.NotificacaoPaginaResponse;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.model.Usuario;
import com.portifolio.realtime.NotificacaoSseService;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final NotificacaoSseService sseService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    public ResponseEntity<NotificacaoPaginaResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(notificacaoService.listar(page, size));
    }

    @GetMapping("/nao-lidas/count")
    public ResponseEntity<NotificacaoNaoLidaCountResponse> contarNaoLidas() {
        return ResponseEntity.ok(new NotificacaoNaoLidaCountResponse(
                notificacaoService.contarNaoLidas()));
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        notificacaoService.marcarComoLida(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas() {
        notificacaoService.marcarTodasComoLidas();
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Usuario usuario = authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado nao encontrado."));
        return sseService.conectar(usuario.getId());
    }
}
