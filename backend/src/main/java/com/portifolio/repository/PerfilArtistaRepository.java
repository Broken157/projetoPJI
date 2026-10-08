package com.portifolio.repository;

import com.portifolio.model.PerfilArtista;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilArtistaRepository extends JpaRepository<PerfilArtista, Long>, JpaSpecificationExecutor<PerfilArtista> {

    @EntityGraph(attributePaths = {"usuario", "areas", "areas.area", "areas.funcoes",
            "areas.funcoes.area", "areas.funcoes.especializacoes", "areas.especializacoes"})
    @Query("select distinct p from PerfilArtista p where p.usuarioId = :id")
    Optional<PerfilArtista> buscarProfissional(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"usuario", "areas", "areas.funcoes", "areas.funcoes.area"})
    Optional<PerfilArtista> findOne(Specification<PerfilArtista> specification);

    @EntityGraph(attributePaths = {
            "usuario", "areas", "areas.funcoes", "areas.funcoes.area"
    })
    @Query("select distinct perfil from PerfilArtista perfil where perfil.usuarioId = :usuarioId")
    Optional<PerfilArtista> buscarPublicoPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("select distinct perfil from PerfilArtista perfil where perfil.usuarioId in :usuarioIds")
    List<PerfilArtista> buscarPublicosPorUsuarioIds(@Param("usuarioIds") List<Long> usuarioIds);

}
