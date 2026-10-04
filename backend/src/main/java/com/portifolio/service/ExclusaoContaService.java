package com.portifolio.service;

import com.portifolio.dto.ExclusaoContaRequest;
import com.portifolio.dto.ExclusaoContaResponse;
import com.portifolio.exception.ConflictException;
import com.portifolio.exception.ForbiddenException;
import com.portifolio.exception.UnauthorizedException;
import com.portifolio.exception.UnprocessableEntityException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Orquestra DML por domínio sobre database05; não chama a procedure legada nem cascades JPA. */
@Service
public class ExclusaoContaService {
    private static final Logger LOG = LoggerFactory.getLogger(ExclusaoContaService.class);
    private static final String REPORTADA = """
            (exists(select 1 from reportes_usuario r where r.tipo_conteudo='MENSAGEM' and r.conteudo_id=m.id)
             or exists(select 1 from moderacao_conteudo r where r.tipo_conteudo='MENSAGEM' and r.conteudo_id=m.id))
            """;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwords;
    private final PortfolioStorageService portfolio;
    private final ChatAnexoStorage chat;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    public ExclusaoContaService(JdbcTemplate jdbc, BCryptPasswordEncoder passwords,
            PortfolioStorageService portfolio, ChatAnexoStorage chat, Clock clock,
            PlatformTransactionManager manager) {
        this.jdbc = jdbc; this.passwords = passwords; this.portfolio = portfolio;
        this.chat = chat; this.clock = clock; this.tx = new TransactionTemplate(manager);
    }

    public ExclusaoContaResponse excluir(Long id, String email, ExclusaoContaRequest request) {
        if (id == null || id <= 0 || email == null) throw new UnauthorizedException("Autenticação necessária.");
        AtomicBoolean limpezaPendente = new AtomicBoolean();
        ExclusaoContaResponse result;
        try {
            result = tx.execute(status -> processar(id, email, request, limpezaPendente));
        } catch (ForbiddenException | UnauthorizedException | UnprocessableEntityException | IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // Não devolver SQL, causas, PII ou detalhes do conjunto de dados ao cliente.
            throw new ConflictException("Não foi possível concluir a exclusão; a transação foi revertida.");
        }
        return new ExclusaoContaResponse(result.comprovanteHash(), result.dataExclusao(),
                limpezaPendente.get() ? "CONCLUIDA_COM_LIMPEZA_PENDENTE" : "CONCLUIDA");
    }

