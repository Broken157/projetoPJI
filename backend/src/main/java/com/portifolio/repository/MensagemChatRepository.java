package com.portifolio.repository;

import com.portifolio.model.MensagemChat;
import com.portifolio.repository.projection.ChatMensagemProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    @Query(value = """
            select m.id as id,
                   m.sala.id as salaId,
                   m.remetente.id as remetenteId,
                   m.remetente.nome as remetenteNome,
                   m.remetente.fotoPerfil as remetenteAvatar,
                   m.texto as texto,
                   m.urlAnexo as urlAnexo,
                   m.lida as lida,
                   m.dataEnvio as dataEnvio
            from MensagemChat m
            where m.sala.id = :salaId
            order by m.dataEnvio desc, m.id desc
            """, countQuery = "select count(m) from MensagemChat m where m.sala.id = :salaId")
    Page<ChatMensagemProjection> findHistorico(
            @Param("salaId") Long salaId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"sala", "remetente"})
    Optional<MensagemChat> findDetalhadaById(Long id);

    @Query("""
            select m.id from MensagemChat m
            where m.sala.id = :salaId
              and m.remetente.id <> :usuarioId
              and coalesce(m.lida, false) = false
            order by m.id
            """)
    List<Long> findIdsNaoLidasRecebidas(
            @Param("salaId") Long salaId,
            @Param("usuarioId") Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MensagemChat m set m.lida = true
            where m.sala.id = :salaId
              and m.remetente.id <> :usuarioId
              and coalesce(m.lida, false) = false
            """)
    int marcarRecebidasComoLidas(
            @Param("salaId") Long salaId,
            @Param("usuarioId") Long usuarioId);

    @Query("""
            select count(m) from MensagemChat m
            where m.remetente.id <> :usuarioId
              and coalesce(m.lida, false) = false
              and exists (
                  select p.id from ParticipanteChat p
                  where p.sala.id = m.sala.id and p.usuario.id = :usuarioId
              )
            """)
    long countNaoLidasRecebidas(@Param("usuarioId") Long usuarioId);
}
