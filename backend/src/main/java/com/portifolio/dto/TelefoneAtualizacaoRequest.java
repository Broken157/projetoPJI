package com.portifolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TelefoneAtualizacaoRequest(
        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        @Pattern(regexp = CadastroDadosRequest.TELEFONE, message = "Telefone inválido; informe DDD e número")
        String telefone) {
    @Override public String toString() { return "TelefoneAtualizacaoRequest[telefone=omitido]"; }
}
