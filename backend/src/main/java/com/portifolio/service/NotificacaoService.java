package com.portifolio.service;

import com.portifolio.dto.NotificacaoPaginaResponse;
import com.portifolio.dto.NotificacaoResponse;
import com.portifolio.exception.ResourceNotFoundException;
import com.portifolio.model.Notificacao;
import com.portifolio.model.Usuario;
import com.portifolio.repository.NotificacaoRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 50;

    private final NotificacaoRepository notificacaoRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional(readOnly = true)
    public NotificacaoPaginaResponse listar(Integer page, Integer size) {
        Usuario usuario = exigirUsuarioAtual();
        int pagina = page == null ? 0 : page;
        int tamanho = size == null ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
        if (pagina < 0) {
            throw new IllegalArgumentException("page nao pode ser negativo.");
        }
        if (tamanho < 1) {
            throw new IllegalArgumentException("size deve ser maior que zero.");
        }

        Page<Notificacao> resultado = notificacaoRepository.findByUsuarioDestinoId(
                usuario.getId(),
                PageRequest.of(pagina, tamanho,
                        Sort.by(Sort.Order.desc("dataCriacao"), Sort.Order.desc("id"))));
        return NotificacaoPaginaResponse.builder()
                .content(resultado.stream().map(this::toResponse).toList())
                .page(resultado.getNumber())
                .size(resultado.getSize())
                .totalElements(resultado.getTotalElements())
                .totalPages(resultado.getTotalPages())
                .first(resultado.isFirst())
                .last(resultado.isLast())
                .hasNext(resultado.hasNext())
                .hasPrevious(resultado.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas() {
        return notificacaoRepository.countByUsuarioDestinoIdAndLidaFalse(
                exigirUsuarioAtual().getId());
    }

    @Transactional
    public void marcarComoLida(Long id) {
        Usuario usuario = exigirUsuarioAtual();
        Notificacao notificacao = notificacaoRepository.findByIdAndUsuarioDestinoId(id, usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificacao nao encontrada."));
        if (!Boolean.TRUE.equals(notificacao.getLida())) {
            notificacao.setLida(true);
            notificacaoRepository.save(notificacao);
        }
    }

    @Transactional
    public void marcarTodasComoLidas() {
        notificacaoRepository.marcarTodasComoLidas(exigirUsuarioAtual().getId());
    }

    public NotificacaoResponse toResponse(Notificacao notificacao) {
        return NotificacaoResponse.builder()
                .id(notificacao.getId())
                .tipo(notificacao.getTipo())
                .mensagem(notificacao.getMensagem())
                .link(notificacao.getLink())
                .lida(notificacao.getLida())
                .data(notificacao.getDataCriacao())
                .build();
    }

    private Usuario exigirUsuarioAtual() {
        return authenticatedUserResolver.usuarioAtual()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado nao encontrado."));
    }
}
