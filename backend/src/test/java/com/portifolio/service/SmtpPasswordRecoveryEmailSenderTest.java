package com.portifolio.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpPasswordRecoveryEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpPasswordRecoveryEmailSender sender =
            new SmtpPasswordRecoveryEmailSender(
                    mailSender, "nao-responda@palco.test", "https://palco.test/");

    @Test
    void contaLocalDeveReceberLinkNoFragmentoSemQueryParameter() {
        String token = "TOKEN_BRUTO_SEGURO";

        sender.enviarLinkRedefinicao("artista@palco.test", token);

        SimpleMailMessage mensagem = mensagemEnviada();
        assertThat(mensagem.getFrom()).isEqualTo("nao-responda@palco.test");
        assertThat(mensagem.getTo()).containsExactly("artista@palco.test");
        assertThat(mensagem.getSubject()).containsIgnoringCase("recuperação de senha");
        assertThat(mensagem.getText())
                .contains("https://palco.test/redefinir-senha.html#token=" + token)
                .contains("1 hora")
                .containsIgnoringCase("ignore este e-mail")
                .doesNotContain("?token=");
    }

    @Test
    void contaGoogleOnlyDeveReceberOrientacaoSemLinkDeRedefinicao() {
        sender.enviarOrientacaoLoginGoogle("google@palco.test");

        SimpleMailMessage mensagem = mensagemEnviada();
        assertThat(mensagem.getTo()).containsExactly("google@palco.test");
        assertThat(mensagem.getSubject()).containsIgnoringCase("acesso à sua conta");
        assertThat(mensagem.getText())
                .contains("Entrar com Google")
                .doesNotContain("#token=", "?token=", "redefinir-senha.html");
    }

    private SimpleMailMessage mensagemEnviada() {
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
