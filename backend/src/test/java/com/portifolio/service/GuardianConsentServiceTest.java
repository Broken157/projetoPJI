package com.portifolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.portifolio.dto.GuardianDecisionRequest;
import com.portifolio.exception.ConflictException;
import com.portifolio.model.AreaArtistica;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilArtistaArea;
import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoPerfilArtistico;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.ResponsavelLegalRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class GuardianConsentServiceTest {
    private static final String TOKEN = "convite-rf27-sem-raio";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);

    @Mock ResponsavelLegalRepository responsaveis;
    @Mock PerfilArtistaRepository perfis;
    @Mock ObjectProvider<GuardianConsentEmailSender> senderProvider;
    @Mock ResendConfirmationRateLimiter rateLimiter;

    private GuardianConsentService consentimento;
    private Usuario usuario;
    private ResponsavelLegal responsavel;
    private PerfilArtista perfil;

    @BeforeEach
    void preparar() throws Exception {
        consentimento = new GuardianConsentService(responsaveis, perfis, senderProvider, rateLimiter, CLOCK);

        usuario = new Usuario();
        usuario.setId(41L);
        usuario.setNome("Artista RF27");
        usuario.setEmail("artista@palco.test");
        usuario.setTelefone("11999999999");
        usuario.setDataNascimento(LocalDate.now(CLOCK).minusYears(16));
        usuario.setTipoUsuario(TipoUsuario.ARTISTA);
        usuario.setEmailVerificado(true);
        usuario.setStatusConta(StatusConta.PENDENTE_CONSENTIMENTO);
        usuario.setPerfilCompleto(false);

        responsavel = new ResponsavelLegal();
        responsavel.setNomeResponsavel("Responsável RF27");
        responsavel.setTelefoneResponsavel("11888888888");
        responsavel.setEmailResponsavel("responsavel@palco.test");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TOKEN.getBytes(StandardCharsets.UTF_8)));
        responsavel.setTokenConsentimento("v1:" + hash + ":" + CLOCK.instant().getEpochSecond() + ":0");
        usuario.setResponsavelLegal(responsavel);

        perfil = new PerfilArtista();
        perfil.setUsuario(usuario);
        perfil.setTipoPerfilArtistico(TipoPerfilArtistico.ARTISTA_SOLO);
        AreaArtistica area = new AreaArtistica();
        area.setId((short) 1);
        PerfilArtistaArea principal = new PerfilArtistaArea();
        principal.setPerfil(perfil);
        principal.setArea(area);
        principal.setPrincipal(true);
        perfil.getAreas().add(principal);

        when(responsaveis.findByTokenPrefixoForUpdate(anyString())).thenReturn(Optional.of(responsavel));
        when(perfis.findById(usuario.getId())).thenReturn(Optional.of(perfil));
    }

    @Test
    void autorizaMenorSemRaioSemDeclararPerfilRf08Completo() {
        assertThat(perfil.getRaioAtuacao()).isNull();

        consentimento.decidir(new GuardianDecisionRequest(TOKEN, GuardianDecisionRequest.Decision.AUTORIZAR));

        assertThat(usuario.getStatusConta()).isEqualTo(StatusConta.ATIVA);
        assertThat(usuario.getPerfilCompleto()).isFalse();
        assertThat(responsavel.getDataConsentimento()).isNotNull();
        assertThat(responsavel.getConsentimentoRevogado()).isFalse();
        assertThat(responsavel.getTokenConsentimento()).isNull();
    }

    @Test
    void naoAutorizaSemSubtipoArtistico() {
        perfil.setTipoPerfilArtistico(null);
        assertCadastroIncompleto();
    }

    @Test
    void naoAutorizaSemAreaPrincipal() {
        perfil.getAreas().clear();
        assertCadastroIncompleto();
    }

    private void assertCadastroIncompleto() {
        assertThatThrownBy(() -> consentimento.decidir(
                new GuardianDecisionRequest(TOKEN, GuardianDecisionRequest.Decision.AUTORIZAR)))
                .isInstanceOf(ConflictException.class);
        assertThat(usuario.getStatusConta()).isEqualTo(StatusConta.PENDENTE_CONSENTIMENTO);
        assertThat(responsavel.getDataConsentimento()).isNull();
        assertThat(responsavel.getTokenConsentimento()).isNotNull();
    }
}
