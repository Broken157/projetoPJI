package com.portifolio.service;

/** Ponto de integração do RF09 com a infraestrutura de e-mail. */
public interface PasswordRecoveryEmailSender {

    void enviarLinkRedefinicao(String email, String token);

    void enviarOrientacaoLoginGoogle(String email);
}
