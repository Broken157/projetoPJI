package com.portifolio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.portifolio.model.enums.ModeloTrabalho;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VagaAtualizacaoRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 150, message = "Título deve ter no máximo 150 caracteres")
    private String titulo;

    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;

    // RF04 não exige um campo separado; o serviço grava texto vazio quando omitido.
    private String requisitos;

    @DecimalMin(value = "0.00", message = "Remuneração não pode ser negativa")
    @Digits(integer = 8, fraction = 2, message = "Remuneração deve respeitar numeric(10,2)")
    private BigDecimal valorMinimo;
    @Size(max=100) private String formaPagamento;
    @Size(max=100) private String categoria;

    @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
    private BigDecimal valorMaximo;

    // Alias antigo de valor fixo; não substitui a faixa canônica.
    @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
    private BigDecimal remuneraValor;

    @NotNull(message = "Área artística é obrigatória")
    @Positive
    private Short areaId;

    @NotNull(message = "Forma de remuneração é obrigatória")
    private com.portifolio.model.enums.FormaRemuneracao formaRemuneracao;

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    private String cidade;

    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 2, message = "Estado deve usar exatamente 2 caracteres")
    @Pattern(regexp = "[A-Za-z]{2}", message = "Estado deve conter uma UF válida com 2 letras")
    private String estado;

    private String enderecoCompleto;
    private String beneficios;

    // Rascunho pode omitir modalidade; publicação valida este campo no serviço.
    private ModeloTrabalho modeloTrabalho;

    @NotBlank(message = "Tipo de contrato é obrigatório")
    @Size(max = 100, message = "Tipo de contrato deve ter no máximo 100 caracteres")
    private String tipoContrato;

    @Size(max = 3, message = "Selecione no máximo 3 funções")
    private Set<@NotNull(message = "ID de funcao não pode ser nulo")
            @Positive(message = "ID de funcao deve ser positivo") Long> funcaoIds;

    @Size(max = 3, message = "Selecione no máximo 3 especializações")
    private Set<@NotNull @Positive Long> especializacaoIds;

    private Set<@NotNull @Positive Integer> categoriaAfirmativaIds;
    private Boolean afirmativa;

    @Size(max = 100, message = "Experiência deve ter no máximo 100 caracteres")
    private String experiencia;

    private LocalDate dataLimiteCandidatura;

    @NotNull(message = "Abrangência é obrigatória")
    private com.portifolio.model.enums.Abrangencia abrangencia;

    private List<@Size(max = 500, message = "URL da foto deve ter no máximo 500 caracteres") String> fotos;

    @com.fasterxml.jackson.annotation.JsonSetter("funcaoIds")
    public void receberFuncoes(List<Long> valores) {
        funcaoIds = selecao(valores);
    }

    @com.fasterxml.jackson.annotation.JsonSetter("especializacaoIds")
    public void receberEspecializacoes(List<Long> valores) {
        especializacaoIds = selecao(valores);
    }

    private Set<Long> selecao(List<Long> valores) {
        if (valores == null) return null;
        // Bean Validation mantém o envelope global para IDs nulos/não positivos.
        if (valores.stream().anyMatch(id -> id == null || id <= 0))
            return new java.util.LinkedHashSet<>(valores);
        return com.portifolio.validation.TaxonomiaProfissional.ids(valores, 3);
    }

    @com.fasterxml.jackson.annotation.JsonSetter("tagIds")
    public void rejeitarTagsLegadas(Object ignored) {
        throw new IllegalArgumentException("tagIds foi substituído por funcaoIds do catálogo oficial.");
    }
}
