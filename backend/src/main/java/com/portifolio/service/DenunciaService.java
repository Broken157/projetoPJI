package com.portifolio.service;

import com.portifolio.dto.DenunciaRequest;
import com.portifolio.dto.DenunciaResponse;
import com.portifolio.dto.DenunciaResponse.Categoria;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.UnauthorizedException;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.DenunciaRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DenunciaService {
    // Vocabulário do enum oficial tipo_violacao_enum, não do frontend demonstrativo.
    private static final Set<String> VIOLACOES = Set.of(
            "PLAGIO DE IMAGEM", "PLAGIO DE AUDIO", "COPIA DE BIOGRAFIA", "OUTRO");
    private final DenunciaRepository denuncias;
    private final AuthenticatedUserResolver autenticado;
    private final UsuarioRepository usuarios;
    private final PerfilPublicoService perfis;
    private final VagaService vagas;

    @Transactional
    public DenunciaResponse registrar(DenunciaRequest request) {
        Long denunciante = usuario().getId();
        String motivo = request.motivo().strip();
        String descricao = request.descricao() == null ? null : request.descricao().strip();
        Categoria categoria;
        if (request.tipoAlvo() == DenunciaRequest.TipoAlvo.VAGA) {
            // RF05 conserva descoberta por ID, ownership e visibilidade da vaga.
            vagas.buscarPorId(request.alvoId());
            categoria = Categoria.CONTEUDO;
        } else {
            if (!VIOLACOES.contains(motivo)) {
                throw new IllegalArgumentException("Para perfis, informe um tipo de violação de autoria válido.");
            }
            if (descricao == null || descricao.isBlank()) {
                throw new IllegalArgumentException("A descrição detalhada é obrigatória para denúncia de plágio.");
            }
            TipoUsuario tipo = request.tipoAlvo() == DenunciaRequest.TipoAlvo.PERFIL_ARTISTA
                    ? TipoUsuario.ARTISTA : TipoUsuario.CONTRATANTE;
            usuarios.findById(request.alvoId()).filter(u -> u.getStatusConta() == StatusConta.ATIVA)
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil publico nao encontrado."));
            // RF10 impede descobrir perfil de menor ou de outro tipo através da denúncia.
            perfis.buscar(tipo, request.alvoId());
            categoria = Categoria.PLAGIO;
        }
        long id = denuncias.inserir(denunciante, request, motivo, descricao);
        return denuncias.buscarPropria(denunciante, categoria, id).orElseThrow();
    }

    public DenunciaResponse buscar(Categoria categoria, Long id) {
        Long denunciante = usuario().getId();
        if (id <= 0) throw new IllegalArgumentException("O ID deve ser positivo.");
        return denuncias.buscarPropria(denunciante, categoria, id)
                .orElseThrow(() -> new ResourceNotFoundException("Denúncia não encontrada."));
    }

    public DenunciaResponse.Pagina listar(int page, int size) {
        Long denunciante = usuario().getId();
        if (page < 0 || size < 1) throw new IllegalArgumentException("page deve ser não negativo e size deve ser positivo.");
        size = Math.min(size, 50);
        long total = denuncias.contarProprias(denunciante);
        long offset = (long) page * size;
        var content = denuncias.listarProprias(denunciante, size, offset);
        return new DenunciaResponse.Pagina(content, page, size, total, (int) Math.ceil((double) total / size),
                offset + size < total, page > 0);
    }

    private Usuario usuario() {
        Usuario usuario = autenticado.usuarioAtual()
                .orElseThrow(() -> new UnauthorizedException("Autenticação necessária."));
        if (usuario.getStatusConta() != StatusConta.ATIVA
                || (usuario.getTipoUsuario() != TipoUsuario.ARTISTA && usuario.getTipoUsuario() != TipoUsuario.CONTRATANTE)) {
            throw new ForbiddenException("Uma conta ativa de artista ou contratante é necessária para denunciar.");
        }
        return usuario;
    }
}
