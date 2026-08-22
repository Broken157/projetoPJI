package com.portifolio.model;

import com.portifolio.model.enums.TipoNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_destino_id", nullable = false)
    private Usuario usuarioDestino;

    @Column(name = "tipo_notificacao", nullable = false,
            columnDefinition = "tipo_notificacao_enum")
    private TipoNotificacao tipo;

    @Column(name = "mensagem_alerta", nullable = false, columnDefinition = "text")
    private String mensagem;

    @Column(name = "link_contexto", nullable = false, length = 255)
    private String link;

    @Column(nullable = false)
    private Boolean lida;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
}
