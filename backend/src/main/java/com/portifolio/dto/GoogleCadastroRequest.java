package com.portifolio.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleCadastroRequest extends CadastroDadosRequest {
    @NotBlank(message = "Contexto Google e obrigatorio")
    @Size(max = 4096, message = "Contexto Google invalido")
    private String contexto;

    @JsonAnySetter
    public void rejeitarCampoDesconhecido(String campo, Object valor) {
        throw new IllegalArgumentException("Campo nao permitido na conclusao do cadastro Google.");
    }
}
