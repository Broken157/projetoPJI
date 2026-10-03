package com.portifolio.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VagaStatusAcaoRequest {

    @NotBlank(message = "Ação é obrigatória")
    private String acao;
    private LocalDate dataLimiteCandidatura;
}
