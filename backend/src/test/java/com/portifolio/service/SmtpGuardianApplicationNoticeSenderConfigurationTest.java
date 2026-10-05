package com.portifolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpGuardianApplicationNoticeSenderConfigurationTest {
    @Test void smtpAusenteNaoCriaSenderReal() {
        try (var contexto = contexto()) {
            contexto.register(SmtpGuardianApplicationNoticeSender.class);
            contexto.refresh();
            assertThat(contexto.getBeansOfType(GuardianApplicationNoticeSender.class)).isEmpty();
        }
    }

    @Test void smtpConfiguradoReutilizaJavaMailSenderSemCredencialEmCodigo() {
        try (var contexto = contexto()) {
            TestPropertyValues.of("spring.mail.host=smtp.palco.test", "app.mail.from=avisos@palco.test", "app.frontend.base-url=https://palco.test")
                    .applyTo(contexto);
            contexto.register(SmtpGuardianApplicationNoticeSender.class);
            contexto.refresh();
            assertThat(contexto.getBeansOfType(GuardianApplicationNoticeSender.class)).hasSize(1);
        }
    }

    private AnnotationConfigApplicationContext contexto() {
        var contexto = new AnnotationConfigApplicationContext();
        contexto.registerBean(JavaMailSender.class, () -> mock(JavaMailSender.class));
        return contexto;
    }
}
