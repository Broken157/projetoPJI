package com.portifolio.service;

import com.portifolio.dto.PerfilArtistaAreaRequest;
import com.portifolio.dto.PerfilArtistaRequest;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.*;
import com.portifolio.model.enums.NivelExperiencia;
import com.portifolio.repository.*;
import com.portifolio.validation.TaxonomiaProfissional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerfilProfissionalService {
    private final AreaArtisticaRepository areas;
    private final FuncaoRepository funcoes;
    private final EspecializacaoRepository especializacoes;
    private final PerfilArtistaRepository perfis;

    record Selecao(AreaArtistica area, boolean principal, NivelExperiencia experiencia,
            Set<Funcao> funcoes, Set<Especializacao> especializacoes) {}

    /** Resolve e valida o estado final inteiro antes de modificar entidades gerenciadas. */
    List<Selecao> validar(PerfilArtista perfil, PerfilArtistaRequest request) {
        List<PerfilArtistaAreaRequest> pedidos = request.getAreas();
        if (pedidos != null && (request.getAreaPrincipalId() != null || request.getFuncaoIds() != null))
            throw invalido("Use areas ou os campos profissionais legados, sem misturá-los.");
        if (pedidos == null) pedidos = adaptarLegado(perfil, request);
        if (pedidos.stream().anyMatch(Objects::isNull)) throw invalido("Área inválida.");
        if (!pedidos.isEmpty() && pedidos.stream().filter(PerfilArtistaAreaRequest::isPrincipal).count() != 1)
            throw invalido("Selecione exatamente uma Área Principal.");
        Set<Short> areaIds = new HashSet<>();
        Map<Short, Set<Long>> funcoesPorArea = new HashMap<>();
        Set<Long> todasFuncoes = new HashSet<>();
        Set<Long> todasSpecs = new HashSet<>();
        for (var pedido : pedidos) {
            if (pedido == null || pedido.getAreaId() == null || pedido.getAreaId() <= 0
                    || !areaIds.add(pedido.getAreaId())) throw invalido("Área inválida ou duplicada.");
            var atual = atual(perfil, pedido.getAreaId());
            var ids = TaxonomiaProfissional.ids(pedido.getFuncaoIds() == null
                    ? atual == null ? List.of() : atual.getFuncoes().stream().map(Funcao::getId).toList()
                    : pedido.getFuncaoIds(), TaxonomiaProfissional.MAX_FUNCOES_ARTISTA);
            funcoesPorArea.put(pedido.getAreaId(), ids);
            todasFuncoes.addAll(ids);
            if (pedido.getEspecializacaoIds() != null)
                todasSpecs.addAll(TaxonomiaProfissional.ids(pedido.getEspecializacaoIds(),
                        TaxonomiaProfissional.MAX_ESPECIALIZACOES_ARTISTA));
        }
        var catalogoAreas = areas.findAllById(areaIds).stream().collect(Collectors.toMap(AreaArtistica::getId, Function.identity()));
        var catalogoFuncoes = todasFuncoes.isEmpty() ? Map.<Long, Funcao>of()
                : funcoes.buscarTaxonomia(todasFuncoes).stream().collect(Collectors.toMap(Funcao::getId, Function.identity()));
        var catalogoSpecs = todasSpecs.isEmpty() ? Map.<Long, Especializacao>of()
                : especializacoes.findAllById(todasSpecs).stream().collect(Collectors.toMap(Especializacao::getId, Function.identity()));
        if (catalogoAreas.size() != areaIds.size() || catalogoFuncoes.size() != todasFuncoes.size()
                || catalogoSpecs.size() != todasSpecs.size())
            throw new com.portifolio.exception.ResourceNotFoundException("ID inexistente no catálogo oficial.");
        List<Selecao> selecoes = new ArrayList<>();
        for (var pedido : pedidos) {
            var atual = atual(perfil, pedido.getAreaId());
            Set<Funcao> funcs = funcoesPorArea.get(pedido.getAreaId()).stream().map(catalogoFuncoes::get)
                    .collect(Collectors.toSet());
            if (funcs.stream().anyMatch(f -> !pedido.getAreaId().equals(f.getArea().getId())))
                throw invalido("Função incompatível com a Área.");
            var permitidas = TaxonomiaProfissional.especializacoesCompativeis(funcs);
            Set<Especializacao> specs;
            if (pedido.getEspecializacaoIds() == null) {
                specs = atual == null ? Set.of() : atual.getEspecializacoes().stream()
                        .filter(e -> permitidas.contains(e.getId())).collect(Collectors.toSet());
            } else {
                if (!permitidas.containsAll(pedido.getEspecializacaoIds()))
                    throw invalido("Especialização incompatível com as Funções da Área.");
                specs = pedido.getEspecializacaoIds().stream().map(catalogoSpecs::get).collect(Collectors.toSet());
            }
            if (specs.size() > TaxonomiaProfissional.MAX_ESPECIALIZACOES_ARTISTA)
                throw invalido("Informe até cinco Especializações por Área.");
            selecoes.add(new Selecao(catalogoAreas.get(pedido.getAreaId()), pedido.isPrincipal(),
                    pedido.getNivelExperiencia() != null ? pedido.getNivelExperiencia()
                            : atual == null ? null : atual.getNivelExperiencia(), funcs, specs));
        }
        return selecoes;
    }

    void reconciliar(PerfilArtista perfil, List<Selecao> selecoes) {
        var principal = selecoes.stream().filter(Selecao::principal).map(s -> s.area().getId()).findFirst().orElse(null);
        boolean despromoveu = false;
        for (var atual : perfil.getAreas()) {
            if (atual.isPrincipal() && !atual.getArea().getId().equals(principal)) {
                atual.setPrincipal(false);
                atual.setUltimaAtualizacao(LocalDateTime.now());
                despromoveu = true;
            }
        }
        // A demissão e a promoção são invisíveis fora da mesma transação. Evita ordem de
        // UPDATE/INSERT incompatível com o UNIQUE parcial do snapshot oficial.
        if (despromoveu) perfis.flush();
        Set<Short> mantidas = selecoes.stream().map(s -> s.area().getId()).collect(Collectors.toSet());
        perfil.getAreas().removeIf(a -> !mantidas.contains(a.getArea().getId()));
        for (var selecao : selecoes) {
            var atual = atual(perfil, selecao.area().getId());
            boolean novo = atual == null;
            if (novo) {
                atual = new PerfilArtistaArea();
                atual.setPerfil(perfil);
                atual.setArea(selecao.area());
                perfil.getAreas().add(atual);
            }
            boolean mudou = novo || atual.isPrincipal() != selecao.principal()
                    || atual.getNivelExperiencia() != selecao.experiencia()
                    || !atual.getFuncoes().equals(selecao.funcoes())
                    || !atual.getEspecializacoes().equals(selecao.especializacoes());
            atual.setPrincipal(selecao.principal());
            atual.setNivelExperiencia(selecao.experiencia());
            atual.getFuncoes().retainAll(selecao.funcoes());
            atual.getFuncoes().addAll(selecao.funcoes());
            atual.getEspecializacoes().retainAll(selecao.especializacoes());
            atual.getEspecializacoes().addAll(selecao.especializacoes());
            if (mudou) atual.setUltimaAtualizacao(LocalDateTime.now());
        }
    }

    private List<PerfilArtistaAreaRequest> adaptarLegado(PerfilArtista perfil, PerfilArtistaRequest request) {
        if (request.getFuncaoIds() != null && request.getAreaPrincipalId() == null)
            throw invalido("Informe areaPrincipalId para editar funções.");
        List<PerfilArtistaAreaRequest> pedidos = new ArrayList<>();
        for (var atual : perfil.getAreas()) {
            var pedido = new PerfilArtistaAreaRequest();
            pedido.setAreaId(atual.getArea().getId());
            pedido.setPrincipal(request.getAreaPrincipalId() == null ? atual.isPrincipal()
                    : request.getAreaPrincipalId().equals(pedido.getAreaId()));
            pedidos.add(pedido);
        }
        if (request.getAreaPrincipalId() != null) {
            var principal = pedidos.stream().filter(p -> p.getAreaId().equals(request.getAreaPrincipalId())).findFirst().orElse(null);
            if (principal == null) {
                principal = new PerfilArtistaAreaRequest();
                principal.setAreaId(request.getAreaPrincipalId());
                principal.setPrincipal(true);
                pedidos.add(principal);
            }
            if (request.getFuncaoIds() != null) principal.setFuncaoIds(new ArrayList<>(request.getFuncaoIds()));
        }
        return pedidos;
    }

    private PerfilArtistaArea atual(PerfilArtista perfil, Short id) {
        return perfil.getAreas().stream().filter(a -> id.equals(a.getArea().getId())).findFirst().orElse(null);
    }
    private UnprocessableEntityException invalido(String mensagem) { return new UnprocessableEntityException(mensagem); }
}
