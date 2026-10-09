package com.portifolio.service;

import com.portifolio.dto.UsuarioAtualizacaoRequest;
import com.portifolio.dto.UsuarioRequest;
import com.portifolio.dto.UsuarioResponse;
import com.portifolio.exception.ConflictException;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.Usuario;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.validation.PasswordPolicy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final PerfilCompletoService perfilCompletoService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordPolicy passwordPolicy;
    private final com.portifolio.validation.ConteudoPublicoValidator conteudoPublico;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.portifolio.security.JwtService jwtService;
    private final jakarta.validation.Validator validator;
    private final java.time.Clock clock;

    @Transactional(readOnly = true)
    public UsuarioResponse buscarAtual() {
        return toResponseCompleto(usuarioAtual());
    }

    @Transactional
    public UsuarioResponse atualizarAtual(UsuarioAtualizacaoRequest request) {
        Usuario usuario = usuarioAtualBloqueado();
        validarEmailSemAlteracao(request.getEmail(), usuario);
        validarTelefone(request.getTelefone());
        bloquearAlteracaoResponsavel(usuario, request.getNomeResponsavel(),
                request.getTelefoneResponsavel(), request.getEmailResponsavel(), request.getVinculoResponsavel());

        conteudoPublico.texto(request.getNome(), "Nome público", 150, true);
        usuario.setNome(request.getNome());
        usuario.setTelefone(request.getTelefone());
        // dataNascimento permanece imutável mesmo que o cliente legado a envie.

        boolean senhaAlterada = atualizarSenhaSeSolicitada(usuario, request);
        Usuario salvo = usuarioRepository.save(usuario);
        perfilCompletoService.recalcular(salvo);
        if (senhaAlterada) {
            refreshTokenService.invalidarTodosDoUsuario(salvo.getId());
            jwtService.revogarTodosDoUsuario(salvo.getId());
        }
        return toResponseCompleto(salvo);
    }

    @Transactional
    public void deletarAtual() {
        usuarioAtual();
        throw exclusaoDeContaIndisponivel();
    }

    /** Lista compatível, sem expor e-mail, telefone, nascimento ou responsável. */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponsePublico)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        // Dados de terceiros pertencem ao contrato público RF10, inclusive a política de menores.
        return toResponseCompleto(exigirProprioUsuario(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("E-mail já cadastrado.");
        }
        Usuario usuario = new Usuario();
        preencherUsuarioNaCriacao(usuario, request);
        usuario.setSenha(passwordPolicy.encode(request.getSenha()));
        usuario.setPerfilCompleto(false);
        usuario.setDataCriacao(LocalDateTime.now());
        return toResponseCompleto(usuarioRepository.save(usuario));
    }

    /** Rota legada por ID, limitada aos mesmos campos seguros de /me. */
    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        exigirProprioUsuario(id);
        Usuario usuario = usuarioAtualBloqueado();
        validarEmailSemAlteracao(request.getEmail(), usuario);
        validarTelefone(request.getTelefone());
        bloquearAlteracaoResponsavel(usuario, request.getNomeResponsavel(),
                request.getTelefoneResponsavel(), request.getEmailResponsavel(), request.getVinculoResponsavel());
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            throw new UnprocessableEntityException(
                    "Troque a senha por /api/usuarios/me informando a senha atual.");
        }
        conteudoPublico.texto(request.getNome(), "Nome público", 150, true);
        usuario.setNome(request.getNome());
        usuario.setTelefone(request.getTelefone());
        Usuario salvo = usuarioRepository.save(usuario);
        perfilCompletoService.recalcular(salvo);
        return toResponseCompleto(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        exigirProprioUsuario(id);
        throw exclusaoDeContaIndisponivel();
    }

    private UnprocessableEntityException exclusaoDeContaIndisponivel() {
        return new UnprocessableEntityException(
                "Exclusão de conta temporariamente indisponível até a implementação segura do RF22.");
    }

    private boolean atualizarSenhaSeSolicitada(Usuario usuario, UsuarioAtualizacaoRequest request) {
        if (request.getNovaSenha() == null || request.getNovaSenha().isEmpty()) {
            return false;
        }
        alterarSenha(usuario, request.getSenhaAtual(), request.getNovaSenha());
        return true;
    }

    @Transactional
    public void alterarSenhaAtual(com.portifolio.dto.AlteracaoSenhaRequest request) {
        Usuario usuario = usuarioAtualBloqueado();
        alterarSenha(usuario, request.getSenhaAtual(), request.getNovaSenha());
        usuarioRepository.save(usuario);
        refreshTokenService.invalidarTodosDoUsuario(usuario.getId());
        jwtService.revogarTodosDoUsuario(usuario.getId());
    }

    private void alterarSenha(Usuario usuario, String senhaAtual, String novaSenha) {
        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            throw new UnprocessableEntityException(
                    "Contas exclusivamente Google não podem criar senha por este fluxo.");
        }
        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new UnprocessableEntityException("Informe a senha atual para definir uma nova senha.");
        }
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new ForbiddenException("Senha atual incorreta.");
        }
        usuario.setSenha(passwordPolicy.encode(novaSenha));
    }

    private void validarEmailSemAlteracao(String email, Usuario usuario) {
        if (email != null && email.trim().equalsIgnoreCase(usuario.getEmail().trim())) return;
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(existente -> !existente.getId().equals(usuario.getId()))
                .ifPresent(existente -> {
                    throw new ConflictException("E-mail já cadastrado.");
                });
        throw new UnprocessableEntityException(
                "Troca de e-mail indisponível até existir suporte durável à verificação do novo endereço.");
    }

    private void bloquearAlteracaoResponsavel(Usuario usuario, String nome, String telefone,
            String email, String vinculo) {
        // C07/D05: um único registro não preserva responsável validado + alteração pendente/trilha.
        // Campos iguais vindos do cliente legado são aceitos, mas nunca reescritos.
        if (vinculo != null || alterado(nome, usuario.getNomeResponsavel(), false)
                || alterado(telefone, usuario.getTelefoneResponsavel(), false)
                || alterado(email, usuario.getEmailResponsavel(), true)) {
            throw new UnprocessableEntityException(
                    "Alteração do responsável indisponível até existir suporte durável à revalidação e trilha.");
        }
    }

    private boolean alterado(String enviado, String atual, boolean ignorarCaixa) {
        return enviado != null && (atual == null || (ignorarCaixa
                ? !enviado.equalsIgnoreCase(atual) : !enviado.equals(atual)));
    }

    private void validarTelefone(String telefone) {
        if (!validator.validateValue(com.portifolio.dto.CadastroDadosRequest.class, "telefone", telefone).isEmpty()) {
            throw new IllegalArgumentException("Telefone inválido; informe DDD e número.");
        }
    }

    @Transactional
    public void alterarTelefoneAtual(com.portifolio.dto.TelefoneAtualizacaoRequest request) {
        Usuario usuario = usuarioAtualBloqueado();
        validarTelefone(request.telefone());
        usuario.setTelefone(request.telefone());
        usuarioRepository.save(usuario);
        perfilCompletoService.recalcular(usuario);
    }

    @Transactional(readOnly = true)
    public com.portifolio.dto.ResponsavelAtualResponse buscarResponsavelAtual() {
        Usuario usuario = usuarioAtual();
        if (usuario.getTipoUsuario() != com.portifolio.model.enums.TipoUsuario.ARTISTA
                || usuario.getDataNascimento() == null
                || Period.between(usuario.getDataNascimento(), LocalDate.now(clock)).getYears() < 14
                || Period.between(usuario.getDataNascimento(), LocalDate.now(clock)).getYears() >= 18) {
            throw new UnprocessableEntityException("Responsável legal aplicável somente a artista de 14 a 17 anos.");
        }
        var responsavel = usuario.getResponsavelLegal();
        if (responsavel == null) throw new ResourceNotFoundException("Responsável legal não encontrado.");
        return new com.portifolio.dto.ResponsavelAtualResponse(responsavel.getNomeResponsavel(),
                responsavel.getEmailResponsavel(), responsavel.getTelefoneResponsavel(),
                responsavel.getDataConsentimento(), Boolean.TRUE.equals(responsavel.getConsentimentoRevogado()));
    }

    private Usuario exigirProprioUsuario(Long id) {
        Usuario atual = usuarioAtual();
        if (!atual.getId().equals(id)) {
            throw new ForbiddenException("Você só pode alterar a própria conta.");
        }
        return atual;
    }

    private void preencherUsuarioNaCriacao(Usuario usuario, UsuarioRequest request) {
        conteudoPublico.texto(request.getNome(), "Nome público", 150, true);
        usuario.setNome(request.getNome());
        usuario.setDataNascimento(request.getDataNascimento());
        usuario.setTelefone(request.getTelefone());
        usuario.setEmail(request.getEmail());
        usuario.setTipoUsuario(request.getTipoUsuario());
        usuario.setNomeResponsavel(request.getNomeResponsavel());
        usuario.setTelefoneResponsavel(request.getTelefoneResponsavel());
        usuario.setEmailResponsavel(request.getEmailResponsavel());
    }

    private UsuarioResponse toResponseCompleto(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .dataNascimento(usuario.getDataNascimento())
                .telefone(usuario.getTelefone())
                .email(usuario.getEmail())
                .tipoUsuario(usuario.getTipoUsuario())
                .perfilCompleto(usuario.getPerfilCompleto())
                .dataCriacao(usuario.getDataCriacao())
                .nomeResponsavel(usuario.getNomeResponsavel())
                .telefoneResponsavel(usuario.getTelefoneResponsavel())
                .emailResponsavel(usuario.getEmailResponsavel())
                .build();
    }

    private UsuarioResponse toResponsePublico(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .tipoUsuario(usuario.getTipoUsuario())
                .perfilCompleto(usuario.getPerfilCompleto())
                .dataCriacao(usuario.getDataCriacao())
                .build();
    }

    private Usuario usuarioAtual() {
        return authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
    }

    private Usuario usuarioAtualBloqueado() {
        Long id = usuarioAtual().getId();
        Usuario usuario = usuarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
        entityManager.refresh(usuario);
        return usuario;
    }
}
