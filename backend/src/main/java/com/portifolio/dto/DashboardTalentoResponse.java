package com.portifolio.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardTalentoResponse {
    private Long artistaId;
    private String nomeExibicao;
    private String biografia;
    private String localizacao;
    private String urlPortfolio;
    private String avatarUrl;
    private Set<FuncaoResponse> funcoes;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private long quantidadeFuncoesCoincidentes;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private long quantidadeEspecializacoesCoincidentes;
}
