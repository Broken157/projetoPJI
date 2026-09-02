package com.portifolio.service;

import com.portifolio.dto.ChatEventoResponse;
import com.portifolio.dto.ChatMensagemPaginaResponse;
import com.portifolio.dto.ChatMensagemResponse;
import com.portifolio.dto.ChatNaoLidasCountResponse;
import com.portifolio.dto.ChatSalaPaginaResponse;
import com.portifolio.dto.ChatSalaResponse;
import com.portifolio.event.ChatEventoPosCommit;
import com.portifolio.event.NotificacaoEvento;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.MensagemChat;
import com.portifolio.model.ParticipanteChat;
import com.portifolio.model.SalaChat;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.ChatEventoTipo;
import com.portifolio.model.enums.TipoNotificacao;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.MensagemChatRepository;
import com.portifolio.repository.ParticipanteChatRepository;
import com.portifolio.repository.SalaChatRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.repository.projection.ChatMensagemProjection;
import com.portifolio.repository.projection.ChatSalaResumoProjection;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    public static final String MENSAGEM_EXCLUIDA = "Mensagem excluída pelo autor";
    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 50;

    private final UsuarioRepository usuarioRepository;
    private final SalaChatRepository salaChatRepository;
    private final ParticipanteChatRepository participanteChatRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Transactional
    public ChatSalaResponse criarOuReutilizarSala(String emailAutenticado, Long usuarioDestinoId) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        Usuario destino = usuarioRepository.findById(usuarioDestinoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario de destino nao encontrado."));
        validarDupla(atual, destino);
        bloquearDupla(atual.getId(), destino.getId());

        return participanteChatRepository.findSalaIdDaDupla(atual.getId(), destino.getId())
                .map(salaId -> salaResponse(salaId, destino))
                .orElseGet(() -> criarSala(atual, destino));
    }

    @Transactional(readOnly = true)
    public ChatSalaPaginaResponse listarSalas(String emailAutenticado, Integer page, Integer size) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        Page<ChatSalaResumoProjection> pagina = participanteChatRepository.findSalasDoUsuario(
                atual.getId(), PageRequest.of(validarPagina(page), validarTamanho(size)));
        return ChatSalaPaginaResponse.builder()
                .content(pagina.getContent().stream().map(this::toSalaResponse).toList())
                .page(pagina.getNumber())
                .size(pagina.getSize())
                .totalElements(pagina.getTotalElements())
                .totalPages(pagina.getTotalPages())
                .hasMore(pagina.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public ChatMensagemPaginaResponse listarMensagens(
            String emailAutenticado, Long salaId, Integer page, Integer size) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        exigirParticipante(salaId, atual.getId());
        Page<ChatMensagemProjection> pagina = mensagemChatRepository.findHistorico(
                salaId, PageRequest.of(validarPagina(page), validarTamanho(size)));
        List<ChatMensagemResponse> mensagens = pagina.getContent().stream()
                .map(this::toMensagemResponse)
                .toList();
        return ChatMensagemPaginaResponse.builder()
                .content(mensagens)
                .page(pagina.getNumber())
                .size(pagina.getSize())
                .totalElements(pagina.getTotalElements())
                .totalPages(pagina.getTotalPages())
                .hasMore(pagina.hasNext())
                .build();
    }

    @Transactional
    public ChatMensagemResponse enviarMensagem(
            String emailAutenticado, Long salaId, String texto) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        SalaChat sala = salaChatRepository.findById(salaId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala nao encontrada."));
        List<Long> participantes = exigirParticipante(salaId, atual.getId());
        String textoValidado = validarTexto(texto);

        MensagemChat mensagem = new MensagemChat();
        mensagem.setSala(sala);
        mensagem.setRemetente(atual);
        mensagem.setTexto(textoValidado);
        mensagem.setUrlAnexo(null);
        mensagem.setLida(false);
        mensagem.setDataEnvio(LocalDateTime.now());
        mensagem = mensagemChatRepository.saveAndFlush(mensagem);

        ChatMensagemResponse resposta = toMensagemResponse(mensagem);
        publicarChat(participantes, ChatEventoTipo.NOVA_MENSAGEM, salaId, resposta, List.of());
        Long destinatarioId = outroParticipante(participantes, atual.getId());
        eventPublisher.publishEvent(new NotificacaoEvento(
                Set.of(destinatarioId),
                TipoNotificacao.MENSAGEM,
                "Você recebeu uma nova mensagem.",
                "/mensagens?sala=" + salaId));
        return resposta;
    }

    @Transactional
    public ChatMensagemResponse editarMensagem(
            String emailAutenticado, Long mensagemId, String texto) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        MensagemChat mensagem = mensagemDetalhada(mensagemId);
        exigirAutor(mensagem, atual.getId());
        if (mensagemExcluida(mensagem)) {
            throw new UnprocessableEntityException("Mensagem excluida nao pode ser editada.");
        }
        if (mensagem.getDataEnvio() == null
                || mensagem.getDataEnvio().plusMinutes(15).isBefore(LocalDateTime.now())) {
            throw new UnprocessableEntityException("Prazo de 15 minutos para edicao encerrado.");
        }
        mensagem.setTexto(validarTexto(texto));
        mensagem = mensagemChatRepository.saveAndFlush(mensagem);
        ChatMensagemResponse resposta = toMensagemResponse(mensagem);
        publicarChat(
                participanteChatRepository.findUsuarioIdsBySalaId(mensagem.getSala().getId()),
                ChatEventoTipo.EDICAO,
                mensagem.getSala().getId(),
                resposta,
                List.of(mensagem.getId()));
        return resposta;
    }

    @Transactional
    public void excluirMensagem(String emailAutenticado, Long mensagemId) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        MensagemChat mensagem = mensagemDetalhada(mensagemId);
        exigirAutor(mensagem, atual.getId());
        if (!mensagemExcluida(mensagem)) {
            mensagem.setTexto(MENSAGEM_EXCLUIDA);
            mensagem.setUrlAnexo(null);
            mensagem = mensagemChatRepository.saveAndFlush(mensagem);
        }
        ChatMensagemResponse resposta = toMensagemResponse(mensagem);
        publicarChat(
                participanteChatRepository.findUsuarioIdsBySalaId(mensagem.getSala().getId()),
                ChatEventoTipo.EXCLUSAO,
                mensagem.getSala().getId(),
                resposta,
                List.of(mensagem.getId()));
    }

    @Transactional
    public int marcarRecebidasComoLidas(String emailAutenticado, Long salaId) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        List<Long> participantes = exigirParticipante(salaId, atual.getId());
        List<Long> mensagemIds = mensagemChatRepository.findIdsNaoLidasRecebidas(
                salaId, atual.getId());
        if (mensagemIds.isEmpty()) {
            return 0;
        }
        int atualizadas = mensagemChatRepository.marcarRecebidasComoLidas(salaId, atual.getId());
        publicarChat(
                List.of(outroParticipante(participantes, atual.getId())),
                ChatEventoTipo.LEITURA,
                salaId,
                null,
                mensagemIds);
        return atualizadas;
    }

    @Transactional(readOnly = true)
    public ChatNaoLidasCountResponse contarNaoLidas(String emailAutenticado) {
        Usuario atual = usuarioPorEmail(emailAutenticado);
        return new ChatNaoLidasCountResponse(
                mensagemChatRepository.countNaoLidasRecebidas(atual.getId()));
    }

    private ChatSalaResponse criarSala(Usuario atual, Usuario destino) {
        SalaChat sala = new SalaChat();
        sala.setDataCriacao(LocalDateTime.now());
        sala = salaChatRepository.saveAndFlush(sala);
        participanteChatRepository.saveAllAndFlush(List.of(
                new ParticipanteChat(sala, atual),
                new ParticipanteChat(sala, destino)));
        return salaResponse(sala.getId(), destino);
    }

    private void validarDupla(Usuario atual, Usuario destino) {
        if (atual.getId().equals(destino.getId())) {
            throw new UnprocessableEntityException("Nao e permitido criar conversa consigo mesmo.");
        }
        if (atual.getTipoUsuario() == destino.getTipoUsuario()) {
            throw new UnprocessableEntityException(
                    "O chat privado exige um ARTISTA e um CONTRATANTE.");
        }
        if (menorDeIdade(atual) || menorDeIdade(destino)) {
            Long artistaId = atual.getTipoUsuario() == TipoUsuario.ARTISTA
                    ? atual.getId() : destino.getId();
            Long contratanteId = atual.getTipoUsuario() == TipoUsuario.CONTRATANTE
                    ? atual.getId() : destino.getId();
            if (!candidaturaRepository.existsByArtistaUsuarioIdAndVagaContratanteUsuarioId(
                    artistaId, contratanteId)) {
                throw new UnprocessableEntityException(
                        "Chat com menor exige interacao profissional valida entre os participantes.");
            }
        }
    }

    private boolean menorDeIdade(Usuario usuario) {
        return usuario.getDataNascimento() != null
                && Period.between(usuario.getDataNascimento(), LocalDate.now()).getYears() < 18;
    }

    private void bloquearDupla(Long usuarioA, Long usuarioB) {
        String chave = Math.min(usuarioA, usuarioB) + ":" + Math.max(usuarioA, usuarioB);
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(:chave as text), 0))")
                .setParameter("chave", chave)
                .getSingleResult();
    }

    private Usuario usuarioPorEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException("Usuario autenticado nao encontrado.");
        }
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado nao encontrado."));
    }

    private List<Long> exigirParticipante(Long salaId, Long usuarioId) {
        if (!participanteChatRepository.existsBySala_IdAndUsuario_Id(salaId, usuarioId)) {
            throw new ResourceNotFoundException("Sala nao encontrada.");
        }
        List<Long> participantes = participanteChatRepository.findUsuarioIdsBySalaId(salaId);
        if (participantes.size() != 2) {
            throw new UnprocessableEntityException("Sala nao representa uma conversa direta valida.");
        }
        return participantes;
    }

    private Long outroParticipante(List<Long> participantes, Long usuarioId) {
        return participantes.stream()
                .filter(id -> !id.equals(usuarioId))
                .findFirst()
                .orElseThrow(() -> new UnprocessableEntityException(
                        "Sala nao possui destinatario valido."));
    }

    private MensagemChat mensagemDetalhada(Long mensagemId) {
        return mensagemChatRepository.findDetalhadaById(mensagemId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem nao encontrada."));
    }

    private void exigirAutor(MensagemChat mensagem, Long usuarioId) {
        exigirParticipante(mensagem.getSala().getId(), usuarioId);
        if (!mensagem.getRemetente().getId().equals(usuarioId)) {
            throw new ResourceNotFoundException("Mensagem nao encontrada.");
        }
    }

    private String validarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Texto da mensagem e obrigatorio.");
        }
        String normalizado = texto.trim();
        if (normalizado.length() > 4000) {
            throw new IllegalArgumentException(
                    "Texto da mensagem deve ter no maximo 4000 caracteres.");
        }
        if (MENSAGEM_EXCLUIDA.equals(normalizado)) {
            throw new IllegalArgumentException("Texto reservado pelo sistema.");
        }
        return normalizado;
    }

    private void publicarChat(
            List<Long> destinatarios,
            ChatEventoTipo tipo,
            Long salaId,
            ChatMensagemResponse mensagem,
            List<Long> mensagemIds) {
        eventPublisher.publishEvent(new ChatEventoPosCommit(
                Set.copyOf(destinatarios),
                ChatEventoResponse.builder()
                        .tipo(tipo)
                        .salaId(salaId)
                        .mensagem(mensagem)
                        .mensagemIds(List.copyOf(mensagemIds))
                        .build()));
    }

    private ChatSalaResponse salaResponse(Long salaId, Usuario destino) {
        return ChatSalaResponse.builder()
                .salaId(salaId)
                .participanteId(destino.getId())
                .participanteNome(destino.getNome())
                .participanteAvatar(destino.getFotoPerfil())
                .ultimaMensagem(null)
                .ultimaMensagemData(null)
                .naoLidas(0)
                .build();
    }

    private ChatSalaResponse toSalaResponse(ChatSalaResumoProjection item) {
        return ChatSalaResponse.builder()
                .salaId(item.getSalaId())
                .participanteId(item.getParticipanteId())
                .participanteNome(item.getParticipanteNome())
                .participanteAvatar(item.getParticipanteAvatar())
                .ultimaMensagem(item.getUltimaMensagem())
                .ultimaMensagemData(item.getUltimaMensagemData())
                .naoLidas(item.getNaoLidas() == null ? 0 : item.getNaoLidas())
                .build();
    }

    private ChatMensagemResponse toMensagemResponse(ChatMensagemProjection item) {
        return ChatMensagemResponse.builder()
                .id(item.getId())
                .salaId(item.getSalaId())
                .remetenteId(item.getRemetenteId())
                .remetenteNome(item.getRemetenteNome())
                .remetenteAvatar(item.getRemetenteAvatar())
                .texto(item.getTexto())
                .urlAnexo(item.getUrlAnexo())
                .lida(Boolean.TRUE.equals(item.getLida()))
                .excluida(MENSAGEM_EXCLUIDA.equals(item.getTexto()))
                .dataEnvio(item.getDataEnvio())
                .build();
    }

    private ChatMensagemResponse toMensagemResponse(MensagemChat mensagem) {
        Usuario remetente = mensagem.getRemetente();
        return ChatMensagemResponse.builder()
                .id(mensagem.getId())
                .salaId(mensagem.getSala().getId())
                .remetenteId(remetente.getId())
                .remetenteNome(remetente.getNome())
                .remetenteAvatar(remetente.getFotoPerfil())
                .texto(mensagem.getTexto())
                .urlAnexo(mensagem.getUrlAnexo())
                .lida(Boolean.TRUE.equals(mensagem.getLida()))
                .excluida(mensagemExcluida(mensagem))
                .dataEnvio(mensagem.getDataEnvio())
                .build();
    }

    private boolean mensagemExcluida(MensagemChat mensagem) {
        return MENSAGEM_EXCLUIDA.equals(mensagem.getTexto());
    }

    private int validarPagina(Integer page) {
        int pagina = page == null ? 0 : page;
        if (pagina < 0) {
            throw new IllegalArgumentException("page deve ser maior ou igual a zero.");
        }
        return pagina;
    }

    private int validarTamanho(Integer size) {
        int tamanho = size == null ? TAMANHO_PADRAO : size;
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("size deve estar entre 1 e 50.");
        }
        return tamanho;
    }
}
