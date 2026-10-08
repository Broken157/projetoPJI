package com.portifolio.service;

import com.portifolio.dto.FuncaoResponse;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.model.Funcao;
import com.portifolio.repository.FuncaoRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FuncaoService {

    private final FuncaoRepository funcaoRepository;
    private final com.portifolio.repository.AreaArtisticaRepository areaRepository;

    @Transactional(readOnly = true)
    public com.portifolio.dto.TalentoResponse.Pagina<FuncaoResponse> listarPorArea(Short areaId, int page, int size) {
        if (page < 0 || size < 1 || size > 50)
            throw new IllegalArgumentException("page deve ser não negativo e size entre 1 e 50.");
        if (areaId == null || !areaRepository.existsById(areaId))
            throw new com.portifolio.exception.UnprocessableEntityException("Área inexistente.");
        var pagina = funcaoRepository.findByAreaId(areaId,
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id")))
                .map(this::toResponse);
        return new com.portifolio.dto.TalentoResponse.Pagina<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.hasNext(), null);
    }

    @Transactional(readOnly = true)
    public List<FuncaoResponse> listarTodos() {
        return funcaoRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FuncaoResponse buscarPorId(Long id) {
        Funcao funcao = funcaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcao não encontrada."));
        return toResponse(funcao);
    }

    private FuncaoResponse toResponse(Funcao funcao) {
        return FuncaoResponse.builder()
                .id(funcao.getId())
                .areaId(funcao.getArea().getId())
                .nome(funcao.getNome())
                .build();
    }
}
