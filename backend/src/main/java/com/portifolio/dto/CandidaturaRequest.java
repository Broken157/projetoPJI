package com.portifolio.dto;

import com.portifolio.model.enums.StatusCandidatura;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidaturaRequest {

    @NotNull(message = "Vaga é obrigatória")
    private Long vagaId;

    @NotNull(message = "Artista é obrigatório")
    private Long artistaId;

    @Size(max = 2000, message = "Mensagem de apresentação deve ter no máximo 2000 caracteres")
    private String mensagemApresentacao;

    @Size(max = 255, message = "Link do portfólio deve ter no máximo 255 caracteres")
    private String linkPortfolioCandidatura;

    private StatusCandidatura status;
}
