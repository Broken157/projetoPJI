package com.portifolio.repository;

import com.portifolio.model.Candidatura;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.StatusCandidatura;
import com.portifolio.repository.projection.CandidaturaDashboardProjection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidaturaRepository extends JpaRepository<Candidatura, Long> {
    List<Candidatura> findByVagaId(Long vagaId);
    @Query("""
            select c.id from Candidatura c
            where c.vaga.id = :vagaId and c.status in :statusAtivos
              and c.artista.usuarioId > 0
              and not exists (
                  select posterior.id from Candidatura posterior
                  where posterior.vaga.id = c.vaga.id
                    and posterior.artista.usuarioId = c.artista.usuarioId
                    and posterior.id > c.id
              )
            order by c.dataCandidatura desc, c.id desc
            """)
    Page<Long> findIdsPorVagaSemTaxonomia(
            @Param("vagaId") Long vagaId,
            @Param("statusAtivos") Set<StatusCandidatura> statusAtivos, Pageable pageable);
    List<Candidatura> findByArtistaUsuarioId(Long usuarioId);
    List<Candidatura> findByVagaContratanteUsuarioId(Long usuarioId);
    boolean existsByVagaIdAndArtistaUsuarioId(Long vagaId, Long usuarioId);
    boolean existsByArtistaUsuarioIdAndVagaContratanteUsuarioId(
            Long artistaId, Long contratanteId);
    List<Candidatura> findByVagaIdAndArtistaUsuarioIdOrderByIdDesc(Long vagaId, Long usuarioId);

    @Query("""
            select distinct c.artista.usuarioId
            from Candidatura c
            where c.vaga.id = :vagaId
              and (:cursor is null or c.artista.usuarioId > :cursor)
            order by c.artista.usuarioId
            """)
    List<Long> findArtistaUsuarioIdsByVagaIdAposCursor(
            @Param("vagaId") Long vagaId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    @EntityGraph(attributePaths = {"vaga", "artista"})
    Page<Candidatura> findByArtistaUsuarioId(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"vaga", "artista"})
    Page<Candidatura> findByVagaContratanteUsuarioId(Long usuarioId, Pageable pageable);

    // RF45: filtrar antes de paginar/contar; maior ID é a última tentativa RF06.
    @Query(value = """
            select c.id from candidaturas c
            join perfis_artistas a on a.usuario_id = c.artista_id
            left join perfil_artista_funcao f on f.perfil_artista_id = a.usuario_id
            where c.vaga_id = :vagaId and c.status in ('PENDENTE', 'EM_ANALISE')
              and c.artista_id > 0
              and not exists (
                  select 1 from candidaturas posterior
                  where posterior.vaga_id = c.vaga_id and posterior.artista_id = c.artista_id
                    and posterior.id > c.id
              )
            group by c.id, a.ultima_atualizacao
            order by sum(case when f.funcao_id in (:funcaoIds) then 1 else 0 end) desc,
                     a.ultima_atualizacao desc nulls last, c.id asc
            """, countQuery = """
            select count(*) from candidaturas c
            where c.vaga_id = :vagaId and c.status in ('PENDENTE', 'EM_ANALISE')
              and c.artista_id > 0
              and not exists (
                  select 1 from candidaturas posterior
                  where posterior.vaga_id = c.vaga_id and posterior.artista_id = c.artista_id
                    and posterior.id > c.id
              )
            """, nativeQuery = true)
    Page<Long> findIdsPorVagaOrdenadosPorCompatibilidade(
            @Param("vagaId") Long vagaId,
            @Param("funcaoIds") Set<Long> funcaoIds,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "artista", "artista.usuario", "artista.areas", "artista.areas.funcoes"
    })
    @Query("select distinct c from Candidatura c where c.id in :ids")
    List<Candidatura> findDetalhadasByIdIn(@Param("ids") List<Long> ids);

    // RF03 Fase 2 — candidaturas do artista em vagas que foram canceladas (RF25),
    // usado para decidir quais vagas CANCELADA ainda devem aparecer para ele.
    List<Candidatura> findByArtista_UsuarioIdAndVaga_Status(Long usuarioId, StatusVaga status);

    @Query("""
            select distinct c.vaga.id
            from Candidatura c
            where c.artista.usuarioId = :artistaId
              and c.vaga.status = :status
              and (:cursor is null or c.vaga.id > :cursor)
            order by c.vaga.id asc
            """)
    List<Long> findVagaIdsDoArtistaPorStatusAposCursor(
            @Param("artistaId") Long artistaId,
            @Param("status") StatusVaga status,
            @Param("cursor") Long cursor,
            Pageable pageable);

    @Query(value = """
            select c.id as id,
                   vaga.id as vagaId,
                   vaga.titulo as tituloVaga,
                   artista.usuarioId as artistaId,
                   usuario.nome as nomeArtista,
                   usuario.fotoPerfil as fotoPerfilArtista,
                   usuario.fotoPerfil as fotoPerfilUsuario,
                   c.status as status,
                   c.dataCandidatura as dataCandidatura
            from Candidatura c
            join c.vaga vaga
            join c.artista artista
            join artista.usuario usuario
            where vaga.contratante.usuarioId = :contratanteId
              and vaga.status in :statusAtivos
              and c.status in ('PENDENTE', 'EM_ANALISE') and artista.usuarioId > 0
              and not exists (select posterior.id from Candidatura posterior
                  where posterior.vaga.id=c.vaga.id and posterior.artista.usuarioId=artista.usuarioId
                    and posterior.id > c.id)
            order by c.dataCandidatura desc, c.id desc
            """, countQuery = """
            select count(c.id)
            from Candidatura c
            join c.vaga vaga
            where vaga.contratante.usuarioId = :contratanteId
              and vaga.status in :statusAtivos
              and c.status in ('PENDENTE', 'EM_ANALISE') and c.artista.usuarioId > 0
              and not exists (select posterior.id from Candidatura posterior
                  where posterior.vaga.id=c.vaga.id and posterior.artista.usuarioId=c.artista.usuarioId
                    and posterior.id > c.id)
            """)
    Page<CandidaturaDashboardProjection> findRecentesDoContratanteEmVagasAtivas(
            @Param("contratanteId") Long contratanteId,
            @Param("statusAtivos") Set<StatusVaga> statusAtivos,
            Pageable pageable);

    @Query(value = """
            select c.id as id, c.vaga.id as vagaId, c.vaga.titulo as tituloVaga,
                   c.status as status, c.dataCandidatura as dataCandidatura
            from Candidatura c where c.artista.usuarioId=:dono
            order by c.dataCandidatura desc, c.id desc
            """, countQuery = "select count(c) from Candidatura c where c.artista.usuarioId=:dono")
    Page<CandidaturaDashboardProjection> findPreviewDoArtista(@Param("dono") Long dono, Pageable pageable);
}
