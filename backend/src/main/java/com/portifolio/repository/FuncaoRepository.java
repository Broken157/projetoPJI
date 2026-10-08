package com.portifolio.repository;

import com.portifolio.model.Funcao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncaoRepository extends JpaRepository<Funcao, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"area", "especializacoes"})
    @org.springframework.data.jpa.repository.Query("select distinct f from Funcao f where f.id in :ids")
    java.util.List<Funcao> buscarTaxonomia(@org.springframework.data.repository.query.Param("ids") java.util.Set<Long> ids);
    long countByAreaIdAndIdIn(Short areaId, java.util.Set<Long> ids);
    long countByIdIn(java.util.Set<Long> ids);
    org.springframework.data.domain.Page<Funcao> findByAreaId(Short areaId, org.springframework.data.domain.Pageable pageable);
    List<Funcao> findByNomeContainingIgnoreCase(String nome);

}
