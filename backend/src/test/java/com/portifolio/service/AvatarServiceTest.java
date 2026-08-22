package com.portifolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AvatarServiceTest {

    private AvatarService avatarService;

    @BeforeEach
    void configurar() {
        avatarService = new AvatarService();
        ReflectionTestUtils.setField(avatarService, "estilo", "adventurer");
    }

    @Test
    void fotoDoPerfilTemPrioridadeSobreFotoDoUsuario() {
        assertThat(avatarService.resolverUrl(
                123L, "https://cdn.example/google.jpg", "https://cdn.example/custom.jpg"))
                .isEqualTo("https://cdn.example/custom.jpg");
    }

    @Test
    void fotoDoUsuarioTemPrioridadeSobreFallback() {
        assertThat(avatarService.resolverUrl(123L, "https://cdn.example/google.jpg", null))
                .isEqualTo("https://cdn.example/google.jpg");
    }

    @Test
    void fallbackEhEstavelESeedNaoRevelaIdSequencial() {
        String primeira = avatarService.resolverUrl(123L, null, null);
        String segunda = avatarService.resolverUrl(123L, " ", " ");

        assertThat(primeira).isEqualTo(segunda)
                .matches("https://api\\.dicebear\\.com/9\\.x/adventurer/svg\\?seed=[0-9a-f]{64}")
                .doesNotContain("seed=123");
    }
}
