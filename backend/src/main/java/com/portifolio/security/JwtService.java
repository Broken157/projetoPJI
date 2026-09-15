package com.portifolio.security;

import com.portifolio.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    @org.springframework.beans.factory.annotation.Autowired
    private com.portifolio.repository.RefreshTokenRepository refreshTokens;
    private final java.util.concurrent.ConcurrentMap<String, Long> revogados = new java.util.concurrent.ConcurrentHashMap<>();
    public void revogar(String token) {
        try { var claims = extrairClaims(token); revogados.put(claims.getId() == null ? token : claims.getId(), claims.getExpiration().getTime()); } catch (Exception ignored) { }
    }

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String gerarToken(Usuario usuario) {
        return gerarToken(usuario, null);
    }

    public String gerarToken(Usuario usuario, String refreshToken) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("tipoUsuario", usuario.getTipoUsuario().name())
                .claim("nome", usuario.getNome())
                .claim("sessao", refreshToken == null ? null : refreshTokens.findByTokenHash(hash(refreshToken)).orElseThrow().getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    private static String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public Claims extrairClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public Long extrairUsuarioId(String token) {
        Object id = extrairClaims(token).get("id");
        return id instanceof Number numero ? numero.longValue() : null;
    }

    public boolean tokenValido(String token) {
        try {
            var claims = extrairClaims(token);
            Object sessao = claims.get("sessao");
            if (sessao != null && (!(sessao instanceof Number id) || !refreshTokens.existsByIdAndAtivoTrueAndExpiracaoAfter(id.longValue(), java.time.LocalDateTime.now()))) return false;
            revogados.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
            return !revogados.containsKey(claims.getId() == null ? token : claims.getId()) && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
