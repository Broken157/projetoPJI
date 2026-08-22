package com.portifolio.dto;

import com.portifolio.model.enums.TipoNotificacao;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoResponse {
    private Long id;
    private TipoNotificacao tipo;
    private String mensagem;
    private String link;
    private Boolean lida;
    private LocalDateTime data;
}
