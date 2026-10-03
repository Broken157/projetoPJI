package com.portifolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// A identidade do denunciante nunca faz parte do contrato de entrada.
public record DenunciaRequest(
        @NotNull TipoAlvo tipoAlvo,
        @NotNull @Positive Long alvoId,
        @NotBlank @Size(max = 150) String motivo,
        @Size(max = 2000) String descricao) {
    public enum TipoAlvo { VAGA, PERFIL_ARTISTA, PERFIL_CONTRATANTE }
}
