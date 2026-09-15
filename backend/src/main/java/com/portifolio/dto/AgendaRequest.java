package com.portifolio.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
public record AgendaRequest(
    @NotBlank @Size(max=150) String titulo,
    @Size(max=5000) String descricao,
    @NotBlank @Size(max=50) String tipo,
    @NotNull LocalDateTime inicio,
    @NotNull LocalDateTime fim,
    @NotBlank @Size(max=255) String localizacao) {}
