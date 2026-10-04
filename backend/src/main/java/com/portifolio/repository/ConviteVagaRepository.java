package com.portifolio.repository;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Registro RF42: CONVITE + destinatário + contexto canônico. Não interpreta mensagem/URL. */
@Repository
@RequiredArgsConstructor
public class ConviteVagaRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public static String contexto(Long vagaId) {
        return "/vagas/" + vagaId;
    }

    // Chamado sob o lock PostgreSQL da vaga, inclusive na repetição.
    public Optional<Long> buscar(Long artistaId, Long vagaId) {
        return jdbc.query("""
                select id from notificacoes
                 where tipo_notificacao = 'CONVITE'
                   and usuario_destino_id = :artista
                   and link_contexto = :contexto
                 order by id limit 1
                """, Map.of("artista", artistaId, "contexto", contexto(vagaId)),
                (rs, row) -> rs.getLong("id")).stream().findFirst();
    }

    public boolean existeInteracao(Long contratanteId, Long artistaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (
                    select 1 from notificacoes n
                    join vagas v on n.link_contexto = concat('/vagas/', v.id)
                    where n.tipo_notificacao = 'CONVITE'
                      and n.usuario_destino_id = :artista
                      and v.contratante_id = :contratante
                      and v.status <> 'RASCUNHO')
                """, Map.of("contratante", contratanteId, "artista", artistaId), Boolean.class));
    }
}
