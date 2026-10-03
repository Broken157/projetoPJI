package com.portifolio.dto;

import com.portifolio.model.enums.TipoUsuario;
import java.util.Locale;

/** Somente filtros públicos; não contém disponibilidade, Banco ou ranking RF17. */
public record FiltroDescobertaPublica(String q, TipoUsuario tipo, String cidade, String estado,
        Short areaId, int page, int size) {
    public FiltroDescobertaPublica {
        if (page < 0 || size < 1 || size > 50 || (long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page deve ser não negativo e size entre 1 e 50, dentro do limite de paginação.");
        }
        if (tipo != null && tipo != TipoUsuario.ARTISTA && tipo != TipoUsuario.CONTRATANTE) {
            throw new IllegalArgumentException("tipo deve ser ARTISTA ou CONTRATANTE.");
        }
        q = texto(q, 150, "q");
        cidade = texto(cidade, 100, "cidade");
        estado = texto(estado, 2, "estado");
        if (estado != null) {
            if (!estado.matches("[A-Za-z]{2}")) {
                throw new IllegalArgumentException("estado deve conter uma UF com 2 letras.");
            }
            estado = estado.toUpperCase(Locale.ROOT);
        }
        if (areaId != null && areaId < 1) {
            throw new IllegalArgumentException("areaId deve ser positivo.");
        }
        if (areaId != null && tipo == TipoUsuario.CONTRATANTE) {
            throw new IllegalArgumentException("Área artística se aplica somente a ARTISTA.");
        }
        if ("@".equals(q)) {
            throw new IllegalArgumentException("Informe o username após @.");
        }
    }

    private static String texto(String valor, int limite, String parametro) {
        if (valor == null || valor.isBlank()) return null;
        String texto = valor.strip();
        if (texto.length() > limite || texto.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Parâmetro '" + parametro + "' possui valor inválido.");
        }
        return texto;
    }
}
