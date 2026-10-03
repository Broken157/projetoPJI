package com.portifolio.repository;

import com.portifolio.model.CategoriaAfirmativa;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Adaptador do catalogo oficial enum; nao depende de tabela inexistente. */
@Component
public class CategoriaAfirmativaRepository {
    public List<CategoriaAfirmativa> findAll() { return List.of(CategoriaAfirmativa.values()); }
    public List<CategoriaAfirmativa> findAllById(Set<Integer> ids) {
        return Arrays.stream(CategoriaAfirmativa.values()).filter(c -> ids.contains(c.getId())).toList();
    }
}
