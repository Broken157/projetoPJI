package com.portifolio.dto;

import com.portifolio.model.enums.StatusCandidatura;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardCandidaturaResponse {
    private Long id;
    private Long vagaId;
    private String tituloVaga;
    private Long artistaId;
    private String nomeArtista;
    private String avatarUrl;
    private StatusCandidatura status;
    private LocalDateTime dataCandidatura;
}
