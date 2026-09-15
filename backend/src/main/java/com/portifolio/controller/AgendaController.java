package com.portifolio.controller;
import com.portifolio.dto.AgendaRequest;
import com.portifolio.service.AgendaService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/agenda") @RequiredArgsConstructor
public class AgendaController {
    private final AgendaService service;
    @GetMapping public AgendaService.Pagina listar(@RequestParam(required=false) LocalDateTime inicio,
            @RequestParam(required=false) LocalDateTime fim,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="30") int size) {
        return service.listar(inicio,fim,page,size);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AgendaService.Item criar(@Valid @RequestBody AgendaRequest request) { return service.salvar(null,request); }
    @PutMapping("/{id}") public AgendaService.Item editar(@PathVariable Long id,@Valid @RequestBody AgendaRequest request) { return service.salvar(id,request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}
