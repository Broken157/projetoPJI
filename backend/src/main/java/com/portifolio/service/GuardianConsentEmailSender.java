package com.portifolio.service;

public interface GuardianConsentEmailSender {
    void enviarConvite(String emailResponsavel, String nomeArtista, String token);
}
