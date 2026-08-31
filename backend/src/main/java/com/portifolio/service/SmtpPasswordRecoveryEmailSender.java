package com.portifolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Conditional(SmtpPasswordRecoveryEmailSender.SmtpConfiguradoCondition.class)
public class SmtpPasswordRecoveryEmailSender implements PasswordRecoveryEmailSender {

    private final JavaMailSender mailSender;
    private final String remetente;
    private final String frontendBaseUrl;

    public SmtpPasswordRecoveryEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String remetente,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.frontendBaseUrl = removerBarrasFinais(frontendBaseUrl);
    }

    @Override
    public void enviarLinkRedefinicao(String email, String token) {
        String link = frontendBaseUrl + "/redefinir-senha#token=" + token;
        SimpleMailMessage mensagem = novaMensagem(email, "Recuperação de senha — Palco");
        mensagem.setText("Olá,\n\n"
                + "Recebemos uma solicitação para redefinir a senha da sua conta Palco.\n"
                + "Este link é válido por 1 hora:\n\n"
                + link + "\n\n"
                + "Se você não fez esta solicitação, ignore este e-mail. Sua senha não será alterada.");
        mailSender.send(mensagem);
    }

    @Override
    public void enviarOrientacaoLoginGoogle(String email) {
        SimpleMailMessage mensagem = novaMensagem(email, "Acesso à sua conta — Palco");
        mensagem.setText("Olá,\n\n"
                + "Sua conta Palco utiliza o login do Google e não possui uma senha local para redefinir.\n"
                + "Na tela de login, escolha “Entrar com Google” para acessar sua conta.\n\n"
                + "Se você não fez esta solicitação, ignore este e-mail.");
        mailSender.send(mensagem);
    }

    private SimpleMailMessage novaMensagem(String destinatario, String assunto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        return mensagem;
    }

    private static String removerBarrasFinais(String url) {
        return url.replaceAll("/+$", "");
    }

    static final class SmtpConfiguradoCondition implements Condition {

        @Override
        public boolean matches(
                ConditionContext context, AnnotatedTypeMetadata metadata) {
            return StringUtils.hasText(context.getEnvironment().getProperty("spring.mail.host"))
                    && StringUtils.hasText(context.getEnvironment().getProperty("app.mail.from"));
        }
    }
}
