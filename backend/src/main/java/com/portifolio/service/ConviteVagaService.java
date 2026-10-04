package com.portifolio.service;

import com.portifolio.dto.ConviteVagaOpcao;
import com.portifolio.dto.ConviteVagaResponse;
import com.portifolio.dto.TalentoResponse.Pagina;
import com.portifolio.event.NotificacaoEvento;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.UnauthorizedException;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.StatusVaga;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.BancoTalentosRepository;
import com.portifolio.repository.ConviteVagaRepository;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.VagaRepository;
import com.portifolio.repository.specification.VagaSpecifications;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.security.GoogleAccountAccessPolicy;
import com.portifolio.security.MenorAutorizadoPolicy;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConviteVagaService {
    private final AuthenticatedUserResolver autenticado;
    private final GoogleAccountAccessPolicy acesso;
    private final MenorAutorizadoPolicy menor;
    private final BancoTalentosRepository banco;
    private final PerfilArtistaRepository artistas;
    private final VagaRepository vagas;
    private final VagaPrazoPolicy prazo;
    private final ConviteVagaRepository convites;
    private final NotificacaoPersistenceService notificacoes;
    private final ApplicationEventPublisher eventos;
    @Value("${app.database.legacy:false}") private boolean legacySchema;

    @Transactional(readOnly = true)
    public Pagina<ConviteVagaOpcao> listarVagas(Long artistaId, int page, int size) {
        Usuario dono = contratante();
        validarArtista(dono.getId(), artistaId);
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page deve ser não negativo e size entre 1 e 50.");
        }
        var filtro = Specification.where(VagaSpecifications.doContratante(dono.getId()))
                .and(VagaSpecifications.comStatus(StatusVaga.ABERTA))
                .and(VagaSpecifications.prazoAindaValido(prazo.hoje()));
        var pagina = vagas.findAll(filtro, PageRequest.of(page, size, Sort.by("id")));
        return new Pagina<>(pagina.stream().map(v -> new ConviteVagaOpcao(v.getId(), v.getTitulo())).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.hasNext(), null);
    }

    @Transactional
    public Resultado convidar(Long artistaId, Long vagaId, Boolean confirmado) {
        Usuario dono = contratante();
        validarArtista(dono.getId(), artistaId);
        if (!Boolean.TRUE.equals(confirmado)) {
            throw new UnprocessableEntityException("Confirme o convite para a vaga.");
        }
        if (vagaId == null || vagaId <= 0) throw new IllegalArgumentException("Vaga inválida.");
        // O mesmo lock usado pelas transições RF23/RF28 protege estado e check/insert RF42.
        var vaga = vagas.findByIdForUpdate(vagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada."));
        if (!vaga.getContratante().getUsuarioId().equals(dono.getId())) {
            throw new ResourceNotFoundException("Vaga não encontrada.");
        }
        if (vaga.getStatus() != StatusVaga.ABERTA || prazo.estaVencida(vaga)) {
            throw new UnprocessableEntityException("Convite exige vaga ABERTA e dentro do prazo.");
        }
        var anterior = convites.buscar(artistaId, vagaId);
        if (anterior.isPresent()) {
            return new Resultado(false, new ConviteVagaResponse(artistaId, vagaId, anterior.get()));
        }
        var persistida = notificacoes.persistirNaTransacaoAtual(new NotificacaoEvento(
                Set.of(artistaId), TipoNotificacao.CONVITE,
                "Um contratante convidou você para conhecer uma vaga.", ConviteVagaRepository.contexto(vagaId)))
                .getFirst();
        eventos.publishEvent(persistida);
        return new Resultado(true, new ConviteVagaResponse(artistaId, vagaId, persistida.notificacao().getId()));
    }

    /** Prova persistente de interação, sem substituir consentimento/conta atuais do RF35. */
    @Transactional(readOnly = true)
    public boolean interacaoProfissionalValida(Usuario contratante, Usuario artista) {
        return !legacySchema && contratante.getTipoUsuario() == TipoUsuario.CONTRATANTE
                && artista.getTipoUsuario() == TipoUsuario.ARTISTA
                && acesso.acessoNormalPermitido(contratante) && acesso.acessoNormalPermitido(artista)
                && (!menor.exigeProtecao(artista) || menor.autorizado(artista))
                && convites.existeInteracao(contratante.getId(), artista.getId());
    }

    private Usuario contratante() {
        Usuario usuario = autenticado.usuarioAtual().orElseThrow(() -> new UnauthorizedException("Não autenticado."));
        if (usuario.getTipoUsuario() != TipoUsuario.CONTRATANTE) {
            throw new ForbiddenException("Somente contratantes podem convidar para vaga.");
        }
        if (legacySchema || usuario.getStatusConta() != StatusConta.ATIVA || !acesso.acessoNormalPermitido(usuario)) {
            throw new UnprocessableEntityException("Conta sem acesso ao convite para vaga.");
        }
        return usuario;
    }

    private void validarArtista(Long dono, Long artistaId) {
        if (artistaId == null || artistaId <= 0 || !banco.participa(dono, artistaId)) {
            throw new ResourceNotFoundException("Artista não encontrado no seu Banco.");
        }
        var perfil = artistas.findOne(menor.perfilPublicavel(TipoUsuario.ARTISTA, artistaId))
                .orElseThrow(() -> new ResourceNotFoundException("Artista não encontrado no seu Banco."));
        if (!acesso.acessoNormalPermitido(perfil.getUsuario())) {
            throw new ResourceNotFoundException("Artista não encontrado no seu Banco.");
        }
    }

    public record Resultado(boolean criado, ConviteVagaResponse estado) {}
}
