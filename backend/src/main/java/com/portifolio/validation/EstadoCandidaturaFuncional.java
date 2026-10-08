package com.portifolio.validation;

import com.portifolio.model.enums.StatusCandidatura;

/** Projeção D10; não altera o enum físico nem atribui resultados de seleção. */
public final class EstadoCandidaturaFuncional {
    private EstadoCandidaturaFuncional() {}

    public static String de(StatusCandidatura estado) {
        if (estado == StatusCandidatura.PENDENTE || estado == StatusCandidatura.EM_ANALISE) return "ATIVA";
        if (estado == StatusCandidatura.RETIRADA) return "RETIRADA";
        return null;
    }

    public static boolean legado(StatusCandidatura estado) {
        return estado != StatusCandidatura.PENDENTE && estado != StatusCandidatura.RETIRADA;
    }

    public static String codigoLegado(StatusCandidatura estado) {
        return legado(estado) && estado != null ? estado.name() : null;
    }
}
