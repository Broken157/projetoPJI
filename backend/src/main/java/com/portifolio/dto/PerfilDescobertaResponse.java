package com.portifolio.dto;

import com.portifolio.model.enums.TipoUsuario;
import java.util.List;

/** Card público RF37. Identificador e tipo navegam ao contrato existente RF10. */
public record PerfilDescobertaResponse(Long usuarioId, TipoUsuario tipo, String username,
        String nomeExibicao, String avatarUrl, String cidade, String estado) {
    public record Pagina(List<PerfilDescobertaResponse> content, int page, int size,
            long totalElements, int totalPages, boolean first, boolean last,
            boolean hasNext, boolean hasPrevious) {}
}
