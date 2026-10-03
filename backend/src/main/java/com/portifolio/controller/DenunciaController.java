package com.portifolio.controller;

import com.portifolio.dto.DenunciaRequest;
import com.portifolio.dto.DenunciaResponse;
import com.portifolio.dto.DenunciaResponse.Categoria;
import com.portifolio.service.DenunciaService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/denuncias")
@RequiredArgsConstructor
public class DenunciaController {
    private final DenunciaService service;

    @PostMapping
    public ResponseEntity<DenunciaResponse> registrar(@Valid @RequestBody DenunciaRequest request) {
        var response = service.registrar(request);
        return ResponseEntity.created(URI.create("/api/denuncias/" + response.categoria() + "/" + response.id()))
                .body(response);
    }

    @GetMapping
    public DenunciaResponse.Pagina listar(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(page, size);
    }

    @GetMapping("/{categoria}/{id}")
    public DenunciaResponse buscar(@PathVariable Categoria categoria, @PathVariable Long id) {
        return service.buscar(categoria, id);
    }
}
