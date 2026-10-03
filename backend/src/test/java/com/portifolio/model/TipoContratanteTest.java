package com.portifolio.model;

import static org.assertj.core.api.Assertions.*;
import com.portifolio.model.enums.TipoContratante;
import com.portifolio.exception.UnprocessableEntityException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TipoContratanteTest {
    @ParameterizedTest @CsvSource({"Pessoa Física,PESSOA_FISICA", "Empresa,SETOR_PRIVADO", "Produtora,SETOR_PRIVADO", "Instituição Pública,SETOR_PUBLICO", "ONG,ONG", "SETOR_PRIVADO,SETOR_PRIVADO"})
    void preservaEntradasLegadasInequivocas(String texto, TipoContratante esperado) {
        var perfil = new PerfilContratante(); perfil.setTipoPerfil(texto);
        assertThat(perfil.getTipoContratante()).isEqualTo(esperado);
        assertThat(perfil.getTipoPerfil()).isEqualTo(esperado.name());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"Outro", "Instituição de Ensino", " "})
    void classificacaoAusenteOuAmbiguaNaoEInventada(String texto) {
        assertThatThrownBy(() -> TipoContratante.deContrato(texto)).isInstanceOf(UnprocessableEntityException.class);
    }
}
