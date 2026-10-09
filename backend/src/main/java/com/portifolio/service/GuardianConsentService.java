package com.portifolio.service;

import com.portifolio.dto.GuardianDecisionRequest;
import com.portifolio.dto.GuardianInviteResponse;
import com.portifolio.dto.PasswordRecoveryResponse;
import com.portifolio.dto.ResendConfirmationRequest;
import com.portifolio.exception.ConflictException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.TokenExpiredException;
import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.ResponsavelLegalRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianConsentService {
    public static final String MENSAGEM_REENVIO =
            "Se a conta estiver aguardando autorização, o responsável receberá as instruções em breve.";
    private static final String TOKEN_INVALIDO = "Convite inválido ou já utilizado.";
    private static final Duration VALIDADE = Duration.ofHours(24);
    private static final Duration INTERVALO_REENVIO = Duration.ofMinutes(1);
    private static final int MAXIMO_REENVIOS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ResponsavelLegalRepository responsaveis;
    private final PerfilArtistaRepository perfis;
    private final ObjectProvider<GuardianConsentEmailSender> senderProvider;
    private final ResendConfirmationRateLimiter rateLimiter;
    private final Clock clock;
    private final com.portifolio.security.AuthenticatedUserResolver authenticatedUserResolver;

    // Participa da transação RF26/Google, mas falha de SMTP não desfaz o e-mail já confirmado.
    public boolean iniciarConvite(Usuario usuario) {
        if (!pendenteValido(usuario)) return false;
        ResponsavelLegal responsavel = usuario.getResponsavelLegal();
        if (responsavel.getTokenConsentimento() != null) return true;
        return enviar(responsavel, usuario, 0);
    }

    @Transactional(readOnly = true)
    public GuardianInviteResponse consultar(String token) {
        ResponsavelLegal responsavel = responsaveis.findByTokenPrefixo(prefixo(token))
                .orElseThrow(() -> new ResourceNotFoundException(TOKEN_INVALIDO));
        validarConvite(responsavel);
        return new GuardianInviteResponse(responsavel.getUsuario().getNome(),
                "Escolha Autorizar ou Recusar a participação deste artista adolescente no Palco.");
    }

    @Transactional
    public PasswordRecoveryResponse decidir(GuardianDecisionRequest request) {
        ResponsavelLegal responsavel = responsaveis.findByTokenPrefixoForUpdate(prefixo(request.token()))
                .orElseThrow(() -> new ResourceNotFoundException(TOKEN_INVALIDO));
        Usuario usuario = validarConvite(responsavel);
        if (request.decisao() == GuardianDecisionRequest.Decision.AUTORIZAR) {
            if (!cadastroInicialCompleto(usuario)) {
                throw new ConflictException("O cadastro do artista ainda não está apto para autorização.");
            }
            responsavel.setConsentimentoRevogado(false);
            responsavel.setDataConsentimento(LocalDateTime.now(clock));
            responsavel.setTokenConsentimento(null);
            usuario.setStatusConta(StatusConta.ATIVA);
            return new PasswordRecoveryResponse("Autorização registrada. O artista já pode entrar na conta.");
        }
        // O schema não tem enum de recusa; BLOQUEADA + revogado + data registra
        // uma decisão negativa terminal, sem habilitar o fluxo de revogação futura.
        responsavel.setConsentimentoRevogado(true);
        responsavel.setDataConsentimento(LocalDateTime.now(clock));
        responsavel.setTokenConsentimento(null);
        usuario.setStatusConta(StatusConta.BLOQUEADA);
        return new PasswordRecoveryResponse("Recusa registrada. A conta permanece sem acesso.");
    }

    @Transactional
    public PasswordRecoveryResponse reenviar(ResendConfirmationRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        // Pendentes não têm sessão normal: mantém o reenvio público/genérico já existente.
        // Se há titular autenticado, ele não pode usar a sessão para reenviar convite alheio.
        authenticatedUserResolver.usuarioAtual().ifPresent(atual -> {
            if (!email.equalsIgnoreCase(atual.getEmail().trim())) {
                throw new com.portifolio.exception.ForbiddenException("Solicitação não pertence à conta autenticada.");
            }
        });
        rateLimiter.exigirDisponibilidade("rf27:" + hash(email),
                "Aguarde antes de solicitar outro convite.");
        PasswordRecoveryResponse resposta = new PasswordRecoveryResponse(MENSAGEM_REENVIO);
        ResponsavelLegal responsavel = responsaveis.findByUsuarioEmailForUpdate(email).orElse(null);
        if (responsavel == null || !pendenteValido(responsavel.getUsuario())) return resposta;
        Registro anterior = lerRegistro(responsavel.getTokenConsentimento());
        long agora = clock.instant().getEpochSecond();
        if (anterior != null && agora - anterior.emissao() < INTERVALO_REENVIO.toSeconds()) return resposta;
        int tentativas = anterior == null || agora - anterior.emissao() >= VALIDADE.toSeconds()
                ? 0 : anterior.tentativas();
        if (tentativas >= MAXIMO_REENVIOS) return resposta;
        enviar(responsavel, responsavel.getUsuario(), tentativas + 1);
        return resposta;
    }

    private boolean enviar(ResponsavelLegal responsavel, Usuario usuario, int tentativas) {
        GuardianConsentEmailSender sender = senderProvider.getIfAvailable();
        if (sender == null) return false;
        String token = novoToken();
        String anterior = responsavel.getTokenConsentimento();
        responsavel.setTokenConsentimento("v1:" + hash(token) + ":"
                + clock.instant().getEpochSecond() + ":" + tentativas);
        responsaveis.saveAndFlush(responsavel);
        try {
            sender.enviarConvite(responsavel.getEmailResponsavel(), usuario.getNome(), token);
            return true;
        } catch (RuntimeException ex) {
            responsavel.setTokenConsentimento(anterior);
            responsaveis.saveAndFlush(responsavel);
            log.warn("Falha ao enviar convite do responsável: {}", ex.getClass().getSimpleName());
            return false;
        }
    }

    private Usuario validarConvite(ResponsavelLegal responsavel) {
        Registro registro = lerRegistro(responsavel.getTokenConsentimento());
        if (registro == null) throw new ResourceNotFoundException(TOKEN_INVALIDO);
        if (clock.instant().getEpochSecond() >= registro.emissao() + VALIDADE.toSeconds()) {
            throw new TokenExpiredException("Convite expirado. Solicite um novo envio.");
        }
        Usuario usuario = responsavel.getUsuario();
        if (!pendenteValido(usuario)) {
            throw new ConflictException("A conta não está aguardando autorização.");
        }
        return usuario;
    }

    private boolean pendenteValido(Usuario usuario) {
        if (usuario == null || usuario.getStatusConta() != StatusConta.PENDENTE_CONSENTIMENTO
                || !Boolean.TRUE.equals(usuario.getEmailVerificado())
                || usuario.getTipoUsuario() != TipoUsuario.ARTISTA
                || usuario.getDataNascimento() == null) return false;
        int idade = Period.between(usuario.getDataNascimento(), LocalDate.now(clock)).getYears();
        ResponsavelLegal r = usuario.getResponsavelLegal();
        return idade >= 14 && idade < 18 && r != null
                && r.getNomeResponsavel() != null && !r.getNomeResponsavel().isBlank()
                && r.getTelefoneResponsavel() != null && !r.getTelefoneResponsavel().isBlank()
                && r.getEmailResponsavel() != null && !r.getEmailResponsavel().isBlank()
                && !r.getEmailResponsavel().trim().equalsIgnoreCase(usuario.getEmail().trim())
                && r.getDataConsentimento() == null && !Boolean.TRUE.equals(r.getConsentimentoRevogado());
    }

    private boolean cadastroInicialCompleto(Usuario usuario) {
        return perfis.findById(usuario.getId()).map(perfil ->
                perfil.getTipoPerfilArtistico() != null
                        && perfil.getAreas().stream().anyMatch(area -> area.isPrincipal()))
                .orElse(false);
    }

    private String prefixo(String token) {
        if (token == null || token.isBlank() || token.length() > 255) {
            throw new ResourceNotFoundException(TOKEN_INVALIDO);
        }
        return "v1:" + hash(token.trim()) + ":";
    }

    private Registro lerRegistro(String valor) {
        if (valor == null) return null;
        String[] partes = valor.split(":", -1);
        if (partes.length != 4 || !"v1".equals(partes[0]) || partes[1].length() != 64) return null;
        try {
            return new Registro(Long.parseLong(partes[2]), Integer.parseInt(partes[3]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record Registro(long emissao, int tentativas) {}

    private static String novoToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível.", ex);
        }
    }
}
