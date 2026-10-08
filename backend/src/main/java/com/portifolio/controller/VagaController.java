package com.portifolio.controller;

import com.portifolio.dto.VagaBuscaFiltro;
import com.portifolio.dto.CandidaturaVagaPaginaResponse;
import com.portifolio.dto.CandidatosVagaFiltro;
import com.portifolio.dto.ChatSalaResponse;
import com.portifolio.dto.VagaAtualizacaoRequest;
import com.portifolio.dto.VagaCancelamentoRequest;
import com.portifolio.dto.VagaListagemResponse;
import com.portifolio.dto.VagaRequest;
import com.portifolio.dto.VagaResponse;
import com.portifolio.dto.VagaStatusAcaoRequest;
import com.portifolio.model.enums.ModeloTrabalho;
import com.portifolio.model.enums.Abrangencia;
import com.portifolio.model.enums.FormaRemuneracao;
import com.portifolio.service.VagaService;
import com.portifolio.service.CandidaturaService;
import com.portifolio.repository.CategoriaAfirmativaRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vagas")
@RequiredArgsConstructor
public class VagaController {

    private final VagaService vagaService;
    private final CandidaturaService candidaturaService;
    private final CategoriaAfirmativaRepository categoriasAfirmativas;

    public record CategoriaAfirmativaOpcao(Integer id, String nome) {}

    @GetMapping("/categorias-afirmativas")
    public java.util.List<CategoriaAfirmativaOpcao> listarCategoriasAfirmativas() {
        return categoriasAfirmativas.findAll().stream()
                .map(categoria -> new CategoriaAfirmativaOpcao(categoria.getId(), categoria.getNome()))
                .toList();
    }

