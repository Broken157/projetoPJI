package com.portifolio.dto;

import com.portifolio.model.enums.StatusVaga;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DashboardVagaPropriaResponse(Long id, String titulo, StatusVaga status,
        LocalDateTime dataPublicacao, LocalDate dataLimiteCandidatura, boolean prazoVencido) {}
