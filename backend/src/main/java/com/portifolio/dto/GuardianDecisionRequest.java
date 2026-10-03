package com.portifolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GuardianDecisionRequest(@NotBlank String token, @NotNull Decision decisao) {
    public enum Decision { AUTORIZAR, RECUSAR }
}
