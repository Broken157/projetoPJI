package com.portifolio.repository.projection;

import com.portifolio.model.enums.StatusCandidatura;
import java.time.LocalDateTime;

public interface CandidaturaDashboardProjection {
    Long getId();
    Long getVagaId();
    String getTituloVaga();
    Long getArtistaId();
    String getNomeArtista();
    String getFotoPerfilArtista();
    String getFotoPerfilUsuario();
    StatusCandidatura getStatus();
    LocalDateTime getDataCandidatura();
}
