package com.portifolio.service;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.AuthenticatedUserResolver;
import com.portifolio.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class AvatarUpdateService {
    private final UsuarioRepository usuarios;
    private final AuthenticatedUserResolver current;
    @Transactional public void atualizar(String url){
        var user=current.usuarioAtual().orElseThrow(()->new UnauthorizedException("Autenticação obrigatória."));
        if(url!=null&&!url.isBlank()){
            java.net.URI uri;
            try{uri=java.net.URI.create(url);}catch(Exception e){throw new IllegalArgumentException("URL de avatar inválida.");}
            if(url.length()>255 || !java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null)throw new IllegalArgumentException("URL de avatar inválida.");
        }
        user.setFotoPerfil(url==null||url.isBlank()?null:url);usuarios.save(user);
    }
}
