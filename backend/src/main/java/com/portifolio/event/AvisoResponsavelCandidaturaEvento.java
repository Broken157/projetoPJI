package com.portifolio.event;

import java.util.concurrent.atomic.AtomicBoolean;

/** Referências técnicas; destinatário e autorização são resolvidos após commit. */
public final class AvisoResponsavelCandidaturaEvento {
    private final Long candidaturaId;
    private final Long artistaId;
    private final Long vagaId;
    private final AtomicBoolean tentativaIniciada = new AtomicBoolean();

    public AvisoResponsavelCandidaturaEvento(Long candidaturaId, Long artistaId, Long vagaId) {
        this.candidaturaId = candidaturaId;
        this.artistaId = artistaId;
        this.vagaId = vagaId;
    }

    public Long candidaturaId() { return candidaturaId; }
    public Long artistaId() { return artistaId; }
    public Long vagaId() { return vagaId; }

    /** Apenas a mesma instância no fluxo local; não é idempotência durável. */
    public boolean iniciarTentativa() { return tentativaIniciada.compareAndSet(false, true); }
}
