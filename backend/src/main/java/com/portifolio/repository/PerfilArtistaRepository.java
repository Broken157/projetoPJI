package com.portifolio.repository;

import com.portifolio.model.PerfilArtista;
import com.portifolio.repository.projection.TalentoSugeridoProjection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilArtistaRepository extends JpaRepository<PerfilArtista, Long> {

    @EntityGraph(attributePaths = {"usuario", "tags"})
    @Query("select distinct perfil from PerfilArtista perfil where perfil.usuarioId = :usuarioId")
    Optional<PerfilArtista> buscarPublicoPorUsuarioId(@Param("usuarioId") Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "tags"})
    @Query("select distinct perfil from PerfilArtista perfil where perfil.usuarioId in :usuarioIds")
    List<PerfilArtista> buscarPublicosPorUsuarioIds(@Param("usuarioIds") List<Long> usuarioIds);

    @Query(value = """
            select pa.usuario_id as usuarioId,
                   count(distinct ta.tag_id) as quantidadeTagsCoincidentes
            from perfis_artistas pa
            join usuarios u on u.id = pa.usuario_id
            join tags_artista ta on ta.artista_id = pa.usuario_id
            where u.tipo_usuario = 'artista'
              and u.perfil_completo = true
              and u.data_nascimento <= current_date - interval '18 years'
              and ta.tag_id in (:tagIds)
            group by pa.usuario_id, pa.ultima_atualizacao
            order by count(distinct ta.tag_id) desc,
                     pa.ultima_atualizacao desc nulls last,
                     pa.usuario_id asc
            """, countQuery = """
            select count(distinct pa.usuario_id)
            from perfis_artistas pa
            join usuarios u on u.id = pa.usuario_id
            join tags_artista ta on ta.artista_id = pa.usuario_id
            where u.tipo_usuario = 'artista'
              and u.perfil_completo = true
              and u.data_nascimento <= current_date - interval '18 years'
              and ta.tag_id in (:tagIds)
            """, nativeQuery = true)
    Page<TalentoSugeridoProjection> findSugeridosPorTags(
            @Param("tagIds") Set<Long> tagIds,
            Pageable pageable);
}
