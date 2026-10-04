package com.portifolio.repository;

import com.portifolio.dto.DenunciaRequest;
import com.portifolio.dto.DenunciaResponse;
import com.portifolio.dto.DenunciaResponse.Categoria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** RF14/RF18: usa exclusivamente as duas tabelas já existentes no schema oficial. */
@Repository
@RequiredArgsConstructor
public class DenunciaRepository {
    private final JdbcTemplate jdbc;

    public boolean existeReporteMensagem(Long mensagemId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from reportes_usuario
                    where tipo_conteudo = 'MENSAGEM' and conteudo_id = ?)
                """, Boolean.class, mensagemId));
    }

    // O filtro de denunciante está nas duas partes da consulta, inclusive no detalhe.
    // reportes_usuario não possui status: null é intencional, nunca um estado inventado.
    private static final String PROPRIAS = """
            select id, 'CONTEUDO' as categoria, tipo_conteudo::text as tipo_alvo,
                   conteudo_id as alvo_id, motivo_reporte as motivo, descricao_adicional as descricao,
                   null::text as status, data_reporte as data_registro
              from reportes_usuario where denunciante_id = ?
            union all
            select d.id, 'PLAGIO', 'PERFIL_' || u.tipo_usuario::text, d.perfil_denunciado_id,
                   replace(d.tipo_violacao::text, '_', ' '), d.descricao_detalhada, d.status_denuncia::text, d.data_registro
              from denuncias_plagio d join usuarios u on u.id = d.perfil_denunciado_id
             where d.denunciante_id = ?
            """;

    public long inserir(Long denunciante, DenunciaRequest request, String motivo, String descricao) {
        if (geral(request)) {
            return jdbc.queryForObject("""
                    insert into reportes_usuario(denunciante_id,tipo_conteudo,conteudo_id,motivo_reporte,descricao_adicional)
                    values (?,cast(? as tipo_conteudo_enum),?,?,?) returning id
                    """, Long.class, denunciante, request.tipoAlvo().name(), request.alvoId(), motivo, descricao);
        }
        return jdbc.queryForObject("""
                insert into denuncias_plagio(denunciante_id,perfil_denunciado_id,tipo_violacao,descricao_detalhada)
                values (?,?,cast(? as tipo_violacao_enum),?) returning id
                """, Long.class, denunciante, request.alvoId(), motivo.replace(' ', '_'), descricao);
    }

    private boolean geral(DenunciaRequest request) {
        return request.tipoAlvo() == DenunciaRequest.TipoAlvo.VAGA || request.tipoAlvo() == DenunciaRequest.TipoAlvo.COMUNIDADE;
    }

    public boolean comunidadePublica(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from comunidades c join usuarios u on u.id=c.criador_id where c.id=? and c.privacidade='PUBLICA' and u.status_conta='ATIVA' and "
                + ModeracaoConteudoRepository.COMUNIDADE_PUBLICAVEL + ")", Boolean.class, id));
    }

    public Optional<Long> duplicada(Long denunciante, DenunciaRequest request, String motivo, String descricao) {
        // Lock transacional, compartilhado entre instâncias, sem coluna/constraint nova.
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?,0))", r -> {},
                "RF18:" + denunciante + ":" + request.tipoAlvo() + ":" + request.alvoId());
        String sql = geral(request) ? """
                select id from reportes_usuario where denunciante_id=? and conteudo_id=? and tipo_conteudo::text=?
                and motivo_reporte=? and descricao_adicional is not distinct from cast(? as text)
                and data_reporte >= current_timestamp - interval '60 seconds' order by id desc limit 1
                """ : """
                select id from denuncias_plagio where denunciante_id=? and perfil_denunciado_id=? and tipo_violacao::text=?
                and descricao_detalhada=? and status_denuncia in ('RECEBIDA','EM_ANALISE') order by id desc limit 1
                """;
        return (geral(request)
                ? jdbc.query(sql, (r,n)->r.getLong("id"), denunciante, request.alvoId(), request.tipoAlvo().name(), motivo, descricao)
                : jdbc.query(sql, (r,n)->r.getLong("id"), denunciante, request.alvoId(), motivo.replace(' ', '_'), descricao))
                .stream().findFirst();
    }

    public Optional<DenunciaResponse> buscarPropria(Long denunciante, Categoria categoria, Long id) {
        return jdbc.query("select * from (" + PROPRIAS + ") d where categoria = ? and id = ?",
                this::mapear, denunciante, denunciante, categoria.name(), id).stream().findFirst();
    }

    public List<DenunciaResponse> listarProprias(Long denunciante, int size, long offset) {
        return jdbc.query("select * from (" + PROPRIAS
                        + ") d order by data_registro desc nulls last, categoria, id desc limit ? offset ?",
                this::mapear, denunciante, denunciante, size, offset);
    }

    public long contarProprias(Long denunciante) {
        return jdbc.queryForObject("""
                select (select count(*) from reportes_usuario where denunciante_id = ?)
                     + (select count(*) from denuncias_plagio where denunciante_id = ?)
                """, Long.class, denunciante, denunciante);
    }

    private DenunciaResponse mapear(ResultSet row, int index) throws SQLException {
        var data = row.getTimestamp("data_registro");
        return new DenunciaResponse(row.getLong("id"), Categoria.valueOf(row.getString("categoria")),
                row.getString("tipo_alvo"), row.getLong("alvo_id"), row.getString("motivo"),
                row.getString("descricao"), row.getString("status"), data == null ? null : data.toLocalDateTime());
    }
}
