package com.portifolio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

/** O alvo vem da rota e o artista do contexto autenticado, nunca do payload. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BancoTalentosParticipacaoRequest(@NotNull Boolean confirmado) {}
