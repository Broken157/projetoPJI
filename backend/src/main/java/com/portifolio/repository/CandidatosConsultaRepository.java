package com.portifolio.repository;

import com.portifolio.dto.CandidatosVagaFiltro;
import com.portifolio.model.Candidatura;
import com.portifolio.model.Funcao;
import com.portifolio.model.ItemSalvo;
import com.portifolio.model.PerfilArtistaArea;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusCandidatura;
import com.portifolio.model.enums.TipoAlvoSalvo;
import com.portifolio.security.MenorAutorizadoPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CandidatosConsultaRepository {
    private final EntityManager entityManager;
    private final MenorAutorizadoPolicy menorAutorizadoPolicy;

    public Page<Long> buscar(Long vagaId, Long ownerId, CandidatosVagaFiltro filtro, Pageable pagina) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> consulta = cb.createQuery(Long.class);
        Root<Candidatura> candidatura = consulta.from(Candidatura.class);
        consulta.select(candidatura.get("id")).where(predicado(cb, consulta, candidatura, vagaId, ownerId, filtro));
        consulta.orderBy(cb.desc(candidatura.get("dataCandidatura")), cb.desc(candidatura.get("id")));
        List<Long> ids = entityManager.createQuery(consulta)
                .setFirstResult(Math.toIntExact(pagina.getOffset())).setMaxResults(pagina.getPageSize()).getResultList();
        CriteriaQuery<Long> contagem = cb.createQuery(Long.class);
        Root<Candidatura> paraContar = contagem.from(Candidatura.class);
        contagem.select(cb.count(paraContar)).where(predicado(cb, contagem, paraContar, vagaId, ownerId, filtro));
        return new PageImpl<>(ids, pagina, entityManager.createQuery(contagem).getSingleResult());
    }

    private Predicate predicado(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Candidatura> c,
            Long vagaId, Long ownerId, CandidatosVagaFiltro filtro) {
        Join<?, ?> artista = c.join("artista");
        Join<?, Usuario> usuario = artista.join("usuario");
        List<Predicate> p = new ArrayList<>();
        p.add(cb.equal(c.get("vaga").get("id"), vagaId));
        p.add(menorAutorizadoPolicy.publicavel(cb, query, usuario));
        Subquery<Long> posterior = query.subquery(Long.class);
        Root<Candidatura> outra = posterior.from(Candidatura.class);
        posterior.select(outra.get("id")).where(
                cb.equal(outra.get("vaga"), c.get("vaga")),
                cb.equal(outra.get("artista"), artista),
                cb.greaterThan(outra.<Long>get("id"), c.<Long>get("id")));
        p.add(cb.not(cb.exists(posterior)));

        String status = filtro.getStatus() == null ? "TODAS" : filtro.getStatus().trim().toUpperCase(Locale.ROOT);
        if ("ATIVAS".equals(status)) p.add(c.get("status").in(StatusCandidatura.PENDENTE, StatusCandidatura.EM_ANALISE));
        if ("RETIRADAS".equals(status)) p.add(cb.equal(c.get("status"), StatusCandidatura.RETIRADA));
        if (filtro.getBusca() != null && !filtro.getBusca().isBlank()) {
            String busca = filtro.getBusca().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            p.add(cb.like(cb.lower(usuario.get("nome")), "%" + busca + "%", '\\'));
        }
        if (filtro.getDataInicio() != null)
            p.add(cb.greaterThanOrEqualTo(c.<LocalDateTime>get("dataCandidatura"), filtro.getDataInicio().atStartOfDay()));
        if (filtro.getDataFim() != null)
            p.add(cb.lessThan(c.<LocalDateTime>get("dataCandidatura"), filtro.getDataFim().plusDays(1).atStartOfDay()));
        if (Boolean.TRUE.equals(filtro.getSomenteFavoritas())) {
            Subquery<Long> salvo = query.subquery(Long.class);
            Root<ItemSalvo> item = salvo.from(ItemSalvo.class);
            salvo.select(item.get("id")).where(cb.equal(item.get("usuarioId"), ownerId),
                    cb.equal(item.get("tipoAlvo"), TipoAlvoSalvo.PERFIL_ARTISTA),
                    cb.equal(item.get("alvoId"), artista.get("usuarioId")));
            p.add(cb.exists(salvo));
        }
        if (filtro.getAreaId() != null || filtro.getFuncaoId() != null || filtro.getEspecializacaoId() != null) {
            Subquery<Integer> classificacao = query.subquery(Integer.class);
            Root<PerfilArtistaArea> area = classificacao.from(PerfilArtistaArea.class);
            List<Predicate> profissional = new ArrayList<>();
            profissional.add(cb.equal(area.get("perfil"), artista));
            if (filtro.getAreaId() != null) profissional.add(cb.equal(area.get("area").get("id"), filtro.getAreaId()));
            if (filtro.getFuncaoId() != null || filtro.getEspecializacaoId() != null) {
                Join<PerfilArtistaArea, Funcao> funcao = area.join("funcoes");
                profissional.add(cb.equal(funcao.get("area"), area.get("area")));
                if (filtro.getFuncaoId() != null) profissional.add(cb.equal(funcao.get("id"), filtro.getFuncaoId()));
                if (filtro.getEspecializacaoId() != null) {
                    var selecionada = area.join("especializacoes");
                    var compativel = funcao.join("especializacoes");
                    profissional.add(cb.equal(selecionada.get("id"), filtro.getEspecializacaoId()));
                    profissional.add(cb.equal(compativel.get("id"), selecionada.get("id")));
                }
            }
            classificacao.select(cb.literal(1)).where(profissional.toArray(Predicate[]::new));
            p.add(cb.exists(classificacao));
        }
        return cb.and(p.toArray(Predicate[]::new));
    }
}
