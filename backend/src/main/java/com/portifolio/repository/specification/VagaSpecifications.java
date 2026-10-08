package com.portifolio.repository.specification;

import com.portifolio.model.Vaga;
import com.portifolio.model.enums.ModeloTrabalho;
import com.portifolio.model.enums.Abrangencia;
import com.portifolio.model.enums.FormaRemuneracao;
import com.portifolio.model.enums.StatusVaga;
import jakarta.persistence.criteria.Join;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class VagaSpecifications {

    private VagaSpecifications() {
    }

    public static Specification<Vaga> semBloqueioModeracao() {
        return (root, query, cb) -> {
            var ultimoId = query.subquery(Long.class);
            var historico = ultimoId.from(com.portifolio.model.ModeracaoConteudo.class);
            ultimoId.select(cb.max(historico.<Long>get("id"))).where(
                    cb.equal(historico.get("tipoConteudo"), "VAGA"),
                    cb.equal(historico.get("conteudoId"), root.get("id")));
            var bloqueio = query.subquery(Long.class);
            var acao = bloqueio.from(com.portifolio.model.ModeracaoConteudo.class);
            bloqueio.select(acao.get("id")).where(cb.equal(acao.get("id"), ultimoId),
                    cb.or(cb.isNull(acao.get("statusModeracao")),
                            acao.get("statusModeracao").in("BLOQUEADO", "SOB_ANALISE")));
            return cb.not(cb.exists(bloqueio));
        };
    }

    public static Specification<Vaga> comStatus(StatusVaga status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Vaga> prazoAindaValido(LocalDate hoje) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("dataLimiteCandidatura")),
                cb.greaterThan(root.get("dataLimiteCandidatura"), hoje));
    }

    public static Specification<Vaga> buscaTituloOuContratante(String busca) {
        return tituloContem(busca);
    }

    public static Specification<Vaga> idMaiorQue(Long cursor) {
        return (root, query, cb) -> cursor == null
                ? cb.conjunction()
                : cb.greaterThan(root.get("id"), cursor);
    }

    public static Specification<Vaga> tituloContem(String titulo) {
        return (root, query, cb) -> (titulo == null || titulo.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("titulo")), termoLiteral(titulo), '\\');
    }

    public static Specification<Vaga> empresaContem(String empresa) {
        return (root, query, cb) -> {
            if (empresa == null || empresa.isBlank()) {
                return cb.conjunction();
            }
            String termo = "%" + empresa.trim().toLowerCase() + "%";
            var contratante = root.join("contratante");
            var usuario = contratante.join("usuario");
            return cb.or(
                    cb.like(cb.lower(contratante.get("nomeEmpresa")), termo),
                    cb.like(cb.lower(usuario.get("nome")), termo));
        };
    }

    public static Specification<Vaga> cidadeIgual(String cidade) {
        return (root, query, cb) -> (cidade == null || cidade.isBlank())
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("cidade")), cidade.trim().toLowerCase());
    }

    public static Specification<Vaga> estadoIgual(String estado) {
        return (root, query, cb) -> (estado == null || estado.isBlank())
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("estado")), estado.trim().toLowerCase());
    }

    public static Specification<Vaga> modeloTrabalhoIgual(ModeloTrabalho modelo) {
        return (root, query, cb) -> modelo == null
                ? cb.conjunction()
                : cb.equal(root.get("modeloTrabalho"), modelo);
    }

    public static Specification<Vaga> tipoContratoIgual(String tipoContrato) {
        return (root, query, cb) -> (tipoContrato == null || tipoContrato.isBlank())
                ? cb.conjunction()
                : cb.lower(root.<String>get("tipoContrato")).in(
                        com.portifolio.validation.CatalogoVaga.contratosParaLeitura(tipoContrato));
    }

    public static Specification<Vaga> remuneracaoMinima(BigDecimal min) {
        return (root, query, cb) -> min == null
                ? cb.conjunction()
                : cb.and(cb.notEqual(root.get("formaRemuneracao"), FormaRemuneracao.A_COMBINAR),
                        cb.greaterThanOrEqualTo(root.get("valorMaximo"), min));
    }

    public static Specification<Vaga> remuneracaoMaxima(BigDecimal max) {
        return (root, query, cb) -> max == null
                ? cb.conjunction()
                : cb.and(cb.notEqual(root.get("formaRemuneracao"), FormaRemuneracao.A_COMBINAR),
                        cb.lessThanOrEqualTo(root.get("valorMinimo"), max));
    }

    public static Specification<Vaga> areaAtuacaoContem(String areaAtuacao) {
        return (root, query, cb) -> (areaAtuacao == null || areaAtuacao.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.join("area").get("nome")),
                        "%" + areaAtuacao.trim().toLowerCase() + "%");
    }

    public static Specification<Vaga> areaIgual(Short areaId) {
        return (root, query, cb) -> areaId == null
                ? cb.conjunction() : cb.equal(root.get("area").get("id"), areaId);
    }

    public static Specification<Vaga> experienciaIgual(String experiencia) {
        return (root, query, cb) -> experiencia == null || experiencia.isBlank()
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("experiencia")), experiencia.trim().toLowerCase());
    }

    public static Specification<Vaga> abrangenciaIgual(Abrangencia abrangencia) {
        return (root, query, cb) -> abrangencia == null
                ? cb.conjunction() : cb.equal(root.get("abrangencia"), abrangencia);
    }

    public static Specification<Vaga> formaRemuneracaoIgual(FormaRemuneracao forma) {
        return (root, query, cb) -> forma == null
                ? cb.conjunction() : cb.equal(root.get("formaRemuneracao"), forma);
    }

    public static Specification<Vaga> afirmativa(Boolean afirmativa) {
        return (root, query, cb) -> afirmativa == null ? cb.conjunction()
                : afirmativa ? cb.isNotNull(root.get("categoriaAfirmativa"))
                : cb.isNull(root.get("categoriaAfirmativa"));
    }

    public static Specification<Vaga> comAlgumaCategoriaAfirmativa(Set<Integer> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) return cb.conjunction();
            var categorias = java.util.Arrays.stream(com.portifolio.model.CategoriaAfirmativa.values())
                    .filter(c -> ids.contains(c.getId())).toList();
            return root.get("categoriaAfirmativa").in(categorias);
        };
    }

    public static Specification<Vaga> comAlgumaEspecializacao(Set<Long> ids) {
        return classificacao(null, ids);
    }

    /** EXISTS evita multiplicação da página e exige a cadeia função/especialização da própria vaga. */
    public static Specification<Vaga> classificacao(Set<Long> funcoes, Set<Long> especializacoes) {
        return (root, query, cb) -> {
            boolean temFuncoes = funcoes != null && !funcoes.isEmpty();
            boolean temEspecializacoes = especializacoes != null && !especializacoes.isEmpty();
            if (!temFuncoes && !temEspecializacoes) return cb.conjunction();
            var sub = query.subquery(Long.class);
            var vaga = sub.from(Vaga.class);
            var funcao = vaga.join("funcoes");
            var condicoes = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            condicoes.add(cb.equal(vaga.get("id"), root.get("id")));
            condicoes.add(cb.equal(funcao.get("area").get("id"), vaga.get("area").get("id")));
            if (temFuncoes) condicoes.add(funcao.get("id").in(funcoes));
            if (temEspecializacoes) {
                var selecionada = vaga.join("especializacoes");
                var permitida = funcao.join("especializacoes");
                condicoes.add(selecionada.get("id").in(especializacoes));
                condicoes.add(cb.equal(selecionada.get("id"), permitida.get("id")));
            }
            sub.select(vaga.get("id")).where(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
            return cb.exists(sub);
        };
    }

    public static Specification<Vaga> idDiferente(Long id) {
        return (root, query, cb) -> id == null
                ? cb.conjunction()
                : cb.notEqual(root.get("id"), id);
    }

    // distinct(true) evita vaga duplicada no resultado quando ela casa com mais de uma funcao do filtro
    public static Specification<Vaga> comAlgumaFuncao(Set<Long> funcaoIds) {
        return classificacao(funcaoIds, null);
    }

    public static Specification<Vaga> naoRascunho() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), StatusVaga.RASCUNHO);
    }

    public static Specification<Vaga> comCandidaturaDe(Long usuarioId) {
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var candidatura = sub.from(com.portifolio.model.Candidatura.class);
            sub.select(candidatura.get("id")).where(
                    cb.equal(candidatura.get("vaga").get("id"), root.get("id")),
                    cb.equal(candidatura.get("artista").get("usuarioId"), usuarioId));
            return cb.exists(sub);
        };
    }

    public static Specification<Vaga> favoritaDe(Long usuarioId) {
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var salvo = sub.from(com.portifolio.model.ItemSalvo.class);
            sub.select(salvo.get("id")).where(cb.equal(salvo.get("usuarioId"), usuarioId),
                    cb.equal(salvo.get("tipoAlvo"), com.portifolio.model.enums.TipoAlvoSalvo.VAGA),
                    cb.equal(salvo.get("alvoId"), root.get("id")));
            return cb.exists(sub);
        };
    }

    public static Specification<Vaga> publicacaoEntre(LocalDate inicio, LocalDate fim) {
        return (root, query, cb) -> cb.and(
                inicio == null ? cb.conjunction() : cb.greaterThanOrEqualTo(
                        root.get("dataPublicacao"), inicio.atStartOfDay()),
                fim == null ? cb.conjunction() : cb.lessThan(
                        root.get("dataPublicacao"), fim.plusDays(1).atStartOfDay()));
    }

    public static Specification<Vaga> prazoEntre(LocalDate inicio, LocalDate fim, Boolean comPrazo) {
        return (root, query, cb) -> cb.and(
                inicio == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("dataLimiteCandidatura"), inicio),
                fim == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("dataLimiteCandidatura"), fim),
                comPrazo == null ? cb.conjunction() : comPrazo
                        ? cb.isNotNull(root.get("dataLimiteCandidatura")) : cb.isNull(root.get("dataLimiteCandidatura")));
    }

    private static String termoLiteral(String valor) {
        return "%" + valor.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    public static Specification<Vaga> doContratante(Long usuarioId) {
        return (root, query, cb) -> usuarioId == null
                ? cb.conjunction()
                : cb.equal(root.get("contratante").get("usuarioId"), usuarioId);
    }
}
