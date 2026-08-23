package com.portifolio.realtime;

import com.portifolio.dto.NotificacaoResponse;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificacaoSseService {

    private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private final ConcurrentMap<Long, Set<SseEmitter>> emissores = new ConcurrentHashMap<>();

    public SseEmitter conectar(Long usuarioId) {
        SseEmitter emissor = new SseEmitter(TIMEOUT_MILLIS);
        emissores.computeIfAbsent(usuarioId, ignorado -> ConcurrentHashMap.newKeySet())
                .add(emissor);
        emissor.onCompletion(() -> remover(usuarioId, emissor));
        emissor.onTimeout(() -> remover(usuarioId, emissor));
        emissor.onError(erro -> remover(usuarioId, emissor));
        try {
            emissor.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (IOException erro) {
            remover(usuarioId, emissor);
            emissor.completeWithError(erro);
        }
        return emissor;
    }

    public void entregar(Long usuarioId, NotificacaoResponse notificacao) {
        Set<SseEmitter> doUsuario = emissores.get(usuarioId);
        if (doUsuario == null) {
            return;
        }
        for (SseEmitter emissor : Set.copyOf(doUsuario)) {
            try {
                emissor.send(SseEmitter.event().name("notificacao").data(notificacao));
            } catch (Exception erro) {
                remover(usuarioId, emissor);
                emissor.completeWithError(erro);
            }
        }
    }

    int conexoesAtivas(Long usuarioId) {
        return emissores.getOrDefault(usuarioId, Set.of()).size();
    }

    private void remover(Long usuarioId, SseEmitter emissor) {
        emissores.computeIfPresent(usuarioId, (id, atuais) -> {
            atuais.remove(emissor);
            return atuais.isEmpty() ? null : atuais;
        });
    }
}
