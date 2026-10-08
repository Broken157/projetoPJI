package com.portifolio.controller;

import com.portifolio.dto.FuncaoResponse;
import com.portifolio.service.FuncaoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/funcoes")
@RequiredArgsConstructor
public class FuncaoController {

    private final FuncaoService funcaoService;

    @GetMapping(params = "!areaId")
    public ResponseEntity<List<FuncaoResponse>> listarTodos() {
        return ResponseEntity.ok(funcaoService.listarTodos());
    }

    @GetMapping(params = "areaId")
    public com.portifolio.dto.TalentoResponse.Pagina<FuncaoResponse> porArea(
            @org.springframework.web.bind.annotation.RequestParam Short areaId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        return funcaoService.listarPorArea(areaId, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcaoService.buscarPorId(id));
    }


}
