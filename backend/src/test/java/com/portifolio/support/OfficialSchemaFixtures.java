package com.portifolio.support;

import com.portifolio.model.*;
import com.portifolio.model.enums.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Dados explícitos de testes; nenhum default de negócio é imposto às entities. */
public final class OfficialSchemaFixtures {
    private OfficialSchemaFixtures() {}
    /** Identidade explicita apenas de fixture; nao gera username no cadastro real. */
    public static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setUsername("fixture_" + UUID.randomUUID().toString().replace("-", "").substring(0, 22));
        return usuario;
    }
    public static AreaArtistica area() { return area((short) 1); }
    public static AreaArtistica area(short id) {
        AreaArtistica area = new AreaArtistica(); area.setId(id);
        area.setNome(switch (id) {
            case 1 -> "Artes Cênicas"; case 2 -> "Música"; case 3 -> "Dança";
            case 4 -> "Artes Visuais"; case 5 -> "Audiovisual";
            case 6 -> "Arte e Tecnologia"; case 7 -> "Artes Literárias";
            default -> throw new IllegalArgumentException("Área fora do seed oficial: " + id);
        }); return area;
    }
    public static void funcoes(PerfilArtista perfil, Set<Funcao> funcoes) {
        if (perfil.getAreas().isEmpty()) {
            PerfilArtistaArea vinculo = new PerfilArtistaArea();
            vinculo.setPerfil(perfil); vinculo.setArea(area()); vinculo.setPrincipal(true);
            vinculo.setId(new PerfilArtistaAreaId(perfil.getUsuarioId(), (short) 1));
            perfil.getAreas().add(vinculo);
        }
        perfil.getAreas().iterator().next().setFuncoes(new HashSet<>(funcoes));
    }

    /**
     * Completa uma fixture de artista com dados coerentes com o database04.
     * O estado final continua sendo calculado por {@code fn_verificar_perfil_completo}.
     */
    public static void completarArtista(JdbcTemplate jdbc, long perfilId) {
        String sufixo = Long.toUnsignedString(perfilId);
        Long funcaoId = jdbc.queryForObject("""
                insert into funcoes(area_id, nome)
                values (1, ?)
                on conflict (area_id, nome) do update set nome = excluded.nome
                returning id
                """, Long.class, "Função fixture válida " + sufixo);
        completarArtista(jdbc, perfilId, funcaoId);
    }

    /**
     * Completa uma fixture usando uma função já escolhida pelo cenário. A função é
     * vinculada antes da especialização compatível para respeitar o trigger real.
     */
    public static void completarArtista(JdbcTemplate jdbc, long perfilId, long funcaoId) {
        Short areaId = jdbc.queryForObject(
                "select area_id from funcoes where id = ?", Short.class, funcaoId);
        if (areaId == null) {
            throw new IllegalArgumentException("Função sem área: " + funcaoId);
        }

        jdbc.update("""
                update perfis_artistas
                   set biografia = coalesce(nullif(btrim(biografia), ''), 'Biografia de fixture válida'),
                       cidade = coalesce(nullif(btrim(cidade), ''), 'São Paulo'),
                       estado = coalesce(nullif(btrim(estado), ''), 'SP'),
                       url_portfolio = coalesce(nullif(btrim(url_portfolio), ''), 'https://portfolio.example/fixture')
                 where usuario_id = ?
                """, perfilId);
        jdbc.update(
                "update perfil_artista_area set principal = false where perfil_artista_id = ? and area_id <> ?",
                perfilId, areaId);
        jdbc.update("""
                insert into perfil_artista_area(perfil_artista_id, area_id, principal, nivel_experiencia)
                values (?, ?, true, 'INICIANTE')
                on conflict (perfil_artista_id, area_id) do update
                   set principal = true,
                       nivel_experiencia = coalesce(perfil_artista_area.nivel_experiencia, excluded.nivel_experiencia)
                """, perfilId, areaId);
        jdbc.update("""
                insert into perfil_artista_funcao(perfil_artista_id, area_id, funcao_id)
                values (?, ?, ?)
                on conflict do nothing
                """, perfilId, areaId, funcaoId);

        String nomeEspecializacao = "Especialização fixture " + perfilId + " " + funcaoId;
        Long especializacaoId = jdbc.queryForObject("""
                insert into especializacoes(nome)
                values (?)
                on conflict (nome) do update set nome = excluded.nome
                returning id
                """, Long.class, nomeEspecializacao);
        jdbc.update("""
                insert into funcao_especializacao(funcao_id, especializacao_id)
                values (?, ?)
                on conflict do nothing
                """, funcaoId, especializacaoId);
        jdbc.update("""
                insert into perfil_artista_especializacao(perfil_artista_id, area_id, especializacao_id)
                values (?, ?, ?)
                on conflict do nothing
                """, perfilId, areaId, especializacaoId);

        String cpf = String.format(Locale.ROOT, "8%010d", Math.floorMod(perfilId, 10_000_000_000L));
        jdbc.update("update usuarios set cpf = ? where id = ?", cpf, perfilId);
        Boolean completo = jdbc.queryForObject(
                "select fn_verificar_perfil_completo(?::bigint)", Boolean.class, perfilId);
        if (!Boolean.TRUE.equals(completo)) {
            throw new IllegalStateException("Fixture de artista permaneceu incompleta: " + perfilId);
        }
    }

    /** Cria dados de contratante pessoa fisica no database04, sem depender de trigger historico. */
    public static void completarContratante(JdbcTemplate jdbc, long perfilId) {
        jdbc.update("""
                update perfis_contratantes
                   set tipo_contratante = 'PESSOA_FISICA',
                       biografia = coalesce(nullif(btrim(biografia), ''), 'Biografia de fixture válida'),
                       cidade = coalesce(nullif(btrim(cidade), ''), 'São Paulo'),
                       estado = coalesce(nullif(btrim(estado), ''), 'SP')
                 where usuario_id = ?
                """, perfilId);
        String cpf = String.format(Locale.ROOT, "7%010d", Math.floorMod(perfilId, 10_000_000_000L));
        jdbc.update("update usuarios set cpf = ?, cnpj = null where id = ?", cpf, perfilId);
        Boolean completo = jdbc.queryForObject(
                "select fn_verificar_perfil_completo(?::bigint)", Boolean.class, perfilId);
        if (!Boolean.TRUE.equals(completo)) {
            throw new IllegalStateException("Fixture de contratante permaneceu incompleta: " + perfilId);
        }
    }
}
