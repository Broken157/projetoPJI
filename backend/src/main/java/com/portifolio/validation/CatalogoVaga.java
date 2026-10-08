package com.portifolio.validation;

import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.enums.NivelExperiencia;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Códigos funcionais sobre VARCHAR existentes; sem novos enums ou dados físicos. */
public final class CatalogoVaga {
    private CatalogoVaga() {}
    private static final Map<String, List<String>> CONTRATOS = Map.of(
            "CLT", List.of("CLT"),
            "PJ", List.of("PJ", "Prestação de Serviço", "PJ / Prestação de Serviço"),
            "FREELANCER", List.of("FREELANCER", "Freelance", "Autônomo", "Freelancer / Autônomo"),
            "TEMPORARIO", List.of("TEMPORARIO", "Temporário"),
            "ESTAGIO", List.of("ESTAGIO", "Estágio"),
            "PROJETO_EVENTO", List.of("PROJETO_EVENTO", "Projeto", "Evento", "Contrato por Projeto / Evento"));
    public static String contrato(String valor) {
        if (valor != null)
            for (var entrada : CONTRATOS.entrySet())
                if (entrada.getValue().stream().anyMatch(v -> v.equalsIgnoreCase(valor.strip())))
                    return entrada.getKey();
        throw new UnprocessableEntityException("Tipo de contrato fora do catálogo funcional.");
    }
    public static List<String> contratosParaLeitura(String codigo) {
        return CONTRATOS.get(contrato(codigo)).stream().map(v -> v.toLowerCase(Locale.ROOT)).toList();
    }
    public static String experiencia(String valor, boolean obrigatoria) {
        if (valor == null || valor.isBlank()) {
            if (!obrigatoria) return null;
            throw new UnprocessableEntityException("Experiência é obrigatória para publicar.");
        }
        try { return NivelExperiencia.valueOf(valor.strip().toUpperCase(Locale.ROOT)).name(); }
        catch (IllegalArgumentException erro) {
            throw new UnprocessableEntityException("Experiência deve usar código do catálogo vigente.");
        }
    }
}
