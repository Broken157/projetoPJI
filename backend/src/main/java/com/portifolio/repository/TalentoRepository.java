package com.portifolio.repository;

import com.portifolio.dto.FiltroTalentos;
import com.portifolio.dto.TalentoResponse;
import com.portifolio.dto.TalentoResponse.*;
import com.portifolio.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** RF17: Banco próprio, filtros/ordenação no PostgreSQL e taxonomia em lote. */
@Repository
@RequiredArgsConstructor
public class TalentoRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public record ContextoConsulta(Contexto dados, Set<Long> funcoes, Set<Long> especializacoes) {}

    public ContextoConsulta contexto(Long contratanteId, Long vagaId) {
        var params = new HashMap<String, Object>();
        params.put("dono", contratanteId);
        params.put("vaga", vagaId);
        String where = vagaId == null
                ? "v.contratante_id = :dono and v.status in ('ABERTA','PAUSADA')"
                : "v.id = :vaga and v.contratante_id = :dono";
        var rows = jdbc.query("""
                select v.id, v.titulo, v.area_id,
                    array(select funcao_id from vaga_funcao where vaga_id=v.id) funcoes,
                    array(select especializacao_id from vaga_especializacao where vaga_id=v.id) especializacoes
                from vagas v where
                """ + where + " order by v.data_publicacao desc nulls last, v.id desc limit 1", params,
                (rs, n) -> new ContextoConsulta(new Contexto(rs.getLong("id"), rs.getString("titulo"), rs.getShort("area_id")),
                        new HashSet<>(Arrays.asList((Long[]) rs.getArray("funcoes").getArray())),
                        new HashSet<>(Arrays.asList((Long[]) rs.getArray("especializacoes").getArray()))));
        if (rows.isEmpty() && vagaId != null) throw new ResourceNotFoundException("Vaga de contexto não encontrada.");
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public Pagina<Contexto> contextos(Long dono, int page, int size) {
        var params = Map.of("dono", dono, "limite", size, "offset", (long) page * size);
        String from = " from vagas where contratante_id=:dono and status in ('ABERTA','PAUSADA')";
        long total = jdbc.queryForObject("select count(*)" + from, params, Long.class);
        var content = jdbc.query("select id,titulo,area_id" + from
                + " order by data_publicacao desc nulls last,id desc limit :limite offset :offset", params,
                (rs, n) -> new Contexto(rs.getLong("id"), rs.getString("titulo"), rs.getShort("area_id")));
        return new Pagina<>(content, page, size, total, ((long) page + 1) * size < total, null);
    }

    public Pagina<TalentoResponse> buscar(Long dono, FiltroTalentos f, Short area, ContextoConsulta contexto) {
        var params = new HashMap<String, Object>();
        Set<Long> matchFuncoes = contexto == null ? f.funcaoIds() : contexto.funcoes();
        Set<Long> matchEspecializacoes = contexto == null ? f.especializacaoIds() : contexto.especializacoes();
        params.put("matchFuncoes", matchFuncoes.isEmpty() ? Set.of(-1L) : matchFuncoes);
        params.put("matchEspecializacoes", matchEspecializacoes.isEmpty() ? Set.of(-1L) : matchEspecializacoes);
        params.put("area", area);
        params.put("limite", f.size());
        params.put("offset", (long) f.page() * f.size());
        // Mesmos limites civis de MenorAutorizadoPolicy.publicavel; nenhum dado privado no DTO.
        LocalDate hoje = LocalDate.now(clock);
        params.put("adultoAte", hoje.minusYears(18));
        params.put("menorAte", hoje.minusYears(14));
        params.put("dono", dono);

        StringBuilder where = new StringBuilder("""
                 from perfis_artistas p join usuarios u on u.id=p.usuario_id
                 where u.tipo_usuario='ARTISTA' and u.status_conta='ATIVA'
                   and exists(select 1 from banco_talentos b
                       where b.contratante_id=:dono and b.artista_id=p.usuario_id)
                   and (u.data_nascimento <= :adultoAte
                       or (u.data_nascimento > :adultoAte and u.data_nascimento <= :menorAte
                           and exists(select 1 from responsaveis_legais r
                               where r.usuario_id=u.id and r.data_consentimento is not null
                                   and coalesce(r.consentimento_revogado,false)=false)))
                """);
        if (f.q() != null) {
            // POSITION é literal: %, _ e barras não são curingas e o valor permanece bindado.
            where.append(" and position(lower(:nome) in lower(u.nome)) > 0");
            params.put("nome", f.q());
        }
        if (area != null) {
            where.append(" and exists(select 1 from perfil_artista_area a where a.perfil_artista_id=p.usuario_id and a.area_id=:area");
            if (f.experienciaMinima() != null && f.experienciaMinima() != com.portifolio.model.enums.NivelExperiencia.SEM_EXPERIENCIA) {
                // Não transformar experiência privada de menor em oráculo de presença/ausência.
                where.append(" and (u.data_nascimento > :adultoAte or a.nivel_experiencia >= cast(:experiencia as nivel_experiencia_enum))");
                params.put("experiencia", f.experienciaMinima().name());
            }
            where.append(")");
        }
        if (!f.funcaoIds().isEmpty()) {
            where.append(" and exists(select 1 from perfil_artista_funcao af where af.perfil_artista_id=p.usuario_id and af.area_id=:area and af.funcao_id in (:funcoes))");
            params.put("funcoes", f.funcaoIds());
        }
        if (!f.especializacaoIds().isEmpty()) {
            where.append(" and exists(select 1 from perfil_artista_especializacao ae where ae.perfil_artista_id=p.usuario_id and ae.area_id=:area and ae.especializacao_id in (:especializacoes)"
                    + " and exists(select 1 from perfil_artista_funcao af join funcao_especializacao fe on fe.funcao_id=af.funcao_id where af.perfil_artista_id=ae.perfil_artista_id and af.area_id=ae.area_id and fe.especializacao_id=ae.especializacao_id and af.funcao_id in (:funcoes)))");
            params.put("especializacoes", f.especializacaoIds());
        }
        if (f.localizacao() != null && !f.localizacao().isBlank()) {
            where.append(" and position(lower(:localizacao) in lower(concat_ws(', ',nullif(trim(p.cidade),''),nullif(trim(p.estado),'')))) > 0");
            params.put("localizacao", f.localizacao().trim());
        }
        if (f.cidade() != null) {
            where.append(" and lower(p.cidade)=lower(:cidade)");
            params.put("cidade", f.cidade());
        }
        if (f.estado() != null) {
            where.append(" and p.estado=:estado");
            params.put("estado", f.estado());
        }
        if (f.disponivel() != null) {
            where.append(" and p.disponivel_oportunidades=:disponivel");
            params.put("disponivel", f.disponivel());
        }
        if (!f.tipos().isEmpty()) {
            where.append(" and p.tipo_perfil_artistico::text in (:tipos)");
            params.put("tipos", f.tipos().stream().map(Enum::name).toList());
        }
        String areaMatch = area == null ? "" : " and af.area_id=:area";
        String areaSpec = area == null ? "" : " and ae.area_id=:area";
        String select = """
                select p.usuario_id,u.username,u.nome,u.foto_perfil_url,left(p.biografia,400) biografia,
                       p.cidade,p.estado,p.url_portfolio,p.tipo_perfil_artistico,
                       p.disponivel_oportunidades,p.ultima_atualizacao,
                       exists(select 1 from itens_salvos s where s.usuario_id=:dono
                           and s.tipo_alvo='PERFIL_ARTISTA' and s.alvo_id=p.usuario_id) salvo,
                       (select count(*) from perfil_artista_funcao af
                         where af.perfil_artista_id=p.usuario_id and af.funcao_id in (:matchFuncoes)
                """ + areaMatch + """
                ) quantidade_funcoes,
                       (select count(*) from perfil_artista_especializacao ae
                         where ae.perfil_artista_id=p.usuario_id and ae.especializacao_id in (:matchEspecializacoes)
                """ + areaSpec + """
                         and exists(select 1 from perfil_artista_funcao af
                           join funcao_especializacao fe on fe.funcao_id=af.funcao_id
                           where af.perfil_artista_id=ae.perfil_artista_id and af.area_id=ae.area_id
                             and fe.especializacao_id=ae.especializacao_id)
                       ) quantidade_especializacoes
                """;
        String order = f.ordenacao() == FiltroTalentos.Ordenacao.ATUALIZACAO ? ""
                : "quantidade_funcoes desc, quantidade_especializacoes desc, ";
        long total = jdbc.queryForObject("select count(distinct p.usuario_id)" + where, params, Long.class);
        var rows = jdbc.query(select + where + " order by " + order
                + "p.ultima_atualizacao desc nulls last,p.usuario_id asc limit :limite offset :offset", params,
                (rs, n) -> TalentoResponse.builder()
                        .artistaId(rs.getLong("usuario_id")).username(rs.getString("username")).nomeExibicao(rs.getString("nome"))
                        .avatarUrl(rs.getString("foto_perfil_url")).biografia(rs.getString("biografia"))
                        .cidade(rs.getString("cidade")).estado(rs.getString("estado"))
                        .localizacao(com.portifolio.validation.LocalizacaoArtista.formatar(rs.getString("cidade"), rs.getString("estado")))
                        .urlPortfolio(rs.getString("url_portfolio"))
                        .tipoPerfilArtistico(rs.getString("tipo_perfil_artistico")).salvo(rs.getBoolean("salvo"))
                        .disponivelOportunidades(rs.getObject("disponivel_oportunidades", Boolean.class))
                        .ultimaAtualizacao(rs.getObject("ultima_atualizacao", java.time.LocalDateTime.class))
                        .quantidadeFuncoesCoincidentes(rs.getLong("quantidade_funcoes"))
                        .quantidadeEspecializacoesCoincidentes(rs.getLong("quantidade_especializacoes")));
        if (rows.isEmpty()) return new Pagina<>(List.of(), f.page(), f.size(), total, false, contexto == null ? null : contexto.dados());
        var ids = rows.stream().map(b -> b.build().getArtistaId()).toList();
        var areas = taxonomia(ids, area, hoje.minusYears(18));
        var content = rows.stream().map(b -> {
            Long id = b.build().getArtistaId();
            return b.areas(areas.getOrDefault(id, List.of())).build();
        }).toList();
        return new Pagina<>(content, f.page(), f.size(), total, ((long) f.page() + 1) * f.size() < total,
                contexto == null ? null : contexto.dados());
    }

    private Map<Long, List<Area>> taxonomia(List<Long> ids, Short area, LocalDate adultoAte) {
        var params = new HashMap<String, Object>();
        params.put("ids", ids); params.put("area", area);
        params.put("adultoAte", adultoAte);
        String areaWhere = area == null ? "" : " and a.area_id=:area";
        Map<Long, List<Area>> result = new HashMap<>();
        Map<String, Area> byKey = new HashMap<>();
        jdbc.query("""
                select a.perfil_artista_id,a.area_id,c.nome,
                    case when u.data_nascimento <= :adultoAte then a.nivel_experiencia::text end nivel_experiencia
                from perfil_artista_area a join areas_artisticas c on c.id=a.area_id
                join usuarios u on u.id=a.perfil_artista_id
                where a.perfil_artista_id in (:ids)
                """ + areaWhere + " order by a.area_id", params, rs -> {
            long id = rs.getLong("perfil_artista_id"); short areaId = rs.getShort("area_id");
            var item = new Area(areaId, rs.getString("nome"), rs.getString("nivel_experiencia"), new ArrayList<>(), new ArrayList<>());
            result.computeIfAbsent(id, k -> new ArrayList<>()).add(item);
            byKey.put(id + ":" + areaId, item);
        });
        jdbc.query("""
                select a.perfil_artista_id,a.area_id,f.id,f.nome
                from perfil_artista_funcao a join funcoes f on f.id=a.funcao_id
                where a.perfil_artista_id in (:ids)
                """ + areaWhere + " order by f.nome,f.id", params, rs -> {
            var parent = byKey.get(rs.getLong("perfil_artista_id") + ":" + rs.getShort("area_id"));
            if (parent != null) parent.funcoes().add(new Item(rs.getLong("id"), rs.getString("nome")));
        });
        jdbc.query("""
                select a.perfil_artista_id,a.area_id,e.id,e.nome
                from perfil_artista_especializacao a join especializacoes e on e.id=a.especializacao_id
                where a.perfil_artista_id in (:ids)
                  and exists(select 1 from perfil_artista_funcao af join funcao_especializacao fe on fe.funcao_id=af.funcao_id
                    where af.perfil_artista_id=a.perfil_artista_id and af.area_id=a.area_id and fe.especializacao_id=e.id)
                """ + areaWhere + " order by e.nome,e.id", params, rs -> {
            var parent = byKey.get(rs.getLong("perfil_artista_id") + ":" + rs.getShort("area_id"));
            if (parent != null) parent.especializacoes().add(new Item(rs.getLong("id"), rs.getString("nome")));
        });
        return result;
    }
}
