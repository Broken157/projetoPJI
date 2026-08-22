package com.portifolio.controller;

import com.portifolio.dto.ChatMensagemPaginaResponse;
import com.portifolio.dto.ChatMensagemRequest;
import com.portifolio.dto.ChatMensagemResponse;
import com.portifolio.dto.ChatNaoLidasCountResponse;
import com.portifolio.dto.ChatSalaCriacaoRequest;
import com.portifolio.dto.ChatSalaPaginaResponse;
import com.portifolio.dto.ChatSalaResponse;
import com.portifolio.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/salas")
    public ResponseEntity<ChatSalaResponse> criarSala(
            @Valid @RequestBody ChatSalaCriacaoRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                chatService.criarOuReutilizarSala(
                        principal.getName(), request.usuarioDestinoId()));
    }

    @GetMapping("/salas")
    public ChatSalaPaginaResponse listarSalas(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Principal principal) {
        return chatService.listarSalas(principal.getName(), page, size);
    }

    @GetMapping("/salas/{salaId}/mensagens")
    public ChatMensagemPaginaResponse listarMensagens(
            @PathVariable Long salaId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Principal principal) {
        return chatService.listarMensagens(principal.getName(), salaId, page, size);
    }

    @PostMapping("/salas/{salaId}/mensagens")
    public ResponseEntity<ChatMensagemResponse> enviarMensagem(
            @PathVariable Long salaId,
            @Valid @RequestBody ChatMensagemRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                chatService.enviarMensagem(principal.getName(), salaId, request.texto()));
    }

    @PatchMapping("/salas/{salaId}/lidas")
    public ResponseEntity<Void> marcarLidas(
            @PathVariable Long salaId,
            Principal principal) {
        chatService.marcarRecebidasComoLidas(principal.getName(), salaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nao-lidas/count")
    public ChatNaoLidasCountResponse contarNaoLidas(Principal principal) {
        return chatService.contarNaoLidas(principal.getName());
    }

    @PatchMapping("/mensagens/{mensagemId}")
    public ChatMensagemResponse editarMensagem(
            @PathVariable Long mensagemId,
            @Valid @RequestBody ChatMensagemRequest request,
            Principal principal) {
        return chatService.editarMensagem(principal.getName(), mensagemId, request.texto());
    }

    @DeleteMapping("/mensagens/{mensagemId}")
    public ResponseEntity<Void> excluirMensagem(
            @PathVariable Long mensagemId,
            Principal principal) {
        chatService.excluirMensagem(principal.getName(), mensagemId);
        return ResponseEntity.noContent().build();
    }
}
