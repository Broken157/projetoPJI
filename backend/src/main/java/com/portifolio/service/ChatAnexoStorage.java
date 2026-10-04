package com.portifolio.service;

import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.validation.ArquivoPortfolioValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Storage privado: referências geradas pelo servidor, nunca caminhos do cliente. */
@Service
public class ChatAnexoStorage {
    private static final Map<String, String> MIMES = Map.of("jpg", "image/jpeg", "jpeg", "image/jpeg",
            "png", "image/png", "pdf", "application/pdf", "mp3", "audio/mpeg");
    private final Path root;

    public ChatAnexoStorage(@Value("${app.chat.storage-root:./storage/chat}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
        for (Path part : root) {
            if (Set.of("frontend", "palco-comunidades-agenda", "static", "public", "build", "target")
                    .contains(part.toString().toLowerCase(Locale.ROOT))) throw invalido();
        }
    }

    public String novaReferencia(Long sala, String extensao) {
        if (sala == null || sala <= 0 || !MIMES.containsKey(extensao)) throw invalido();
        return sala + "/" + UUID.randomUUID() + "." + extensao;
    }

    public void gravar(Long sala, String referencia, byte[] bytes) {
        Path destino = resolver(sala, referencia), temporario = null;
        if (bytes == null || bytes.length == 0 || bytes.length > limite(referencia)) throw invalido();
        try {
            Files.createDirectories(destino.getParent());
            verificarLinks(destino);
            temporario = Files.createTempFile(destino.getParent(), ".upload-", ".tmp");
            try (var out = java.nio.channels.FileChannel.open(temporario, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) out.write(buffer);
                out.force(true);
            }
            verificarLinks(destino);
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) { throw invalido(); }
        finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); } catch (IOException ex) { throw invalido(); }
            }
        }
    }

    public Conteudo ler(Long sala, String referencia) {
        Path caminho = resolver(sala, referencia);
        try (var in = Files.newInputStream(caminho, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = in.readNBytes(limite(referencia) + 1);
            if (bytes.length == 0 || bytes.length > limite(referencia)) throw invalido();
            String ext = extensao(referencia);
            return new Conteudo(bytes, MIMES.get(ext), "anexo." + ext);
        } catch (IOException ex) { throw invalido(); }
    }

    public void limpar(Long sala, String referencia) {
        try { Files.deleteIfExists(resolver(sala, referencia)); }
        catch (IOException ex) { throw invalido(); }
    }

    private Path resolver(Long sala, String referencia) {
        if (sala == null || sala <= 0 || referencia == null
                || !referencia.matches("[1-9][0-9]*/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|pdf|mp3)")
                || !referencia.startsWith(sala + "/")) throw invalido();
        Path caminho = root.resolve(referencia).normalize();
        if (!caminho.startsWith(root) || caminho.equals(root)) throw invalido();
        verificarLinks(caminho);
        return caminho;
    }

    private void verificarLinks(Path caminho) {
        for (Path parte = caminho; parte != null; parte = parte.getParent()) {
            if (Files.isSymbolicLink(parte)) throw invalido();
            if (Files.exists(parte, LinkOption.NOFOLLOW_LINKS)) {
                try { if (!parte.toRealPath().equals(parte.toAbsolutePath().normalize())) throw invalido(); }
                catch (IOException ex) { throw invalido(); }
            }
        }
    }

    private int limite(String referencia) {
        String ext = extensao(referencia);
        return ext.equals("mp3") ? ArquivoPortfolioValidator.AUDIO_LIMIT
                : ext.equals("pdf") ? ArquivoPortfolioValidator.PDF_LIMIT : ArquivoPortfolioValidator.IMAGE_LIMIT;
    }

    private String extensao(String referencia) { return referencia.substring(referencia.lastIndexOf('.') + 1); }
    private UnprocessableEntityException invalido() { return new UnprocessableEntityException("Anexo indisponível ou referência inválida."); }
    public record Conteudo(byte[] bytes, String mime, String nome) {}
}
