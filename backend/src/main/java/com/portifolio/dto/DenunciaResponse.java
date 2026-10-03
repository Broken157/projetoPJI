package com.portifolio.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DenunciaResponse(Long id, Categoria categoria, String tipoAlvo, Long alvoId,
        String motivo, String descricao, String status, LocalDateTime dataRegistro) {
    public enum Categoria { CONTEUDO, PLAGIO }
    public record Pagina(List<DenunciaResponse> content, int page, int size, long totalElements,
            int totalPages, boolean hasNext, boolean hasPrevious) {}
}
