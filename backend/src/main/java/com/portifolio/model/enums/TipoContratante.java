package com.portifolio.model.enums;

import com.portifolio.exception.UnprocessableEntityException;
import java.text.Normalizer;
import java.util.Locale;

public enum TipoContratante implements DatabaseEnum {
    PESSOA_FISICA, SETOR_PUBLICO, SETOR_PRIVADO, ONG;

    @Override public String getDatabaseValue() { return name(); }

    public static TipoContratante deContrato(String texto) {
        if (texto == null || texto.isBlank())
            throw new UnprocessableEntityException("Informe tipoPerfilContratante/tipoPerfil do catálogo oficial.");
        String value = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        return switch (value) {
            case "PESSOA_FISICA" -> PESSOA_FISICA;
            case "SETOR_PUBLICO", "INSTITUICAO_PUBLICA" -> SETOR_PUBLICO;
            case "SETOR_PRIVADO", "EMPRESA", "PRODUTORA", "PRODUTORA_CULTURAL", "AGENCIA", "INSTITUICAO_PRIVADA" -> SETOR_PRIVADO;
            case "ONG" -> ONG;
            default -> throw new UnprocessableEntityException("Tipo de contratante incompatível. Use PESSOA_FISICA, SETOR_PUBLICO, SETOR_PRIVADO ou ONG.");
        };
    }
}
