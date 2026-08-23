package com.portifolio.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SmtpPasswordRecoveryEmailSenderConfigurationTest {

    @Test
    void beanNaoDeveExistirSemHostSmtpERemetente() {
        try (AnnotationConfigApplicationContext contexto = novoContexto()) {
            contexto.register(SmtpPasswordRecoveryEmailSender.class);
            contexto.refresh();

            assertThat(contexto.getBeansOfType(PasswordRecoveryEmailSender.class)).isEmpty();
        }
    }

    @Test
    void beanDeveExistirQuandoHostSmtpERemetenteEstaoConfigurados() {
        try (AnnotationConfigApplicationContext contexto = novoContexto()) {
            TestPropertyValues.of(
                    "spring.mail.host=smtp.palco.test",
                    "app.mail.from=nao-responda@palco.test",
                    "app.frontend.base-url=https://palco.test")
                    .applyTo(contexto);
            contexto.register(SmtpPasswordRecoveryEmailSender.class);
            contexto.refresh();

            assertThat(contexto.getBeansOfType(PasswordRecoveryEmailSender.class))
                    .hasSize(1);
        }
    }

    private AnnotationConfigApplicationContext novoContexto() {
        AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext();
        contexto.registerBean(JavaMailSender.class, () -> mock(JavaMailSender.class));
        return contexto;
    }
}
