package com.portifolio.service;

public interface EmailVerificationEmailSender {
    void enviarConfirmacao(String email, String token);
}
