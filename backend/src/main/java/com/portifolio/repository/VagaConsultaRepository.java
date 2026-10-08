package com.portifolio.repository;

import com.portifolio.model.Vaga;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Página somente de IDs; relações são carregadas após o limite físico. */
@Repository
@RequiredArgsConstructor
public class VagaConsultaRepository {
    private final EntityManager entityManager;
    public List<Long> ids(Specification<Vaga> filtro, int offset, int limite) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var raiz = query.from(Vaga.class);
        query.select(raiz.get("id")).where(filtro.toPredicate(raiz, query, cb))
                .orderBy(cb.asc(raiz.get("id")));
        return entityManager.createQuery(query).setFirstResult(offset).setMaxResults(limite).getResultList();
    }
}
