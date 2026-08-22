package com.portifolio.controller;

import com.portifolio.dto.PerfilPublicoResponse;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.service.PerfilPublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfis/publicos")
@RequiredArgsConstructor
public class PerfilPublicoController {

    private final PerfilPublicoService perfilPublicoService;

    @GetMapping("/{tipo}/{id}")
    public ResponseEntity<PerfilPublicoResponse> buscar(
            @PathVariable TipoUsuario tipo,
            @PathVariable Long id) {
        return ResponseEntity.ok(perfilPublicoService.buscar(tipo, id));
    }
}
