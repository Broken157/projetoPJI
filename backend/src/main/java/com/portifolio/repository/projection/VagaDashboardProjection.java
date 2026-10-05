package com.portifolio.repository.projection;

import com.portifolio.model.enums.StatusVaga;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface VagaDashboardProjection {
    Long getId();
    String getTitulo();
    StatusVaga getStatus();
    LocalDateTime getDataPublicacao();
    LocalDate getDataLimiteCandidatura();
}
