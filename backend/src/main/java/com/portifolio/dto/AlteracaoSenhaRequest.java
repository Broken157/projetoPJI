package com.portifolio.dto;

import lombok.Getter;
import lombok.Setter;

/** Credenciais nunca participam de toString; RNF19 é validado pela policy central. */
@Getter
@Setter
public class AlteracaoSenhaRequest {
    private String senhaAtual;
    private String novaSenha;
}
