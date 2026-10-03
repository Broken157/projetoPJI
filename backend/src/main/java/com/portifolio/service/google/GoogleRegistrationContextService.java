package com.portifolio.service.google;

import com.portifolio.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleRegistrationContextService {
    public static final Duration VALIDADE = Duration.ofMinutes(10);
    private static final String FINALIDADE = "palco:google:cadastro:v1";
    private final SecretKey key;
    private final Clock clock;

    public GoogleRegistrationContextService(@Value("${jwt.secret}") String secret, Clock clock) {
        this.clock = clock;
        try {
            Mac derivador = Mac.getInstance("HmacSHA256");
            derivador.init(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));
            this.key = Keys.hmacShaKeyFor(derivador.doFinal(FINALIDADE.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Nao foi possivel configurar o contexto Google.");
        }
    }

    public String emitir(GoogleTokenClaims identidade, Boolean rememberMe) {
        Instant agora = clock.instant();
        return Jwts.builder().issuer(FINALIDADE).claim("finalidade", FINALIDADE)
                .id(UUID.randomUUID().toString()).subject(identidade.subject())
                .claim("email", identidade.email()).claim("nome", identidade.nome())
                .claim("foto", identidade.foto()).claim("rememberMe", Boolean.TRUE.equals(rememberMe))
                .issuedAt(Date.from(agora)).expiration(Date.from(agora.plus(VALIDADE)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public Contexto validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(FINALIDADE)
                    .require("finalidade", FINALIDADE).clock(() -> Date.from(clock.instant()))
                    .build().parseSignedClaims(token).getPayload();
            if (claims.getExpiration() == null || claims.getIssuedAt() == null
                    || !claims.getExpiration().toInstant().isAfter(clock.instant())
                    || claims.getId() == null
                    || claims.getIssuedAt().toInstant().isAfter(clock.instant())
                    || claims.getExpiration().toInstant().isAfter(claims.getIssuedAt().toInstant().plus(VALIDADE))) {
                throw new IllegalArgumentException();
            }
            return new Contexto(new GoogleTokenClaims(claims.getSubject(), claims.get("email", String.class),
                    claims.get("nome", String.class), claims.get("foto", String.class)),
                    Boolean.TRUE.equals(claims.get("rememberMe", Boolean.class)));
        } catch (RuntimeException ex) {
            throw new UnauthorizedException(GoogleTokenVerifier.MENSAGEM_ERRO);
        }
    }

    public record Contexto(GoogleTokenClaims identidade, boolean rememberMe) {}
}
