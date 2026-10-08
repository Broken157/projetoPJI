package com.portifolio.controller;

import com.portifolio.dto.PerfilPublicoResponse;
import com.portifolio.dto.PerfilDescobertaResponse;
import com.portifolio.dto.FiltroDescobertaPublica;
import java.util.Map;
import java.util.Set;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.service.PerfilPublicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfis/publicos")
@RequiredArgsConstructor
public class PerfilPublicoController {

    private final PerfilPublicoService perfilPublicoService;

    @GetMapping
    public ResponseEntity<PerfilDescobertaResponse.Pagina> descobrir(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TipoUsuario tipo,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Short areaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long funcaoId,
            @RequestParam(required = false) Long especializacaoId,
            @RequestParam(required = false) com.portifolio.model.enums.TipoPerfilArtistico tipoPerfil,
            @RequestParam(required = false) com.portifolio.model.enums.TipoContratante tipoContratante,
            @RequestParam(defaultValue = "false") boolean somenteFavoritos,
            @RequestParam(required = false) Boolean bancoTalentosAtivo,
            @RequestParam Map<String, String> parametros) {
        if (!Set.of("q", "tipo", "cidade", "estado", "areaId", "page", "size", "funcaoId",
                "especializacaoId", "tipoPerfil", "tipoContratante", "somenteFavoritos", "bancoTalentosAtivo")
                .containsAll(parametros.keySet())) {
            throw new IllegalArgumentException("Parâmetro não suportado na descoberta pública.");
        }
        if (parametros.containsKey("bancoTalentosAtivo") && bancoTalentosAtivo == null)
            throw new IllegalArgumentException("Informe true ou false para bancoTalentosAtivo.");
        return ResponseEntity.ok(perfilPublicoService.descobrir(
                new FiltroDescobertaPublica(q, tipo, cidade, estado, areaId, page, size,
                        funcaoId, especializacaoId, tipoPerfil, tipoContratante, somenteFavoritos, bancoTalentosAtivo)));
    }

    @GetMapping("/{tipo}/{id}")
    public ResponseEntity<PerfilPublicoResponse> buscar(
            @PathVariable TipoUsuario tipo,
            @PathVariable Long id) {
        return ResponseEntity.ok(perfilPublicoService.buscar(tipo, id));
    }
}
