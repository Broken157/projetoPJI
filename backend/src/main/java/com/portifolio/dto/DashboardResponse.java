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
    private Boolean perfilIncompleto;
    private DashboardDisponibilidadeResponse notificacoes;
    private DashboardDisponibilidadeResponse mensagens;
    private DashboardSecaoResponse<DashboardVagaResponse> vagasRecomendadas;
    private DashboardSecaoResponse<DashboardCandidaturaResponse> candidaturasRecentes;
    private DashboardSecaoResponse<DashboardTalentoResponse> talentosSugeridos;
    private DashboardSecaoResponse<DashboardCandidaturaResponse> minhasCandidaturas;
    private DashboardSecaoResponse<DashboardVagaPropriaResponse> minhasVagas;
    private java.util.Map<com.portifolio.model.enums.StatusVaga, Long> vagasPorStatus;
    private Long quantidadeBancoTalentos;
}
