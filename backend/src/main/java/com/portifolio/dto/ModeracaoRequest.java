package com.portifolio.dto;

import jakarta.validation.constraints.*;

public record ModeracaoRequest(@NotBlank String status, @NotBlank @Size(max=1000) String justificativa) {}
