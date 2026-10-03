package com.portifolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Relacao persistente por contratante; mapeamento somente, sem novos fluxos RF13/RF17. */
@Entity @Table(name = "banco_talentos") @IdClass(BancoTalentosId.class)
@Getter @Setter @NoArgsConstructor
public class BancoTalentos {
    @Id @Column(name = "contratante_id", nullable = false)
    private Long contratanteId;
    @Id @Column(name = "artista_id", nullable = false)
    private Long artistaId;
    @Column(name = "data_adicao", nullable = false)
    private LocalDateTime dataAdicao;
}
