package com.portifolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMensagemRequest(
        @NotBlank(message = "Texto da mensagem e obrigatorio.")
        @Size(max = 4000, message = "Texto da mensagem deve ter no maximo 4000 caracteres.")
        String texto) {
}
