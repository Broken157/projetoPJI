package com.portifolio.repository;

import com.portifolio.model.Vaga;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.repository.projection.VagaPrazoProjection;
import com.portifolio.repository.projection.VagaRecomendadaProjection;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VagaRepository extends JpaRepository<Vaga, Long>, JpaSpecificationExecutor<Vaga> {

    List<Vaga> findByTituloContainingIgnoreCase(String titulo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vaga v where v.id = :id")
    Optional<Vaga> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select v.id as id, v.titulo as titulo
            from Vaga v
            where v.status in :statusElegiveis
              and v.dataLimiteCandidatura is not null
              and v.dataLimiteCandidatura <= :hoje
            order by v.id
            """)
    List<VagaPrazoProjection> findElegiveisParaEncerramento(
            @Param("statusElegiveis") Set<StatusVaga> statusElegiveis,
            @Param("hoje") LocalDate hoje,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Vaga v
               set v.status = :statusEncerrado
             where v.id = :id
               and v.status in :statusElegiveis
               and v.dataLimiteCandidatura is not null
               and v.dataLimiteCandidatura <= :hoje
            """)
    int encerrarSeElegivelEVencida(
            @Param("id") Long id,
            @Param("hoje") LocalDate hoje,
            @Param("statusElegiveis") Set<StatusVaga> statusElegiveis,
            @Param("statusEncerrado") StatusVaga statusEncerrado);

    // RNF05: carrega funcoes (ManyToMany) e contratante junto, evitando N+1.
    // Usado APÓS a paginação (conjunto de IDs já delimitado) — nunca combine
    // fetch join de coleção com LIMIT/OFFSET na mesma query.
    @EntityGraph(attributePaths = {"funcoes", "contratante", "contratante.usuario", "area"})
    List<Vaga> findByIdIn(List<Long> ids);

    // RF05: uma única vaga detalhada pode carregar funcoes e dados públicos do
    // contratante juntos; fotos permanecem em consulta própria dentro da transação.
    @EntityGraph(attributePaths = {"contratante", "contratante.usuario", "area"})
    @Query("select v from Vaga v where v.id = :id")
    Optional<Vaga> findDetalhesById(@Param("id") Long id);

    // RF03: matching hierárquico compartilhado com RF11, sem algoritmo no agregador.
    @Query(value = """
            select v.id as id,
                   (select count(*) from vaga_funcao vf join perfil_artista_funcao pf
                       on pf.funcao_id=vf.funcao_id and pf.area_id=v.area_id
                       where vf.vaga_id=v.id and pf.perfil_artista_id=:artistaId) as quantidadeFuncoesCoincidentes,
                   (select count(*) from vaga_especializacao ve join perfil_artista_especializacao pe
                       on pe.especializacao_id=ve.especializacao_id and pe.area_id=v.area_id
                       where ve.vaga_id=v.id and pe.perfil_artista_id=:artistaId
                         and exists (select 1 from funcao_especializacao fe
                             join vaga_funcao vf on vf.funcao_id=fe.funcao_id and vf.vaga_id=v.id
                             join perfil_artista_funcao pf on pf.funcao_id=fe.funcao_id
                               and pf.area_id=v.area_id and pf.perfil_artista_id=:artistaId
                             where fe.especializacao_id=ve.especializacao_id)) as quantidadeEspecializacoesCoincidentes
            from vagas v
            where v.status = 'ABERTA'
              and (v.data_limite_candidatura is null or v.data_limite_candidatura > :hoje)
              and exists (select 1 from perfil_artista_area pa
                  where pa.perfil_artista_id=:artistaId and pa.area_id=v.area_id)
              and coalesce((select coalesce(m.status_moderacao::text,'SOB_ANALISE') from moderacao_conteudo m
                  where m.tipo_conteudo='VAGA' and m.conteudo_id=v.id order by m.id desc limit 1),'APROVADO')='APROVADO'
            order by quantidadeFuncoesCoincidentes desc, quantidadeEspecializacoesCoincidentes desc,
                     v.data_publicacao desc nulls last,
                     v.id desc
            """, countQuery = """
            select count(*)
            from vagas v
            where v.status = 'ABERTA'
              and (v.data_limite_candidatura is null or v.data_limite_candidatura > :hoje)
              and exists (select 1 from perfil_artista_area pa
                  where pa.perfil_artista_id=:artistaId and pa.area_id=v.area_id)
              and coalesce((select coalesce(m.status_moderacao::text,'SOB_ANALISE') from moderacao_conteudo m
                  where m.tipo_conteudo='VAGA' and m.conteudo_id=v.id order by m.id desc limit 1),'APROVADO')='APROVADO'
            """, nativeQuery = true)
    Page<VagaRecomendadaProjection> findRecomendadasParaArtista(
            @Param("artistaId") Long artistaId,
            @Param("hoje") LocalDate hoje,
            Pageable pageable);

    @Query(value = """
            select v.id as id, v.titulo as titulo, v.status as status,
                   v.dataPublicacao as dataPublicacao, v.dataLimiteCandidatura as dataLimiteCandidatura
            from Vaga v where v.contratante.usuarioId=:dono
            order by v.dataPublicacao desc nulls last, v.id desc
            """, countQuery = "select count(v) from Vaga v where v.contratante.usuarioId=:dono")
    Page<com.portifolio.repository.projection.VagaDashboardProjection> findPreviewDoContratante(
            @Param("dono") Long dono, Pageable pageable);

    @Query("""
            select v.status as status, count(v) as quantidade from Vaga v
            where v.contratante.usuarioId=:dono group by v.status
            """)
    List<com.portifolio.repository.projection.VagaStatusProjection> contarPorStatusDoContratante(@Param("dono") Long dono);

    @Query(value = "select distinct vf.funcao_id from vaga_funcao vf join vagas v on v.id=vf.vaga_id where v.contratante_id=:contratanteId and v.status in (:statusAtivos)", nativeQuery=true)
    Set<Long> findFuncaoIdsDasVagasAtivasDoContratante(
            @Param("contratanteId") Long contratanteId,
            @Param("statusAtivos") Set<StatusVaga> statusAtivos);
}
