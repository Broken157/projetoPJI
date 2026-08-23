package com.portifolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerfilContratanteRequest {

    @NotNull(message = "Usuário é obrigatório")
    private Long usuarioId;

    @Size(max = 150, message = "Nome da empresa deve ter no máximo 150 caracteres")
    private String nomeEmpresa;

    @Size(max = 100, message = "Tipo de perfil deve ter no máximo 100 caracteres")
    private String tipoPerfil;
    private String biografia;

    @Size(max = 150, message = "Localização deve ter no máximo 150 caracteres")
    private String localizacao;

    @Size(max = 255, message = "URL do banner deve ter no máximo 255 caracteres")
    @URL(message = "URL do banner deve ser válida")
    private String bannerUrl;
}
