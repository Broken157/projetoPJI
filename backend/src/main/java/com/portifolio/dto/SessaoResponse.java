package com.portifolio.dto;

import java.time.LocalDateTime;

/** Metadados reais da sessão persistente; nenhum segredo ou dispositivo inferido. */
public record SessaoResponse(Long sessionId, LocalDateTime criadaEm, LocalDateTime expiraEm,
                            boolean ativa, boolean atual) {
}
