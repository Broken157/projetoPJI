package com.portifolio.dto;

import com.portifolio.model.enums.StatusVaga;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CandidaturaVagaResponse {
    private Long candidaturaId;
    private Long artistaId;
    private String username;
    private String nomeArtista;
    private String biografia;
    private String localizacao;
    private String urlPortfolio;
    private String avatarUrl;
    private Set<Long> funcaoIds;
    private Set<Long> funcoesCoincidentes;
    private int quantidadeFuncoesCoincidentes;
    private String mensagemApresentacao;
    private String linkPortfolioCandidatura;
    private String status;
    private boolean registroLegado;
    private StatusVaga statusVaga;
    private boolean favorito;
    private String cidade;
    private String estado;
    private Set<Short> areaIds;
    private Set<Long> especializacaoIds;
    private String perfilUrl;
    private String conversaUrl;
    private LocalDateTime dataCandidatura;
}
