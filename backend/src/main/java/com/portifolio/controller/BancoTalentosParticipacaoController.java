package com.portifolio.controller;

import com.portifolio.dto.BancoTalentosParticipacaoRequest;
import com.portifolio.dto.BancoTalentosParticipacaoResponse;
import com.portifolio.service.BancoTalentosParticipacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/talentos/contratantes/{contratanteId}/participacao")
@RequiredArgsConstructor
public class BancoTalentosParticipacaoController {
    private final BancoTalentosParticipacaoService service;

    @PostMapping
    public ResponseEntity<BancoTalentosParticipacaoResponse> participar(
            @PathVariable Long contratanteId, @Valid @RequestBody BancoTalentosParticipacaoRequest request) {
        var resultado = service.participar(contratanteId, request.confirmado());
        return ResponseEntity.status(resultado.criada() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.estado());
    }

    @GetMapping
    public BancoTalentosParticipacaoResponse consultar(@PathVariable Long contratanteId) {
        return service.consultar(contratanteId);
    }
}
