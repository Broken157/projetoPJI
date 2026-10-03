package com.portifolio.security;

import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoUsuario;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Proteção compartilhada por perfil público e abertura de conversa do RF45. */
@Component
@RequiredArgsConstructor
public class MenorAutorizadoPolicy {
    private static final int IDADE_MINIMA = 14;
    private static final int MAIORIDADE = 18;
    private final Clock clock;

    public boolean exigeProtecao(Usuario usuario) {
        return usuario.getDataNascimento() == null
                || idade(usuario) < MAIORIDADE;
    }

    public boolean autorizado(Usuario usuario) {
        if (usuario.getDataNascimento() == null || usuario.getTipoUsuario() != TipoUsuario.ARTISTA
                || usuario.getStatusConta() != StatusConta.ATIVA) {
            return false;
        }
        int idade = idade(usuario);
        if (idade < IDADE_MINIMA || idade >= MAIORIDADE) return false;
        ResponsavelLegal responsavel = usuario.getResponsavelLegal();
        return responsavel != null && responsavel.getDataConsentimento() != null
                && !Boolean.TRUE.equals(responsavel.getConsentimentoRevogado());
    }

    private int idade(Usuario usuario) {
        return Period.between(usuario.getDataNascimento(), LocalDate.now(clock)).getYears();
    }

    /** Mesmo predicado persistente para detalhe RF10 e descoberta RF37, antes de count/página. */
    public Predicate publicavel(CriteriaBuilder cb, CommonAbstractCriteria query, From<?, Usuario> usuario) {
        LocalDate hoje = LocalDate.now(clock);
        var nascimento = usuario.<LocalDate>get("dataNascimento");
        var consentimento = query.subquery(Integer.class);
        var responsavel = consentimento.from(ResponsavelLegal.class);
        consentimento.select(cb.literal(1)).where(
                cb.equal(responsavel.get("usuario"), usuario),
                cb.isNotNull(responsavel.get("dataConsentimento")),
                cb.isFalse(cb.coalesce(responsavel.<Boolean>get("consentimentoRevogado"), false)));
        return cb.and(
                cb.equal(usuario.get("statusConta"), StatusConta.ATIVA),
                usuario.get("tipoUsuario").in(TipoUsuario.ARTISTA, TipoUsuario.CONTRATANTE),
                cb.or(cb.lessThanOrEqualTo(nascimento, hoje.minusYears(MAIORIDADE)),
                        cb.and(cb.equal(usuario.get("tipoUsuario"), TipoUsuario.ARTISTA),
                                cb.greaterThan(nascimento, hoje.minusYears(MAIORIDADE)),
                                cb.lessThanOrEqualTo(nascimento, hoje.minusYears(IDADE_MINIMA)),
                                cb.exists(consentimento))));
    }

    public <T> Specification<T> perfilPublicavel(TipoUsuario tipo, Long id) {
        return (perfil, query, cb) -> {
            Join<T, Usuario> usuario = perfil.join("usuario");
            return cb.and(cb.equal(perfil.get("usuarioId"), id),
                    cb.equal(usuario.get("tipoUsuario"), tipo), publicavel(cb, query, usuario));
        };
    }
}
