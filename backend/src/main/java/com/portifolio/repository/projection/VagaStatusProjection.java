package com.portifolio.repository.projection;

import com.portifolio.model.enums.StatusVaga;

public interface VagaStatusProjection {
    StatusVaga getStatus();
    long getQuantidade();
}
