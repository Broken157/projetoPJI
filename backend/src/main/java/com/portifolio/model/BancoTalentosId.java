package com.portifolio.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class BancoTalentosId implements Serializable {
    private Long contratanteId;
    private Long artistaId;
}
