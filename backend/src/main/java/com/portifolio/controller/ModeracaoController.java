package com.portifolio.controller;

import com.portifolio.dto.ModeracaoRequest;
import com.portifolio.repository.ModeracaoConteudoRepository;
import com.portifolio.service.ModeracaoConteudoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/moderacao") @RequiredArgsConstructor
public class ModeracaoController {
    @GetMapping("/denuncias")
    public ModeracaoConteudoService.PaginaDenuncias denuncias(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return service.denuncias(page,size); }
    private final ModeracaoConteudoService service;
    @PostMapping("/{tipo}/{id}/acoes") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ModeracaoConteudoRepository.Acao agir(@PathVariable String tipo,@PathVariable long id,@Valid @RequestBody ModeracaoRequest request) {
        return service.agir(tipo,id,request);
    }
    @GetMapping("/{tipo}/{id}/historico")
    public ModeracaoConteudoService.Pagina historico(@PathVariable String tipo,@PathVariable long id,
            @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size) {
        return service.historico(tipo,id,page,size);
    }
}
