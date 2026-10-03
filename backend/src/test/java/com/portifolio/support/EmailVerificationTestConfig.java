package com.portifolio.support;

import com.portifolio.service.EmailVerificationEmailSender;
import com.portifolio.service.GuardianConsentEmailSender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class EmailVerificationTestConfig {
    @Bean
    @Primary
    public CapturingEmailVerificationSender emailVerificationEmailSender() {
        return new CapturingEmailVerificationSender();
    }

    @Bean
    @Primary
    public CapturingGuardianConsentSender guardianConsentEmailSender() {
        return new CapturingGuardianConsentSender();
    }

    public static class CapturingGuardianConsentSender implements GuardianConsentEmailSender {
        private final Map<String, String> tokens = new ConcurrentHashMap<>();
        private volatile boolean falhar;

        @Override
        public void enviarConvite(String emailResponsavel, String nomeArtista, String token) {
            if (falhar) throw new IllegalStateException("Entrega de teste indisponível");
            tokens.put(emailResponsavel.toLowerCase(java.util.Locale.ROOT), token);
        }

        public String token(String email) { return tokens.get(email.toLowerCase(java.util.Locale.ROOT)); }
        public void falhar(boolean value) { falhar = value; }
        public void limpar() { tokens.clear(); falhar = false; }
    }

    public static class CapturingEmailVerificationSender implements EmailVerificationEmailSender {
        private final Map<String, String> tokens = new ConcurrentHashMap<>();
        private volatile boolean falhar;

        @Override
        public void enviarConfirmacao(String email, String token) {
            tokens.put(email.toLowerCase(java.util.Locale.ROOT), token);
            if (falhar) throw new IllegalStateException("Entrega de teste indisponível");
        }

        public String token(String email) {
            return tokens.get(email.toLowerCase(java.util.Locale.ROOT));
        }

        public void falhar(boolean value) {
            falhar = value;
        }

        public void limpar() {
            tokens.clear();
            falhar = false;
        }
    }
}
