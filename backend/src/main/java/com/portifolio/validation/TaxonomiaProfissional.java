package com.portifolio.validation;

import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.Funcao;
import com.portifolio.model.PerfilArtistaArea;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Limites funcionais e compatibilidade da seleção, independentes da carga do catálogo. */
public final class TaxonomiaProfissional {
    public static final int MAX_FUNCOES_ARTISTA = 5;
    public static final int MAX_ESPECIALIZACOES_ARTISTA = 5;
    private TaxonomiaProfissional() {}

    public static Set<Long> ids(Collection<Long> valores, int limite) {
        if (valores.size() > limite || valores.stream().anyMatch(id -> id == null || id <= 0))
            throw new UnprocessableEntityException("Informe até " + limite + " IDs positivos do catálogo.");
        Set<Long> ids = Set.copyOf(valores);
        if (ids.size() != valores.size())
            throw new UnprocessableEntityException("IDs duplicados na seleção profissional.");
        return ids;
    }

    public static Set<Long> especializacoesCompativeis(Collection<Funcao> funcoes) {
        return funcoes.stream().flatMap(f -> f.getEspecializacoes().stream())
                .map(e -> e.getId()).collect(Collectors.toSet());
    }

    public static boolean coerente(PerfilArtistaArea area) {
        if (area.getArea() == null || area.getArea().getId() == null
                || area.getFuncoes() == null || area.getEspecializacoes() == null
                || area.getFuncoes().size() > MAX_FUNCOES_ARTISTA
                || area.getEspecializacoes().size() > MAX_ESPECIALIZACOES_ARTISTA) return false;
        if (area.getFuncoes().stream().anyMatch(f -> f.getId() == null || f.getArea() == null
                || !area.getArea().getId().equals(f.getArea().getId()))) return false;
        var permitidas = especializacoesCompativeis(area.getFuncoes());
        return area.getEspecializacoes().stream().allMatch(e -> e.getId() != null && permitidas.contains(e.getId()));
    }
}
