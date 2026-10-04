package com.portifolio.controller;

import com.portifolio.dto.ConviteVagaOpcao;
import com.portifolio.dto.ConviteVagaRequest;
import com.portifolio.dto.ConviteVagaResponse;
import com.portifolio.dto.TalentoResponse.Pagina;
import com.portifolio.service.ConviteVagaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/talentos/{artistaId}/convites")
@RequiredArgsConstructor
public class ConviteVagaController {
    private final ConviteVagaService service;

    @GetMapping("/vagas")
    public Pagina<ConviteVagaOpcao> listar(@PathVariable Long artistaId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.listarVagas(artistaId, page, size);
    }

    @PostMapping
    public ResponseEntity<ConviteVagaResponse> convidar(@PathVariable Long artistaId,
            @Valid @RequestBody ConviteVagaRequest request) {
        var resultado = service.convidar(artistaId, request.vagaId(), request.confirmado());
        return ResponseEntity.status(resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.estado());
    }
}
