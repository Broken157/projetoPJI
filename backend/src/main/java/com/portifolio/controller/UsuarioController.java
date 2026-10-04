package com.portifolio.controller;

import com.portifolio.dto.UsuarioRequest;
import com.portifolio.dto.UsuarioResponse;
import com.portifolio.dto.UsuarioAtualizacaoRequest;
import com.portifolio.service.UsuarioService;
import com.portifolio.service.ExclusaoContaService;
import com.portifolio.security.JwtService;
import com.portifolio.dto.ExclusaoContaRequest;
import com.portifolio.dto.ExclusaoContaResponse;
import java.security.Principal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ExclusaoContaService exclusaoContaService;
    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> buscarAtual() {
        return ResponseEntity.ok(usuarioService.buscarAtual());
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> atualizarAtual(
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizarAtual(request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ExclusaoContaResponse> deletarAtual(
            Principal principal, @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ExclusaoContaRequest request) {
        String token = authorization.substring(7);
        // O filtro valida a assinatura; o serviço também fixa ID + email para impedir reutilização de email.
        return ResponseEntity.ok(exclusaoContaService.excluir(
                jwtService.extrairUsuarioId(token), principal.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
