package com.portifolio.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMensagemResponse {
    private Long id;
    private Long salaId;
    private Long remetenteId;
    private String remetenteNome;
    private String remetenteAvatar;
    private String texto;
    private String urlAnexo;
    private Boolean lida;
    private Boolean excluida;
    private LocalDateTime dataEnvio;
}
