package com.portifolio.repository;

import com.portifolio.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository @RequiredArgsConstructor
public class ModeracaoConteudoRepository {
    private final JdbcTemplate jdbc;
    public record Acao(long id, String tipo, long alvoId, Long moderadorId, String status,
            String justificativa, LocalDateTime data) {}
    public record Alvo(long autorId, boolean publico) {}
    public record Denuncia(long id,String tipo,long alvoId,String motivo,LocalDateTime data) {}
    public long contarDenuncias() {
        return jdbc.queryForObject("select count(*) from reportes_usuario where tipo_conteudo in ('VAGA','COMUNIDADE')",Long.class);
    }
    public List<Denuncia> denuncias(int size,long offset) {
        return jdbc.query("select id,tipo_conteudo,conteudo_id,motivo_reporte,data_reporte from reportes_usuario where tipo_conteudo in ('VAGA','COMUNIDADE') order by data_reporte desc nulls last,id desc limit ? offset ?",
                (r,n)->new Denuncia(r.getLong("id"),r.getString("tipo_conteudo"),r.getLong("conteudo_id"),r.getString("motivo_reporte"),
                        r.getTimestamp("data_reporte")==null?null:r.getTimestamp("data_reporte").toLocalDateTime()),size,offset);
    }

    public Alvo bloquearAlvo(String tipo, long id) {
        // Somente dois identificadores selecionados pelo servidor; valores sempre vinculados.
        var rows = tipo.equals("VAGA")
            ? jdbc.query("select contratante_id as autor, status::text='ABERTA' as publico from vagas where id=? for update",
                    (r,n)->new Alvo(r.getLong("autor"),r.getBoolean("publico")),id)
            : jdbc.query("select criador_id as autor,privacidade::text='PUBLICA' as publico from comunidades where id=? for update",
                    (r,n)->new Alvo(r.getLong("autor"),r.getBoolean("publico")),id);
        return rows.stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Conteúdo não encontrado."));
    }
    public Optional<Acao> ultima(String tipo, long alvo) {
        return jdbc.query("select * from moderacao_conteudo where tipo_conteudo::text=? and conteudo_id=? order by id desc limit 1",
                this::mapear,tipo,alvo).stream().findFirst();
    }
    public boolean oculto(String tipo, long alvo) {
        return ultima(tipo,alvo).map(a -> !"APROVADO".equals(a.status())).orElse(false);
    }
    public Acao registrar(String tipo,long alvo,long autor,long ator,String status,String justificativa,LocalDateTime data) {
        long id=jdbc.queryForObject("""
                insert into moderacao_conteudo(tipo_conteudo,conteudo_id,autor_id,moderador_id,status_moderacao,justificativa_acao,data_analise,data_criacao)
                values(cast(? as tipo_conteudo_enum),?,?,?,cast(? as status_moderacao_enum),?,?,?) returning id
                """,Long.class,tipo,alvo,autor,ator,status,justificativa,data,data);
        return new Acao(id,tipo,alvo,ator,status,justificativa,data);
    }
    public List<Acao> historico(String tipo,long alvo,int size,long offset) {
        return jdbc.query("select * from moderacao_conteudo where tipo_conteudo::text=? and conteudo_id=? order by id desc limit ? offset ?",
                this::mapear,tipo,alvo,size,offset);
    }
    public long contar(String tipo,long alvo) {
        return jdbc.queryForObject("select count(*) from moderacao_conteudo where tipo_conteudo::text=? and conteudo_id=?",Long.class,tipo,alvo);
    }
    public boolean arquivoComEvidencia(long arquivo) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from itens_galeria i where i.arquivo_id=? and
                    (exists(select 1 from reportes_usuario r where r.tipo_conteudo='GALERIA' and r.conteudo_id=i.galeria_id)
                     or exists(select 1 from moderacao_conteudo m where m.tipo_conteudo='GALERIA' and m.conteudo_id=i.galeria_id)))
                """,Boolean.class,arquivo));
    }
    public static final String COMUNIDADE_PUBLICAVEL = """
            coalesce((select coalesce(m.status_moderacao::text,'SOB_ANALISE') from moderacao_conteudo m
                where m.tipo_conteudo='COMUNIDADE' and m.conteudo_id=c.id order by m.id desc limit 1),'APROVADO')='APROVADO'
            """;
    private Acao mapear(java.sql.ResultSet r,int n)throws java.sql.SQLException {
        var data=r.getTimestamp("data_criacao");
        return new Acao(r.getLong("id"),r.getString("tipo_conteudo"),r.getLong("conteudo_id"),
                r.getObject("moderador_id",Long.class),r.getString("status_moderacao"),r.getString("justificativa_acao"),
                data==null?null:data.toLocalDateTime());
    }
}
