package com.portifolio.service;

import com.portifolio.dto.DashboardCandidaturaResponse;
import com.portifolio.dto.DashboardDisponibilidadeResponse;
import com.portifolio.dto.DashboardResponse;
import com.portifolio.dto.DashboardSecaoResponse;
import com.portifolio.dto.DashboardTalentoResponse;
import com.portifolio.dto.DashboardVagaResponse;
import com.portifolio.dto.FuncaoResponse;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Funcao;
import com.portifolio.model.Usuario;
import com.portifolio.model.Vaga;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.MensagemChatRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.repository.projection.CandidaturaDashboardProjection;
import com.portifolio.repository.projection.VagaRecomendadaProjection;
import com.portifolio.security.AuthenticatedUserResolver;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    @org.springframework.beans.factory.annotation.Value("${app.database.legacy:false}") private boolean legacySchema;

    private static final int TAMANHO_PADRAO = 5;
    private static final int TAMANHO_MAXIMO = 50;
    private static final Set<StatusVaga> STATUS_ATIVOS = Set.of(StatusVaga.ABERTA, StatusVaga.PAUSADA);

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final PerfilContratanteRepository perfilContratanteRepository;
    private final VagaRepository vagaRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final AvatarService avatarService;
    private final TalentoService talentoService;
    private final NotificacaoService notificacaoService;
    private final VagaService vagaService;
    private final VagaPrazoPolicy vagaPrazoPolicy;
    private final com.portifolio.repository.BancoTalentosRepository bancoTalentosRepository;
    private final com.portifolio.security.GoogleAccountAccessPolicy accountAccessPolicy;
    private final com.portifolio.security.MenorAutorizadoPolicy menorPolicy;

    @Transactional(readOnly = true)
    public DashboardResponse buscar(Integer size) {
        int tamanho = validarTamanho(size);
        Usuario usuario = authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado nao encontrado."));

        if (!accountAccessPolicy.acessoNormalPermitido(usuario)) {
            throw new com.portifolio.exception.ForbiddenException("Conta sem acesso normal.");
        }
        if (usuario.getTipoUsuario() == com.portifolio.model.enums.TipoUsuario.ARTISTA
                && menorPolicy.exigeProtecao(usuario) && !menorPolicy.autorizado(usuario)) {
            throw new com.portifolio.exception.ForbiddenException("Artista sem autorização vigente.");
        }

        return switch (usuario.getTipoUsuario()) {
            case ARTISTA -> dashboardArtista(usuario, tamanho);
            case CONTRATANTE -> dashboardContratante(usuario, tamanho);
            default -> throw new com.portifolio.exception.ForbiddenException("Dashboard disponível para artista e contratante.");
        };
    }

    private DashboardResponse dashboardArtista(Usuario usuario, int tamanho) {
        return DashboardResponse.builder()
                .tipoUsuario(usuario.getTipoUsuario())
                .nomeExibicao(usuario.getNome())
                .avatarUrl(avatarService.resolverUrl(
                        usuario.getId(), usuario.getFotoPerfil(), null))
                .perfilCompleto(Boolean.TRUE.equals(usuario.getPerfilCompleto()))
                .perfilIncompleto(!Boolean.TRUE.equals(usuario.getPerfilCompleto()))
                .notificacoes(notificacoesDisponiveis())
                .mensagens(mensagensDisponiveis(usuario.getId()))
                .vagasRecomendadas(buscarVagasRecomendadas(tamanho))
                .minhasCandidaturas(buscarMinhasCandidaturas(usuario.getId(), tamanho))
                .build();
    }

    private DashboardResponse dashboardContratante(Usuario usuario, int tamanho) {
        PerfilContratante perfil = perfilContratanteRepository.buscarPublicoPorUsuarioId(usuario.getId())
                .orElse(null);
        String nomeExibicao = perfil == null || perfil.getNomeEmpresa() == null
                || perfil.getNomeEmpresa().isBlank()
                ? usuario.getNome()
                : perfil.getNomeEmpresa();

        return DashboardResponse.builder()
                .tipoUsuario(usuario.getTipoUsuario())
                .nomeExibicao(nomeExibicao)
                .avatarUrl(avatarService.resolverUrl(
                        usuario.getId(), usuario.getFotoPerfil(), null))
                .perfilCompleto(Boolean.TRUE.equals(usuario.getPerfilCompleto()))
                .perfilIncompleto(!Boolean.TRUE.equals(usuario.getPerfilCompleto()))
                .notificacoes(notificacoesDisponiveis())
                .mensagens(mensagensDisponiveis(usuario.getId()))
                .minhasVagas(buscarMinhasVagas(usuario.getId(), tamanho))
                .vagasPorStatus(vagasPorStatus(usuario.getId()))
                .quantidadeBancoTalentos(bancoTalentosRepository.contarDoContratante(usuario.getId()))
                .candidaturasRecentes(buscarCandidaturasRecentes(usuario.getId(), tamanho))
                .talentosSugeridos(!legacySchema && usuario.getStatusConta() == com.portifolio.model.enums.StatusConta.ATIVA
                        ? buscarTalentosSugeridos(usuario.getId(), tamanho) : secaoVazia())
                .build();
    }

    private DashboardSecaoResponse<DashboardVagaResponse> buscarVagasRecomendadas(int tamanho) {
        Page<VagaRecomendadaProjection> pagina = vagaService.recomendarParaArtista(tamanho);
        if (pagina.isEmpty()) {
            return secaoVazia();
        }
        List<Long> ids = pagina.getContent().stream().map(VagaRecomendadaProjection::getId).toList();
        Map<Long, Vaga> vagas = vagaRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Vaga::getId, Function.identity()));
        Map<Long, VagaRecomendadaProjection> coincidencias = pagina.getContent().stream()
                .collect(Collectors.toMap(
                        VagaRecomendadaProjection::getId,
                        Function.identity()));

        List<DashboardVagaResponse> content = ids.stream()
                .map(vagas::get)
                .filter(vaga -> vaga != null)
                .map(vaga -> toVagaResponse(vaga, coincidencias.get(vaga.getId())))
                .toList();
        return secao(content, pagina.getTotalElements(), pagina.hasNext());
    }

    private DashboardSecaoResponse<DashboardCandidaturaResponse> buscarCandidaturasRecentes(
            Long contratanteId, int tamanho) {
        Page<CandidaturaDashboardProjection> pagina =
                candidaturaRepository.findRecentesDoContratanteEmVagasAtivas(
                        contratanteId, STATUS_ATIVOS, PageRequest.of(0, tamanho));
        List<DashboardCandidaturaResponse> content = pagina.getContent().stream()
                .map(item -> DashboardCandidaturaResponse.builder()
                        .id(item.getId())
                        .vagaId(item.getVagaId())
                        .tituloVaga(item.getTituloVaga())
                        .artistaId(item.getArtistaId())
                        .nomeArtista(item.getNomeArtista())
                        .avatarUrl(avatarService.resolverUrl(
                                item.getArtistaId(),
                                item.getFotoPerfilUsuario(),
                                item.getFotoPerfilArtista()))
                        .status(item.getStatus())
                        .dataCandidatura(item.getDataCandidatura())
                        .build())
                .toList();
        return secao(content, pagina.getTotalElements(), pagina.hasNext());
    }

    private DashboardSecaoResponse<DashboardTalentoResponse> buscarTalentosSugeridos(Long dono, int tamanho) {
        var pagina = talentoService.recomendarDoContratante(dono, tamanho);
        var content = pagina.content().stream().map(t -> DashboardTalentoResponse.builder()
                .artistaId(t.getArtistaId()).nomeExibicao(t.getNomeExibicao())
                .biografia(t.getBiografia()).localizacao(t.getLocalizacao()).urlPortfolio(t.getUrlPortfolio())
                .avatarUrl(t.getAvatarUrl())
                .funcoes(t.getAreas().stream().flatMap(a -> a.funcoes().stream().map(f ->
                        FuncaoResponse.builder().id(f.id()).areaId(a.id()).nome(f.nome()).build()))
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .quantidadeFuncoesCoincidentes(t.getQuantidadeFuncoesCoincidentes())
                .quantidadeEspecializacoesCoincidentes(t.getQuantidadeEspecializacoesCoincidentes()).build()).toList();
        return secao(content, pagina.totalElements(), pagina.hasMore());
    }

    private DashboardVagaResponse toVagaResponse(Vaga vaga, VagaRecomendadaProjection coincidencias) {
        PerfilContratante contratante = vaga.getContratante();
        String nomeContratante = contratante.getNomeEmpresa() == null
                || contratante.getNomeEmpresa().isBlank()
                ? contratante.getUsuario().getNome()
                : contratante.getNomeEmpresa();
        return DashboardVagaResponse.builder()
                .id(vaga.getId())
                .titulo(vaga.getTitulo())
                .nomeContratante(nomeContratante)
                .remuneraValor(vaga.getValorMinimo() != null && vaga.getValorMinimo().equals(vaga.getValorMaximo()) ? vaga.getValorMinimo() : null)
                .cidade(vaga.getCidade())
                .estado(vaga.getEstado())
                .modeloTrabalho(vaga.getModeloTrabalho())
                .dataPublicacao(vaga.getDataPublicacao())
                .funcoes(toFuncoes(vaga.getFuncoes()))
                .quantidadeFuncoesCoincidentes(coincidencias.getQuantidadeFuncoesCoincidentes())
                .areaId(vaga.getArea().getId())
                .areaCompativel(vaga.getArea().getNome())
                .quantidadeEspecializacoesCoincidentes(coincidencias.getQuantidadeEspecializacoesCoincidentes())
                .motivoRecomendacao("Área compatível; " + coincidencias.getQuantidadeFuncoesCoincidentes()
                        + " função(ões) e " + coincidencias.getQuantidadeEspecializacoesCoincidentes()
                        + " especialização(ões) em comum nesta área. Ordem: funções, especializações, publicação e ID.")
                .build();
    }

    private Set<FuncaoResponse> toFuncoes(Set<Funcao> funcoes) {
        return funcoes.stream()
                .sorted(Comparator.comparing(Funcao::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(funcao -> FuncaoResponse.builder().id(funcao.getId()).areaId(funcao.getArea().getId()).nome(funcao.getNome()).build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int validarTamanho(Integer size) {
        int tamanho = size == null ? TAMANHO_PADRAO : size;
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("size deve estar entre 1 e 50.");
        }
        // Preserva size 1–50; RF11 oferece somente preview com o default histórico de 5.
        return Math.min(tamanho, TAMANHO_PADRAO);
    }

    private DashboardDisponibilidadeResponse notificacoesDisponiveis() {
        return DashboardDisponibilidadeResponse.builder().disponivel(true)
                .mensagem("Alertas de candidaturas e mudancas nas vagas em tempo real.")
                .quantidadeNaoLidas(notificacaoService.contarNaoLidas()).build();
    }

    private DashboardSecaoResponse<DashboardCandidaturaResponse> buscarMinhasCandidaturas(Long dono, int tamanho) {
        var pagina = candidaturaRepository.findPreviewDoArtista(dono, PageRequest.of(0, tamanho));
        var content = pagina.stream().map(c -> DashboardCandidaturaResponse.builder()
                .id(c.getId()).vagaId(c.getVagaId()).tituloVaga(c.getTituloVaga())
                .status(c.getStatus()).dataCandidatura(c.getDataCandidatura()).build()).toList();
        return secao(content, pagina.getTotalElements(), pagina.hasNext());
    }

    private DashboardSecaoResponse<com.portifolio.dto.DashboardVagaPropriaResponse> buscarMinhasVagas(Long dono, int tamanho) {
        var pagina = vagaRepository.findPreviewDoContratante(dono, PageRequest.of(0, tamanho));
        var content = pagina.stream().map(v -> new com.portifolio.dto.DashboardVagaPropriaResponse(
                v.getId(), v.getTitulo(), v.getStatus(), v.getDataPublicacao(), v.getDataLimiteCandidatura(),
                vagaPrazoPolicy.estaVencida(v.getDataLimiteCandidatura()))).toList();
        return secao(content, pagina.getTotalElements(), pagina.hasNext());
    }

    private Map<StatusVaga, Long> vagasPorStatus(Long dono) {
        Map<StatusVaga, Long> resultado = new java.util.EnumMap<>(StatusVaga.class);
        for (StatusVaga status : StatusVaga.values()) resultado.put(status, 0L);
        for (var row : vagaRepository.contarPorStatusDoContratante(dono)) {
            resultado.put(row.getStatus(), row.getQuantidade());
        }
        return resultado;
    }

    private DashboardDisponibilidadeResponse mensagensDisponiveis(Long usuarioId) {
        return DashboardDisponibilidadeResponse.builder()
                .disponivel(true)
                .mensagem("Converse com seus contatos profissionais com privacidade.")
                .quantidadeNaoLidas(mensagemChatRepository.countNaoLidasRecebidas(usuarioId))
                .build();
    }

    private <T> DashboardSecaoResponse<T> secao(List<T> content, long total, boolean hasMore) {
        return DashboardSecaoResponse.<T>builder()
                .content(content)
                .totalElements(total)
                .hasMore(hasMore)
                .build();
    }

    private <T> DashboardSecaoResponse<T> secaoVazia() {
        return secao(List.of(), 0, false);
    }
}
