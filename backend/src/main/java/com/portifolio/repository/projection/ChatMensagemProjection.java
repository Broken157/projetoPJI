package com.portifolio.repository.projection;

import java.time.LocalDateTime;

public interface ChatMensagemProjection {
    Long getId();
    Long getSalaId();
    Long getRemetenteId();
    String getRemetenteNome();
    String getRemetenteAvatar();
    String getTexto();
    String getUrlAnexo();
    Boolean getLida();
    Boolean getExcluida();
    Boolean getEditada();
    LocalDateTime getDataEdicao();
    LocalDateTime getDataEnvio();
}
