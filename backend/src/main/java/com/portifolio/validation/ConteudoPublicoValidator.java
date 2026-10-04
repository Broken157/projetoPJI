package com.portifolio.validation;

import com.portifolio.exception.UnprocessableEntityException;
import java.io.StringReader;
import java.net.URI;
import java.util.regex.Pattern;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import org.springframework.stereotype.Component;

/** Texto puro: valida sem remover palavras, reescrever arte ou executar markup. Não usado no chat. */
@Component
public class ConteudoPublicoValidator {
    private static final Pattern ESQUEMA = Pattern.compile("(?i)(?<![\\p{L}\\p{N}_])(?:javascript|vbscript|data|file)\\s*:");
    private static final Pattern URL = Pattern.compile("(?i)https?://");
    public void texto(String valor, String campo, int max, boolean obrigatorio) {
        if (valor == null) { if (obrigatorio) throw invalido(campo, "é obrigatório"); return; }
        if (valor.length() > max) throw invalido(campo, "excede " + max + " caracteres");
        if (valor.isBlank()) { if (obrigatorio) throw invalido(campo, "não pode ser vazio"); return; }
        int anterior = -1, repeticoes = 0;
        for (int cp : valor.codePoints().toArray()) {
            if ((Character.isISOControl(cp) && cp != '\n' && cp != '\r' && cp != '\t')
                    || cp == 0x200b || cp == 0xfeff || (cp >= 0x202a && cp <= 0x202e)
                    || (cp >= 0x2066 && cp <= 0x2069)) throw invalido(campo, "contém controles não permitidos");
            repeticoes = cp == anterior && !Character.isWhitespace(cp) ? repeticoes + 1 : 1;
            if (repeticoes > 256) throw invalido(campo, "contém repetição excessiva");
            anterior = cp;
        }
        if (ESQUEMA.matcher(valor).find()) throw invalido(campo, "contém esquema de URL não permitido");
        var urls = URL.matcher(valor); int quantidade = 0;
        while (urls.find()) if (++quantidade > 20) throw invalido(campo, "contém mais de 20 links");
        boolean[] markup = {false};
        try {
            new ParserDelegator().parse(new StringReader(valor), new HTMLEditorKit.ParserCallback() {
                private void tag(MutableAttributeSet attrs) {
                    if (attrs.getAttribute(HTMLEditorKit.ParserCallback.IMPLIED) == null) markup[0] = true;
                }
                @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) { tag(attrs); }
                @Override public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) { tag(attrs); }
                @Override public void handleComment(char[] data, int pos) { markup[0] = true; }
            }, true);
        } catch (java.io.IOException ex) { throw invalido(campo, "não pôde ser validado"); }
        // Fecha entradas truncadas que o parser tolerante poderia tratar como texto.
        for (int i = 0; i + 1 < valor.length(); i++) {
            if (valor.charAt(i) == '<' && (Character.isLetter(valor.charAt(i + 1))
                    || "/!?".indexOf(valor.charAt(i + 1)) >= 0)) markup[0] = true;
        }
        if (markup[0]) throw invalido(campo, "aceita texto puro, sem HTML");
    }

    public void url(String valor, String campo, int max, boolean permitirInterna) {
        if (valor == null || valor.isBlank()) return;
        if (valor.length() > max || !valor.equals(valor.strip())) throw invalido(campo, "contém URL inválida");
        try {
            URI uri = new URI(valor);
            if (permitirInterna && uri.getScheme() == null && uri.getRawAuthority() == null
                    && (valor.startsWith("/assets/") || valor.startsWith("assets/") || valor.startsWith("/api/"))
                    && !valor.contains("%") && !valor.contains("\\") && !valor.contains("..")) return;
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getPort() != -1) throw new IllegalArgumentException();
        } catch (Exception ex) { throw invalido(campo, "exige URL HTTPS válida sem credenciais"); }
    }

    private static UnprocessableEntityException invalido(String campo, String regra) {
        return new UnprocessableEntityException(campo + ": " + regra + ".");
    }
}
