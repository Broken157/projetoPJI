package com.portifolio.repository;

import com.portifolio.model.MensagemChat;
import com.portifolio.repository.projection.ChatMensagemProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    @Query(value = """
            select m.id as id,
                   m.sala.id as salaId,
                   r.id as remetenteId,
                   r.nome as remetenteNome,
                   r.fotoPerfil as remetenteAvatar,
                   m.texto as texto,
                   m.urlAnexo as urlAnexo,
                   m.lida as lida,
                   m.excluida as excluida,
                   m.editada as editada,
                   m.dataEdicao as dataEdicao,
                   m.dataEnvio as dataEnvio
            from MensagemChat m
            left join m.remetente r
            where m.sala.id = :salaId
            order by m.dataEnvio desc, m.id desc
            """, countQuery = "select count(m) from MensagemChat m where m.sala.id = :salaId")
    Page<ChatMensagemProjection> findHistorico(
            @Param("salaId") Long salaId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"sala", "remetente"})
    Optional<MensagemChat> findDetalhadaById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"sala"})
    @Query("select m from MensagemChat m where m.id = :id")
    Optional<MensagemChat> findByIdForUpdate(@Param("id") Long id);

    @Query("select max(m.id) from MensagemChat m where m.sala.id = :salaId")
    Long findUltimoIdDaSala(@Param("salaId") Long salaId);

    @Query("""
            select m.id from MensagemChat m
            where m.sala.id = :salaId
              and (m.remetente is null or m.remetente.id <> :usuarioId)
              and coalesce(m.lida, false) = false
              and coalesce(m.excluida, false) = false
              and m.id <= :limite
            order by m.dataEnvio desc, m.id desc
            """)
    List<Long> findIdsNaoLidasRecebidas(
            @Param("salaId") Long salaId,
            @Param("usuarioId") Long usuarioId,
            @Param("limite") Long limite,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MensagemChat m set m.lida = true
            where m.sala.id = :salaId
              and (m.remetente is null or m.remetente.id <> :usuarioId)
              and m.id in :ids
              and coalesce(m.lida, false) = false
              and coalesce(m.excluida, false) = false
            """)
    int marcarRecebidasComoLidas(
            @Param("salaId") Long salaId,
            @Param("usuarioId") Long usuarioId,
            @Param("ids") List<Long> ids);

    @Query("""
            select count(m) from MensagemChat m
            where (m.remetente is null or m.remetente.id <> :usuarioId)
              and coalesce(m.lida, false) = false
              and coalesce(m.excluida, false) = false
              and exists (
                  select p.id from ParticipanteChat p
                  where p.sala.id = m.sala.id and p.usuario.id = :usuarioId
              )
              and (select count(p) from ParticipanteChat p where p.sala.id = m.sala.id) in (1, 2)
            """)
    long countNaoLidasRecebidas(@Param("usuarioId") Long usuarioId);
}
