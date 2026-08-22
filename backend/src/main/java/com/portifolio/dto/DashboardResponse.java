package com.portifolio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.portifolio.model.enums.TipoUsuario;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {
    private TipoUsuario tipoUsuario;
    private String nomeExibicao;
    private String avatarUrl;
    private Boolean perfilCompleto;
    private DashboardDisponibilidadeResponse notificacoes;
    private DashboardDisponibilidadeResponse mensagens;
    private DashboardSecaoResponse<DashboardVagaResponse> vagasRecomendadas;
    private DashboardSecaoResponse<DashboardCandidaturaResponse> candidaturasRecentes;
    private DashboardSecaoResponse<DashboardTalentoResponse> talentosSugeridos;
}
