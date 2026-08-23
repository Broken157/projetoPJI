package com.portifolio.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardDisponibilidadeResponse {
    private boolean disponivel;
    private String mensagem;
    private Long quantidadeNaoLidas;
}
