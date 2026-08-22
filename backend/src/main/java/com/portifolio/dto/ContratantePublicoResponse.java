package com.portifolio.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContratantePublicoResponse implements PerfilPublicoResponse {
    private Long usuarioId;
    private String nomeExibicao;
    private String nomeEmpresa;
    private String tipoPerfil;
    private String biografia;
    private String localizacao;
    private String bannerUrl;
    private String avatarUrl;
}
