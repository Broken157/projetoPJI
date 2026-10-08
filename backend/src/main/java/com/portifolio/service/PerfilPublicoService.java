package com.portifolio.service;

import com.portifolio.dto.ArtistaPublicoResponse;
import com.portifolio.dto.ContratantePublicoResponse;
import com.portifolio.dto.PerfilPublicoResponse;
import com.portifolio.dto.FuncaoResponse;
import com.portifolio.dto.FiltroDescobertaPublica;
import com.portifolio.dto.PerfilDescobertaResponse;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.PerfilDescobertaRepository;
import com.portifolio.security.MenorAutorizadoPolicy;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilPublicoService {

    private final PerfilArtistaRepository perfilArtistaRepository;
    private final PerfilContratanteRepository perfilContratanteRepository;
    private final AvatarService avatarService;
    private final MenorAutorizadoPolicy menorAutorizadoPolicy;
    private final com.portifolio.repository.ItemSalvoRepository itemSalvoRepository;
    private final PerfilDescobertaRepository descobertaRepository;
    private final com.portifolio.security.AuthenticatedUserResolver autenticado;
    private final com.portifolio.repository.AreaArtisticaRepository areas;
    private final com.portifolio.repository.FuncaoRepository funcoes;
    private final com.portifolio.repository.EspecializacaoRepository especializacoes;

    @Transactional(readOnly = true)
    public PerfilDescobertaResponse.Pagina descobrir(FiltroDescobertaPublica filtro) {
        if (filtro.bancoTalentosAtivo() != null)
            throw new com.portifolio.exception.UnprocessableEntityException(
                    "BLOQUEADO POR C01: estado Banco de Talentos ATIVO/DESATIVADO ausente no database05.");
        Long usuarioId = autenticado.usuarioAtual().map(Usuario::getId).orElse(null);
        if (filtro.somenteFavoritos()) {
            if (usuarioId == null) throw new com.portifolio.exception.UnauthorizedException("Autenticação obrigatória para favoritos.");
            if (filtro.tipo() == TipoUsuario.CONTRATANTE || filtro.tipoContratante() != null)
                throw new com.portifolio.exception.UnprocessableEntityException(
                        "BLOQUEADO POR C06: favorito de CONTRATANTE não é representável no database05.");
            if (!filtro.contextoArtista())
                throw new IllegalArgumentException("Selecione o contexto ARTISTA para consultar favoritos.");
        }
        validarTaxonomia(filtro);
        var pagina = descobertaRepository.buscar(filtro, usuarioId);
        Set<Long> idsArtistas = pagina.getContent().stream().filter(i -> i.tipo() == TipoUsuario.ARTISTA)
                .map(PerfilDescobertaRepository.Resumo::usuarioId).collect(Collectors.toSet());
        Set<Long> favoritos = usuarioId == null || idsArtistas.isEmpty() ? Set.of()
                : itemSalvoRepository.buscarAlvosSalvos(usuarioId,
                        com.portifolio.model.enums.TipoAlvoSalvo.PERFIL_ARTISTA, idsArtistas);
        var content = pagina.getContent().stream().map(item -> new PerfilDescobertaResponse(
                item.usuarioId(), item.tipo(), item.username(), item.nomeExibicao(),
                avatarService.resolverUrl(item.usuarioId(), item.fotoPerfil(), null), item.cidade(), item.estado(),
                item.tipoPerfil(), item.tipoContratante(), usuarioId == null || item.tipo() != TipoUsuario.ARTISTA
                        ? null : favoritos.contains(item.usuarioId()))).toList();
        return new PerfilDescobertaResponse.Pagina(content, pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages(), pagina.isFirst(), pagina.isLast(),
                pagina.hasNext(), pagina.hasPrevious());
    }

    private void validarTaxonomia(FiltroDescobertaPublica filtro) {
        if (filtro.areaId() != null && !areas.existsById(filtro.areaId()))
            throw new com.portifolio.exception.UnprocessableEntityException("Área inexistente no catálogo oficial.");
        if (filtro.especializacaoId() != null && !especializacoes.existsById(filtro.especializacaoId()))
            throw new com.portifolio.exception.UnprocessableEntityException("Especialização inexistente no catálogo oficial.");
        if (filtro.funcaoId() != null) {
            var selecao = funcoes.buscarTaxonomia(Set.of(filtro.funcaoId()));
            if (selecao.isEmpty()) throw new com.portifolio.exception.UnprocessableEntityException("Função inexistente no catálogo oficial.");
            var funcao = selecao.getFirst();
            if (filtro.areaId() != null && !filtro.areaId().equals(funcao.getArea().getId()))
                throw new com.portifolio.exception.UnprocessableEntityException("Função incompatível com a Área.");
            if (filtro.especializacaoId() != null && !com.portifolio.validation.TaxonomiaProfissional
                    .especializacoesCompativeis(selecao).contains(filtro.especializacaoId()))
                throw new com.portifolio.exception.UnprocessableEntityException("Especialização incompatível com a Função.");
        } else if (filtro.areaId() != null && filtro.especializacaoId() != null
                && especializacoes.contarDaArea(filtro.areaId(), Set.of(filtro.especializacaoId())) != 1) {
            throw new com.portifolio.exception.UnprocessableEntityException("Especialização incompatível com a Área.");
        }
    }

    @Transactional(readOnly = true)
    public PerfilPublicoResponse buscar(TipoUsuario tipo, Long usuarioId) {
        return switch (tipo) {
            case ARTISTA -> buscarArtista(usuarioId);
            case CONTRATANTE -> buscarContratante(usuarioId);
            default -> throw perfilNaoEncontrado();
        };
    }

    private ArtistaPublicoResponse buscarArtista(Long usuarioId) {
        PerfilArtista perfil = perfilArtistaRepository.findOne(
                menorAutorizadoPolicy.perfilPublicavel(TipoUsuario.ARTISTA, usuarioId))
                .orElseThrow(this::perfilNaoEncontrado);
        Usuario usuario = perfil.getUsuario();

        Set<FuncaoResponse> funcoes = perfil.getFuncoes().stream()
                .sorted(Comparator.comparing(funcao -> funcao.getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(funcao -> FuncaoResponse.builder().id(funcao.getId()).areaId(funcao.getArea().getId()).nome(funcao.getNome()).build())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return ArtistaPublicoResponse.builder()
                .usuarioId(perfil.getUsuarioId())
                .nomeExibicao(usuario.getNome())
                .biografia(perfil.getBiografia())
                .localizacao(perfil.getLocalizacao())
                .urlPortfolio(perfil.getUrlPortfolio())
                .bannerUrl(perfil.getBannerUrl())
                .avatarUrl(avatarService.resolverUrl(
                        perfil.getUsuarioId(), usuario.getFotoPerfil(), null))
                .funcoes(funcoes)
                .quantidadeSalvos(itemSalvoRepository.countByTipoAlvoAndAlvoId(
                        com.portifolio.model.enums.TipoAlvoSalvo.PERFIL_ARTISTA,usuarioId))
                .build();
    }

    private ContratantePublicoResponse buscarContratante(Long usuarioId) {
        PerfilContratante perfil = perfilContratanteRepository.findOne(
                menorAutorizadoPolicy.perfilPublicavel(TipoUsuario.CONTRATANTE, usuarioId))
                .orElseThrow(this::perfilNaoEncontrado);
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

    private ResourceNotFoundException perfilNaoEncontrado() {
        return new ResourceNotFoundException("Perfil publico nao encontrado.");
    }
}
