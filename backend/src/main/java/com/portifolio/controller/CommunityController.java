package com.portifolio.controller;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.exception.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/comunidades") @RequiredArgsConstructor
public class CommunityController {
    private final JdbcTemplate jdbc;
    private final AuthenticatedUserResolver authenticated;
    public record Item(Long id,String nome,String descricao,String categoria) {}
    public record Pagina(List<Item> content,int page,int size,boolean hasMore) {}
    private final org.springframework.jdbc.core.RowMapper<Item> mapper=(r,n)->new Item(r.getLong("id"),r.getString("nome"),r.getString("descricao"),r.getString("categoria_artistica"));
    private Long usuario(){return authenticated.usuarioAtual().orElseThrow(()->new UnauthorizedException("Autenticação obrigatória.")).getId();}
    private static final String VISIBLE="(lower(c.privacidade::text)='publica' or c.criador_id=? or exists(select 1 from membros_comunidade m where m.comunidade_id=c.id and m.usuario_id=? and m.aprovado=true))";
    @GetMapping public Pagina listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size){
        Long user=usuario();if(page<0||size<1||size>50)throw new IllegalArgumentException("Paginação inválida.");
        var rows=jdbc.query("select c.* from comunidades c where "+VISIBLE+" order by c.id desc limit ? offset ?",mapper,user,user,size+1,(long)page*size);
        return new Pagina(rows.stream().limit(size).toList(),page,size,rows.size()>size);
    }
    @GetMapping("/{id}") public Item detalhe(@PathVariable Long id){
        Long user=usuario();return jdbc.query("select c.* from comunidades c where c.id=? and "+VISIBLE,mapper,id,user,user).stream().findFirst().orElseThrow(()->new ResourceNotFoundException("Comunidade não encontrada."));
    }
}
