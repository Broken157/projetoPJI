package com.portifolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatSalaCriacaoRequest(
        @NotNull @Positive Long usuarioDestinoId) {
}
