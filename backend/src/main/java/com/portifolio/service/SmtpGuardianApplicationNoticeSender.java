package com.portifolio.service;

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
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void enviarAviso(String emailResponsavel, Long vagaId) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(emailResponsavel);
        mensagem.setSubject("Aviso de candidatura — Palco");
        mensagem.setText("Olá,\n\nO artista adolescente sob sua responsabilidade se candidatou "
                + "a uma vaga no Palco. Esta mensagem é informativa; nenhuma nova aprovação é necessária.\n\n"
                + "Vaga: " + frontendBaseUrl + "/vagas/" + vagaId + "\n");
        mailSender.send(mensagem);
    }
}
