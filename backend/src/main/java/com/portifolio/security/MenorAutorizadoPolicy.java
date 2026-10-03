package com.portifolio.security;

import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoUsuario;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Proteção compartilhada por perfil público e abertura de conversa do RF45. */
@Component
@RequiredArgsConstructor
public class MenorAutorizadoPolicy {
    private final Clock clock;

    public boolean exigeProtecao(Usuario usuario) {
        return usuario.getDataNascimento() == null
                || idade(usuario) < 18;
    }

    public boolean autorizado(Usuario usuario) {
        if (usuario.getDataNascimento() == null || usuario.getTipoUsuario() != TipoUsuario.ARTISTA
                || usuario.getStatusConta() != StatusConta.ATIVA) {
            return false;
        }
        int idade = idade(usuario);
        if (idade < 14 || idade >= 18) return false;
        ResponsavelLegal responsavel = usuario.getResponsavelLegal();
        return responsavel != null && responsavel.getDataConsentimento() != null
                && !Boolean.TRUE.equals(responsavel.getConsentimentoRevogado());
    }

    private int idade(Usuario usuario) {
        return Period.between(usuario.getDataNascimento(), LocalDate.now(clock)).getYears();
    }
}
