package com.portifolio.service;

import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.UnauthorizedException;
import com.portifolio.dto.SessaoResponse;
import com.portifolio.model.RefreshToken;
import com.portifolio.model.Usuario;
import com.portifolio.repository.RefreshTokenRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF25 — sessão persistente; RF53 — gestão das próprias sessões.
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh.expiration-days:30}")
    private long expiracaoDias;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final jakarta.persistence.EntityManager entityManager;
    private final JwtService jwtService;
    private static final String SESSAO_INVALIDA = "Sessão expirada. Entre novamente.";

    /**
     * Gera um refresh token para o usuario.
     * O token retornado (UUID raw) eh enviado ao cliente.
     * Apenas o hash SHA-256 e persistido no banco (RNF01 — nada sensivel em texto puro).
     */
    @Transactional
    public String gerarRefreshToken(Usuario usuario) {
        // Mesma ordem de refresh/reset/senha: usuário antes da sessão.
        usuarioRepository.findByIdForUpdate(usuario.getId())
                .orElseThrow(() -> new UnauthorizedException(SESSAO_INVALIDA));
        String rawToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiracao(LocalDateTime.now().plusDays(expiracaoDias));
        refreshToken.setAtivo(true);
        refreshToken.setDataCriacao(LocalDateTime.now());

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Valida o token recebido do cliente.
     * Retorna o Usuario dono do token se valido e nao expirado.
     */
    @Transactional
    public Usuario validarRefreshToken(String rawToken) {
        return sessaoValidaBloqueada(rawToken).getUsuario();
    }

    /** Mantém a identidade da sessão e troca atomicamente o segredo sob bloqueio da linha. */
    @Transactional
    public String rotacionar(String rawToken) {
        RefreshToken rt = sessaoValidaBloqueada(rawToken);
        String next = UUID.randomUUID().toString();
        rt.setTokenHash(hashToken(next));
        rt.setExpiracao(LocalDateTime.now().plusDays(expiracaoDias));
        rt.setUltimoUso(LocalDateTime.now());
        refreshTokenRepository.saveAndFlush(rt);
        return next;
    }

    /** Invalida um token especifico (logout de um dispositivo). */
    @Transactional
    public void invalidarRefreshToken(String rawToken) {
        RefreshToken anterior = refreshTokenRepository.findByTokenHash(hashToken(rawToken)).orElse(null);
        if (anterior == null || anterior.getUsuario() == null) return;
        Long usuarioId = anterior.getUsuario().getId();
        if (usuarioRepository.findByIdForUpdate(usuarioId).isEmpty()) return;
        // O ID estável preserva a sessão mesmo se a rotação terminou durante a espera.
        refreshTokenRepository.findByIdAndUsuarioId(anterior.getId(), usuarioId).ifPresent(rt -> {
            entityManager.refresh(rt);
            rt.setAtivo(false);
            refreshTokenRepository.save(rt);
        });
    }

    /**
     * Invalida todos os tokens de um usuario (logout global / exclusao de conta).
     */
    @Transactional
    public void invalidarTodosDoUsuario(Long usuarioId) {
        usuarioRepository.findByIdForUpdate(usuarioId);
        refreshTokenRepository.invalidarTodosPorUsuario(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<SessaoResponse> listarProprias(String cookieAtual) {
        Long usuarioId = usuarioAtualId();
        String hashAtual = cookieAtual == null ? null : hashToken(cookieAtual);
        return refreshTokenRepository
                .findByUsuarioIdAndAtivoTrueAndExpiracaoAfterOrderByDataCriacaoDescIdDesc(
                        usuarioId, LocalDateTime.now())
                .stream().map(rt -> new SessaoResponse(rt.getId(), rt.getDataCriacao(),
                        rt.getExpiracao(), true, rt.getTokenHash().equals(hashAtual))).toList();
    }

    @Transactional
    public boolean encerrarPropria(Long sessionId, String cookieAtual) {
        Long usuarioId = usuarioAtualId();
        usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new UnauthorizedException(SESSAO_INVALIDA));
        RefreshToken rt = refreshTokenRepository.findByIdAndUsuarioId(sessionId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada."));
        entityManager.refresh(rt);
        boolean atual = cookieAtual != null && rt.getTokenHash().equals(hashToken(cookieAtual));
        rt.setAtivo(false);
        refreshTokenRepository.save(rt);
        return atual;
    }

    @Transactional
    public void encerrarTodasProprias() {
        Long usuarioId = usuarioAtualId();
        invalidarTodosDoUsuario(usuarioId);
        jwtService.revogarTodosDoUsuario(usuarioId);
    }

    private Long usuarioAtualId() {
        return authenticatedUserResolver.usuarioAtual().map(Usuario::getId)
                .orElseThrow(() -> new UnauthorizedException(SESSAO_INVALIDA));
    }

    private RefreshToken sessaoValidaBloqueada(String rawToken) {
        String hash = hashToken(rawToken);
        RefreshToken anterior = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException(SESSAO_INVALIDA));
        if (anterior.getUsuario() == null) throw new UnauthorizedException(SESSAO_INVALIDA);
        Usuario usuario = usuarioRepository.findByIdForUpdate(anterior.getUsuario().getId())
                .orElseThrow(() -> new UnauthorizedException(SESSAO_INVALIDA));
        entityManager.refresh(usuario);
        RefreshToken rt = refreshTokenRepository.findByTokenHashAndAtivoTrue(hash)
                .orElseThrow(() -> new UnauthorizedException(SESSAO_INVALIDA));
        entityManager.refresh(rt);
        if (!hash.equals(rt.getTokenHash()) || !Boolean.TRUE.equals(rt.getAtivo())
                || !rt.getExpiracao().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException(SESSAO_INVALIDA);
        }
        return rt;
    }

    // Hash SHA-256 do token raw — RNF01: nada sensivel gravado em texto puro
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro interno ao processar token.", e);
        }
    }
}
