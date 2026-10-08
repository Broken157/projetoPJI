package com.portifolio.dto;

import com.portifolio.model.enums.NivelExperiencia;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Seleção por área; listas omitidas preservam vínculos existentes compatíveis. */
@Getter
@Setter
public class PerfilArtistaAreaRequest {
    private Short areaId;
    private boolean principal;
    private NivelExperiencia nivelExperiencia;
    private List<Long> funcaoIds;
    private List<Long> especializacaoIds;
}
