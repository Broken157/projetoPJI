package com.portifolio.repository;

import com.portifolio.dto.FiltroDescobertaPublica;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilArtistaArea;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Usuario;
import com.portifolio.model.ItemSalvo;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.model.enums.TipoPerfilArtistico;
import com.portifolio.model.enums.TipoContratante;
import com.portifolio.model.enums.TipoAlvoSalvo;
import com.portifolio.security.MenorAutorizadoPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/** Projeção escalar RF37: nenhum fetch de entity, experiência ou coleção profissional. */
@Repository
@RequiredArgsConstructor
public class PerfilDescobertaRepository {
    private final EntityManager entityManager;
    private final MenorAutorizadoPolicy publicabilidade;

    public Page<Resumo> buscar(FiltroDescobertaPublica filtro, Long usuarioFavoritos) {
        var cb = entityManager.getCriteriaBuilder();
        var count = cb.createQuery(Long.class);
        var contagem = consulta(cb, count, filtro, usuarioFavoritos);
        count.select(cb.count(contagem.usuario)).where(contagem.predicado);
        long total = parametros(entityManager.createQuery(count), filtro, usuarioFavoritos).getSingleResult();

        var pageable = PageRequest.of(filtro.page(), filtro.size());
        if (total == 0 || pageable.getOffset() >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        var select = cb.createTupleQuery();
        var pagina = consulta(cb, select, filtro, usuarioFavoritos);
        select.select(cb.tuple(
                pagina.usuario.get("id"), pagina.usuario.get("tipoUsuario"),
                pagina.usuario.get("username"), pagina.nome,
                pagina.usuario.get("fotoPerfil"), pagina.cidade, pagina.estado, pagina.tipoPerfil, pagina.tipoContratante))
                .where(pagina.predicado)
                .orderBy(cb.asc(cb.lower(pagina.nome)), cb.asc(pagina.usuario.get("id")));
        List<Resumo> content = parametros(entityManager.createQuery(select), filtro, usuarioFavoritos)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(filtro.size())
                .getResultList().stream().map(this::resumo).toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Consulta consulta(CriteriaBuilder cb, CriteriaQuery<?> query, FiltroDescobertaPublica filtro, Long usuarioFavoritos) {
        // Entity joins fazem parte do Jakarta Persistence 3.2 usado por este backend.
        var usuario = query.from(Usuario.class);
        var artista = usuario.join(PerfilArtista.class, JoinType.LEFT);
        artista.on(cb.equal(artista.get("usuarioId"), usuario.get("id")));
        var contratante = usuario.join(PerfilContratante.class, JoinType.LEFT);
        contratante.on(cb.equal(contratante.get("usuarioId"), usuario.get("id")));
        var tipoArtista = cb.equal(usuario.get("tipoUsuario"), TipoUsuario.ARTISTA);
        var tipoContratante = cb.equal(usuario.get("tipoUsuario"), TipoUsuario.CONTRATANTE);
        Expression<String> nome = cb.<String>selectCase().when(cb.and(tipoContratante,
                cb.isNotNull(contratante.get("nomeEmpresa")),
                cb.notEqual(cb.trim(contratante.get("nomeEmpresa")), "")), contratante.get("nomeEmpresa"))
                .otherwise(usuario.get("nome"));
        Expression<String> cidade = cb.<String>selectCase().when(tipoArtista, artista.get("cidade"))
                .otherwise(contratante.get("cidade"));
        Expression<String> estado = cb.<String>selectCase().when(tipoArtista, artista.get("estado"))
                .otherwise(contratante.get("estado"));
        List<Predicate> filtros = new ArrayList<>();
        filtros.add(publicabilidade.publicavel(cb, query, usuario));
        filtros.add(cb.or(cb.and(tipoArtista, cb.isNotNull(artista.get("usuarioId"))),
                cb.and(tipoContratante, cb.isNotNull(contratante.get("usuarioId")))));
        if (filtro.tipo() != null) {
            filtros.add(cb.equal(usuario.get("tipoUsuario"), cb.parameter(TipoUsuario.class, "tipo")));
        }
        if (filtro.q() != null) {
            var username = cb.like(cb.lower(usuario.get("username")), cb.parameter(String.class, "q"), '\\');
            filtros.add(filtro.q().startsWith("@") ? username
                    : cb.or(username, cb.like(cb.lower(nome), cb.parameter(String.class, "q"), '\\')));
        }
        if (filtro.cidade() != null) {
            filtros.add(cb.equal(cb.lower(cidade), cb.parameter(String.class, "cidade")));
        }
        if (filtro.estado() != null) {
            filtros.add(cb.equal(cb.upper(estado), cb.parameter(String.class, "estado")));
        }
        if (filtro.tipoPerfil() != null) {
            filtros.add(tipoArtista);
            filtros.add(cb.equal(artista.get("tipoPerfilArtistico"), cb.parameter(TipoPerfilArtistico.class, "tipoPerfil")));
        }
        if (filtro.tipoContratante() != null) {
            filtros.add(tipoContratante);
            filtros.add(cb.equal(contratante.get("tipoContratante"), cb.parameter(TipoContratante.class, "tipoContratante")));
        }
        if (filtro.areaId() != null || filtro.funcaoId() != null || filtro.especializacaoId() != null) {
            var area = query.subquery(Integer.class);
            var vinculo = area.from(PerfilArtistaArea.class);
            List<Predicate> cadeia = new ArrayList<>();
            cadeia.add(cb.equal(vinculo.get("id").get("perfilArtistaId"), usuario.get("id")));
            if (filtro.areaId() != null)
                cadeia.add(cb.equal(vinculo.get("id").get("areaId"), cb.parameter(Short.class, "areaId")));
            if (filtro.funcaoId() != null || filtro.especializacaoId() != null) {
                var funcao = vinculo.join("funcoes");
                cadeia.add(cb.equal(funcao.get("area").get("id"), vinculo.get("id").get("areaId")));
                if (filtro.funcaoId() != null)
                    cadeia.add(cb.equal(funcao.get("id"), cb.parameter(Long.class, "funcaoId")));
                if (filtro.especializacaoId() != null) {
                    var selecionada = vinculo.join("especializacoes");
                    var compativel = funcao.join("especializacoes");
                    cadeia.add(cb.equal(selecionada.get("id"), cb.parameter(Long.class, "especializacaoId")));
                    cadeia.add(cb.equal(compativel.get("id"), selecionada.get("id")));
                }
            }
            area.select(cb.literal(1)).where(cadeia.toArray(Predicate[]::new));
            filtros.add(tipoArtista);
            filtros.add(cb.exists(area));
        }
        if (filtro.somenteFavoritos()) {
            if (usuarioFavoritos == null) throw new com.portifolio.exception.UnauthorizedException("Autenticação obrigatória para favoritos.");
            var favoritos = query.subquery(Integer.class);
            var salvo = favoritos.from(ItemSalvo.class);
            favoritos.select(cb.literal(1)).where(
                    cb.equal(salvo.get("usuarioId"), cb.parameter(Long.class, "usuarioFavoritos")),
                    cb.equal(salvo.get("tipoAlvo"), TipoAlvoSalvo.PERFIL_ARTISTA),
                    cb.equal(salvo.get("alvoId"), usuario.get("id")));
            filtros.add(tipoArtista);
            filtros.add(cb.exists(favoritos));
        }
        return new Consulta(usuario, nome, cidade, estado, artista.get("tipoPerfilArtistico"),
                contratante.get("tipoContratante"), cb.and(filtros.toArray(Predicate[]::new)));
    }

    private <T> TypedQuery<T> parametros(TypedQuery<T> query, FiltroDescobertaPublica filtro, Long usuarioFavoritos) {
        if (filtro.tipo() != null) query.setParameter("tipo", filtro.tipo());
        if (filtro.q() != null) {
            String texto = filtro.q().startsWith("@") ? filtro.q().substring(1) : filtro.q();
            String literal = texto.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            query.setParameter("q", "%" + literal + "%");
        }
        if (filtro.cidade() != null) query.setParameter("cidade", filtro.cidade().toLowerCase(Locale.ROOT));
        if (filtro.estado() != null) query.setParameter("estado", filtro.estado());
        if (filtro.areaId() != null) query.setParameter("areaId", filtro.areaId());
        if (filtro.funcaoId() != null) query.setParameter("funcaoId", filtro.funcaoId());
        if (filtro.especializacaoId() != null) query.setParameter("especializacaoId", filtro.especializacaoId());
        if (filtro.tipoPerfil() != null) query.setParameter("tipoPerfil", filtro.tipoPerfil());
        if (filtro.tipoContratante() != null) query.setParameter("tipoContratante", filtro.tipoContratante());
        if (filtro.somenteFavoritos()) query.setParameter("usuarioFavoritos", usuarioFavoritos);
        return query;
    }

    private Resumo resumo(Tuple item) {
        var tipo = item.get(1, TipoUsuario.class);
        return new Resumo(item.get(0, Long.class), tipo, item.get(2, String.class),
                item.get(3, String.class), item.get(4, String.class), item.get(5, String.class), item.get(6, String.class),
                tipo == TipoUsuario.ARTISTA ? item.get(7, TipoPerfilArtistico.class) : null,
                tipo == TipoUsuario.CONTRATANTE ? item.get(8, TipoContratante.class) : null);
    }

    public record Resumo(Long usuarioId, TipoUsuario tipo, String username, String nomeExibicao,
            String fotoPerfil, String cidade, String estado, TipoPerfilArtistico tipoPerfil,
            TipoContratante tipoContratante) {}
    private record Consulta(jakarta.persistence.criteria.Root<Usuario> usuario, Expression<String> nome,
            Expression<String> cidade, Expression<String> estado, Expression<TipoPerfilArtistico> tipoPerfil,
            Expression<TipoContratante> tipoContratante, Predicate predicado) {}
}
