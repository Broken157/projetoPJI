package com.portifolio.repository;

import com.portifolio.model.Notificacao;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByUsuarioDestinoId(Long usuarioId, Pageable pageable);

    long countByUsuarioDestinoIdAndLidaFalse(Long usuarioId);

    Optional<Notificacao> findByIdAndUsuarioDestinoId(Long id, Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notificacao n
               set n.lida = true
             where n.usuarioDestino.id = :usuarioId
               and n.lida = false
            """)
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId);
}
