package com.portifolio.service;

public interface GuardianApplicationNoticeSender {
    void enviarAviso(String emailResponsavel, Long vagaId);
}
