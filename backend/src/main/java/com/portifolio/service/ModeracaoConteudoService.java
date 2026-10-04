package com.portifolio.service;

import com.portifolio.dto.ModeracaoRequest;
import com.portifolio.exception.*;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.*;
import com.portifolio.repository.ModeracaoConteudoRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.validation.ConteudoPublicoValidator;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ModeracaoConteudoService {
    private final ModeracaoConteudoRepository repository;
    private final AuthenticatedUserResolver authenticated;
    private final ConteudoPublicoValidator validator;
    private final Clock clock;
    public record Pagina(List<ModeracaoConteudoRepository.Acao> content,int page,int size,long totalElements,boolean hasMore) {}
    public record PaginaDenuncias(List<ModeracaoConteudoRepository.Denuncia> content,int page,int size,long totalElements,boolean hasMore) {}
    public PaginaDenuncias denuncias(int page,int size) {
        equipe();
        if(page<0 || size<1 || size>50) throw new IllegalArgumentException("Paginação entre 1 e 50.");
        long total=repository.contarDenuncias(), offset=(long)page*size;
        return new PaginaDenuncias(repository.denuncias(size,offset),page,size,total,offset+size<total);
    }
    private Usuario equipe() {
        var u=authenticated.usuarioAtual().orElseThrow(()->new UnauthorizedException("Autenticação necessária."));
        if(u.getStatusConta()!=StatusConta.ATIVA || (u.getTipoUsuario()!=TipoUsuario.ADMIN && u.getTipoUsuario()!=TipoUsuario.MODERADOR))
            throw new ForbiddenException("Equipe de moderação com conta ativa necessária.");
        return u;
    }
    private void alvo(String tipo,long id) {
        if(!Set.of("VAGA","COMUNIDADE").contains(tipo) || id<=0) throw new IllegalArgumentException("Alvo permitido: VAGA ou COMUNIDADE com ID positivo.");
    }
    @Transactional
    public ModeracaoConteudoRepository.Acao agir(String tipo,long id,ModeracaoRequest request) {
        var ator=equipe(); alvo(tipo,id);
        validator.texto(request.justificativa(),"Justificativa",1000,true);
        if(request.status()==null || !Set.of("SOB_ANALISE","BLOQUEADO","APROVADO").contains(request.status()))
            throw new IllegalArgumentException("Estado de moderação inválido.");
        var owner=repository.bloquearAlvo(tipo,id);
        if(owner.autorId()<=0 || !owner.publico()) throw new UnprocessableEntityException("Moderação pública requer autor real e conteúdo publicado.");
        var anterior=repository.ultima(tipo,id);
        String estado=anterior.map(a->a.status()==null?"SOB_ANALISE":a.status()).orElse("APROVADO");
        if(ator.getTipoUsuario()==TipoUsuario.MODERADOR &&
                ("APROVADO".equals(request.status()) || anterior.map(a->a.moderadorId()==null || !a.moderadorId().equals(ator.getId())).orElse(false)))
            throw new ForbiddenException("Restauração e revisão de análise de outro ator são restritas a ADMIN.");
        if(request.status().equals(estado)) throw new ConflictException("Conteúdo já está nesse estado.");
        return repository.registrar(tipo,id,owner.autorId(),ator.getId(),request.status(),request.justificativa().strip(),LocalDateTime.now(clock));
    }
    public Pagina historico(String tipo,long id,int page,int size) {
        equipe(); alvo(tipo,id);
        if(page<0 || size<1 || size>50) throw new IllegalArgumentException("Paginação entre 1 e 50.");
        long total=repository.contar(tipo,id);
        if(total==0) throw new ResourceNotFoundException("Histórico não encontrado.");
        long offset=(long)page*size;
        return new Pagina(repository.historico(tipo,id,size,offset),page,size,total,offset+size<total);
    }
}
