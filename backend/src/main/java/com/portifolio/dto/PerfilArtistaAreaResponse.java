package com.portifolio.dto;

import com.portifolio.model.enums.NivelExperiencia;
import java.time.LocalDateTime;
import java.util.Set;

public record PerfilArtistaAreaResponse(Short areaId, boolean principal,
        NivelExperiencia nivelExperiencia, Set<Long> funcaoIds,
        Set<Long> especializacaoIds, LocalDateTime ultimaAtualizacao) {}
