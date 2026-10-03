package com.portifolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portifolio.exception.UnauthorizedException;
import com.portifolio.service.google.GoogleRegistrationContextService;
import com.portifolio.service.google.GoogleTokenClaims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;

class GoogleRegistrationContextServiceTest {
    private static final String SECRET = "fixture-exclusiva-rf01-contexto-google-2026-com-32-bytes";
    private static final Instant AGORA = Instant.parse("2026-10-02T12:00:00Z");
    private final GoogleTokenClaims identidade = new GoogleTokenClaims("sub-validado", "validado@palco.test", "Pessoa Google", null);

    @Test
    void preservaIdentidadeEPreferenciaValidadasPorDezMinutos() {
        String token = servico(AGORA).emitir(identidade, true);
        var contexto = servico(AGORA.plusSeconds(599)).validar(token);
        assertThat(contexto.identidade()).isEqualTo(identidade);
        assertThat(contexto.rememberMe()).isTrue();
        assertThatThrownBy(() -> servico(AGORA.plusSeconds(600)).validar(token))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void assinaturaDeSessaoNaoAutorizaCadastroENemContextoAutorizaSessao() {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String sessao = Jwts.builder().subject("validado@palco.test")
                .expiration(Date.from(AGORA.plusSeconds(600))).signWith(key).compact();
        assertThatThrownBy(() -> servico(AGORA).validar(sessao)).isInstanceOf(UnauthorizedException.class);
        String contexto = servico(AGORA).emitir(identidade, false);
        assertThatThrownBy(() -> Jwts.parser().verifyWith(key).build().parseSignedClaims(contexto))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    void contextoInvalidoOuDeOutraChaveFalhaGenericamente() {
        for (String token : new String[]{null, "", "invalido", "eyJhbGciOiJub25lIn0.e30."}) {
            assertThatThrownBy(() -> servico(AGORA).validar(token)).isInstanceOf(UnauthorizedException.class)
                    .hasMessage(com.portifolio.service.google.GoogleTokenVerifier.MENSAGEM_ERRO);
        }
        String token = servico(AGORA).emitir(identidade, false);
        var outraInstancia = new GoogleRegistrationContextService(SECRET + "outra-chave", Clock.fixed(AGORA, ZoneOffset.UTC));
        assertThatThrownBy(() -> outraInstancia.validar(token)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void contextoDeOutraInstanciaComMesmaChaveFuncionaSemEstadoLocal() {
        String token = servico(AGORA).emitir(identidade, false);
        assertThat(servico(AGORA.plusSeconds(5)).validar(token).identidade()).isEqualTo(identidade);
    }

    private GoogleRegistrationContextService servico(Instant instante) {
        return new GoogleRegistrationContextService(SECRET, Clock.fixed(instante, ZoneOffset.UTC));
    }
}
