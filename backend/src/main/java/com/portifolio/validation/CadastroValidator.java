package com.portifolio.validation;

import com.portifolio.dto.CadastroDadosRequest;
import com.portifolio.exception.ConflictException;
import com.portifolio.model.AreaArtistica;
import com.portifolio.model.enums.TipoContratante;
import com.portifolio.model.enums.TipoPerfilArtistico;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.AreaArtisticaRepository;
import com.portifolio.repository.UsuarioRepository;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CadastroValidator {
    private final Validator validator;
    private final UsuarioRepository usuarios;
    private final AreaArtisticaRepository areas;
    private final Clock clock;

    public DadosValidados validar(CadastroDadosRequest request, String email) {
        var violacoes = validator.validate(request);
        if (!violacoes.isEmpty()) {
            throw new IllegalArgumentException(violacoes.stream()
                    .map(violacao -> violacao.getPropertyPath() + ": " + violacao.getMessage())
                    .sorted().findFirst().orElseThrow());
        }
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("Este email ja esta cadastrado.");
        }
        if (usuarios.existsByUsername(request.getUsername())) {
            throw new ConflictException("Este username ja esta em uso.");
        }
        LocalDate hoje = LocalDate.now(clock);
        if (request.getDataNascimento().isAfter(hoje)) {
            throw new IllegalArgumentException("Data de nascimento não pode estar no futuro.");
        }
        int idade = Period.between(request.getDataNascimento(), hoje).getYears();
        if (idade < 14) throw new IllegalArgumentException("A idade mínima para cadastro é 14 anos.");
        if (request.getTipoUsuario() != TipoUsuario.ARTISTA && request.getTipoUsuario() != TipoUsuario.CONTRATANTE) {
            throw new IllegalArgumentException("Cadastro permitido somente para ARTISTA ou CONTRATANTE.");
        }
        if (idade < 18 && request.getTipoUsuario() == TipoUsuario.CONTRATANTE) {
            throw new IllegalArgumentException("Contratante deve ter pelo menos 18 anos.");
        }
        if (idade < 18) validarResponsavel(request, email);

        AreaArtistica area = null;
        TipoContratante contratante = null;
        boolean empresarial;
        if (request.getTipoUsuario() == TipoUsuario.ARTISTA) {
            if (request.getTipoPerfilArtistico() == null || request.getAreaPrincipalId() == null) {
                throw new IllegalArgumentException("Informe tipoPerfilArtistico e areaPrincipalId para o artista.");
            }
            area = areas.findById(request.getAreaPrincipalId())
                    .orElseThrow(() -> new IllegalArgumentException("Área artística principal não encontrada."));
            empresarial = request.getTipoPerfilArtistico() == TipoPerfilArtistico.ESTUDIO
                    || request.getTipoPerfilArtistico() == TipoPerfilArtistico.PRODUTORA_EMPRESA;
            if (empresarial && idade < 18) {
                throw new IllegalArgumentException("Responsavel por Estudio ou Produtora deve ter pelo menos 18 anos.");
            }
        } else {
            contratante = TipoContratante.deContrato(request.getTipoPerfilContratante());
            empresarial = contratante != TipoContratante.PESSOA_FISICA;
            if (empresarial && ausente(request.getNomeEntidade())) {
                throw new IllegalArgumentException("Nome da entidade e obrigatorio para este tipo de contratante.");
            }
        }
        String cpf = documento(request.getCpf());
        String cnpj = documento(request.getCnpj());
        if (empresarial ? cnpj == null : cpf == null) {
            throw new IllegalArgumentException(empresarial ? "CNPJ e obrigatorio para este subtipo."
                    : "CPF do titular ou responsavel e obrigatorio para este subtipo.");
        }
        if (cpf != null && usuarios.existsByCpf(cpf)) throw new ConflictException("CPF ja cadastrado.");
        if (cnpj != null && usuarios.existsByCnpj(cnpj)) throw new ConflictException("CNPJ ja cadastrado.");
        return new DadosValidados(idade < 18, area, contratante, cpf, cnpj);
    }

    private void validarResponsavel(CadastroDadosRequest request, String email) {
        if (ausente(request.getNomeResponsavel())) {
            throw new IllegalArgumentException("Nome do responsavel e obrigatorio para menores de 18 anos.");
        }
        if (ausente(request.getTelefoneResponsavel())) {
            throw new IllegalArgumentException("Telefone do responsavel e obrigatorio para menores de 18 anos.");
        }
        if (ausente(request.getEmailResponsavel())) {
            throw new IllegalArgumentException("Email do responsavel e obrigatorio para menores de 18 anos.");
        }
        if (email.trim().equalsIgnoreCase(request.getEmailResponsavel().trim())) {
            throw new IllegalArgumentException("O e-mail do responsável deve ser diferente do e-mail do artista.");
        }
    }

    private static boolean ausente(String valor) { return valor == null || valor.isBlank(); }
    private static String documento(String valor) { return valor == null ? null : valor.replaceAll("[./-]", ""); }

    public record DadosValidados(boolean menor, AreaArtistica area, TipoContratante contratante, String cpf, String cnpj) {}
}
