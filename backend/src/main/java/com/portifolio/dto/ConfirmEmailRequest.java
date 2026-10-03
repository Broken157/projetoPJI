package com.portifolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmEmailRequest {
    @NotBlank(message = "Token de confirmação é obrigatório")
    @Size(max = 128, message = "Token de confirmação inválido")
    private String token;
}
