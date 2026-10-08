package com.portifolio.service;

import com.portifolio.dto.PerfilArtistaRequest;
import com.portifolio.dto.PerfilArtistaResponse;
import com.portifolio.exception.ConflictException;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.Funcao;
import com.portifolio.model.Usuario;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilArtistaService {
    @org.springframework.beans.factory.annotation.Value("${app.database.legacy:false}")
    private boolean legacySchema;

    private final PerfilArtistaRepository perfilArtistaRepository;
    private final com.portifolio.repository.UsuarioRepository usuarioRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final PerfilProfissionalService profissional;
    private final AvatarService avatarService; // RF34
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final PerfilCompletoService perfilCompletoService;
    private final com.portifolio.validation.ConteudoPublicoValidator conteudoPublico;

    @Transactional(readOnly = true)
    public List<PerfilArtistaResponse> listarTodos() {
        return perfilArtistaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PerfilArtistaResponse buscarPorId(Long id) {
        exigirArtistaAtual(id);
        PerfilArtista perfil = perfilArtistaRepository.buscarProfissional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de artista nao encontrado."));
        return toResponse(perfil);
    }

    @Transactional
    public PerfilArtistaResponse criar(PerfilArtistaRequest request) {
        Usuario usuarioAtual = exigirArtistaAtual(request.getUsuarioId());
        usuarioAtual = bloquear(usuarioAtual);
        if (perfilArtistaRepository.existsById(usuarioAtual.getId())) {
            throw new ConflictException("Perfil de artista ja cadastrado para este usuario.");
        }
        PerfilArtista perfil = new PerfilArtista();
        perfil.setUsuario(usuarioAtual);
        preencherPerfil(perfil, request);
        perfil.setUltimaAtualizacao(LocalDateTime.now());
        PerfilArtista salvo = perfilArtistaRepository.saveAndFlush(perfil);
        perfilCompletoService.recalcular(usuarioAtual);
        return toResponse(salvo);
    }

    @Transactional
    public PerfilArtistaResponse atualizar(Long id, PerfilArtistaRequest request) {
        Usuario usuarioAtual = exigirArtistaAtual(id);
        validarIdDoPayload(id, request.getUsuarioId());
        usuarioAtual = bloquear(usuarioAtual);
        PerfilArtista perfil = perfilArtistaRepository.buscarProfissional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de artista nao encontrado."));
        preencherPerfil(perfil, request);
        perfil.setUltimaAtualizacao(LocalDateTime.now());
        PerfilArtista salvo = perfilArtistaRepository.saveAndFlush(perfil);
        perfilCompletoService.recalcular(usuarioAtual);
        return toResponse(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        Usuario usuarioAtual = exigirArtistaAtual(id);
        PerfilArtista perfil = perfilArtistaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de artista nao encontrado."));
        perfilArtistaRepository.delete(perfil);
        perfilArtistaRepository.flush();
        perfilCompletoService.recalcular(usuarioAtual);
    }

    private Usuario exigirArtistaAtual(Long usuarioId) {
        Usuario atual = authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new ForbiddenException("Autenticação obrigatória."));
        if (usuarioId != null && !atual.getId().equals(usuarioId)) {
            throw new ForbiddenException("Você só pode alterar o próprio perfil.");
        }
        if (atual.getTipoUsuario() != com.portifolio.model.enums.TipoUsuario.ARTISTA) {
            throw new ForbiddenException("Somente artistas podem alterar perfil de artista.");
        }
        return atual;
    }

    private void validarIdDoPayload(Long id, Long usuarioId) {
        if (usuarioId != null && !id.equals(usuarioId)) {
            throw new ForbiddenException("O usuário do payload deve corresponder ao perfil autenticado.");
        }
    }

    private void preencherPerfil(PerfilArtista perfil, PerfilArtistaRequest request) {
        conteudoPublico.texto(request.getBiografia(), "Biografia", 5000, false);
        conteudoPublico.texto(request.getCidade(), "Cidade", 100, false);
        conteudoPublico.texto(request.getLocalizacao(), "Localização", 150, false);
        conteudoPublico.url(request.getUrlPortfolio(), "Portfólio", 255, false);
        conteudoPublico.url(request.getBannerUrl(), "Banner", 255, true);
        var local = com.portifolio.validation.LocalizacaoArtista.deRequest(
                request.getCidade(), request.getEstado(), request.getLocalizacao());
        if (!legacySchema && perfil.getTipoPerfilArtistico() == null && request.getTipoPerfilArtistico() == null)
            throw new com.portifolio.exception.UnprocessableEntityException("Informe tipoPerfilArtistico.");
        var selecoes = legacySchema ? null : profissional.validar(perfil, request);
        perfil.setBiografia(request.getBiografia());
        perfil.setCidade(local.cidade());
        perfil.setEstado(local.estado());
        perfil.setUrlPortfolio(request.getUrlPortfolio());
        if (legacySchema) {
            if (request.getAreas()!=null || request.getAreaPrincipalId()!=null || (request.getFuncaoIds()!=null && !request.getFuncaoIds().isEmpty()) || request.getTipoPerfilArtistico()!=null || request.getRaioAtuacao()!=null)
                throw new com.portifolio.exception.UnprocessableEntityException("BLOQUEADA POR SCHEMA DO BANCO: campos profissionais avançados indisponíveis.");
            perfil.setBannerUrl(request.getBannerUrl()); return;
        }
        if (request.getTipoPerfilArtistico() != null) perfil.setTipoPerfilArtistico(request.getTipoPerfilArtistico());
        if (request.getRaioAtuacao() != null) perfil.setRaioAtuacao(request.getRaioAtuacao());
        perfil.setBannerUrl(request.getBannerUrl());
        if (request.getDisponivelOportunidades()!=null) perfil.setDisponivelOportunidades(request.getDisponivelOportunidades());
        profissional.reconciliar(perfil, selecoes);
    }

    private Usuario bloquear(Usuario usuario) {
        Usuario bloqueado = usuarioRepository.findByIdForUpdate(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        // O resolver pode tê-lo carregado antes de aguardar o lock. Releitura evita
        // sobrescrever campos da conta alterados durante essa espera.
        entityManager.refresh(bloqueado);
        return bloqueado;
    }

    private PerfilArtistaResponse toResponse(PerfilArtista perfil) {
        Set<Long> funcaoIds = perfil.getFuncoes().stream()
                .map(Funcao::getId)
                .collect(Collectors.toSet());

        // Avatar centralizado em usuarios.foto_perfil_url; fallback DiceBear.
        String avatarUrl = avatarService.resolverUrl(
                perfil.getUsuarioId(),
                perfil.getUsuario().getFotoPerfil(),
                null
        );

        return PerfilArtistaResponse.builder()
                .usuarioId(perfil.getUsuarioId())
                .tipoPerfilArtistico(perfil.getTipoPerfilArtistico())
                .raioAtuacao(perfil.getRaioAtuacao())
                .areaPrincipalId(perfil.getAreas().stream().filter(com.portifolio.model.PerfilArtistaArea::isPrincipal)
                        .map(area -> area.getArea().getId()).findFirst().orElse(null))
                .disponivelOportunidades(perfil.getDisponivelOportunidades())
                .biografia(perfil.getBiografia())
                .localizacao(perfil.getLocalizacao())
                .cidade(perfil.getCidade()).estado(perfil.getEstado())
                .urlPortfolio(perfil.getUrlPortfolio())
                .nivelMedalha(null)
                .scoreEngajamento(null)
                .bannerUrl(perfil.getBannerUrl())
                .ultimaAtualizacao(perfil.getUltimaAtualizacao())
                .funcaoIds(funcaoIds)
                .areas(perfil.getAreas().stream().sorted(java.util.Comparator.comparing(a -> a.getArea().getId()))
                        .map(a -> new com.portifolio.dto.PerfilArtistaAreaResponse(a.getArea().getId(), a.isPrincipal(),
                                a.getNivelExperiencia(), a.getFuncoes().stream().map(Funcao::getId).collect(Collectors.toSet()),
                                a.getEspecializacoes().stream().map(e -> e.getId()).collect(Collectors.toSet()),
                                a.getUltimaAtualizacao())).toList())
                .avatarUrl(avatarUrl)
                .build();
    }
}
