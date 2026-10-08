package com.portifolio.dto;

import com.portifolio.model.enums.TipoUsuario;
import java.util.List;

/** Card público RF37. Identificador e tipo navegam ao contrato existente RF10. */
public record PerfilDescobertaResponse(Long usuarioId, TipoUsuario tipo, String username,
        String nomeExibicao, String avatarUrl, String cidade, String estado,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        com.portifolio.model.enums.TipoPerfilArtistico tipoPerfil,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        com.portifolio.model.enums.TipoContratante tipoContratante,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        Boolean favorito) {
    public record Pagina(List<PerfilDescobertaResponse> content, int page, int size,
            long totalElements, int totalPages, boolean first, boolean last,
            boolean hasNext, boolean hasPrevious) {}
}
