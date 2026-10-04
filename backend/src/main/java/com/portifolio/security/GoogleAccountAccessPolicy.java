package com.portifolio.security;

import com.portifolio.model.Usuario;
import com.portifolio.model.ResponsavelLegal;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoPerfilArtistico;
import com.portifolio.model.enums.TipoContratante;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Estado persistido prevalece sobre o provedor e sobre JWTs emitidos anteriormente. */
@Component
@RequiredArgsConstructor
public final class GoogleAccountAccessPolicy {
    private final PerfilArtistaRepository artistas;
    private final PerfilContratanteRepository contratantes;
    private final Clock clock;

    public boolean acessoNormalPermitido(Usuario usuario) {
        if (usuario.getId() != null && usuario.getId() <= 0) return false;
        if (usuario.getStatusConta() != StatusConta.ATIVA) return false;
        if (usuario.getGoogleId() == null || Boolean.TRUE.equals(usuario.getPerfilCompleto())) return true;
        ResponsavelLegal responsavel = usuario.getResponsavelLegal();
        boolean autorizado = usuario.getTipoUsuario() == TipoUsuario.ARTISTA
                && responsavel != null && responsavel.getDataConsentimento() != null
                && !Boolean.TRUE.equals(responsavel.getConsentimentoRevogado());
        if (autorizado) return true;
        if (usuario.getDataNascimento() == null
                || Period.between(usuario.getDataNascimento(), LocalDate.now(clock)).getYears() < 18
                || !Boolean.TRUE.equals(usuario.getEmailVerificado())) return false;
        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA) {
            return artistas.buscarPublicoPorUsuarioId(usuario.getId()).filter(perfil -> {
                if (perfil.getTipoPerfilArtistico() == null
                        || perfil.getAreas().stream().noneMatch(area -> area.isPrincipal())) return false;
                boolean empresarial = perfil.getTipoPerfilArtistico() == TipoPerfilArtistico.ESTUDIO
                        || perfil.getTipoPerfilArtistico() == TipoPerfilArtistico.PRODUTORA_EMPRESA;
                return empresarial ? usuario.getCnpj() != null : usuario.getCpf() != null;
            }).isPresent();
        }
        if (usuario.getTipoUsuario() == TipoUsuario.CONTRATANTE) {
            return contratantes.findById(usuario.getId()).filter(perfil ->
                    perfil.getTipoContratante() == TipoContratante.PESSOA_FISICA ? usuario.getCpf() != null
                            : perfil.getTipoContratante() != null && usuario.getCnpj() != null
                                && perfil.getNomeEmpresa() != null && !perfil.getNomeEmpresa().isBlank()).isPresent();
        }
        return false;
    }
}
