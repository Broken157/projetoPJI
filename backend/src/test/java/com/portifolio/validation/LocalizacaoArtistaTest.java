package com.portifolio.validation;

import static org.assertj.core.api.Assertions.*;
import com.portifolio.exception.UnprocessableEntityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocalizacaoArtistaTest {
    @ParameterizedTest @ValueSource(strings={"São Paulo, SP", " São Paulo - sp ", "São Paulo/SP"})
    void converteFormatosLegadosInequivocos(String texto) {
        assertThat(LocalizacaoArtista.deTexto(texto)).isEqualTo(new LocalizacaoArtista("São Paulo", "SP"));
    }
    @ParameterizedTest @ValueSource(strings={"São Paulo", "SP", "São Paulo, Brasil, SP", "São Paulo, S", "São Paulo, 12"})
    void rejeitaLocalizacaoAmbiguaSemInventarUf(String texto) {
        assertThatThrownBy(() -> LocalizacaoArtista.deTexto(texto)).isInstanceOf(UnprocessableEntityException.class);
    }
    @Test void aceitaEstruturaMasRejeitaParcialOuContraditoria() {
        assertThat(LocalizacaoArtista.deRequest(" Recife ", "pe", "Recife/PE")).isEqualTo(new LocalizacaoArtista("Recife", "PE"));
        assertThatThrownBy(() -> LocalizacaoArtista.deRequest("Recife", null, null)).isInstanceOf(UnprocessableEntityException.class);
        assertThatThrownBy(() -> LocalizacaoArtista.deRequest("Recife", "PE", "Campinas, SP")).isInstanceOf(UnprocessableEntityException.class);
        assertThat(LocalizacaoArtista.deTexto(" ")).isEqualTo(new LocalizacaoArtista(null, null));
    }
}
