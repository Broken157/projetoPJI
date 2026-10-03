package com.portifolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "perfis_contratantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilContratante {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "nome_empresa", length = 150)
    private String nomeEmpresa;

    @Column(name = "tipo_contratante", columnDefinition = "tipo_contratante_enum")
    private com.portifolio.model.enums.TipoContratante tipoContratante;

    @Column(columnDefinition = "text")
    private String biografia;

    @Column(length = 100)
    private String cidade;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(length = 2, columnDefinition = "char(2)")
    private String estado;

    @jakarta.persistence.Transient
    public String getLocalizacao() {
        return com.portifolio.validation.LocalizacaoArtista.formatar(cidade, estado);
    }

    public void setLocalizacao(String texto) {
        if (texto == null || texto.isBlank()) {
            cidade = null;
            estado = null;
            return;
        }
        var par = java.util.regex.Pattern.compile("^(.+?)(?:,|/|\\s+-\\s+)\\s*([A-Za-z]{2})$")
                .matcher(texto.trim());
        if (par.matches()) {
            cidade = par.group(1).trim();
            estado = par.group(2).toUpperCase(java.util.Locale.ROOT);
        } else {
            // Preserva o texto legado incompleto, sem inventar uma UF.
            cidade = texto.trim();
            estado = null;
        }
    }

    @Column(name = "banner_url", length = 255)
    private String bannerUrl;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.Transient
    public String getCpf() { return usuario == null ? null : usuario.getCpf(); }

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.persistence.Transient
    public String getCnpj() { return usuario == null ? null : usuario.getCnpj(); }

    @jakarta.persistence.Transient
    public String getTipoPerfil() { return tipoContratante == null ? null : tipoContratante.name(); }

    public void setTipoPerfil(String texto) {
        tipoContratante = com.portifolio.model.enums.TipoContratante.deContrato(texto);
    }
}
