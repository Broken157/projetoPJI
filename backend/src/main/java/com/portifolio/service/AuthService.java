package com.portifolio.service;

import com.portifolio.dto.CadastroRequest;
import com.portifolio.dto.CadastroDadosRequest;
import com.portifolio.dto.GoogleCadastroRequest;
import com.portifolio.service.google.GoogleRegistrationContextService;
import com.portifolio.validation.CadastroValidator;
import com.portifolio.dto.CadastroResponse;
import com.portifolio.dto.GoogleAuthRequest;
import com.portifolio.dto.GoogleAuthResponse;
import com.portifolio.dto.LoginRequest;
import com.portifolio.dto.LoginResponse;
import com.portifolio.dto.RefreshRequest;
import com.portifolio.dto.RefreshResponse;
import com.portifolio.exception.ConflictException;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.UnauthorizedException;
import com.portifolio.model.Usuario;
import com.portifolio.model.PerfilArtistaArea;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.security.GoogleAccountAccessPolicy;
import com.portifolio.service.google.GoogleLinkLock;
import com.portifolio.service.google.GoogleTokenClaims;
import com.portifolio.service.google.GoogleTokenVerifier;
import com.portifolio.validation.PasswordPolicy;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PerfilArtistaRepository perfilArtistaRepository;
    private final PerfilContratanteRepository perfilContratanteRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AvatarService avatarService;
    private final PasswordPolicy passwordPolicy;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final GoogleLinkLock googleLinkLock;
    private final EmailVerificationService emailVerificationService;
    private final GuardianConsentService guardianConsentService;
    private final CadastroValidator cadastroValidator;
    private final GoogleRegistrationContextService googleRegistrationContext;
    private final GoogleAccountAccessPolicy accountAccessPolicy;
    private final com.portifolio.validation.ConteudoPublicoValidator conteudoPublico;
    private final jakarta.persistence.EntityManager entityManager;

    // ──────────────────────────────────────────────────────────
    // RF01 — Cadastro convencional e vínculo com uma área principal
    // ──────────────────────────────────────────────────────────

    @Transactional
    public CadastroResponse cadastrar(CadastroRequest request) {
        googleLinkLock.bloquearEmail(request.getEmail());
        CadastroValidator.DadosValidados dados = cadastroValidator.validar(request, request.getEmail());
        String senha = passwordPolicy.encode(request.getSenha());
        Usuario usuario = novoUsuario(request, dados, request.getNome(), request.getEmail());
        usuario.setSenha(senha);
        usuario.setStatusConta(StatusConta.PENDENTE_VERIFICACAO_EMAIL);
        Usuario salvo = usuarioRepository.saveAndFlush(usuario);
        criarPerfilInicial(salvo, request, dados);
        emailVerificationService.iniciarCadastro(salvo);
        return CadastroResponse.builder()
                .id(salvo.getId()).nome(salvo.getNome()).email(salvo.getEmail())
                .username(salvo.getUsername()).tipoUsuario(salvo.getTipoUsuario())
                .menorDeIdade(dados.menor())
                .mensagem("Cadastro realizado; conta pendente de verificação de e-mail.")
                .build();
    }

    private Usuario novoUsuario(CadastroDadosRequest request, CadastroValidator.DadosValidados dados,
            String nome, String email) {
        conteudoPublico.texto(nome, "Nome público", 150, true);
        conteudoPublico.texto(request.getNomeEntidade(), "Nome da entidade", 150, false);
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setUsername(request.getUsername());
        usuario.setEmail(email);
        usuario.setTelefone(request.getTelefone());
        usuario.setDataNascimento(request.getDataNascimento());
        usuario.setTipoUsuario(request.getTipoUsuario());
        usuario.setCpf(dados.cpf());
        usuario.setCnpj(dados.cnpj());
        usuario.setPerfilCompleto(false);
        usuario.setDataCriacao(LocalDateTime.now());
        if (dados.menor()) {
            usuario.setNomeResponsavel(request.getNomeResponsavel());
            usuario.setTelefoneResponsavel(request.getTelefoneResponsavel());
            usuario.setEmailResponsavel(request.getEmailResponsavel());
        }
        return usuario;
    }

    private void criarPerfilInicial(Usuario usuario, CadastroDadosRequest request,
            CadastroValidator.DadosValidados dados) {
        if (usuario.getTipoUsuario() == TipoUsuario.CONTRATANTE) {
            PerfilContratante perfil = new PerfilContratante();
            perfil.setUsuario(usuario);
            perfil.setTipoContratante(dados.contratante());
            perfil.setNomeEmpresa(request.getNomeEntidade());
            perfilContratanteRepository.saveAndFlush(perfil);
            return;
        }
        PerfilArtista perfil = new PerfilArtista();
        perfil.setUsuario(usuario);
        perfil.setTipoPerfilArtistico(request.getTipoPerfilArtistico());
        PerfilArtistaArea vinculo = new PerfilArtistaArea();
        vinculo.setPerfil(perfil);
        vinculo.setArea(dados.area());
        vinculo.setPrincipal(true);
        perfil.getAreas().add(vinculo);
        perfilArtistaRepository.saveAndFlush(perfil);
    }

    // ──────────────────────────────────────────────────────────
    // RF02 — Login convencional
    // RF33 — "Lembrar de mim" (rememberMe)
    // RF34 — avatarUrl no response
    // ──────────────────────────────────────────────────────────

    // @Transactional (nao readOnly) pois RF33 pode escrever refresh_token no banco
    @Transactional
    public LoginResponse login(LoginRequest request) {

        Usuario usuario = usuarioRepository.findByEmailForUpdate(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email ou senha incorretos."));
        entityManager.refresh(usuario);

        // Conta Google sem senha local nao revela a existencia do e-mail.
        if (usuario.getSenha() == null) {
            throw new UnauthorizedException("Email ou senha incorretos.");
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new UnauthorizedException("Email ou senha incorretos.");
        }

        exigirAcessoNormal(usuario);


        // RF33: gera refresh token apenas se rememberMe = true
        String refreshToken = null;
        if (Boolean.TRUE.equals(request.getRememberMe())) {
            refreshToken = refreshTokenService.gerarRefreshToken(usuario);
        }

        String token = jwtService.gerarToken(usuario, refreshToken);
        // RF34: resolve avatar a partir da foto do usuario (perfil ainda pode nao existir)
        String avatarUrl = avatarService.resolverUrl(usuario.getId(), usuario.getFotoPerfil(), null);

        return LoginResponse.builder()
                .token(token)
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipoUsuario(usuario.getTipoUsuario())
                .perfilCompleto(usuario.getPerfilCompleto())
                .avatarUrl(avatarUrl)
                .refreshToken(refreshToken)
                .build();
    }

    // ──────────────────────────────────────────────────────────
    // RF32 — Login com Google (OAuth2)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GoogleAuthResponse loginComGoogle(GoogleAuthRequest request) {
        GoogleTokenClaims claims = googleTokenVerifier.verificar(request.getIdToken());
        validarDadosGoogleNoSchema(claims.subject(), claims.email(), claims.nome(), claims.foto());
        googleLinkLock.bloquear(claims.email(), claims.subject());
        Optional<Usuario> usuarioPorEmail = usuarioRepository.findByEmailIgnoreCaseForUpdate(claims.email());
        Optional<Usuario> usuarioPorGoogle = usuarioRepository.findByGoogleIdForUpdate(claims.subject());
        if (usuarioPorGoogle.isPresent()) {
            Usuario usuario = usuarioPorGoogle.get();
            if (!usuario.getEmail().equalsIgnoreCase(claims.email())
                    || (usuarioPorEmail.isPresent()
                        && !Objects.equals(usuario.getId(), usuarioPorEmail.get().getId()))) {
                throw falhaGoogle();
            }
            emailVerificationService.confirmarPeloGoogle(usuario);
            return autenticarUsuario(usuario, request.getRememberMe());
        }
        if (usuarioPorEmail.isPresent()) {
            Usuario usuario = usuarioPorEmail.get();
            if (usuario.getGoogleId() != null && !usuario.getGoogleId().equals(claims.subject())) {
                throw falhaGoogle();
            }
            if (usuario.getGoogleId() == null) {
                usuario.setGoogleId(claims.subject());
                if (usuario.getFotoPerfil() == null) usuario.setFotoPerfil(claims.foto());
                try {
                    usuarioRepository.saveAndFlush(usuario);
                } catch (DataIntegrityViolationException ex) {
                    throw falhaGoogle();
                }
            }
            emailVerificationService.confirmarPeloGoogle(usuario);
            return autenticarUsuario(usuario, request.getRememberMe());
        }
        return GoogleAuthResponse.builder().status("AGUARDANDO_DADOS")
                .nomeGoogle(claims.nome()).emailGoogle(claims.email()).fotoGoogle(claims.foto())
                .contexto(googleRegistrationContext.emitir(claims, request.getRememberMe()))
                .contextoExpiraEmSegundos(GoogleRegistrationContextService.VALIDADE.toSeconds()).build();
    }

    @Transactional
    public GoogleAuthResponse concluirCadastroGoogle(GoogleCadastroRequest request) {
        var contexto = googleRegistrationContext.validar(request.getContexto());
        GoogleTokenClaims claims = contexto.identidade();
        validarDadosGoogleNoSchema(claims.subject(), claims.email(), claims.nome(), claims.foto());
        googleLinkLock.bloquear(claims.email(), claims.subject());
        if (usuarioRepository.findByGoogleIdForUpdate(claims.subject()).isPresent()
                || usuarioRepository.findByEmailIgnoreCaseForUpdate(claims.email()).isPresent()) {
            throw new ConflictException("Cadastro Google ja concluido ou conta existente. Entre novamente.");
        }
        CadastroValidator.DadosValidados dados = cadastroValidator.validar(request, claims.email());
        Usuario usuario = novoUsuario(request, dados, claims.nome(), claims.email());
        usuario.setGoogleId(claims.subject());
        usuario.setFotoPerfil(claims.foto());
        usuario.setEmailVerificado(true);
        usuario.setStatusConta(dados.menor() ? StatusConta.PENDENTE_CONSENTIMENTO : StatusConta.ATIVA);
        Usuario salvo = usuarioRepository.saveAndFlush(usuario);
        criarPerfilInicial(salvo, request, dados);
        if (dados.menor()) guardianConsentService.iniciarConvite(salvo);
        return autenticarUsuario(salvo, contexto.rememberMe());
    }

    // ──────────────────────────────────────────────────────────
    // RF33 — Renovar Access Token via Refresh Token
    // ──────────────────────────────────────────────────────────

    @Transactional
    public RefreshResponse refreshToken(RefreshRequest request) {
        Usuario usuario = refreshTokenService.validarRefreshToken(request.getRefreshToken());
        exigirAcessoNormal(usuario);

        String novoRefresh = refreshTokenService.rotacionar(request.getRefreshToken());
        String novoToken = jwtService.gerarToken(usuario, novoRefresh);
        return RefreshResponse.builder().token(novoToken).refreshToken(novoRefresh).build();
    }

    // ──────────────────────────────────────────────────────────
    // RF33 — Logout (invalida o refresh token informado)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenService.invalidarRefreshToken(request.getRefreshToken());
    }

    // ──────────────────────────────────────────────────────────
    // Helpers privados
    // ──────────────────────────────────────────────────────────

    private GoogleAuthResponse autenticarUsuario(Usuario usuario, Boolean rememberMe) {
        if (!accountAccessPolicy.acessoNormalPermitido(usuario)) {
            if (usuario.getStatusConta() == StatusConta.BLOQUEADA) {
                throw new ForbiddenException("Conta indisponível para autenticação.");
            }
            return GoogleAuthResponse.builder()
                    .status("AGUARDANDO_DADOS")
                    .statusConta(usuario.getStatusConta())
                    .id(usuario.getId())
                    .nomeGoogle(usuario.getNome())
                    .emailGoogle(usuario.getEmail())
                    .fotoGoogle(usuario.getFotoPerfil())
                    .tipoUsuario(usuario.getTipoUsuario())
                    .perfilCompleto(usuario.getPerfilCompleto())
                    .build();
        }


        String refreshToken = null;
        if (Boolean.TRUE.equals(rememberMe)) {
            refreshToken = refreshTokenService.gerarRefreshToken(usuario);
        }

        String token = jwtService.gerarToken(usuario, refreshToken);
        String avatarUrl = avatarService.resolverUrl(usuario.getId(), usuario.getFotoPerfil(), null);

        return GoogleAuthResponse.builder()
                .status("AUTENTICADO")
                .statusConta(usuario.getStatusConta())
                .token(token)
                .refreshToken(refreshToken)
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipoUsuario(usuario.getTipoUsuario())
                .perfilCompleto(usuario.getPerfilCompleto())
                .avatarUrl(avatarUrl)
                .build();
    }

    private void exigirAcessoNormal(Usuario usuario) {
        if (!accountAccessPolicy.acessoNormalPermitido(usuario)) {
            if (usuario.getStatusConta() == StatusConta.PENDENTE_VERIFICACAO_EMAIL) {
                throw new ForbiddenException("Confirme seu e-mail antes de entrar.");
            }
            if (usuario.getStatusConta() == StatusConta.PENDENTE_CONSENTIMENTO) {
                throw new ForbiddenException("Aguarde a autorização do responsável antes de entrar.");
            }
            throw new ForbiddenException("Conta indisponível para autenticação.");
        }
    }

    private void validarDadosGoogleNoSchema(String googleId, String email, String nome, String foto) {
        if (googleId == null || googleId.isBlank() || googleId.length() > 255) {
            throw falhaGoogle();
        }
        if (email == null || email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw falhaGoogle();
        }
        if (nome == null || nome.isBlank() || nome.length() > 150) {
            throw falhaGoogle();
        }
        if (foto != null && foto.length() > 255) {
            throw falhaGoogle();
        }
    }

    private UnauthorizedException falhaGoogle() {
        return new UnauthorizedException(GoogleTokenVerifier.MENSAGEM_ERRO);
    }

}
