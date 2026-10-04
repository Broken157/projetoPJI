package com.portifolio.model;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDateTime;

/** Mapping parcial da tabela oficial, somente para subqueries de visibilidade. Sem criação de schema. */
@Entity @Table(name = "moderacao_conteudo") @Getter
public class ModeracaoConteudo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="tipo_conteudo", nullable=false, columnDefinition="tipo_conteudo_enum") private String tipoConteudo;
    @Column(name="conteudo_id", nullable=false) private Long conteudoId;
    @Column(name="status_moderacao", columnDefinition="status_moderacao_enum") private String statusModeracao;
    @Column(name="data_criacao") private LocalDateTime dataCriacao;
}
