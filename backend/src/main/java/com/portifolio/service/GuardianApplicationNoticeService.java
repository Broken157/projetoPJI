package com.portifolio.service;

import com.portifolio.dto.CadastroDadosRequest;
import com.portifolio.event.AvisoResponsavelCandidaturaEvento;
import com.portifolio.repository.CandidaturaRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.MenorAutorizadoPolicy;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Uma tentativa externa, sem recriar a ação ou manter a leitura aberta durante SMTP. */
@Service
@Slf4j
public class GuardianApplicationNoticeService {
    private final CandidaturaRepository candidaturas;
    private final UsuarioRepository usuarios;
    private final MenorAutorizadoPolicy menores;
    private final Validator validator;
    private final ObjectProvider<GuardianApplicationNoticeSender> senderProvider;
    private final TransactionTemplate leitura;

    public GuardianApplicationNoticeService(CandidaturaRepository candidaturas,
            UsuarioRepository usuarios, MenorAutorizadoPolicy menores, Validator validator,
            ObjectProvider<GuardianApplicationNoticeSender> senderProvider,
            PlatformTransactionManager manager) {
        this.candidaturas = candidaturas;
        this.usuarios = usuarios;
        this.menores = menores;
        this.validator = validator;
        this.senderProvider = senderProvider;
        this.leitura = new TransactionTemplate(manager);
        this.leitura.setReadOnly(true);
        this.leitura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void avisar(AvisoResponsavelCandidaturaEvento evento) {
        if (evento == null) return;
        log.info("RF44 evento=CANDIDATURA candidaturaId={} resultado=INICIO", evento.candidaturaId());
        try {
            String destinatario = leitura.execute(status -> resolverDestinatario(evento));
            GuardianApplicationNoticeSender sender = senderProvider.getIfAvailable();
            if (sender == null) {
                log.warn("RF44 evento=CANDIDATURA candidaturaId={} resultado=FALHA categoria=SMTP_INDISPONIVEL",
                        evento.candidaturaId());
                return;
            }
            sender.enviarAviso(destinatario, evento.vagaId());
            log.info("RF44 evento=CANDIDATURA candidaturaId={} resultado=SUCESSO", evento.candidaturaId());
        } catch (RuntimeException erro) {
            log.warn("RF44 evento=CANDIDATURA candidaturaId={} resultado=FALHA categoria={}",
                    evento.candidaturaId(), erro.getClass().getSimpleName());
        }
    }

    private String resolverDestinatario(AvisoResponsavelCandidaturaEvento evento) {
        if (!positivo(evento.candidaturaId()) || !positivo(evento.artistaId()) || !positivo(evento.vagaId())) {
            throw new ContextoInvalidoException();
        }
        var candidatura = candidaturas.findById(evento.candidaturaId())
                .orElseThrow(ContextoInvalidoException::new);
        if (!evento.artistaId().equals(candidatura.getArtista().getUsuarioId())
                || !evento.vagaId().equals(candidatura.getVaga().getId())) {
            throw new ContextoInvalidoException();
        }
        var usuario = usuarios.findById(evento.artistaId()).orElseThrow(ContextoInvalidoException::new);
        if (!menores.autorizado(usuario)) throw new ContextoInvalidoException();
        var responsavel = usuario.getResponsavelLegal();
        if (responsavel.getUsuario() == null
                || !usuario.getId().equals(responsavel.getUsuario().getId())) {
            throw new ContextoInvalidoException();
        }
        String email = responsavel.getEmailResponsavel();
        if (email == null || email.isBlank() || !email.equals(email.trim())
                || !validator.validateValue(CadastroDadosRequest.class, "emailResponsavel", email).isEmpty()
                || email.equalsIgnoreCase(usuario.getEmail().trim())) {
            throw new ContextoInvalidoException();
        }
        return email;
    }

    private static boolean positivo(Long id) { return id != null && id > 0; }

    private static final class ContextoInvalidoException extends RuntimeException { }
}
