package com.portifolio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Nenhum ID/estado enviado pelo cliente define o titular. Não gerar toString de credenciais. */
@Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
public class ExclusaoContaRequest {
    @Size(max = 1024) private String senhaAtual;
    @Size(max = 500) private String motivo;
}
