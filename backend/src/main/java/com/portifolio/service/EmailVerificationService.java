package com.portifolio.service;

import com.portifolio.dto.ConfirmEmailRequest;
import com.portifolio.dto.PasswordRecoveryResponse;
import com.portifolio.dto.ResendConfirmationRequest;
import com.portifolio.exception.ConflictException;
import com.portifolio.exception.EmailDeliveryUnavailableException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.TokenExpiredException;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.UsuarioRepository;
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
public class EmailVerificationService {
    public static final String MENSAGEM_REENVIO =
            "Se a conta estiver pendente de verificação, você receberá as instruções em breve.";
    private static final String TOKEN_INVALIDO = "Token de confirmação inválido ou já utilizado.";
    private static final Duration VALIDADE = Duration.ofHours(1);
    private static final Duration INTERVALO_REENVIO = Duration.ofMinutes(1);
    private static final Duration JANELA_REENVIO = Duration.ofDays(1);
    private static final int MAXIMO_REENVIOS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final ObjectProvider<EmailVerificationEmailSender> emailSenderProvider;
    private final ResendConfirmationRateLimiter rateLimiter;
    private final GuardianConsentService guardianConsentService;
    private final Clock clock;

    // Chamado na mesma transação do cadastro: falha de entrega não deixa conta sem link.
    public void iniciarCadastro(Usuario usuario) {
        EmailVerificationEmailSender sender = emailSenderProvider.getIfAvailable();
        if (sender == null) throw new EmailDeliveryUnavailableException();
        String token = gerarToken();
        usuario.setEmailVerificado(false);
        usuario.setTokenVerificacao(hash(token));
        usuario.setUltimoReenvioVerificacao(agora());
        usuario.setTentativasVerificacaoEmail(0);
        usuarioRepository.saveAndFlush(usuario);
        try {
            sender.enviarConfirmacao(usuario.getEmail(), token);
        } catch (RuntimeException ex) {
            log.warn("Falha ao enviar confirmação de e-mail: {}", ex.getClass().getSimpleName());
            throw new EmailDeliveryUnavailableException();
        }
    }

    @Transactional
    public PasswordRecoveryResponse confirmar(ConfirmEmailRequest request) {
        Usuario usuario = usuarioRepository.findByTokenVerificacaoForUpdate(hash(request.getToken().trim()))
                .orElseThrow(() -> new ResourceNotFoundException(TOKEN_INVALIDO));
        if (usuario.getStatusConta() != StatusConta.PENDENTE_VERIFICACAO_EMAIL
                || Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new ConflictException("A conta não está pendente de verificação de e-mail.");
        }
        LocalDateTime emissao = usuario.getUltimoReenvioVerificacao();
        if (emissao == null || !emissao.plus(VALIDADE).isAfter(agora())) {
            throw new TokenExpiredException("Token de confirmação expirado. Solicite um novo e-mail.");
        }
        ativarAposVerificacao(usuario);
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacao(null);
        usuarioRepository.saveAndFlush(usuario);
        boolean conviteEnviado = usuario.getStatusConta() != StatusConta.PENDENTE_CONSENTIMENTO
                || guardianConsentService.iniciarConvite(usuario);
        return new PasswordRecoveryResponse(usuario.getStatusConta() == StatusConta.PENDENTE_CONSENTIMENTO
                ? (conviteEnviado
                    ? "E-mail confirmado. A conta aguarda o consentimento do responsável."
                    : "E-mail confirmado. A conta aguarda o consentimento do responsável. Reenvie o convite se necessário.")
                : "E-mail confirmado. Você já pode entrar na sua conta.");
    }

    @Transactional
    public PasswordRecoveryResponse reenviar(ResendConfirmationRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        // A mesma barreira antecede a consulta ao banco para e-mails conhecidos e desconhecidos.
        rateLimiter.exigirDisponibilidade(hash(email));
        PasswordRecoveryResponse resposta = new PasswordRecoveryResponse(MENSAGEM_REENVIO);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseForUpdate(email).orElse(null);
        if (usuario == null || usuario.getStatusConta() != StatusConta.PENDENTE_VERIFICACAO_EMAIL
                || Boolean.TRUE.equals(usuario.getEmailVerificado())) return resposta;
        EmailVerificationEmailSender sender = emailSenderProvider.getIfAvailable();
        if (sender == null) return resposta;

        LocalDateTime momento = agora();
        LocalDateTime envioAnterior = usuario.getUltimoReenvioVerificacao();
        int tentativas = usuario.getTentativasVerificacaoEmail() == null ? 0
                : usuario.getTentativasVerificacaoEmail();
        if (envioAnterior != null && !envioAnterior.plus(JANELA_REENVIO).isAfter(momento)) tentativas = 0;
        if (tentativas >= MAXIMO_REENVIOS || (envioAnterior != null
                && envioAnterior.plus(INTERVALO_REENVIO).isAfter(momento))) return resposta;

        String hashAnterior = usuario.getTokenVerificacao();
        int tentativasAnteriores = usuario.getTentativasVerificacaoEmail() == null ? 0
                : usuario.getTentativasVerificacaoEmail();
        String token = gerarToken();
        usuario.setTokenVerificacao(hash(token));
        usuario.setUltimoReenvioVerificacao(momento);
        usuario.setTentativasVerificacaoEmail(tentativas + 1);
        usuarioRepository.saveAndFlush(usuario);
        try {
            sender.enviarConfirmacao(usuario.getEmail(), token);
        } catch (RuntimeException ex) {
            usuario.setTokenVerificacao(hashAnterior);
            usuario.setUltimoReenvioVerificacao(envioAnterior);
            usuario.setTentativasVerificacaoEmail(tentativasAnteriores);
            usuarioRepository.saveAndFlush(usuario);
            log.warn("Falha ao reenviar confirmação de e-mail: {}", ex.getClass().getSimpleName());
        }
        return resposta;
    }

    // O verificador do Google aceita somente ID Token com email_verified=true.
    public void confirmarPeloGoogle(Usuario usuario) {
        if (usuario.getStatusConta() == StatusConta.BLOQUEADA) return;
        boolean aguardavaEmail = usuario.getStatusConta() == StatusConta.PENDENTE_VERIFICACAO_EMAIL;
        if (usuario.getStatusConta() == StatusConta.PENDENTE_VERIFICACAO_EMAIL) {
            ativarAposVerificacao(usuario);
        }
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacao(null);
        usuarioRepository.saveAndFlush(usuario);
        if (aguardavaEmail && usuario.getStatusConta() == StatusConta.PENDENTE_CONSENTIMENTO) {
            guardianConsentService.iniciarConvite(usuario);
        }
    }

    private void ativarAposVerificacao(Usuario usuario) {
        LocalDate nascimento = usuario.getDataNascimento();
        int idade = nascimento == null ? -1 : Period.between(nascimento, LocalDate.now(clock)).getYears();
        if (idade < 14 || (idade < 18 && usuario.getTipoUsuario() != TipoUsuario.ARTISTA)) {
            throw new ConflictException("A conta não pode ser ativada nesta situação.");
        }
        usuario.setStatusConta(idade < 18 ? StatusConta.PENDENTE_CONSENTIMENTO : StatusConta.ATIVA);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível.", ex);
        }
    }
}
