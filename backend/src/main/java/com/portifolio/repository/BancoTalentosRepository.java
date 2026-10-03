package com.portifolio.repository;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BancoTalentosRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public boolean adicionar(Long contratanteId, Long artistaId) {
        return jdbc.update("""
                insert into banco_talentos (contratante_id, artista_id)
                values (:contratante, :artista)
                on conflict (contratante_id, artista_id) do nothing
                """, Map.of("contratante", contratanteId, "artista", artistaId)) == 1;
    }

    public boolean participa(Long contratanteId, Long artistaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from banco_talentos
                 where contratante_id = :contratante and artista_id = :artista)
                """, Map.of("contratante", contratanteId, "artista", artistaId), Boolean.class));
    }
}
