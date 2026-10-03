package com.portifolio.repository;

import com.portifolio.model.PerfilContratante;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilContratanteRepository extends JpaRepository<PerfilContratante, Long>, JpaSpecificationExecutor<PerfilContratante> {

    @Override
    @EntityGraph(attributePaths = "usuario")
    Optional<PerfilContratante> findOne(Specification<PerfilContratante> specification);

    @Query("select perfil from PerfilContratante perfil where perfil.usuarioId = :usuarioId")
    Optional<PerfilContratante> buscarPublicoPorUsuarioId(@Param("usuarioId") Long usuarioId);
}
