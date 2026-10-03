package com.portifolio.controller;

import com.portifolio.dto.*;
import com.portifolio.service.AuthService;
import com.portifolio.service.PasswordRecoveryService;
import com.portifolio.service.EmailVerificationService;
import com.portifolio.service.GuardianConsentService;
import com.portifolio.security.SessionCookiePolicy;
import com.portifolio.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final EmailVerificationService emailVerificationService;
    private final GuardianConsentService guardianConsentService;
    private final SessionCookiePolicy cookies;
    private final JwtService jwtService;

    @PostMapping("/cadastro")
    public ResponseEntity<CadastroResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.cadastrar(request));
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest http, HttpServletResponse response) {
        cookies.validarOrigem(http);
        LoginResponse result = authService.login(request);
        invalidarCookieAnterior(http);
        cookies.gravar(response, result.getRefreshToken());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> loginComGoogle(@Valid @RequestBody GoogleAuthRequest request,
            HttpServletRequest http, HttpServletResponse response) {
        cookies.validarOrigem(http);
        GoogleAuthResponse result = authService.loginComGoogle(request);
        invalidarCookieAnterior(http);
        cookies.gravar(response, result.getRefreshToken());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    @PostMapping("/google/cadastro")
    public ResponseEntity<GoogleAuthResponse> concluirCadastroGoogle(@Valid @RequestBody GoogleCadastroRequest request,
            HttpServletRequest http, HttpServletResponse response) {
        cookies.validarOrigem(http);
        GoogleAuthResponse result = authService.concluirCadastroGoogle(request);
        invalidarCookieAnterior(http);
        cookies.gravar(response, result.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(HttpServletRequest http, HttpServletResponse response) {
        cookies.validarOrigem(http);
        String token = cookies.ler(http);
        if (token == null) throw new com.portifolio.exception.UnauthorizedException("Sessão expirada. Entre novamente.");
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(token);
        RefreshResponse result = authService.refreshToken(request);
        cookies.gravar(response, result.getRefreshToken());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http, HttpServletResponse response) {
        cookies.validarOrigem(http);
        invalidarCookieAnterior(http);
        String authorization = http.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) jwtService.revogar(authorization.substring(7));
        cookies.gravar(response, null);
        return ResponseEntity.noContent().build();
    }
    private void invalidarCookieAnterior(HttpServletRequest http) {
        String previous = cookies.ler(http);
        if (previous != null) {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken(previous);
            authService.logout(request);
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordRecoveryResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordRecoveryService.solicitar(request));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordRecoveryResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordRecoveryService.redefinir(request));
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<PasswordRecoveryResponse> confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(emailVerificationService.confirmar(request));
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<PasswordRecoveryResponse> resendConfirmation(
            @Valid @RequestBody ResendConfirmationRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(emailVerificationService.reenviar(request));
    }

    @PostMapping("/guardian-invite")
    public ResponseEntity<GuardianInviteResponse> guardianInvite(@Valid @RequestBody ConfirmEmailRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(guardianConsentService.consultar(request.getToken()));
    }

    @PostMapping("/guardian-decision")
    public ResponseEntity<PasswordRecoveryResponse> guardianDecision(
            @Valid @RequestBody GuardianDecisionRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(guardianConsentService.decidir(request));
    }

    @PostMapping("/resend-guardian-invite")
    public ResponseEntity<PasswordRecoveryResponse> resendGuardianInvite(
            @Valid @RequestBody ResendConfirmationRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(guardianConsentService.reenviar(request));
    }
}
