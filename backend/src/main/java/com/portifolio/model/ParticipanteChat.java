package com.portifolio.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participantes_chat")
@Getter
@Setter
@NoArgsConstructor
public class ParticipanteChat {

    @EmbeddedId
    private ParticipanteChatId id;

    @MapsId("salaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id")
    private SalaChat sala;

    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public ParticipanteChat(SalaChat sala, Usuario usuario) {
        this.id = new ParticipanteChatId(sala.getId(), usuario.getId());
        this.sala = sala;
        this.usuario = usuario;
    }
}
