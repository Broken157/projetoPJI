package com.portifolio.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VagaCancelamentoRequest {

    @NotNull(message = "Confirmação é obrigatória")
    @AssertTrue(message = "Confirmação deve ser verdadeira")
    private Boolean confirmacao;

    @NotBlank(message = "Motivo é obrigatório")
    @Size(max = 2000, message = "Motivo deve ter no máximo 2000 caracteres")
    private String motivo;
}
