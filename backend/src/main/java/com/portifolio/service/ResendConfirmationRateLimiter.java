package com.portifolio.service;

import com.portifolio.exception.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResendConfirmationRateLimiter {
    private static final Duration INTERVALO = Duration.ofMinutes(1);
    private static final Duration JANELA = Duration.ofDays(1);
    private static final int MAXIMO = 5;
    private static final int MAXIMO_CHAVES = 10_000;

    private final Clock clock;
    private final Map<String, Tentativa> tentativas = new HashMap<>();

    public ResendConfirmationRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized void exigirDisponibilidade(String chave) {
        exigirDisponibilidade(chave, "Aguarde antes de solicitar outro e-mail de confirmação.");
    }

    public synchronized void exigirDisponibilidade(String chave, String mensagem) {
        Instant agora = clock.instant();
        Tentativa anterior = tentativas.get(chave);
        if (anterior != null && agora.isBefore(anterior.inicio.plus(JANELA))) {
            if (agora.isBefore(anterior.ultima.plus(INTERVALO)) || anterior.quantidade >= MAXIMO) {
                throw new TooManyRequestsException(mensagem);
            }
            tentativas.put(chave, new Tentativa(anterior.inicio, agora, anterior.quantidade + 1));
            return;
        }
        if (tentativas.size() >= MAXIMO_CHAVES) {
            Iterator<Map.Entry<String, Tentativa>> iterador = tentativas.entrySet().iterator();
            while (iterador.hasNext()) {
                if (!agora.isBefore(iterador.next().getValue().inicio.plus(JANELA))) iterador.remove();
            }
            if (tentativas.size() >= MAXIMO_CHAVES) {
                throw new TooManyRequestsException(mensagem);
            }
        }
        tentativas.put(chave, new Tentativa(agora, agora, 1));
    }

    private record Tentativa(Instant inicio, Instant ultima, int quantidade) {}
}