    private ExclusaoContaResponse processar(long id, String email, ExclusaoContaRequest request,
            AtomicBoolean limpezaPendente) {
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?, 0))", "rf22:" + id);
        var rows = jdbc.queryForList("select senha,status_conta::text as estado from usuarios where id=? and email=? for update", id, email);
        if (rows.isEmpty() || !"ATIVA".equals(rows.getFirst().get("estado")))
            throw new UnauthorizedException("Conta sem acesso normal.");
        String senha = (String) rows.getFirst().get("senha");
        if (senha == null || senha.isBlank()) throw new UnprocessableEntityException(
                "Exclusão de conta exclusivamente Google aguarda confirmação forte específica.");
        if (request.getSenhaAtual() == null || request.getSenhaAtual().isBlank())
            throw new IllegalArgumentException("Informe a senha atual.");
        if (!passwords.matches(request.getSenhaAtual(), senha)) throw new ForbiddenException("Senha atual incorreta.");
        jdbc.queryForList("select usuario_id from perfis_artistas where usuario_id=? for update", id);
        jdbc.queryForList("select usuario_id from perfis_contratantes where usuario_id=? for update", id);
        jdbc.queryForList("select id from vagas where contratante_id=? order by id for update", id);
        jdbc.queryForList("select id from portfolio_arquivos where artista_id=? order by id for update", id);
        jdbc.queryForList("select id from mensagens_chat where remetente_id=? order by id for update", id);
        validarEstruturaEPolitica(id);
        List<Long> salas = jdbc.queryForList("select sala_id from participantes_chat where usuario_id=?", Long.class, id);
        List<Runnable> limpeza = planejarArquivos(id);
        jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=?", id);
        jdbc.update("delete from refresh_tokens where usuario_id=?", id);

        limparReferenciasPublicas(id);
        anonimizar(id);
        limparRelacoes(id);
        // Exclui apenas salas sem participante e sem evidência reportada/moderação.
        for (long sala : salas) jdbc.update("""
                delete from salas_chat s where not exists(select 1 from participantes_chat p where p.sala_id=s.id)
                  and not exists(select 1 from mensagens_chat m where m.sala_id=s.id and
                    (exists(select 1 from reportes_usuario r where r.tipo_conteudo='MENSAGEM' and r.conteudo_id=m.id)
                     or exists(select 1 from moderacao_conteudo r where r.tipo_conteudo='MENSAGEM' and r.conteudo_id=m.id)))
                  and s.id=?
                """, sala);
        String hash = hashAleatorio();
        LocalDateTime data = LocalDateTime.now(clock);
        jdbc.update("insert into log_exclusoes_lgpd(motivo_opcional,data_exclusao,comprovante_hash) values(null,?,?)", data, hash);
        if (jdbc.update("delete from usuarios where id=?", id) != 1) throw new ConflictException("Exclusão concorrente.");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (Runnable acao : limpeza) try { acao.run(); }
                catch (RuntimeException ex) {
                    limpezaPendente.set(true);
                    // Não registrar path, nome original, conteúdo, usuário ou credencial.
                    LOG.warn("Limpeza física RF22 pendente; classe de falha: {}", ex.getClass().getSimpleName());
                }
            }
        });
        return new ExclusaoContaResponse(hash, data, "CONCLUIDA");
    }

    private void validarEstruturaEPolitica(long id) {
        // A conta reservada já pertence ao seed oficial. Não criá-la ou reparar o banco.
        var reservada = jdbc.queryForList("""
                select u.id from usuarios u where u.id=0 and u.username='usuario.removido'
                  and u.nome='Usuário Removido' and u.google_id is null
                  and u.email='anonimo@sosartistas.local' and u.telefone='00000000000'
                  and u.cpf='00000000000' and u.cnpj is null and u.foto_perfil_url is null
                  and exists(select 1 from perfis_artistas a where a.usuario_id=0 and a.banner_url is null and a.url_portfolio is null)
                  and exists(select 1 from perfis_contratantes c where c.usuario_id=0 and c.nome_empresa='Entidade Removida' and c.banner_url is null) for key share
                """);
        if (reservada.isEmpty()) throw new UnprocessableEntityException("Estrutura oficial de anonimização indisponível.");
        if (existe("select exists(select 1 from comunidades where criador_id=? union all select 1 from editais where publicador_id=?)", id, id))
            throw new UnprocessableEntityException("Exclusão com autoria de comunidade ou edital aguarda política específica.");
        if (existe("""
                select exists(select 1 from vagas v where v.contratante_id=? and
                  (v.status is null or (v.status='RASCUNHO' and
                    (exists(select 1 from candidaturas c where c.vaga_id=v.id)
                     or exists(select 1 from log_vagas_canceladas l where l.vaga_id=v.id)
                     or exists(select 1 from reportes_usuario r where r.tipo_conteudo='VAGA' and r.conteudo_id=v.id)
                     or exists(select 1 from moderacao_conteudo r where r.tipo_conteudo='VAGA' and r.conteudo_id=v.id)))))
                """, id)) throw new UnprocessableEntityException("Vaga com estado ou histórico incompatível requer avaliação antes da exclusão.");
        if (existe("""
                select exists(select 1 from reportes_usuario r where r.tipo_conteudo='GALERIA' and r.conteudo_id in
                  (select g.id from galerias_virtuais g where g.dono_id=? union select i.galeria_id from itens_galeria i
                   join portfolio_arquivos a on a.id=i.arquivo_id where a.artista_id=?)
                  union all select 1 from moderacao_conteudo r where r.tipo_conteudo='GALERIA' and r.conteudo_id in
                  (select g.id from galerias_virtuais g where g.dono_id=? union select i.galeria_id from itens_galeria i
                   join portfolio_arquivos a on a.id=i.arquivo_id where a.artista_id=?)
                  union all select 1 from denuncias_plagio d where d.perfil_denunciado_id=?
                    and exists(select 1 from portfolio_arquivos a where a.artista_id=?))
                """, id, id, id, id, id, id)) throw new UnprocessableEntityException(
                    "Portfólio ou galeria com evidência necessária aguarda retenção restrita específica.");
        if (existe("""
                select exists(select 1 from interacoes_galeria x join itens_galeria i on i.arquivo_id=x.arquivo_id
                  where x.usuario_id=? and
                    (exists(select 1 from reportes_usuario r where r.tipo_conteudo='GALERIA' and r.conteudo_id=i.galeria_id)
                     or exists(select 1 from moderacao_conteudo r where r.tipo_conteudo='GALERIA' and r.conteudo_id=i.galeria_id)))
                """, id)) throw new UnprocessableEntityException("Interação em galeria com evidência requer avaliação antes da exclusão.");
        // Identificadores de arquivo em provas estruturadas também não podem ser destruídos.
        if (existe("""
                select exists(select 1 from portfolio_arquivos a join denuncias_plagio d
                  on d.url_prova_plagio=a.url_arquivo or d.documento_suporte_url=a.url_arquivo
                     or d.url_prova_plagio like '%/arquivos/' || a.id || '/conteudo%'
                     or d.documento_suporte_url like '%/arquivos/' || a.id || '/conteudo%'
                  where a.artista_id=?)
                """, id)) throw new UnprocessableEntityException("Arquivo ligado a evidência requer retenção restrita específica.");
        List<String> urls = jdbc.queryForList("""
                select foto_perfil_url from usuarios where id=? union all select banner_url from perfis_artistas where usuario_id=?
                union all select url_portfolio from perfis_artistas where usuario_id=?
                union all select banner_url from perfis_contratantes where usuario_id=?
                union all select url from fotos_vaga f join vagas v on v.id=f.vaga_id where v.contratante_id=?
                """, String.class, id, id, id, id, id);
        if (urls.stream().anyMatch(url -> url != null && !url.isBlank() && !externa(url)))
            throw new UnprocessableEntityException("Mídia local sem storage conhecido requer avaliação antes da exclusão.");
    }

    private List<Runnable> planejarArquivos(long id) {
        List<Runnable> result = new ArrayList<>();
        for (String ref : jdbc.queryForList("select url_arquivo from portfolio_arquivos where artista_id=?", String.class, id)) {
            if (externa(ref)) continue; // Elimina a referência, sem acessar/deletar conteúdo de outro provedor.
            portfolio.validarReferencia(id, ref);
            result.add(() -> portfolio.limparUpload(id, ref));
        }
        var arquivos = jdbc.queryForList("""
                select m.sala_id,m.url_anexo from mensagens_chat m where
                  (m.remetente_id=? or m.sala_id in(select p.sala_id from participantes_chat p where p.usuario_id=?
                    and not exists(select 1 from participantes_chat outro where outro.sala_id=p.sala_id and outro.usuario_id<>?)))
                  and m.url_anexo is not null and not
                """ + REPORTADA, id, id, id);
        for (var item : arquivos) {
            long sala = ((Number) item.get("sala_id")).longValue();
            String ref = (String) item.get("url_anexo");
            if (externa(ref)) continue;
            chat.validarReferencia(sala, ref);
            result.add(() -> chat.limpar(sala, ref));
        }
        return result;
    }

    private void anonimizar(long id) {
        jdbc.update("update candidaturas set artista_id=0,mensagem_apresentacao=null,link_portfolio_candidatura=null where artista_id=?", id);
        jdbc.update("""
                update vagas set contratante_id=0,status=case when status='CANCELADA' then status else 'ENCERRADA'::status_vaga_enum end,
                  titulo='Vaga de conta removida',descricao='Histórico preservado sem identificação do contratante.',
                  requisitos='Histórico preservado.',endereco_completo=null,beneficios=null,experiencia=null,tipo_contrato='Não informado',
                  ultima_atualizacao=? where contratante_id=? and status<>'RASCUNHO'
                """, LocalDateTime.now(clock), id);
        jdbc.update("update log_vagas_canceladas set cancelado_por_id=0,motivo=null where cancelado_por_id=?", id);
        // Evidência previamente reportada fica inacessível pelas DTOs/download comuns do RF35.
        jdbc.update("update mensagens_chat m set excluida=true,data_exclusao=coalesce(data_exclusao,?) where remetente_id=? and " + REPORTADA,
                LocalDateTime.now(clock), id);
        jdbc.update("""
                update mensagens_chat m set texto_mensagem=coalesce(texto_mensagem,'Anexo removido com a exclusão da conta.'),
                  url_anexo=null,texto_original=null where remetente_id=? and not
                """ + REPORTADA, id);
        jdbc.update("update mensagens_chat set remetente_id=null where remetente_id=?", id);
        jdbc.update("update denuncias_plagio set denunciante_id=0 where denunciante_id=?", id);
        jdbc.update("update denuncias_plagio set perfil_denunciado_id=0 where perfil_denunciado_id=?", id);
        jdbc.update("update reportes_usuario set denunciante_id=0 where denunciante_id=?", id);
        jdbc.update("update moderacao_conteudo set autor_id=0 where autor_id=?", id);
        jdbc.update("update moderacao_conteudo set moderador_id=null where moderador_id=?", id);
    }

    private void limparReferenciasPublicas(long id) {
        jdbc.update("""
                delete from notificacoes where usuario_destino_id=? or link_contexto in (?,?)
                  or link_contexto in(select '/mensagens?sala='||sala_id from participantes_chat where usuario_id=?)
                  or link_contexto in(select '/portfolio/arquivos/'||id from portfolio_arquivos where artista_id=?)
                  or link_contexto in(select '/vagas/'||id from vagas where contratante_id=?)
                  or link_contexto in(select '/vagas/'||id||'?convite=true' from vagas where contratante_id=?)
                """, id, "/perfis/ARTISTA/" + id, "/perfis/CONTRATANTE/" + id, id, id, id, id);
        // Usa subqueries antes de remover os pais: nenhum alvo polimórfico pessoal deve sobreviver.
        jdbc.update("""
                delete from itens_salvos where usuario_id=? or (tipo_alvo='PERFIL_ARTISTA' and alvo_id=?)
                  or (tipo_alvo='OBRA' and alvo_id in(select id from portfolio_arquivos where artista_id=?))
                  or (tipo_alvo='VAGA' and alvo_id in(select id from vagas where contratante_id=? and status='RASCUNHO'))
                """, id, id, id, id);
        jdbc.update("delete from fotos_vaga where vaga_id in(select id from vagas where contratante_id=?)", id);
    }

    private void limparRelacoes(long id) {
        jdbc.update("delete from vagas where contratante_id=? and status='RASCUNHO'", id);
        jdbc.update("delete from interacoes_galeria where usuario_id=? or arquivo_id in(select id from portfolio_arquivos where artista_id=?)", id, id);
        jdbc.update("delete from itens_galeria where arquivo_id in(select id from portfolio_arquivos where artista_id=?) or galeria_id in(select id from galerias_virtuais where dono_id=?)", id, id);
        jdbc.update("delete from galerias_virtuais where dono_id=?", id);
        jdbc.update("delete from participantes_chat where usuario_id=?", id);
        jdbc.update("delete from banco_talentos where artista_id=? or contratante_id=?", id, id);
        // Identificadores SQL são constantes internas, jamais vindos da requisição.
        for (String table : List.of("portfolio_arquivos", "embeds_externos", "agenda_artista", "conquistas_desbloqueadas", "historico_medalhas", "ranking_top_da_semana"))
            jdbc.update("delete from " + table + " where artista_id=?", id);
        for (String table : List.of("perfil_artista_funcao", "perfil_artista_especializacao", "perfil_artista_area"))
            jdbc.update("delete from " + table + " where perfil_artista_id=?", id);
        for (String table : List.of("responsaveis_legais", "autodeclaracoes", "membros_comunidade", "perfis_artistas", "perfis_contratantes"))
            jdbc.update("delete from " + table + " where usuario_id=?", id);
        jdbc.update("delete from visualizacoes_perfil where perfil_visitado_id=?", id);
    }

    private boolean existe(String sql, Object... args) { return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, args)); }
    private boolean externa(String ref) {
        if (ref == null) return false;
        try { URI uri = URI.create(ref); return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null; }
        catch (IllegalArgumentException ex) { return false; }
    }
    private String hashAleatorio() {
        byte[] entropy = new byte[32]; random.nextBytes(entropy);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(entropy)); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível."); }
    }
}
