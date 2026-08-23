package com.portifolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// RF34 — Avatar padrao automatico
@Service
public class AvatarService {

    @Value("${avatar.dicebear.style:adventurer}")
    private String estilo;

    private static final String DICEBEAR_BASE = "https://api.dicebear.com/9.x/";

    /**
     * Resolve a URL do avatar seguindo ordem de prioridade:
     *   1. foto_perfil da tabela perfis_* (definida pelo usuario via RF08)
     *   2. foto_perfil da tabela usuarios  (salva no primeiro acesso Google — RF32 Opcao B)
     *   3. Avatar gerado pelo DiceBear com seed opaca e deterministica (fallback garantido)
     *
     * Nunca retorna null — RF34 exige que avatarUrl sempre venha preenchido no response.
     */
    public String resolverUrl(Long usuarioId, String fotoUsuario, String fotoPerfil) {
        if (fotoPerfil != null && !fotoPerfil.isBlank()) {
            return fotoPerfil;
        }
        if (fotoUsuario != null && !fotoUsuario.isBlank()) {
            return fotoUsuario;
        }
        return DICEBEAR_BASE + estilo + "/svg?seed=" + seedOpaca(usuarioId);
    }

    private String seedOpaca(Long usuarioId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("palco-avatar-v1:" + usuarioId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel para gerar avatar padrao.", ex);
        }
    }
}