    // RF03 — Listagem/busca pública, paginação cursor-based (RNF12)
    @GetMapping
    public ResponseEntity<VagaListagemResponse> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) ModeloTrabalho modeloTrabalho,
            @RequestParam(required = false) String tipoContrato,
            @RequestParam(required = false) BigDecimal faixaSalarialMin,
            @RequestParam(required = false) BigDecimal faixaSalarialMax,
            @RequestParam(required = false) String areaAtuacao,
            @RequestParam(required = false) Short areaId,
            @RequestParam(required = false) Set<Long> funcaoIds,
            @RequestParam(required = false) Set<Long> especializacaoIds,
            @RequestParam(required = false) String experiencia,
            @RequestParam(required = false) Abrangencia abrangencia,
            @RequestParam(required = false) FormaRemuneracao formaRemuneracao,
            @RequestParam(required = false) Boolean afirmativa,
            @RequestParam(required = false) Set<Integer> categoriaAfirmativaIds,
            @RequestParam(required = false) Set<Long> tagIds,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long cursorCanceladas,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal valorMinimo,
            @RequestParam(required = false) BigDecimal valorMaximo,
            @RequestParam(required = false) Boolean somenteMinhasVagas,
            @RequestParam(required = false) Boolean somenteCandidatei,
            @RequestParam(required = false) Boolean somenteFavoritas,
            @RequestParam(required = false) String beneficios,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataPublicacao,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataPublicacaoInicio,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataPublicacaoFim,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataLimite,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataLimiteInicio,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataLimiteFim,
            @RequestParam(required = false) Boolean comPrazo,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Set<String> permitidos = Set.of("titulo", "empresa", "busca", "q", "cidade", "estado",
                "modeloTrabalho", "tipoContrato", "faixaSalarialMin", "faixaSalarialMax",
                "valorMinimo", "valorMaximo", "areaAtuacao", "areaId", "funcaoIds",
                "especializacaoIds", "experiencia", "abrangencia", "formaRemuneracao",
                "afirmativa", "categoriaAfirmativaIds", "tagIds", "cursor", "cursorCanceladas",
                "size", "page", "somenteMinhasVagas", "somenteCandidatei", "somenteFavoritas",
                "beneficios", "dataPublicacao", "dataPublicacaoInicio", "dataPublicacaoFim",
                "dataLimite", "dataLimiteInicio", "dataLimiteFim", "comPrazo");
        if (!permitidos.containsAll(request.getParameterMap().keySet())) {
            throw new IllegalArgumentException("Parâmetro de busca não suportado.");
        }
        if (empresa != null) {
            throw new com.portifolio.exception.UnprocessableEntityException(
                    "RF03 pesquisa o título da vaga; filtro por empresa foi removido.");
        }
        if (beneficios != null) {
            throw new com.portifolio.exception.UnprocessableEntityException(
                    "D08: filtro estruturado de benefícios aguarda catálogo oficial.");
        }
        if (tagIds != null) {
            throw new com.portifolio.exception.UnprocessableEntityException(
                    "O filtro tagIds foi substituído por funcaoIds do catálogo oficial.");
        }
        VagaBuscaFiltro filtro = new VagaBuscaFiltro();
        filtro.setTitulo(titulo);
        filtro.setEmpresa(empresa);
        if (q != null && busca != null && !q.equals(busca))
            throw new IllegalArgumentException("q e busca não podem divergir.");
        filtro.setBusca(q == null ? busca : q);
        filtro.setCidade(cidade);
        filtro.setEstado(estado);
        filtro.setModeloTrabalho(modeloTrabalho);
        filtro.setTipoContrato(tipoContrato);
        filtro.setFaixaSalarialMin(valorCompativel(valorMinimo, faixaSalarialMin));
        filtro.setFaixaSalarialMax(valorCompativel(valorMaximo, faixaSalarialMax));
        filtro.setAreaAtuacao(areaAtuacao);
        filtro.setAreaId(areaId);
        filtro.setFuncaoIds(funcaoIds);
        filtro.setEspecializacaoIds(especializacaoIds);
        filtro.setExperiencia(experiencia);
        filtro.setAbrangencia(abrangencia);
        filtro.setFormaRemuneracao(formaRemuneracao);
        filtro.setAfirmativa(afirmativa);
        filtro.setCategoriaAfirmativaIds(categoriaAfirmativaIds);
        filtro.setCursor(cursor);
        filtro.setCursorCanceladas(cursorCanceladas);
        filtro.setSize(size);
        filtro.setPage(page);
        filtro.setSomenteMinhasVagas(Boolean.TRUE.equals(somenteMinhasVagas));
        filtro.setSomenteCandidatei(Boolean.TRUE.equals(somenteCandidatei));
        filtro.setSomenteFavoritas(Boolean.TRUE.equals(somenteFavoritas));
        filtro.setDataPublicacao(dataPublicacao);
        filtro.setDataPublicacaoInicio(dataPublicacaoInicio);
        filtro.setDataPublicacaoFim(dataPublicacaoFim);
        filtro.setDataLimite(dataLimite);
        filtro.setDataLimiteInicio(dataLimiteInicio);
        filtro.setDataLimiteFim(dataLimiteFim);
        filtro.setComPrazo(comPrazo);
        return ResponseEntity.ok(vagaService.listar(filtro));
    }

    private BigDecimal valorCompativel(BigDecimal canonico, BigDecimal legado) {
        if (canonico != null && legado != null && canonico.compareTo(legado) != 0)
            throw new IllegalArgumentException("Filtros canônico e legado de remuneração divergem.");
        return canonico == null ? legado : canonico;
    }

    @GetMapping("/{id}/similares")
    public ResponseEntity<VagaListagemResponse> listarSimilares(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(vagaService.listarSimilares(id, cursor, size));
    }

    @GetMapping("/minhas")
    public ResponseEntity<VagaListagemResponse> listarMinhas(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(vagaService.listarMinhas(cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VagaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(vagaService.buscarPorId(id));
    }

    @GetMapping("/{id}/candidaturas")
    public ResponseEntity<CandidaturaVagaPaginaResponse> listarCandidaturas(
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.ModelAttribute CandidatosVagaFiltro filtro,
            @RequestParam org.springframework.util.MultiValueMap<String, String> parametros) {
        var permitidos = java.util.Set.of("busca", "status", "areaId", "funcaoId", "especializacaoId",
                "dataInicio", "dataFim", "somenteFavoritas", "page", "size");
        parametros.forEach((nome, valores) -> {
            if (!permitidos.contains(nome) || valores.size() != 1)
                throw new IllegalArgumentException("Parâmetro de candidatos inválido: " + nome);
        });
        return ResponseEntity.ok(candidaturaService.listarPorVaga(id, filtro));
    }

    @PostMapping("/{id}/candidaturas/{candidaturaId}/conversa")
    public ResponseEntity<ChatSalaResponse> conversarComCandidato(
            @PathVariable Long id, @PathVariable Long candidaturaId) {
        return ResponseEntity.ok(candidaturaService.conversarComCandidato(id, candidaturaId));
    }

    @PostMapping
    public ResponseEntity<VagaResponse> criar(@Valid @RequestBody VagaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vagaService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VagaResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody VagaAtualizacaoRequest request) {
        return ResponseEntity.ok(vagaService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VagaResponse> gerenciarStatus(
            @PathVariable Long id, @Valid @RequestBody VagaStatusAcaoRequest request) {
        return ResponseEntity.ok(vagaService.gerenciarStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id, @Valid @RequestBody(required = false) VagaCancelamentoRequest request) {
        vagaService.deletar(id, request);
        return ResponseEntity.noContent().build();
    }
}
