package com.portifolio.service;

import com.portifolio.dto.VagaBuscaFiltro;
import com.portifolio.dto.ContratantePublicoResponse;
import com.portifolio.dto.VagaAtualizacaoRequest;
import com.portifolio.dto.VagaCancelamentoRequest;
import com.portifolio.dto.VagaListagemResponse;
import com.portifolio.dto.VagaRequest;
import com.portifolio.dto.VagaResponse;
import com.portifolio.dto.VagaStatusAcaoRequest;
import com.portifolio.event.NotificacaoEvento;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.Candidatura;
import com.portifolio.model.Especializacao;
import com.portifolio.model.LogVagaCancelada;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Funcao;
import com.portifolio.model.Usuario;
import com.portifolio.model.Vaga;
import com.portifolio.model.enums.StatusCandidatura;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.LogVagaCanceladaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.FuncaoRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.repository.specification.VagaSpecifications;
import com.portifolio.security.AuthenticatedUserResolver;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VagaService {
    @org.springframework.beans.factory.annotation.Value("${app.database.legacy:false}")
    private boolean legacySchema;
    @org.springframework.beans.factory.annotation.Autowired
    private com.portifolio.repository.OfficialLocalVagaRepository officialLocalVagas;

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 50;
    private static final int LOTE_NOTIFICACOES_STATUS = 100;

    private final VagaRepository vagaRepository;
    private final PerfilContratanteRepository perfilContratanteRepository;
    private final FuncaoRepository funcaoRepository;
    private final com.portifolio.repository.EspecializacaoRepository especializacaoRepository;
    private final com.portifolio.repository.CategoriaAfirmativaRepository categoriaAfirmativaRepository;
    private final com.portifolio.repository.AreaArtisticaRepository areaArtisticaRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final LogVagaCanceladaRepository logVagaCanceladaRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final AvatarService avatarService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificacaoPersistenceService notificacaoPersistenceService;
    private final VagaPrazoPolicy vagaPrazoPolicy;

    // RF03 — Listagem e busca paginada (cursor-based) de vagas ABERTAS. Endpoint público.
    // RF03 Fase 2 — se o artista autenticado tiver candidaturas em vagas CANCELADA,
    // elas voltam em uma seção separada da resposta.
    @Transactional(readOnly = true)
    public VagaListagemResponse listar(VagaBuscaFiltro filtro) {
        validarFiltros(filtro);
        int tamanho = normalizarTamanho(filtro.getSize());
        Usuario usuarioAtual = authenticatedUserResolver.usuarioAtual().orElse(null);

        Specification<Vaga> filtrosPublicos = Specification
                .where(VagaSpecifications.comStatus(StatusVaga.ABERTA))
                .and(VagaSpecifications.prazoAindaValido(vagaPrazoPolicy.hoje()))
                .and(VagaSpecifications.buscaTituloOuContratante(filtro.getBusca()))
                .and(VagaSpecifications.tituloContem(filtro.getTitulo()))
                .and(VagaSpecifications.empresaContem(filtro.getEmpresa()))
                .and(VagaSpecifications.cidadeIgual(filtro.getCidade()))
                .and(VagaSpecifications.estadoIgual(filtro.getEstado()))
                .and(VagaSpecifications.modeloTrabalhoIgual(filtro.getModeloTrabalho()))
                .and(VagaSpecifications.tipoContratoIgual(filtro.getTipoContrato()))
                .and(VagaSpecifications.remuneracaoMinima(filtro.getFaixaSalarialMin()))
                .and(VagaSpecifications.remuneracaoMaxima(filtro.getFaixaSalarialMax()))
                .and(VagaSpecifications.areaAtuacaoContem(filtro.getAreaAtuacao()))
                .and(VagaSpecifications.areaIgual(filtro.getAreaId()))
                .and(VagaSpecifications.comAlgumaFuncao(filtro.getFuncaoIds()))
                .and(VagaSpecifications.comAlgumaEspecializacao(filtro.getEspecializacaoIds()))
                .and(VagaSpecifications.experienciaIgual(filtro.getExperiencia()))
                .and(VagaSpecifications.abrangenciaIgual(filtro.getAbrangencia()))
                .and(VagaSpecifications.formaRemuneracaoIgual(filtro.getFormaRemuneracao()))
                .and(VagaSpecifications.afirmativa(filtro.getAfirmativa()))
                .and(VagaSpecifications.comAlgumaCategoriaAfirmativa(
                        filtro.getCategoriaAfirmativaIds()));
        Specification<Vaga> spec = filtrosPublicos.and(
                VagaSpecifications.idMaiorQue(filtro.getCursor()));

        Pageable pageable = PageRequest.of(0, tamanho + 1, Sort.by(Sort.Direction.ASC, "id"));
        List<Vaga> bruto = vagaRepository.findAll(spec, pageable).getContent();
        long totalElements = vagaRepository.count(filtrosPublicos);

        boolean hasMore = bruto.size() > tamanho;
        List<Vaga> pagina = hasMore ? bruto.subList(0, tamanho) : bruto;

        // O feed e publico mesmo quando o proprietario esta autenticado.
        // Dados administrativos permanecem no detalhe/gerenciamento da propria vaga.
        List<VagaResponse> content = carregarComFuncoesEContratante(pagina, null);
        Long nextCursor = hasMore ? pagina.get(pagina.size() - 1).getId() : null;

        PaginaCanceladas canceladas = buscarVagasCanceladasParaArtistaLogado(
                usuarioAtual, filtro.getCursorCanceladas(), tamanho);

        return VagaListagemResponse.builder()
                .content(content)
                .totalElements(totalElements)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .vagasCanceladasComCandidatura(canceladas.content())
                .nextCursorCanceladas(canceladas.nextCursor())
                .hasMoreCanceladas(canceladas.hasMore())
                .build();
    }

    @Transactional(readOnly = true)
    public VagaListagemResponse listarMinhas(Long cursor, Integer size) {
        Usuario usuario = exigirContratanteAtual();
        validarCursor(cursor, "Cursor");
        int tamanho = normalizarTamanho(size);

        Specification<Vaga> spec = Specification
                .where(VagaSpecifications.doContratante(usuario.getId()))
                .and(VagaSpecifications.idMaiorQue(cursor));

        Pageable pageable = PageRequest.of(0, tamanho + 1, Sort.by(Sort.Direction.ASC, "id"));
        List<Vaga> bruto = vagaRepository.findAll(spec, pageable).getContent();
        boolean hasMore = bruto.size() > tamanho;
        List<Vaga> pagina = hasMore ? bruto.subList(0, tamanho) : bruto;
        List<VagaResponse> content = carregarComFuncoesEContratante(pagina, usuario.getId());
        Long nextCursor = hasMore ? pagina.get(pagina.size() - 1).getId() : null;

        return VagaListagemResponse.builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .vagasCanceladasComCandidatura(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public VagaListagemResponse listarSimilares(Long vagaId, Long cursor, Integer size) {
        validarCursor(cursor, "Cursor");
        int tamanho = normalizarTamanho(size);
        Vaga origem = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        if (origem.getStatus() == StatusVaga.RASCUNHO) {
            throw new ResourceNotFoundException("Vaga não encontrada.");
        }

        Set<Long> funcaoIds = origem.getFuncoes().stream().map(Funcao::getId).collect(Collectors.toSet());
        if (funcaoIds.isEmpty()) {
            return paginaVazia();
        }

        Specification<Vaga> spec = Specification
                .where(VagaSpecifications.comStatus(StatusVaga.ABERTA))
                .and(VagaSpecifications.prazoAindaValido(vagaPrazoPolicy.hoje()))
                .and(VagaSpecifications.idMaiorQue(cursor))
                .and(VagaSpecifications.idDiferente(vagaId))
                .and(VagaSpecifications.comAlgumaFuncao(funcaoIds));

        Pageable pageable = PageRequest.of(0, tamanho + 1, Sort.by(Sort.Direction.ASC, "id"));
        List<Vaga> bruto = vagaRepository.findAll(spec, pageable).getContent();
        boolean hasMore = bruto.size() > tamanho;
        List<Vaga> pagina = hasMore ? bruto.subList(0, tamanho) : bruto;

        return VagaListagemResponse.builder()
                .content(carregarComFuncoesEContratante(pagina, null))
                .nextCursor(hasMore ? pagina.get(pagina.size() - 1).getId() : null)
                .hasMore(hasMore)
                .vagasCanceladasComCandidatura(List.of())
                .build();
    }

    // RF03 Fase 2. O artista NUNCA é identificado por parâmetro do cliente —
    // sempre resolvido a partir do token JWT já validado pelo JwtAuthFilter (RNF08).
    private PaginaCanceladas buscarVagasCanceladasParaArtistaLogado(
            Usuario usuario, Long cursor, int tamanho) {
        if (usuario == null || usuario.getTipoUsuario() != TipoUsuario.ARTISTA) {
            return PaginaCanceladas.vazia();
        }

        Pageable limite = PageRequest.of(0, tamanho + 1);
        List<Long> bruto = candidaturaRepository.findVagaIdsDoArtistaPorStatusAposCursor(
                usuario.getId(), StatusVaga.CANCELADA, cursor, limite);
        boolean hasMore = bruto.size() > tamanho;
        List<Long> vagaIds = hasMore ? bruto.subList(0, tamanho) : bruto;
        Long nextCursor = hasMore ? vagaIds.get(vagaIds.size() - 1) : null;
        return new PaginaCanceladas(
                carregarPorIds(vagaIds, null, true), nextCursor, hasMore);
    }

    // Segunda consulta: busca funcoes+contratante para o conjunto de IDs já paginado (RNF05).
    // Pagination + fetch join de coleção não é seguro na mesma query.
    private List<VagaResponse> carregarComFuncoesEContratante(
            List<Vaga> pagina, Long contratanteAtualId) {
        if (pagina.isEmpty()) {
            return List.of();
        }
        List<Long> ids = pagina.stream().map(Vaga::getId).toList();
        return carregarPorIds(ids, contratanteAtualId, false);
    }

    private List<VagaResponse> carregarPorIds(
            List<Long> ids, Long contratanteAtualId, boolean cancelada) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Vaga> vagasComFuncoes = vagaRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Vaga::getId, v -> v, (a, b) -> a, LinkedHashMap::new));

        return ids.stream()
                .map(vagasComFuncoes::get)
                .map(v -> toResponse(v, cancelada, contratanteAtualId))
                .collect(Collectors.toList());
    }

    private VagaListagemResponse paginaVazia() {
        return VagaListagemResponse.builder()
                .content(List.of())
                .hasMore(false)
                .vagasCanceladasComCandidatura(List.of())
                .build();
    }

    private void validarFiltros(VagaBuscaFiltro filtro) {
        if (legacySchema && ((filtro.getAreaAtuacao()!=null && !filtro.getAreaAtuacao().isBlank())
                || filtro.getAreaId() != null
                || (filtro.getFuncaoIds()!=null && !filtro.getFuncaoIds().isEmpty())
                || (filtro.getEspecializacaoIds()!=null && !filtro.getEspecializacaoIds().isEmpty())
                || filtro.getFormaRemuneracao() != null || filtro.getAbrangencia() != null
                || filtro.getAfirmativa() != null
                || (filtro.getCategoriaAfirmativaIds()!=null && !filtro.getCategoriaAfirmativaIds().isEmpty())))
            throw new UnprocessableEntityException("BLOQUEADA POR SCHEMA DO BANCO: filtros de taxonomia indisponíveis.");
        validarCursor(filtro.getCursor(), "Cursor");
        validarCursor(filtro.getCursorCanceladas(), "Cursor de vagas canceladas");
        if (filtro.getFaixaSalarialMin() != null
                && filtro.getFaixaSalarialMin().signum() < 0) {
            throw new IllegalArgumentException("Remuneração mínima não pode ser negativa.");
        }
        if (filtro.getFaixaSalarialMax() != null
                && filtro.getFaixaSalarialMax().signum() < 0) {
            throw new IllegalArgumentException("Remuneração máxima não pode ser negativa.");
        }
        if (filtro.getFaixaSalarialMin() != null && filtro.getFaixaSalarialMax() != null
                && filtro.getFaixaSalarialMin().compareTo(filtro.getFaixaSalarialMax()) > 0) {
            throw new IllegalArgumentException(
                    "Remuneração mínima não pode ser maior que a máxima.");
        }
        if (filtro.getEstado() != null && !filtro.getEstado().isBlank()
                && filtro.getEstado().trim().length() != 2) {
            throw new IllegalArgumentException("Estado deve usar exatamente 2 caracteres.");
        }
        if (filtro.getFuncaoIds() != null
                && filtro.getFuncaoIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("IDs de funcoes devem ser positivos.");
        }
        if (filtro.getAreaId() != null && filtro.getAreaId() <= 0) {
            throw new IllegalArgumentException("ID da área deve ser positivo.");
        }
        Set<Long> funcoes = filtro.getFuncaoIds() == null ? Set.of() : filtro.getFuncaoIds();
        Set<Long> especializacoes = filtro.getEspecializacaoIds() == null
                ? Set.of() : filtro.getEspecializacaoIds();
        Set<Integer> categorias = filtro.getCategoriaAfirmativaIds() == null
                ? Set.of() : filtro.getCategoriaAfirmativaIds();
        if (especializacoes.stream().anyMatch(id -> id == null || id <= 0)
                || categorias.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("IDs de catálogo devem ser positivos.");
        }
        if (funcoes.size() > 50 || especializacoes.size() > 50 || categorias.size() > 50) {
            throw new IllegalArgumentException("Cada filtro de catálogo aceita até 50 IDs.");
        }
        Short area = filtro.getAreaId();
        if (area != null && !areaArtisticaRepository.existsById(area)) {
            throw new UnprocessableEntityException("Área artística inexistente.");
        }
        if (!funcoes.isEmpty()) {
            long validas = area == null ? funcaoRepository.countByIdIn(funcoes)
                    : funcaoRepository.countByAreaIdAndIdIn(area, funcoes);
            if (validas != funcoes.size()) {
                throw new UnprocessableEntityException(
                        "Função inexistente ou incompatível com a área.");
            }
        }
        if (!especializacoes.isEmpty()) {
            long validas = area != null && !funcoes.isEmpty()
                    ? especializacaoRepository.contarCompativeis(area, funcoes, especializacoes)
                    : area != null ? especializacaoRepository.contarDaArea(area, especializacoes)
                    : !funcoes.isEmpty()
                    ? especializacaoRepository.contarDasFuncoes(funcoes, especializacoes)
                    : especializacaoRepository.countByIdIn(especializacoes);
            if (validas != especializacoes.size()) {
                throw new UnprocessableEntityException(
                        "Especialização inexistente ou incompatível com área/função.");
            }
        }
        if (!categorias.isEmpty() && (Boolean.FALSE.equals(filtro.getAfirmativa())
                || categoriaAfirmativaRepository.findAllById(categorias).size() != categorias.size())) {
            throw new UnprocessableEntityException(
                    "Categoria afirmativa inexistente ou incompatível com o filtro.");
        }
    }

    private void validarCursor(Long cursor, String nome) {
        if (cursor != null && cursor < 0) {
            throw new IllegalArgumentException(nome + " não pode ser negativo.");
        }
    }

    private int normalizarTamanho(Integer solicitado) {
        if (solicitado == null || solicitado < 1) {
            return TAMANHO_PADRAO;
        }
        return Math.min(solicitado, TAMANHO_MAXIMO);
    }

    @Transactional(readOnly = true)
    public VagaResponse buscarPorId(Long id) {
        Usuario usuario = authenticatedUserResolver.usuarioAtual().orElse(null);
        Vaga vaga = vagaRepository.findDetalhesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        boolean proprietario = usuario != null
                && usuario.getTipoUsuario() == TipoUsuario.CONTRATANTE
                && vaga.getContratante().getUsuarioId().equals(usuario.getId());
        Optional<Candidatura> candidaturaDoArtista = usuario != null
                && usuario.getTipoUsuario() == TipoUsuario.ARTISTA
                ? candidaturaRepository.findByVagaIdAndArtistaUsuarioIdOrderByIdDesc(vaga.getId(), usuario.getId())
                        .stream().findFirst()
                : Optional.empty();

        if (vaga.getStatus() == StatusVaga.RASCUNHO && !proprietario) {
            throw new ResourceNotFoundException("Vaga não encontrada.");
        }
        if (vaga.getStatus() != StatusVaga.ABERTA
                && !proprietario
                && candidaturaDoArtista.isEmpty()) {
            // Não permite descobrir por ID uma vaga ausente do feed público.
            throw new ResourceNotFoundException("Vaga não encontrada.");
        }

        VagaResponse resposta = toResponse(
                vaga, false, proprietario ? usuario.getId() : null);
        Candidatura candidatura = candidaturaDoArtista.orElse(null);
        return resposta.toBuilder()
                .contratantePublico(toContratantePublico(vaga.getContratante()))
                .minhaCandidaturaId(candidatura == null ? null : candidatura.getId())
                .statusMinhaCandidatura(candidatura == null ? null : candidatura.getStatus())
                .build();
    }

    @Transactional
    public VagaResponse criar(VagaRequest request) {
        Usuario usuario = exigirContratanteAtual();
        StatusVaga statusSolicitado = request.getStatus() == null ? StatusVaga.ABERTA : request.getStatus();
        if (statusSolicitado != StatusVaga.ABERTA && statusSolicitado != StatusVaga.RASCUNHO) {
            throw new UnprocessableEntityException("Criação aceita somente ABERTA ou RASCUNHO.");
        }
        if (legacySchema && statusSolicitado == StatusVaga.RASCUNHO) {
            throw new UnprocessableEntityException(
                    "BLOQUEADA POR SCHEMA DO BANCO: o enum instalado não suporta RASCUNHO.");
        }
        PerfilContratante contratante = perfilContratanteRepository.findById(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contratante não encontrado."));
        Vaga vaga = new Vaga();
        vaga.setContratante(contratante);
        preencherVaga(vaga, request);
        normalizarPublicacao(vaga);
        validarPrazoInformado(vaga.getDataLimiteCandidatura());
        if (statusSolicitado == StatusVaga.ABERTA) {
            validarPublicacao(vaga);
            vaga.setDataPublicacao(LocalDateTime.now());
        }
        vaga.setStatus(statusSolicitado);
        if (legacySchema) {
            Long id = officialLocalVagas.inserir(vaga);
            Vaga persistida = vagaRepository.findById(id).orElseThrow();
            persistida.setFotos(vaga.getFotos());
            return toResponse(vagaRepository.saveAndFlush(persistida));
        }
        // O schema oficial valida vaga_especializacao contra vaga_funcao em um
        // trigger BEFORE INSERT. Hibernate não garante a ordem entre as duas
        // coleções, então materializamos primeiro a vaga e suas funções e só
        // depois as especializações, ainda dentro da mesma transação.
        Set<Especializacao> especializacoes = new HashSet<>(vaga.getEspecializacoes());
        vaga.setEspecializacoes(new HashSet<>());
        Vaga salva = vagaRepository.saveAndFlush(vaga);
        if (!especializacoes.isEmpty()) {
            salva.setEspecializacoes(especializacoes);
            vagaRepository.flush();
        }
        return toResponse(salva);
    }

    @Transactional
    public VagaResponse atualizar(Long id, VagaAtualizacaoRequest request) {
        Vaga vaga = vagaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        exigirProprietario(vaga);
        StatusVaga statusAtual = vaga.getStatus();
        if (statusAtual != StatusVaga.RASCUNHO && statusAtual != StatusVaga.ABERTA
                && statusAtual != StatusVaga.PAUSADA) {
            throw new UnprocessableEntityException("Vaga " + statusAtual + " não pode ser editada.");
        }
        preencherVaga(vaga, request);
        normalizarPublicacao(vaga);
        validarPrazoInformado(vaga.getDataLimiteCandidatura());
        if (statusAtual != StatusVaga.RASCUNHO) validarPublicacao(vaga);
        vaga.setStatus(statusAtual);
        if (legacySchema) officialLocalVagas.atualizarCampos(vaga);
        return toResponse(vagaRepository.save(vaga));
    }

    @Transactional
    public VagaResponse gerenciarStatus(Long id, VagaStatusAcaoRequest request) {
        Vaga vaga = vagaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        exigirProprietario(vaga);

        String acao = request.getAcao().trim().toUpperCase(Locale.ROOT);
        StatusVaga novoStatus = switch (acao) {
            case "PUBLICAR" -> exigirTransicao(
                    vaga.getStatus(), acao, StatusVaga.ABERTA, StatusVaga.RASCUNHO);
            case "SUSPENDER" -> exigirTransicao(
                    vaga.getStatus(), acao, StatusVaga.PAUSADA, StatusVaga.ABERTA);
            case "REABRIR" -> exigirTransicao(
                    vaga.getStatus(), acao, StatusVaga.ABERTA,
                    StatusVaga.PAUSADA, StatusVaga.ENCERRADA);
            case "ENCERRAR" -> exigirTransicao(
                    vaga.getStatus(), acao, StatusVaga.ENCERRADA,
                    StatusVaga.ABERTA, StatusVaga.PAUSADA);
            default -> throw new IllegalArgumentException(
                    "Ação de gerenciamento inválida: " + acao
                            + ". Ações aceitas: PUBLICAR, SUSPENDER, REABRIR e ENCERRAR.");
        };

        if (novoStatus == StatusVaga.ABERTA) {
            LocalDate novoPrazo = request.getDataLimiteCandidatura();
            if (novoPrazo != null) {
                validarPrazoInformado(novoPrazo);
                vaga.setDataLimiteCandidatura(novoPrazo);
            } else if (vagaPrazoPolicy.estaVencida(vaga)) {
                throw new UnprocessableEntityException(
                        "Informe nova data limite futura para reabrir ou publicar a vaga.");
            }
            normalizarPublicacao(vaga);
            validarPublicacao(vaga);
            if (vaga.getStatus() == StatusVaga.RASCUNHO) {
                vaga.setDataPublicacao(LocalDateTime.now());
            }
        } else if (request.getDataLimiteCandidatura() != null) {
            throw new UnprocessableEntityException("A nova data limite é aceita somente ao publicar ou reabrir.");
        }
        vaga.setStatus(novoStatus);
        Vaga salva = vagaRepository.save(vaga);
        if (acao.equals("PUBLICAR")) return toResponse(salva);
        publicarMudancaDeStatus(salva, novoStatus);
        return toResponse(salva);
    }

    @Transactional
    public void deletar(Long id, VagaCancelamentoRequest request) {
        Usuario usuario = exigirContratanteAtual();
        Vaga vaga = vagaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        if (!vaga.getContratante().getUsuarioId().equals(usuario.getId())) {
            throw new ForbiddenException("Somente o proprietário pode alterar esta vaga.");
        }
        if (vaga.getStatus() == StatusVaga.RASCUNHO) {
            if (!candidaturaRepository.findByVagaId(id).isEmpty()) {
                throw new UnprocessableEntityException("Rascunho com candidatura não pode ser excluído.");
            }
            vagaRepository.delete(vaga);
            return;
        }
        if (vaga.getStatus() != StatusVaga.ABERTA && vaga.getStatus() != StatusVaga.PAUSADA) {
            throw new UnprocessableEntityException(
                    "A vaga no estado " + vaga.getStatus()
                            + " não pode ser cancelada. Somente vagas ABERTA ou PAUSADA podem ser canceladas.");
        }
        String motivo = validarCancelamento(request);

        vaga.setStatus(StatusVaga.CANCELADA);
        vagaRepository.save(vaga);

        List<Candidatura> candidaturas = candidaturaRepository.findByVagaId(id);
        List<Candidatura> ativas = candidaturas.stream()
                .filter(c -> c.getStatus() == StatusCandidatura.PENDENTE
                        || c.getStatus() == StatusCandidatura.EM_ANALISE)
                .toList();
        ativas.forEach(c -> c.setStatus(StatusCandidatura.CANCELADA_POR_VAGA));
        candidaturaRepository.saveAll(ativas);

        LogVagaCancelada log = new LogVagaCancelada();
        log.setVaga(vaga);
        log.setCanceladoPor(usuario);
        log.setDataCancelamento(LocalDateTime.now());
        log.setMotivo(motivo);
        logVagaCanceladaRepository.save(log);

        publicarParaCandidatos(
                vaga,
                candidaturas,
                "A vaga \"" + vaga.getTitulo() + "\" foi cancelada.");
    }

    private void publicarMudancaDeStatus(Vaga vaga, StatusVaga novoStatus) {
        String mensagem = switch (novoStatus) {
            case PAUSADA -> "A vaga \"" + vaga.getTitulo() + "\" foi suspensa.";
            case ABERTA -> "A vaga \"" + vaga.getTitulo() + "\" foi reaberta.";
            case ENCERRADA -> "A vaga \"" + vaga.getTitulo() + "\" foi encerrada.";
            default -> null;
        };
        if (mensagem != null) {
            publicarParaCandidatosDaVaga(vaga.getId(), mensagem);
        }
    }

    public void publicarEncerramentoAutomatico(Long vagaId, String titulo) {
        publicarParaCandidatosDaVaga(
                vagaId, "A vaga \"" + titulo + "\" foi encerrada.");
    }

    private void publicarParaCandidatosDaVaga(Long vagaId, String mensagem) {
        Long cursor = null;
        do {
            List<Long> destinatarios = candidaturaRepository
                    .findArtistaUsuarioIdsByVagaIdAposCursor(
                            vagaId, cursor, PageRequest.of(0, LOTE_NOTIFICACOES_STATUS));
            if (destinatarios.isEmpty()) {
                return;
            }
            publicarParaDestinatarios(vagaId, Set.copyOf(destinatarios), mensagem);
            cursor = destinatarios.getLast();
            if (destinatarios.size() < LOTE_NOTIFICACOES_STATUS) {
                return;
            }
        } while (true);
    }

    private void publicarParaCandidatos(
            Vaga vaga, List<Candidatura> candidaturas, String mensagem) {
        Set<Long> destinatarios = candidaturas.stream()
                .map(candidatura -> candidatura.getArtista().getUsuarioId())
                .collect(Collectors.toSet());
        publicarParaDestinatarios(vaga.getId(), destinatarios, mensagem, vaga.getStatus());
    }

    private void publicarParaDestinatarios(
            Long vagaId, Set<Long> destinatarios, String mensagem) {
        publicarParaDestinatarios(vagaId, destinatarios, mensagem, null);
    }

    private void publicarParaDestinatarios(
            Long vagaId, Set<Long> destinatarios, String mensagem, StatusVaga status) {
        if (destinatarios.isEmpty()) {
            return;
        }
        NotificacaoEvento evento = new NotificacaoEvento(
                destinatarios,
                TipoNotificacao.CANDIDATURA,
                mensagem,
                "detalhe-vaga.html?id=" + vagaId);
        if (status == StatusVaga.CANCELADA) {
            // RF28: registros obrigatórios participam do rollback; só a entrega aguarda o commit.
            notificacaoPersistenceService.persistirNaTransacaoAtual(evento)
                    .forEach(eventPublisher::publishEvent);
        } else {
            eventPublisher.publishEvent(evento);
        }
    }

    private String validarCancelamento(VagaCancelamentoRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.getConfirmacao())) {
            throw new IllegalArgumentException("Confirmação deve ser verdadeira.");
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new IllegalArgumentException("Motivo é obrigatório.");
        }
        return request.getMotivo().trim();
    }

    private void preencherVaga(Vaga vaga, VagaAtualizacaoRequest request) {
        if (legacySchema) { preencherVagaLocal(vaga,request); return; }
        vaga.setTitulo(request.getTitulo());
        vaga.setDescricao(request.getDescricao());
        vaga.setRequisitos(request.getRequisitos() == null ? "" : request.getRequisitos());
        if (request.getAreaId() == null || request.getAbrangencia() == null || request.getFormaRemuneracao() == null) {
            throw new IllegalArgumentException("Informe areaId, abrangencia e formaRemuneracao do catálogo oficial.");
        }
        boolean mudouArea = vaga.getArea() != null && !vaga.getArea().getId().equals(request.getAreaId());
        vaga.setArea(areaArtisticaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Área artística não encontrada.")));
        var valorMinimo = request.getValorMinimo();
        var valorMaximo = request.getValorMaximo();

        if (request.getFormaRemuneracao()
                != com.portifolio.model.enums.FormaRemuneracao.A_COMBINAR) {

            if (valorMinimo == null) {
                throw new IllegalArgumentException("Informe o valor da remuneração.");
            }

            if (valorMaximo == null) {
                valorMaximo = valorMinimo;
            }

            if (valorMinimo.compareTo(valorMaximo) > 0) {
                throw new IllegalArgumentException(
                        "Remuneração exige faixa mínima e máxima válida.");
            }
        }

        vaga.setValorMinimo(valorMinimo);
        vaga.setValorMaximo(valorMaximo);
        vaga.setFormaRemuneracao(request.getFormaRemuneracao());

        if (request.getCidade() == null || request.getCidade().isBlank()
                || request.getEstado() == null || request.getEstado().isBlank()) {
            throw new IllegalArgumentException("Cidade e estado são obrigatórios.");
        }

        vaga.setCidade(request.getCidade().trim());
        vaga.setEstado(request.getEstado().trim().toUpperCase());
        vaga.setEnderecoCompleto(request.getEnderecoCompleto());
        vaga.setBeneficios(request.getBeneficios());
        vaga.setModeloTrabalho(request.getModeloTrabalho());
        vaga.setTipoContrato(request.getTipoContrato());
        vaga.setExperiencia(request.getExperiencia());
        vaga.setDataLimiteCandidatura(request.getDataLimiteCandidatura());
        vaga.setAbrangencia(request.getAbrangencia());
        if (request.getFotos() != null) {
            vaga.setFotos(new ArrayList<>(request.getFotos().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .toList()));
        }
        Set<Funcao> funcoesDesejadas = request.getFuncaoIds() != null
                ? resolverFuncoes(request.getFuncaoIds())
                : mudouArea ? new HashSet<>() : new HashSet<>(vaga.getFuncoes());
        if (funcoesDesejadas.stream().anyMatch(
                funcao -> !funcao.getArea().getId().equals(vaga.getArea().getId()))) {
            throw new IllegalArgumentException("As funções devem pertencer à área da vaga.");
        }
        Set<Long> compativeis = funcoesDesejadas.stream()
                .flatMap(funcao -> funcao.getEspecializacoes().stream())
                .map(com.portifolio.model.Especializacao::getId).collect(Collectors.toSet());
        Set<Especializacao> especializacoesDesejadas;
        if (request.getEspecializacaoIds() != null) {
            if (!compativeis.containsAll(request.getEspecializacaoIds())) {
                throw new IllegalArgumentException("As especializações devem pertencer às funções selecionadas da vaga.");
            }
            especializacoesDesejadas = new HashSet<>(
                    especializacaoRepository.findAllById(request.getEspecializacaoIds()));
        } else {
            // Edição legada preserva seleções válidas e remove apenas as que ficaram órfãs.
            especializacoesDesejadas = vaga.getEspecializacoes().stream()
                    .filter(especializacao -> compativeis.contains(especializacao.getId()))
                    .collect(Collectors.toSet());
        }
        if (vaga.getId() == null) {
            vaga.setFuncoes(funcoesDesejadas);
            vaga.setEspecializacoes(especializacoesDesejadas);
        } else {
            sincronizarTaxonomiaIncremental(vaga, funcoesDesejadas, especializacoesDesejadas);
        }
        if (request.getCategoriaAfirmativaIds() != null) {
            if (request.getCategoriaAfirmativaIds().size() > 1) {
                throw new UnprocessableEntityException(
                        "Limitação estrutural do database04 frente ao RF04 revisado: "
                        + "a vaga aceita somente uma categoria afirmativa; nenhuma seleção foi descartada.");
            }
            var categorias = categoriaAfirmativaRepository.findAllById(request.getCategoriaAfirmativaIds());
            if (categorias.size() != request.getCategoriaAfirmativaIds().size()) {
                throw new UnprocessableEntityException(
                        "Categoria afirmativa não representável no enum oficial do database04. "
                        + "Consulte o catálogo atual; 50+ depende de evolução do banco.");
            }
            vaga.setCategoriasAfirmativas(new HashSet<>(categorias));
        }
        if (Boolean.TRUE.equals(request.getAfirmativa())
                && vaga.getCategoriasAfirmativas().isEmpty()) {
            throw new UnprocessableEntityException(
                    "Selecione ao menos uma categoria para a vaga afirmativa.");
        }
        if (Boolean.FALSE.equals(request.getAfirmativa())
                && !vaga.getCategoriasAfirmativas().isEmpty()) {
            throw new UnprocessableEntityException(
                    "Vaga não afirmativa não pode ter categorias afirmativas.");
        }
    }

    private Set<Funcao> resolverFuncoes(Set<Long> funcaoIds) {
        List<Funcao> funcoes = funcaoRepository.findAllById(funcaoIds);
        if (funcoes.size() != funcaoIds.size()) {
            throw new ResourceNotFoundException("Uma ou mais funcoes não foram encontradas.");
        }
        return new HashSet<>(funcoes);
    }

    private void sincronizarTaxonomiaIncremental(
            Vaga vaga,
            Set<Funcao> funcoesDesejadas,
            Set<Especializacao> especializacoesDesejadas) {
        Set<Long> funcaoIdsAtuais = vaga.getFuncoes().stream()
                .map(Funcao::getId)
                .collect(Collectors.toSet());
        List<Funcao> funcoesAdicionadas = funcoesDesejadas.stream()
                .filter(funcao -> !funcaoIdsAtuais.contains(funcao.getId()))
                .toList();
        if (!funcoesAdicionadas.isEmpty()) {
            vaga.getFuncoes().addAll(funcoesAdicionadas);
            // O trigger de compatibilidade das especializações precisa enxergar
            // as novas funções antes da próxima etapa.
            vagaRepository.flush();
        }

        Set<Long> especializacaoIdsDesejadas = especializacoesDesejadas.stream()
                .map(Especializacao::getId)
                .collect(Collectors.toSet());
        boolean alterouEspecializacoes = vaga.getEspecializacoes().removeIf(
                especializacao -> !especializacaoIdsDesejadas.contains(especializacao.getId()));
        Set<Long> especializacaoIdsAtuais = vaga.getEspecializacoes().stream()
                .map(Especializacao::getId)
                .collect(Collectors.toSet());
        List<Especializacao> especializacoesAdicionadas = especializacoesDesejadas.stream()
                .filter(especializacao -> !especializacaoIdsAtuais.contains(especializacao.getId()))
                .toList();
        if (!especializacoesAdicionadas.isEmpty()) {
            vaga.getEspecializacoes().addAll(especializacoesAdicionadas);
            alterouEspecializacoes = true;
        }
        if (alterouEspecializacoes) {
            // Remove as especializações realmente órfãs e materializa as novas
            // antes de retirar qualquer função antiga.
            vagaRepository.flush();
        }

        Set<Long> funcaoIdsDesejadas = funcoesDesejadas.stream()
                .map(Funcao::getId)
                .collect(Collectors.toSet());
        boolean removeuFuncoes = vaga.getFuncoes().removeIf(
                funcao -> !funcaoIdsDesejadas.contains(funcao.getId()));
        if (removeuFuncoes) {
            // Como a coleção gerenciada é alterada por diferença, vínculos
            // mantidos nunca desaparecem no estado intermediário visto pelo trigger.
            vagaRepository.flush();
        }
    }

    private StatusVaga exigirTransicao(
            StatusVaga atual, String acao, StatusVaga destino, StatusVaga... origensPermitidas) {
        for (StatusVaga origem : origensPermitidas) {
            if (atual == origem) {
                return destino;
            }
        }
        throw new UnprocessableEntityException(
                "Transição inválida: a ação " + acao + " não pode ser aplicada à vaga no estado "
                        + atual + ". Ações permitidas no estado atual: " + acoesPermitidas(atual) + ".");
    }

    private String acoesPermitidas(StatusVaga status) {
        return switch (status) {
            case ABERTA -> "SUSPENDER ou ENCERRAR";
            case PAUSADA -> "REABRIR ou ENCERRAR";
            case RASCUNHO -> "PUBLICAR";
            case ENCERRADA -> "REABRIR";
            case CANCELADA -> "nenhuma; este é um estado final";
        };
    }

    private void validarPublicacao(Vaga vaga) {
        if (vaga.getModeloTrabalho() == null) {
            throw new UnprocessableEntityException("Modelo de trabalho é obrigatório para publicar.");
        }
        validarPrazoInformado(vaga.getDataLimiteCandidatura());
    }

    private void validarPrazoInformado(LocalDate prazo) {
        if (vagaPrazoPolicy.estaVencida(prazo)) {
            throw new UnprocessableEntityException("Data limite deve ser futura.");
        }
    }

    private void normalizarPublicacao(Vaga vaga) {
        vaga.setTitulo(vaga.getTitulo().trim());
        vaga.setDescricao(vaga.getDescricao().trim());
        vaga.setRequisitos(vaga.getRequisitos().trim());
        vaga.setTipoContrato(vaga.getTipoContrato().trim());
        vaga.setEnderecoCompleto(normalizarOpcional(vaga.getEnderecoCompleto()));
        vaga.setBeneficios(normalizarOpcional(vaga.getBeneficios()));
        vaga.setExperiencia(normalizarOpcional(vaga.getExperiencia()));
        vaga.setFotos(new ArrayList<>(vaga.getFotos().stream()
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList()));
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private ContratantePublicoResponse toContratantePublico(PerfilContratante perfil) {
        Usuario usuario = perfil.getUsuario();
        String nomeEmpresa = perfil.getNomeEmpresa();
        String nomeExibicao = nomeEmpresa == null || nomeEmpresa.isBlank()
                ? usuario.getNome()
                : nomeEmpresa;
        return ContratantePublicoResponse.builder()
                .usuarioId(perfil.getUsuarioId())
                .nomeExibicao(nomeExibicao)
                .nomeEmpresa(nomeEmpresa)
                .tipoPerfil(perfil.getTipoPerfil())
                .biografia(perfil.getBiografia())
                .localizacao(perfil.getLocalizacao())
                .bannerUrl(perfil.getBannerUrl())
                .avatarUrl(avatarService.resolverUrl(
                        perfil.getUsuarioId(), usuario.getFotoPerfil(), null))
                .build();
    }

    private VagaResponse toResponse(Vaga vaga) {
        Long usuarioId = authenticatedUserResolver.usuarioAtual()
                .map(this::contratanteAtualId)
                .orElse(null);
        return toResponse(vaga, false, usuarioId);
    }

    private VagaResponse toResponse(
            Vaga vaga, boolean cancelada, Long contratanteAtualId) {
        if (legacySchema) officialLocalVagas.carregarCampos(vaga);
        Set<Long> funcaoIds = vaga.getFuncoes().stream()
                .map(Funcao::getId)
                .collect(Collectors.toSet());
        return VagaResponse.builder()
                .id(vaga.getId())
                .contratanteId(vaga.getContratante().getUsuarioId())
                .nomeContratante(vaga.getContratante().getNomeEmpresa() == null
                        || vaga.getContratante().getNomeEmpresa().isBlank()
                        ? vaga.getContratante().getUsuario().getNome()
                        : vaga.getContratante().getNomeEmpresa())
                .titulo(vaga.getTitulo())
                .descricao(vaga.getDescricao())
                .requisitos(vaga.getRequisitos())
                .remuneraValor(valorUnico(vaga))
                .valorMinimo(vaga.getValorMinimo())
                .valorMaximo(vaga.getValorMaximo())
                .formaRemuneracao(legacySchema ? null : vaga.getFormaRemuneracao())
                .areaId(vaga.getArea()==null?null:vaga.getArea().getId())
                .formaPagamento(legacySchema?vaga.getLegacyFormaPagamento():null)
                .cidade(vaga.getCidade())
                .estado(vaga.getEstado())
                .enderecoCompleto(contratanteAtualId!=null && contratanteAtualId.equals(vaga.getContratante().getUsuarioId()) ? vaga.getEnderecoCompleto() : null)
                .beneficios(vaga.getBeneficios())
                .modeloTrabalho(vaga.getModeloTrabalho())
                .tipoContrato(vaga.getTipoContrato())
                .status(vaga.getStatus())
                .dataPublicacao(vaga.getDataPublicacao())
                .funcaoIds(funcaoIds)
                .especializacaoIds(vaga.getEspecializacoes().stream()
                        .map(com.portifolio.model.Especializacao::getId).collect(Collectors.toSet()))
                .categoriaAfirmativaIds(vaga.getCategoriasAfirmativas().stream()
                        .map(com.portifolio.model.CategoriaAfirmativa::getId).collect(Collectors.toSet()))
                .categoria(legacySchema?vaga.getLegacyCategoria():vaga.getArea().getNome())
                .experiencia(vaga.getExperiencia())
                .dataLimiteCandidatura(vaga.getDataLimiteCandidatura())
                .abrangencia(legacySchema?vaga.getLegacyAbrangencia():vaga.getAbrangencia() == null ? null : vaga.getAbrangencia().name())
                .fotos(vaga.getFotos() == null
                     ? List.of()
                      : vaga.getFotos().stream()
                .filter(java.util.Objects::nonNull)
                .toList())
                .propriaDoContratante(contratanteAtualId != null
                        && contratanteAtualId.equals(vaga.getContratante().getUsuarioId()))
                .cancelada(cancelada)
                .build();
    }

    private java.math.BigDecimal valorUnico(Vaga vaga) {
        if (legacySchema) return vaga.getValorMinimo();
        return vaga.getValorMinimo() != null && vaga.getValorMaximo() != null
                && vaga.getValorMinimo().compareTo(vaga.getValorMaximo()) == 0 ? vaga.getValorMinimo() : null;
    }

    private Long contratanteAtualId(Usuario usuario) {
        return usuario != null && usuario.getTipoUsuario() == TipoUsuario.CONTRATANTE
                ? usuario.getId()
                : null;
    }

    private record PaginaCanceladas(
            List<VagaResponse> content, Long nextCursor, boolean hasMore) {
        private static PaginaCanceladas vazia() {
            return new PaginaCanceladas(List.of(), null, false);
        }
    }

    private Usuario exigirContratanteAtual() {
        Usuario usuario = exigirUsuarioAtual();
        if (usuario.getTipoUsuario() != TipoUsuario.CONTRATANTE) {
            throw new ForbiddenException("Somente contratantes podem gerenciar vagas.");
        }
        return usuario;
    }

    private Usuario exigirUsuarioAtual() {
        return authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new ForbiddenException("Autenticação obrigatória."));
    }

    private Usuario exigirProprietario(Vaga vaga) {
        Usuario usuario = exigirContratanteAtual();
        if (!vaga.getContratante().getUsuarioId().equals(usuario.getId())) {
            throw new ForbiddenException("Somente o proprietário pode alterar esta vaga.");
        }
        return usuario;
    }
    private void preencherVagaLocal(Vaga vaga, VagaAtualizacaoRequest r) {
        if (r.getAreaId()!=null || (r.getFuncaoIds()!=null&&!r.getFuncaoIds().isEmpty()) || (r.getEspecializacaoIds()!=null&&!r.getEspecializacaoIds().isEmpty()) || (r.getCategoriaAfirmativaIds()!=null&&!r.getCategoriaAfirmativaIds().isEmpty())) throw new UnprocessableEntityException("BLOQUEADA POR SCHEMA DO BANCO: taxonomia ausente.");
        if (r.getValorMinimo()==null || r.getFormaPagamento()==null || r.getFormaPagamento().isBlank()) throw new IllegalArgumentException("Valor e forma de pagamento são obrigatórios.");
        if (r.getValorMaximo()!=null && r.getValorMinimo().compareTo(r.getValorMaximo())!=0) throw new UnprocessableEntityException("O banco suporta remuneração única, sem faixa.");
        vaga.setTitulo(r.getTitulo()); vaga.setDescricao(r.getDescricao()); vaga.setRequisitos(r.getRequisitos());
        vaga.setValorMinimo(r.getValorMinimo()); vaga.setLegacyFormaPagamento(r.getFormaPagamento()); vaga.setLegacyCategoria(r.getCategoria());
        vaga.setCidade(r.getCidade()); vaga.setEstado(r.getEstado()); vaga.setEnderecoCompleto(r.getEnderecoCompleto());
        vaga.setBeneficios(r.getBeneficios()); vaga.setModeloTrabalho(r.getModeloTrabalho()); vaga.setTipoContrato(r.getTipoContrato());
        vaga.setExperiencia(r.getExperiencia()); vaga.setDataLimiteCandidatura(r.getDataLimiteCandidatura());
        if(r.getAbrangencia()!=null) vaga.setLegacyAbrangencia(r.getAbrangencia().name());
        if(r.getFotos()!=null) vaga.setFotos(new java.util.ArrayList<>(r.getFotos()));
    }
}
