package com.portifolio.service;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Conditional(SmtpPasswordRecoveryEmailSender.SmtpConfiguradoCondition.class)
public class SmtpGuardianApplicationNoticeSender implements GuardianApplicationNoticeSender {
    private final JavaMailSender mailSender;
    private final String remetente;
    private final String frontendBaseUrl;

    public SmtpGuardianApplicationNoticeSender(JavaMailSender mailSender,
            @Value("${app.mail.from}") String remetente,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.frontendBaseUrl = basePublicaSegura(frontendBaseUrl);
    }

    @Override
    public void enviarAviso(String emailResponsavel, Long vagaId) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(emailResponsavel);
        mensagem.setSubject("Aviso de candidatura — Palco");
        mensagem.setText("Olá,\n\nO artista adolescente sob sua responsabilidade se candidatou "
                + "a uma vaga no Palco. Esta mensagem é informativa; nenhuma nova aprovação é necessária.\n\n"
                + (frontendBaseUrl.isEmpty() ? "" : "Vaga: " + frontendBaseUrl + "/vagas/" + vagaId + "\n"));
        mailSender.send(mensagem);
    }

    private static String basePublicaSegura(String valor) {
        if (valor == null || valor.isBlank()) return "";
        try {
            URI uri = URI.create(valor);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) return "";
            return valor.replaceAll("/+$", "");
        } catch (IllegalArgumentException erro) {
            return "";
        }
    }
}
