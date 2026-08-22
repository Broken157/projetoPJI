package com.portifolio.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VagaRequest extends VagaAtualizacaoRequest {

    // Campo legado mantido apenas para compatibilidade com o frontend atual.
    // A propriedade da vaga é sempre derivada do usuário autenticado pelo JWT.
    private Long contratanteId;
}
