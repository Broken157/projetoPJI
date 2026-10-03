package com.portifolio.dto;

import com.portifolio.model.enums.TipoPerfilArtistico;
import com.portifolio.model.enums.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
public class CadastroDadosRequest {
    public static final String TELEFONE = "^(?:\\+?55 ?)?(?:\\([1-9][0-9]\\)|[1-9][0-9]) ?[0-9]{4,5}-?[0-9]{4}$";

    @NotBlank(message = "Username e obrigatorio")
    @Size(min = 1, max = 30, message = "Username deve ter entre 1 e 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username aceita somente letras ASCII, numeros, ponto e sublinhado")
    private String username;

    @NotNull(message = "Data de nascimento e obrigatoria")
    private LocalDate dataNascimento;

    @NotBlank(message = "Telefone e obrigatorio")
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Pattern(regexp = TELEFONE, message = "Telefone invalido; informe DDD e numero")
    private String telefone;

    @NotNull(message = "Tipo de usuario e obrigatorio")
    private TipoUsuario tipoUsuario;

    private TipoPerfilArtistico tipoPerfilArtistico;
    private Short areaPrincipalId;

    @Size(max = 100, message = "Tipo de perfil deve ter no máximo 100 caracteres")
    private String tipoPerfilContratante;

    @Size(max = 150, message = "Nome da entidade deve ter no máximo 150 caracteres")
    private String nomeEntidade;

    @CPF(message = "CPF invalido")
    @Pattern(regexp = "(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})", message = "Formato de CPF invalido")
    private String cpf;

    @CNPJ(message = "CNPJ invalido")
    @Pattern(regexp = "(?:[0-9]{14}|[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}/[0-9]{4}-[0-9]{2})", message = "Formato de CNPJ invalido")
    private String cnpj;

    @Size(max = 150, message = "Nome do responsável deve ter no máximo 150 caracteres")
    private String nomeResponsavel;

    @Size(max = 20, message = "Telefone do responsável deve ter no máximo 20 caracteres")
    @Pattern(regexp = TELEFONE, message = "Telefone do responsavel invalido; informe DDD e numero")
    private String telefoneResponsavel;

    @Email(message = "E-mail do responsável inválido")
    @Size(max = 150, message = "E-mail do responsável deve ter no máximo 150 caracteres")
    private String emailResponsavel;
}
