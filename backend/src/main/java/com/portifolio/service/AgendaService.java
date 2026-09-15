package com.portifolio.service;

import com.portifolio.dto.AgendaRequest;
import com.portifolio.exception.*;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.security.AuthenticatedUserResolver;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AgendaService {
    private final JdbcTemplate jdbc;
    private final AuthenticatedUserResolver current;
    public record Item(Long id, String titulo, String descricao, String tipo,
                       LocalDateTime inicio, LocalDateTime fim, String localizacao) {}
    public record Pagina(List<Item> content, int page, int size, boolean hasMore) {}
    private Long artista() {
        var usuario = current.usuarioAtual().orElseThrow(() -> new UnauthorizedException("Autenticação obrigatória."));
        if (usuario.getTipoUsuario() != TipoUsuario.ARTISTA) throw new ForbiddenException("Agenda disponível para artistas.");
        return usuario.getId();
    }
    private static final org.springframework.jdbc.core.RowMapper<Item> MAP = (rs, n) -> new Item(
            rs.getLong("id"), rs.getString("titulo_compromisso"), rs.getString("descricao_compromisso"),
            rs.getString("tipo_compromisso"), rs.getTimestamp("data_hora_inicio").toLocalDateTime(),
            rs.getTimestamp("data_hora_fim").toLocalDateTime(), rs.getString("localizacao_logistica"));
    @Transactional(readOnly=true)
    public Pagina listar(LocalDateTime inicio, LocalDateTime fim, int page, int size) {
        Long id = artista();
        if (inicio == null) inicio = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        if (fim == null) fim = inicio.plusMonths(1);
        if (!fim.isAfter(inicio) || fim.isAfter(inicio.plusDays(370)) || page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("Período ou paginação inválidos.");
        var items = jdbc.query("select * from agenda_artista where artista_id=? and data_hora_inicio<? and data_hora_fim>? order by data_hora_inicio,id limit ? offset ?",
                MAP, id, fim, inicio, size+1, (long)page*size);
        return new Pagina(items.stream().limit(size).toList(), page, size, items.size()>size);
    }
    @Transactional
    public Item salvar(Long id, AgendaRequest request) {
        Long artista = artista();
        // Serializa escritas do mesmo artista, inclusive quando ainda não há compromissos.
        jdbc.query("select pg_advisory_xact_lock(33001, hashtext(?))", rs -> {}, artista.toString());
        if (id != null) exigirDono(id, artista);
        if (!request.fim().isAfter(request.inicio())) throw new IllegalArgumentException("O fim deve ser posterior ao início.");
        Boolean overlap = jdbc.queryForObject("select exists(select 1 from agenda_artista where artista_id=? and data_hora_inicio<? and data_hora_fim>? and (?::bigint is null or id<>?))",
                Boolean.class, artista, request.fim(), request.inicio(), id, id);
        if (Boolean.TRUE.equals(overlap)) throw new ConflictException("Já existe um compromisso nesse intervalo.");
        if (id == null) id = jdbc.queryForObject("insert into agenda_artista(artista_id,titulo_compromisso,descricao_compromisso,tipo_compromisso,data_hora_inicio,data_hora_fim,localizacao_logistica,exibir_publico) values (?,?,?,?,?,?,?,false) returning id",
                Long.class, artista, request.titulo(), request.descricao(), request.tipo(), request.inicio(), request.fim(), request.localizacao());
        else jdbc.update("update agenda_artista set titulo_compromisso=?,descricao_compromisso=?,tipo_compromisso=?,data_hora_inicio=?,data_hora_fim=?,localizacao_logistica=? where id=? and artista_id=?",
                request.titulo(), request.descricao(), request.tipo(), request.inicio(), request.fim(), request.localizacao(), id, artista);
        return new Item(id,request.titulo(),request.descricao(),request.tipo(),request.inicio(),request.fim(),request.localizacao());
    }
    @Transactional
    public void excluir(Long id) {
        Long artista=artista();
        jdbc.query("select pg_advisory_xact_lock(33001, hashtext(?))", rs -> {}, artista.toString());
        exigirDono(id,artista);
        jdbc.update("delete from agenda_artista where id=? and artista_id=?",id,artista);
    }
    private void exigirDono(Long id,Long artista) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from agenda_artista where id=? and artista_id=?)",Boolean.class,id,artista)))
            throw new ResourceNotFoundException("Compromisso não encontrado.");
    }
}
