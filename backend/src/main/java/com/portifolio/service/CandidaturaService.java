package com.portifolio.service;

import com.portifolio.dto.CandidaturaCriacaoRequest;
import com.portifolio.dto.CandidaturaRequest;
import com.portifolio.dto.CandidaturaResponse;
import com.portifolio.dto.CandidaturaVagaPaginaResponse;
import com.portifolio.dto.CandidaturaVagaResponse;
import com.portifolio.event.NotificacaoEvento;
import com.portifolio.event.AvisoResponsavelCandidaturaEvento;
import com.portifolio.exception.ConflictException;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.Candidatura;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.Funcao;
import com.portifolio.model.Usuario;
import com.portifolio.model.Vaga;
import com.portifolio.model.enums.StatusCandidatura;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class CandidaturaService {
    @org.springframework.beans.factory.annotation.Value("${app.database.legacy:false}")
    private boolean legacySchema;

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 50;
    private static final Set<StatusCandidatura> STATUS_ATIVOS =
            EnumSet.of(StatusCandidatura.PENDENTE, StatusCandidatura.EM_ANALISE);

    private final CandidaturaRepository candidaturaRepository;
    private final VagaRepository vagaRepository;
    private final PerfilArtistaRepository perfilArtistaRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final AvatarService avatarService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificacaoPersistenceService notificacaoPersistenceService;
    private final VagaPrazoPolicy vagaPrazoPolicy;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarDasMinhasVagas() {
        return listarDasMinhasVagas(null, null);
    }

    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarDasMinhasVagas(Integer page, Integer size) {
        Usuario usuario = exigirUsuarioAtual();
        exigirTipo(usuario, TipoUsuario.CONTRATANTE,
                "Somente contratantes podem consultar candidaturas recebidas.");
        return candidaturaRepository.findByVagaContratanteUsuarioId(
                        usuario.getId(), paginaOrdenadaPorId(page, size)).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Mantém a rota genérica por compatibilidade, mas nunca retorna dados globais:
     * artista recebe as próprias candidaturas e contratante recebe apenas as
     * candidaturas vinculadas às suas vagas.
     */
    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarTodos() {
        return listarTodos(null, null);
    }

    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarTodos(Integer page, Integer size) {
        Usuario usuario = exigirUsuarioAtual();
        Page<Candidatura> candidaturas = switch (usuario.getTipoUsuario()) {
            case ARTISTA -> candidaturaRepository.findByArtistaUsuarioId(
                    usuario.getId(), paginaOrdenadaPorId(page, size));
            case CONTRATANTE -> candidaturaRepository.findByVagaContratanteUsuarioId(
                    usuario.getId(), paginaOrdenadaPorId(page, size));
            default -> throw new ForbiddenException("Consulta disponível para artista e contratante.");
        };
        return candidaturas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CandidaturaVagaPaginaResponse listarPorVaga(
            Long vagaId, Integer page, Integer size) {
        Usuario usuario = exigirUsuarioAtual();
        exigirTipo(usuario, TipoUsuario.CONTRATANTE,
                "Somente contratantes podem consultar candidatos da vaga.");
        Vaga vaga = vagaRepository.findDetalhesById(vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        if (!vaga.getContratante().getUsuarioId().equals(usuario.getId())) {
            throw new ForbiddenException(
                    "Somente o proprietário da vaga pode consultar seus candidatos.");
        }

        Pageable pageable = paginaSemOrdenacao(page, size);
        Set<Long> tagsDaVaga = vaga.getFuncoes().stream()
                .map(Funcao::getId)
                .collect(Collectors.toSet());
        Set<Long> tagsParaConsulta = tagsDaVaga.isEmpty() ? Set.of(-1L) : tagsDaVaga;
        Page<Long> paginaIds = legacySchema ? candidaturaRepository.findIdsPorVagaSemTaxonomia(vagaId, STATUS_ATIVOS, pageable) : candidaturaRepository
                .findIdsPorVagaOrdenadosPorCompatibilidade(
                        vagaId, tagsParaConsulta, pageable);

        List<Long> ids = paginaIds.getContent();
        Map<Long, Candidatura> porId = ids.isEmpty()
                ? Map.of()
                : candidaturaRepository.findDetalhadasByIdIn(ids).stream()
                        .collect(Collectors.toMap(
                                Candidatura::getId,
                                candidatura -> candidatura,
                                (primeira, segunda) -> primeira,
                                LinkedHashMap::new));
        List<CandidaturaVagaResponse> content = ids.stream()
                .map(porId::get)
                .map(candidatura -> toCandidaturaVagaResponse(candidatura, tagsDaVaga))
                .toList();

        return CandidaturaVagaPaginaResponse.builder()
                .content(content)
                .page(paginaIds.getNumber())
                .size(paginaIds.getSize())
                .totalElements(paginaIds.getTotalElements())
                .totalPages(paginaIds.getTotalPages())
                .first(paginaIds.isFirst())
                .last(paginaIds.isLast())
                .hasNext(paginaIds.hasNext())
                .hasPrevious(paginaIds.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public CandidaturaResponse buscarPorId(Long id) {
        Usuario usuario = exigirUsuarioAtual();
        Candidatura candidatura = buscarCandidatura(id);
        if (!podeAcessar(candidatura, usuario)) {
            // Não expõe a existência de uma candidatura privada a terceiros.
            throw new ResourceNotFoundException("Candidatura não encontrada.");
        }
        return toResponse(candidatura);
    }

    @Transactional
    public CandidaturaResponse criar(CandidaturaCriacaoRequest request) {
        Usuario usuario = exigirUsuarioAtual();
        exigirTipo(usuario, TipoUsuario.ARTISTA, "Somente artistas podem se candidatar.");

        bloquearPar(request.getVagaId(), usuario.getId());
        Vaga vaga = vagaRepository.findByIdForUpdate(request.getVagaId())
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        if (vaga.getStatus() != StatusVaga.ABERTA) {
            throw new UnprocessableEntityException(
                    "A vaga não aceita candidaturas porque está com status " + vaga.getStatus() + ".");
        }
        if (vagaPrazoPolicy.estaVencida(vaga)) {
            throw new UnprocessableEntityException(
                    "A vaga não aceita mais candidaturas porque a data limite foi atingida.");
        }
        if (!Boolean.TRUE.equals(usuario.getPerfilCompleto())) {
            throw new UnprocessableEntityException("Complete seu perfil antes de se candidatar.");
        }

        PerfilArtista artista = perfilArtistaRepository.findById(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Artista não encontrado."));
        List<Candidatura> historico = candidaturaRepository
                .findByVagaIdAndArtistaUsuarioIdOrderByIdDesc(vaga.getId(), usuario.getId());
        if (historico.stream().anyMatch(c -> STATUS_ATIVOS.contains(c.getStatus()))) {
            throw new ConflictException("Você já possui uma candidatura ativa para esta vaga.");
        }
        if (historico.size() >= 2) {
            throw new ConflictException("Limite de recandidatura atingido para esta vaga.");
        }
        if (!historico.isEmpty() && historico.getFirst().getStatus() != StatusCandidatura.RETIRADA) {
            throw new ConflictException("Uma nova candidatura exige retirada anterior.");
        }

        Candidatura candidatura = new Candidatura();
        candidatura.setVaga(vaga);
        candidatura.setArtista(artista);
        candidatura.setMensagemApresentacao(request.getMensagemApresentacao());
        candidatura.setLinkPortfolioCandidatura(request.getLinkPortfolioCandidatura());
        candidatura.setStatus(StatusCandidatura.PENDENTE);
        candidatura.setDataCandidatura(LocalDateTime.now());
        Candidatura salva = candidaturaRepository.saveAndFlush(candidatura);
        NotificacaoEvento notificacao = new NotificacaoEvento(
                Set.of(vaga.getContratante().getUsuarioId()),
                TipoNotificacao.CANDIDATURA,
                "Nova candidatura recebida para a vaga \"" + vaga.getTitulo() + "\".",
                "dashboard-contratante.html");
        eventPublisher.publishEvent(notificacao);
        avisarResponsavelSeMenor(usuario, vaga);
        return toResponse(salva);
    }

    /**
     * A rota PUT existente passa a ter semântica exclusiva de transição de
     * estado. Vaga, artista, mensagem e link são imutáveis após a candidatura.
     */
    @Transactional
    public CandidaturaResponse atualizar(Long id, CandidaturaRequest request) {
        Usuario usuario = exigirUsuarioAtual();
        Candidatura candidatura = buscarCandidatura(id);

        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA) {
            if (!ehArtistaProprietario(candidatura, usuario)) {
                throw new ResourceNotFoundException("Candidatura não encontrada.");
            }
            bloquearPar(candidatura.getVaga().getId(), usuario.getId());
            Vaga vaga = vagaRepository.findByIdForUpdate(candidatura.getVaga().getId()).orElseThrow();
            entityManager.refresh(vaga);
            entityManager.refresh(candidatura);
            validarVinculosImutaveis(candidatura, request);
            StatusCandidatura destino = exigirStatus(request);
            if (destino != StatusCandidatura.RETIRADA) {
                throw new ForbiddenException("Artistas só podem retirar a própria candidatura.");
            }
            retirar(candidatura);
        } else {
            if (!ehContratanteProprietario(candidatura, usuario)) {
                throw new ForbiddenException("Somente o proprietário da vaga pode analisar esta candidatura.");
            }
            throw new UnprocessableEntityException(
                    "O fluxo atual não permite análise, aceitação ou rejeição formal de candidatura.");
        }

        Candidatura salva = candidaturaRepository.save(candidatura);
        notificarAlteracao(salva);
        return toResponse(salva);
    }

    private StatusCandidatura exigirStatus(CandidaturaRequest request) {
        if (request.getStatus() == null) {
            throw new UnprocessableEntityException("Informe o novo status da candidatura.");
        }
        return request.getStatus();
    }

    /**
     * DELETE é mantido por compatibilidade, mas executa retirada lógica pelo
     * próprio artista. O registro nunca é removido fisicamente.
     */
    @Transactional
    public void deletar(Long id) {
        Usuario usuario = exigirUsuarioAtual();
        exigirTipo(usuario, TipoUsuario.ARTISTA,
                "Somente o artista pode retirar uma candidatura.");
        Candidatura candidatura = buscarCandidatura(id);
        if (!ehArtistaProprietario(candidatura, usuario)) {
            throw new ResourceNotFoundException("Candidatura não encontrada.");
        }
        bloquearPar(candidatura.getVaga().getId(), usuario.getId());
        Vaga vaga = vagaRepository.findByIdForUpdate(candidatura.getVaga().getId()).orElseThrow();
        entityManager.refresh(vaga);
        entityManager.refresh(candidatura);
        retirar(candidatura);
        candidaturaRepository.save(candidatura);
        notificarAlteracao(candidatura);
    }

    private void notificarAlteracao(Candidatura candidatura) {
        boolean retirada = candidatura.getStatus() == StatusCandidatura.RETIRADA;
        Long destinatario = retirada
                ? candidatura.getVaga().getContratante().getUsuarioId()
                : candidatura.getArtista().getUsuarioId();
        String status = "Retirada";
        NotificacaoEvento evento = new NotificacaoEvento(Set.of(destinatario),
                TipoNotificacao.CANDIDATURA,
                "Candidatura à vaga \"" + candidatura.getVaga().getTitulo()
                        + "\": " + status + ".",
                "/vagas/" + candidatura.getVaga().getId() + (retirada ? "/gerenciar" : ""));
        notificacaoPersistenceService.persistirNaTransacaoAtual(evento)
                .forEach(eventPublisher::publishEvent);
    }

    private void retirar(Candidatura candidatura) {
        StatusVaga estadoVaga = candidatura.getVaga().getStatus();
        if (estadoVaga != StatusVaga.ABERTA && estadoVaga != StatusVaga.PAUSADA) {
            throw new UnprocessableEntityException(
                    "A vaga no estado " + estadoVaga + " não permite retirada ativa.");
        }
        if (!STATUS_ATIVOS.contains(candidatura.getStatus())) {
            throw transicaoInvalida(candidatura.getStatus(), StatusCandidatura.RETIRADA);
        }
        candidatura.setStatus(StatusCandidatura.RETIRADA);
    }

    private void bloquearPar(Long vagaId, Long artistaId) {
        String chave = "rf06:" + vagaId + ":" + artistaId;
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(:chave as text), 0))")
                .setParameter("chave", chave)
                .getSingleResult();
    }

    private void avisarResponsavelSeMenor(Usuario usuario, Vaga vaga) {
        if (usuario.getDataNascimento() == null
                || usuario.getStatusConta() != StatusConta.ATIVA) {
            return;
        }
        int idade = Period.between(usuario.getDataNascimento(), vagaPrazoPolicy.hoje()).getYears();
        if (idade < 14 || idade >= 18) {
            return;
        }
        ResponsavelLegal responsavel = usuario.getResponsavelLegal();
        if (responsavel != null && responsavel.getDataConsentimento() != null
                && !Boolean.TRUE.equals(responsavel.getConsentimentoRevogado())
                && responsavel.getEmailResponsavel() != null
                && !responsavel.getEmailResponsavel().isBlank()) {
            eventPublisher.publishEvent(new AvisoResponsavelCandidaturaEvento(
                    responsavel.getEmailResponsavel(), vaga.getId()));
        }
    }

    private UnprocessableEntityException transicaoInvalida(
            StatusCandidatura atual, StatusCandidatura destino) {
        return new UnprocessableEntityException(
                "Transição de candidatura inválida: " + atual + " -> " + destino + ".");
    }

    private void validarVinculosImutaveis(Candidatura candidatura, CandidaturaRequest request) {
        if (!candidatura.getVaga().getId().equals(request.getVagaId())
                || !candidatura.getArtista().getUsuarioId().equals(request.getArtistaId())) {
            throw new UnprocessableEntityException(
                    "Vaga e artista da candidatura não podem ser alterados.");
        }
    }

    private Candidatura buscarCandidatura(Long id) {
        return candidaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada."));
    }

    private Usuario exigirUsuarioAtual() {
        return authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new ForbiddenException("Autenticação obrigatória."));
    }

    private void exigirTipo(Usuario usuario, TipoUsuario tipo, String mensagem) {
        if (usuario.getTipoUsuario() != tipo) {
            throw new ForbiddenException(mensagem);
        }
    }

    private boolean podeAcessar(Candidatura candidatura, Usuario usuario) {
        return usuario.getTipoUsuario() == TipoUsuario.ARTISTA
                ? ehArtistaProprietario(candidatura, usuario)
                : ehContratanteProprietario(candidatura, usuario);
    }

    private boolean ehArtistaProprietario(Candidatura candidatura, Usuario usuario) {
        return candidatura.getArtista().getUsuarioId().equals(usuario.getId());
    }

    private boolean ehContratanteProprietario(Candidatura candidatura, Usuario usuario) {
        return candidatura.getVaga().getContratante().getUsuarioId().equals(usuario.getId());
    }

    private CandidaturaVagaResponse toCandidaturaVagaResponse(
            Candidatura candidatura, Set<Long> tagsDaVaga) {
        PerfilArtista artista = candidatura.getArtista();
        Set<Long> funcaoIds = artista.getFuncoes().stream()
                .map(Funcao::getId)
                .collect(Collectors.toSet());
        Set<Long> funcoesCoincidentes = funcaoIds.stream()
                .filter(tagsDaVaga::contains)
                .collect(Collectors.toSet());

        return CandidaturaVagaResponse.builder()
                .candidaturaId(candidatura.getId())
                .artistaId(artista.getUsuarioId())
                .username(artista.getUsuario().getUsername())
                .nomeArtista(artista.getUsuario().getNome())
                .biografia(artista.getBiografia())
                .localizacao(artista.getLocalizacao())
                .urlPortfolio(artista.getUrlPortfolio())
                .avatarUrl(avatarService.resolverUrl(
                        artista.getUsuarioId(),
                        artista.getUsuario().getFotoPerfil(),
                        null))
                .funcaoIds(funcaoIds)
                .funcoesCoincidentes(funcoesCoincidentes)
                .quantidadeFuncoesCoincidentes(funcoesCoincidentes.size())
                .mensagemApresentacao(candidatura.getMensagemApresentacao())
                .linkPortfolioCandidatura(candidatura.getLinkPortfolioCandidatura())
                .status(candidatura.getStatus())
                .dataCandidatura(candidatura.getDataCandidatura())
                .build();
    }

    private Pageable paginaOrdenadaPorId(Integer page, Integer size) {
        return PageRequest.of(normalizarPagina(page), normalizarTamanho(size),
                Sort.by(Sort.Direction.ASC, "id"));
    }

    private Pageable paginaSemOrdenacao(Integer page, Integer size) {
        return PageRequest.of(normalizarPagina(page), normalizarTamanho(size));
    }

    private int normalizarPagina(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new IllegalArgumentException("Página não pode ser negativa.");
        }
        return page;
    }

    private int normalizarTamanho(Integer size) {
        if (size == null) {
            return TAMANHO_PADRAO;
        }
        if (size < 1) {
            throw new IllegalArgumentException("Tamanho da página deve ser positivo.");
        }
        return Math.min(size, TAMANHO_MAXIMO);
    }

    private CandidaturaResponse toResponse(Candidatura candidatura) {
        return CandidaturaResponse.builder()
                .id(candidatura.getId())
                .vagaId(candidatura.getVaga().getId())
                .artistaId(candidatura.getArtista().getUsuarioId())
                .mensagemApresentacao(candidatura.getMensagemApresentacao())
                .linkPortfolioCandidatura(candidatura.getLinkPortfolioCandidatura())
                .status(candidatura.getStatus())
                .dataCandidatura(candidatura.getDataCandidatura())
                .build();
    }
}
