package com.portifolio.validation;

import com.portifolio.exception.UnprocessableEntityException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Conversão do contrato textual antigo sem inventar uma UF. */
public record LocalizacaoArtista(String cidade, String estado) {
    private static final Pattern TEXTO = Pattern.compile("^([^,/]+?)(?:,|\\s+-\\s+|/)\\s*([A-Za-z]{2})$");

    public LocalizacaoArtista {
        cidade = cidade == null || cidade.isBlank() ? null : cidade.trim();
        estado = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(Locale.ROOT);
        if ((cidade == null) != (estado == null))
            throw new UnprocessableEntityException("Informe cidade e estado juntos.");
        if (cidade != null && (cidade.length() > 100 || !estado.matches("[A-Z]{2}")))
            throw new UnprocessableEntityException("Cidade deve ter até 100 caracteres e estado deve conter duas letras.");
    }

    public static LocalizacaoArtista deTexto(String texto) {
        if (texto == null || texto.isBlank()) return new LocalizacaoArtista(null, null);
        var match = TEXTO.matcher(texto.trim());
        if (!match.matches())
            throw new UnprocessableEntityException("Localização ambígua. Informe cidade e estado ou use 'Cidade, UF'.");
        return new LocalizacaoArtista(match.group(1), match.group(2));
    }

    public static LocalizacaoArtista deRequest(String cidade, String estado, String texto) {
        if (cidade == null && estado == null) return deTexto(texto);
        var estruturada = new LocalizacaoArtista(cidade, estado);
        if (texto != null && !texto.isBlank() && !estruturada.equals(deTexto(texto)))
            throw new UnprocessableEntityException("Localização textual difere de cidade e estado.");
        return estruturada;
    }

    /** A busca continua literal para fragmentos; pares legados usam o formato da resposta atual. */
    public static String normalizarFiltro(String texto) {
        if (texto == null) return null;
        var match = TEXTO.matcher(texto.trim());
        return match.matches() ? formatar(match.group(1), match.group(2).toUpperCase(Locale.ROOT)) : texto.trim();
    }

    public static String formatar(String cidade, String estado) {
        String c = cidade == null ? "" : cidade.trim();
        String e = estado == null ? "" : estado.trim();
        return c.isEmpty() ? (e.isEmpty() ? null : e) : (e.isEmpty() ? c : c + ", " + e);
    }
}
