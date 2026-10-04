package com.portifolio.validation;

import static org.assertj.core.api.Assertions.*;
import com.portifolio.exception.UnprocessableEntityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConteudoPublicoValidatorTest {
    private final ConteudoPublicoValidator validator = new ConteudoPublicoValidator();

    @ParameterizedTest @ValueSource(strings={"<script>alert(1)</script>","<img src=x onerror=alert(1)>",
            "<svg onload=alert(1)>","<iframe src=https://example.invalid>","<SCRIPT", "</script>",
            "<!-- comentário -->","<!DOCTYPE html>","javascript:alert(1)","data:text/html,test",
            "vbscript:teste","file:///teste","\u202econteúdo","\u0000texto","te\u200bsto"})
    void recusaMarkupEsquemasEControlesSemPersistir(String texto) {
        assertThatThrownBy(()->validator.texto(texto,"Conteúdo",5000,true)).isInstanceOf(UnprocessableEntityException.class);
    }
    @ParameterizedTest @ValueSource(strings={"Arte & música — criação original", "Valores: 1 < 2 e 3 > 2", "Roteiro\nCena 1\tDiálogo",
            "Outro'); delete from usuarios; --", "&lt;script&gt;texto literal&lt;/script&gt;", "Opinião crítica e linguagem artística"})
    void aceitaTextoLiteralSemNormalizar(String texto) {
        assertThatCode(()->validator.texto(texto,"Texto",5000,true)).doesNotThrowAnyException();
    }
    @Test void vazioLimitesELinksExcessivos() {
        assertThatThrownBy(()->validator.texto(null,"Texto",5,true)).isInstanceOf(UnprocessableEntityException.class);
        assertThatThrownBy(()->validator.texto(" ","Texto",5,true)).isInstanceOf(UnprocessableEntityException.class);
        assertThatThrownBy(()->validator.texto("abcdef","Texto",5,false)).isInstanceOf(UnprocessableEntityException.class);
        assertThatThrownBy(()->validator.texto("a".repeat(257),"Texto",5000,true)).isInstanceOf(UnprocessableEntityException.class);
        assertThatThrownBy(()->validator.texto("https://example.invalid/ ".repeat(21),"Texto",5000,true)).isInstanceOf(UnprocessableEntityException.class);
        assertThatCode(()->validator.texto(null,"Opcional",5,false)).doesNotThrowAnyException();
        assertThatCode(()->validator.texto("abcde","Texto",5,true)).doesNotThrowAnyException();
    }
    @ParameterizedTest @ValueSource(strings={"javascript:alert(1)","data:image/svg+xml,test","http://example.invalid/a",
            "https://user:pass@example.invalid/a","//example.invalid/a","https://example.invalid:8443/a",
            "/assets/../teste","/assets/%2e%2e/teste"," https://example.invalid/a","https://example.invalid/a b"})
    void urlRecusaCredenciaisEsquemasETraversal(String url) {
        assertThatThrownBy(()->validator.url(url,"URL",500,true)).isInstanceOf(UnprocessableEntityException.class);
    }
    @ParameterizedTest @ValueSource(strings={"https://example.invalid/obra","/assets/artista/capa.png","assets/vaga-foto-1.png","/api/portfolio/arquivos/1/conteudo"})
    void urlAceitaHttpsOuRotasInternasExplicitasSemFetch(String url) {
        assertThatCode(()->validator.url(url,"URL",500,true)).doesNotThrowAnyException();
    }
}
