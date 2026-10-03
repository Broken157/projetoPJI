package com.portifolio.model;

import com.portifolio.model.enums.TipoUsuario;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.portifolio.model.enums.StatusConta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "username", nullable = false, unique = true, length = 30)
    @JsonIgnore
    private String username;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    // RF32: nullable — usuarios Google nao possuem senha local
    @Column(length = 255)
    @JsonIgnore
    private String senha;

    @Column(name = "tipo_usuario", nullable = false, columnDefinition = "tipo_usuario_enum")
    private TipoUsuario tipoUsuario;

    @Column(name = "perfil_completo")
    private Boolean perfilCompleto;

    @Column(name = "token_recuperacao", length = 255)
    @JsonIgnore
    private String tokenRecuperacao;

    @Column(name = "token_expiracao")
    private LocalDateTime tokenExpiracao;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter(lombok.AccessLevel.NONE)
    private java.util.Set<ResponsavelLegal> responsaveisLegais = new java.util.HashSet<>();

    @Column(name = "status_conta", nullable = false, columnDefinition = "status_conta_enum")
    private StatusConta statusConta = StatusConta.PENDENTE_VERIFICACAO_EMAIL;
    @JsonIgnore @Column(length = 14, unique = true)
    private String cpf;
    @JsonIgnore @Column(length = 14, unique = true)
    private String cnpj;
    @Column(name = "versao_termo", length = 50)
    private String versaoTermo;
    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;
    @JsonIgnore @Column(name = "token_verificacao", length = 255)
    private String tokenVerificacao;
    @Column(name = "tentativas_verificacao_email")
    private Integer tentativasVerificacaoEmail = 0;
    @Column(name = "ultimo_reenvio_verificacao")
    private LocalDateTime ultimoReenvioVerificacao;

    // Accessors legados delegam à associação normalizada, sem colunas fictícias.
    @JsonIgnore @Transient
    public ResponsavelLegal getResponsavelLegal() {
        return responsaveisLegais.stream().findFirst().orElse(null);
    }

    public void setResponsavelLegal(ResponsavelLegal responsavelLegal) {
        responsaveisLegais.clear();
        if (responsavelLegal != null) {
            responsavelLegal.setUsuario(this);
            responsaveisLegais.add(responsavelLegal);
        }
    }

    @JsonIgnore public String getNomeResponsavel() {
        ResponsavelLegal responsavelLegal = getResponsavelLegal();
        return responsavelLegal == null ? null : responsavelLegal.getNomeResponsavel();
    }
    @JsonIgnore public String getTelefoneResponsavel() {
        ResponsavelLegal responsavelLegal = getResponsavelLegal();
        return responsavelLegal == null ? null : responsavelLegal.getTelefoneResponsavel();
    }
    @JsonIgnore public String getEmailResponsavel() {
        ResponsavelLegal responsavelLegal = getResponsavelLegal();
        return responsavelLegal == null ? null : responsavelLegal.getEmailResponsavel();
    }
    public void setNomeResponsavel(String value) { if (value != null) responsavel().setNomeResponsavel(value); }
    public void setTelefoneResponsavel(String value) { if (value != null) responsavel().setTelefoneResponsavel(value); }
    public void setEmailResponsavel(String value) { if (value != null) responsavel().setEmailResponsavel(value); }
    private ResponsavelLegal responsavel() {
        ResponsavelLegal responsavelLegal = getResponsavelLegal();
        if (responsavelLegal == null) {
            responsavelLegal = new ResponsavelLegal();
            setResponsavelLegal(responsavelLegal);
        }
        return responsavelLegal;
    }

    // RF32: identificador unico da conta Google (sub do ID Token)
    @Column(name = "google_id", length = 255, unique = true)
    @JsonIgnore
    private String googleId;

    // RF34: foto vinda do Google no primeiro acesso (Opcao B)
    // Avatar centralizado na conta pelo schema oficial.
    @Column(name = "foto_perfil_url", length = 255)
    private String fotoPerfil;
}
