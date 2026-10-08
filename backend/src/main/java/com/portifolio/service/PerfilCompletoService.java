package com.portifolio.service;

import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regra central e exclusivamente server-side de completude do RF08. */
@Service
@RequiredArgsConstructor
public class PerfilCompletoService {
    @org.springframework.beans.factory.annotation.Value("${app.database.legacy:false}")
    private boolean legacySchema;

    private final UsuarioRepository usuarioRepository;
    private final PerfilArtistaRepository perfilArtistaRepository;
    private final PerfilContratanteRepository perfilContratanteRepository;

    @Transactional
    public boolean recalcular(Usuario usuario) {
        if (legacySchema) return Boolean.TRUE.equals(usuario.getPerfilCompleto());
        boolean completo;
        PerfilArtista perfilArtista = null;

        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA) {
            Optional<PerfilArtista> perfil = perfilArtistaRepository.buscarProfissional(usuario.getId());
            perfilArtista = perfil.orElse(null);
            completo = perfil.map(valor -> calcularArtista(usuario, valor)).orElse(false);
        } else if (usuario.getTipoUsuario() == TipoUsuario.CONTRATANTE) {
            completo = perfilContratanteRepository.findById(usuario.getId())
                    .map(perfil -> calcularContratante(usuario, perfil))
                    .orElse(false);
        } else {
            completo = false;
        }

        boolean tornouCompleto = !Boolean.TRUE.equals(usuario.getPerfilCompleto()) && completo;
        usuario.setPerfilCompleto(completo);
        usuarioRepository.save(usuario);

        if (tornouCompleto && perfilArtista != null) {
            perfilArtista.setUltimaAtualizacao(LocalDateTime.now());
            perfilArtistaRepository.save(perfilArtista);
        }
        return completo;
    }

    public boolean calcularArtista(Usuario usuario, PerfilArtista perfil) {
        return cadastroCompleto(usuario)
                && preenchido(usuario.getCpf())
                && preenchido(perfil.getBiografia())
                && preenchido(perfil.getCidade()) && perfil.getCidade().trim().length() <= 100
                && preenchido(perfil.getEstado()) && perfil.getEstado().matches("[A-Za-z]{2}")
                && perfil.getTipoPerfilArtistico() != null
                && (!(perfil.getTipoPerfilArtistico() == com.portifolio.model.enums.TipoPerfilArtistico.ESTUDIO
                    || perfil.getTipoPerfilArtistico() == com.portifolio.model.enums.TipoPerfilArtistico.PRODUTORA_EMPRESA)
                    || preenchido(usuario.getCnpj()))
                && taxonomiaPrincipalCompleta(perfil);
    }

    private boolean taxonomiaPrincipalCompleta(PerfilArtista perfil) {
        if (perfil.getAreas().stream().anyMatch(a -> !com.portifolio.validation.TaxonomiaProfissional.coerente(a))) return false;
        if (perfil.getAreas().stream().map(a -> a.getArea().getId()).distinct().count() != perfil.getAreas().size()) return false;
        var principais = perfil.getAreas().stream()
                .filter(com.portifolio.model.PerfilArtistaArea::isPrincipal).toList();
        if (principais.size() != 1) return false;
        var area = principais.getFirst();
        if (area.getArea() == null || area.getArea().getId() == null || area.getNivelExperiencia() == null
                || area.getFuncoes() == null || area.getFuncoes().isEmpty()
                || area.getEspecializacoes() == null || area.getEspecializacoes().isEmpty()) return false;
        return true;
    }

    public boolean calcularContratante(Usuario usuario, PerfilContratante perfil) {
        return cadastroCompleto(usuario)
                && perfil.getTipoContratante() != null
                && preenchido(perfil.getLocalizacao())
                && (perfil.getTipoContratante() == com.portifolio.model.enums.TipoContratante.PESSOA_FISICA
                    ? preenchido(usuario.getCpf())
                    : preenchido(usuario.getCnpj()) && preenchido(perfil.getNomeEmpresa()));
    }

    private boolean cadastroCompleto(Usuario usuario) {
        return preenchido(usuario.getNome())
                && usuario.getDataNascimento() != null
                && preenchido(usuario.getTelefone())
                && preenchido(usuario.getEmail())
                && (preenchido(usuario.getSenha()) || preenchido(usuario.getGoogleId()));
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
