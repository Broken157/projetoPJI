package com.portifolio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class CandidatosVagaFiltro {
    @Size(max = 150)
    private String busca;
    private String status;
    @Positive
    private Short areaId;
    @Positive
    private Long funcaoId;
    @Positive
    private Long especializacaoId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;
    private Boolean somenteFavoritas;
    @Min(0)
    private Integer page;
    @Min(1)
    private Integer size;
}
