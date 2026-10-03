package com.portifolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Conditional(SmtpPasswordRecoveryEmailSender.SmtpConfiguradoCondition.class)
public class SmtpGuardianConsentEmailSender implements GuardianConsentEmailSender {
    private final JavaMailSender mailSender;
    private final String remetente;
    private final String frontendBaseUrl;

    public SmtpGuardianConsentEmailSender(JavaMailSender mailSender,
            @Value("${app.mail.from}") String remetente,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void enviarConvite(String emailResponsavel, String nomeArtista, String token) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(emailResponsavel);
        mensagem.setSubject("Autorização de responsável — Palco");
        mensagem.setText("Olá,\n\n" + nomeArtista + " informou você como responsável legal para participar do Palco. "
                + "Confira a solicitação e escolha Autorizar ou Recusar neste link, válido por 24 horas:\n\n"
                + frontendBaseUrl + "/consentimento-responsavel#token=" + token + "\n\n"
                + "Se você não reconhece a solicitação, não a autorize.");
        mailSender.send(mensagem);
    }
}
