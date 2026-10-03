package com.portifolio.service;

import com.portifolio.dto.BancoTalentosParticipacaoResponse;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.UnauthorizedException;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.BancoTalentosRepository;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.security.GoogleAccountAccessPolicy;
import com.portifolio.security.MenorAutorizadoPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BancoTalentosParticipacaoService {
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final GoogleAccountAccessPolicy accountAccessPolicy;
    private final MenorAutorizadoPolicy menorPolicy;
    private final PerfilArtistaRepository artistas;
    private final PerfilContratanteRepository contratantes;
    private final BancoTalentosRepository banco;

    @Transactional
    public Resultado participar(Long contratanteId, Boolean confirmado) {
        Usuario artista = artistaApto();
        validarAlvo(contratanteId);
        if (!Boolean.TRUE.equals(confirmado)) {
            throw new UnprocessableEntityException("Confirme a participação no Banco de Talentos.");
        }
        if (!Boolean.TRUE.equals(artista.getPerfilCompleto()) || !artistas.existsById(artista.getId())) {
            throw new UnprocessableEntityException("Complete seu perfil antes de participar do Banco de Talentos.");
        }
        boolean criada = banco.adicionar(contratanteId, artista.getId());
        // RF36 pendente: database04 não possui tipo compatível para entrada em Banco.
        // Não publicar CANDIDATURA/SALVO/CONVITE com um significado diferente do oficial.
        return new Resultado(criada, new BancoTalentosParticipacaoResponse(contratanteId, true));
    }

    @Transactional(readOnly = true)
    public BancoTalentosParticipacaoResponse consultar(Long contratanteId) {
        Usuario artista = artistaApto();
        validarAlvo(contratanteId);
        return new BancoTalentosParticipacaoResponse(contratanteId, banco.participa(contratanteId, artista.getId()));
    }

    private Usuario artistaApto() {
        Usuario usuario = authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new UnauthorizedException("Não autenticado."));
        if (usuario.getTipoUsuario() != TipoUsuario.ARTISTA) {
            throw new ForbiddenException("Somente artistas podem participar do Banco de Talentos.");
        }
        if (usuario.getStatusConta() != StatusConta.ATIVA || !accountAccessPolicy.acessoNormalPermitido(usuario)
                || (menorPolicy.exigeProtecao(usuario) && !menorPolicy.autorizado(usuario))) {
            throw new UnprocessableEntityException("Conta sem acesso à participação no Banco de Talentos.");
        }
        return usuario;
    }

    private void validarAlvo(Long contratanteId) {
        if (contratanteId == null || contratanteId <= 0
                || contratantes.findOne(menorPolicy.perfilPublicavel(TipoUsuario.CONTRATANTE, contratanteId)).isEmpty()) {
            throw new ResourceNotFoundException("Contratante não encontrado.");
        }
    }

    public record Resultado(boolean criada, BancoTalentosParticipacaoResponse estado) {}
}
