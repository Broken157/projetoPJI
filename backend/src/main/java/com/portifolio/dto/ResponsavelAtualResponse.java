package com.portifolio.dto;

import java.time.LocalDateTime;

/** Consulta privada titular; sem Entity, token ou atributo estrutural inventado. */
public record ResponsavelAtualResponse(String nome, String email, String telefone,
        LocalDateTime dataConsentimento, boolean consentimentoRevogado) {
    @Override public String toString() { return "ResponsavelAtualResponse[dados privados omitidos]"; }
}
