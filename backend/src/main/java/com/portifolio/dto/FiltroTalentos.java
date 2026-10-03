package com.portifolio.dto;

import com.portifolio.model.enums.*;
import java.util.Set;

public record FiltroTalentos(Short areaId, Set<Long> funcaoIds, Set<Long> especializacaoIds,
        String localizacao, Set<Abrangencia> raios, NivelExperiencia experienciaMinima,
        Boolean disponivel, Set<TipoPerfilArtistico> tipos, Long vagaId, boolean recomendados,
        Ordenacao ordenacao, int page, int size, String cidade, String estado) {
    public enum Ordenacao { RELEVANCIA, ATUALIZACAO }
    public FiltroTalentos {
        funcaoIds = funcaoIds == null ? Set.of() : Set.copyOf(funcaoIds);
        especializacaoIds = especializacaoIds == null ? Set.of() : Set.copyOf(especializacaoIds);
        raios = raios == null ? Set.of() : Set.copyOf(raios);
        tipos = tipos == null ? Set.of() : Set.copyOf(tipos);
        ordenacao = ordenacao == null ? Ordenacao.RELEVANCIA : ordenacao;
        if (page < 0 || size < 1 || size > 50) throw new IllegalArgumentException("page deve ser não negativo e size entre 1 e 50.");
        if (funcaoIds.size() > 50 || especializacaoIds.size() > 50) throw new IllegalArgumentException("Selecione no máximo 50 opções por filtro.");
        if (localizacao != null && localizacao.length() > 150) throw new IllegalArgumentException("Localização deve ter até 150 caracteres.");
        localizacao = com.portifolio.validation.LocalizacaoArtista.normalizarFiltro(localizacao);
        if (vagaId != null && vagaId < 1) throw new IllegalArgumentException("vagaId inválido.");
        cidade = cidade == null || cidade.isBlank() ? null : cidade.trim();
        estado = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(java.util.Locale.ROOT);
        if (cidade != null && cidade.length() > 100)
            throw new com.portifolio.exception.UnprocessableEntityException("Cidade deve ter até 100 caracteres.");
        if (estado != null && !estado.matches("[A-Z]{2}"))
            throw new com.portifolio.exception.UnprocessableEntityException("Estado deve conter duas letras.");
    }
    public FiltroTalentos(Short areaId, Set<Long> funcaoIds, Set<Long> especializacaoIds,
            String localizacao, Set<Abrangencia> raios, NivelExperiencia experienciaMinima,
            Boolean disponivel, Set<TipoPerfilArtistico> tipos, Long vagaId, boolean recomendados,
            Ordenacao ordenacao, int page, int size) {
        this(areaId, funcaoIds, especializacaoIds, localizacao, raios, experienciaMinima,
                disponivel, tipos, vagaId, recomendados, ordenacao, page, size, null, null);
    }
    public static FiltroTalentos recomendados(int size) {
        return new FiltroTalentos(null, null, null, null, null, null, null, null, null, true, null, 0, size);
    }
}
