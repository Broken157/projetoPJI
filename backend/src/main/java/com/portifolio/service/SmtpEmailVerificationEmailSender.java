package com.portifolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Conditional(SmtpPasswordRecoveryEmailSender.SmtpConfiguradoCondition.class)
public class SmtpEmailVerificationEmailSender implements EmailVerificationEmailSender {
    private final JavaMailSender mailSender;
    private final String remetente;
    private final String frontendBaseUrl;

    public SmtpEmailVerificationEmailSender(JavaMailSender mailSender,
            @Value("${app.mail.from}") String remetente,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void enviarConfirmacao(String email, String token) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Confirme seu e-mail — Palco");
        mensagem.setText("Olá,\n\nConfirme o e-mail da sua conta Palco neste link, válido por 1 hora:\n\n"
                + frontendBaseUrl + "/confirmar-email#token=" + token + "\n\n"
                + "Se você não criou esta conta, ignore este e-mail.");
        mailSender.send(mensagem);
    }
}
