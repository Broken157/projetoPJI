package com.portifolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpGuardianApplicationNoticeSenderTest {
    private final JavaMailSender mail = mock(JavaMailSender.class);

    @Test void mensagemMinimaUsaSomenteDestinatarioEContextoPublicoSemTokens() {
        new SmtpGuardianApplicationNoticeSender(mail, "avisos@palco.test", "https://palco.test/")
                .enviarAviso("responsavel@teste.com", 123L);
        var captura = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(1)).send(captura.capture());
        var mensagem = captura.getValue();
        assertThat(mensagem.getTo()).containsExactly("responsavel@teste.com");
        assertThat(mensagem.getFrom()).isEqualTo("avisos@palco.test");
        assertThat(mensagem.getSubject()).isEqualTo("Aviso de candidatura — Palco");
        assertThat(mensagem.getText()).contains("se candidatou", "informativa", "nenhuma nova aprovação", "https://palco.test/vagas/123")
                .doesNotContain("responsavel@teste.com", "CPF", "telefone", "nascimento", "consentimento", "token", "experiência", "portfólio", "remuneração", "?", "#");
        assertThat(mensagem.getCc()).isNull();
        assertThat(mensagem.getBcc()).isNull();
    }

    @ParameterizedTest @ValueSource(strings = {"http://localhost:5173", "javascript:alert(1)", "https://user:senha@palco.test",
            "https://palco.test?token=secreto", "https://palco.test#token=secreto", "https://palco.test/login", "//palco.test", "url inválida", ""})
    void configuracaoDeLinkInseguraEhOmitidaSemDesativarAviso(String base) {
        new SmtpGuardianApplicationNoticeSender(mail, "avisos@palco.test", base).enviarAviso("responsavel@teste.com", 1L);
        var captura = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captura.capture());
        assertThat(captura.getValue().getText()).contains("se candidatou", "nenhuma nova aprovação")
                .doesNotContain("Vaga:", "/vagas/", "token", "senha", "javascript", "url inválida");
    }

    @Test void excecaoSmtpContinuaTratavelPelaCamadaRf44SemRetryInterno() {
        doThrow(new MailSendException("Falha fictícia")).when(mail).send(any(SimpleMailMessage.class));
        var sender = new SmtpGuardianApplicationNoticeSender(mail, "avisos@palco.test", "https://palco.test");
        assertThatThrownBy(() -> sender.enviarAviso("responsavel@teste.com", 1L)).isInstanceOf(MailSendException.class);
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
    }
}
