package com.portifolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Funcao;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.model.*;
import com.portifolio.model.enums.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PerfilCompletoServiceTest {

    private final PerfilCompletoService service = new PerfilCompletoService(null, null, null);

    @Test
    void artistaCompletoNaoDependeDeCamposOpcionais() {
        Usuario usuario = usuarioCompleto(TipoUsuario.ARTISTA);
        PerfilArtista perfil = perfilArtistaCompleto();
        perfil.setBannerUrl(null);
        perfil.setRaioAtuacao(null);
        perfil.setUrlPortfolio(null);
        perfil.setDisponivelOportunidades(false);
        usuario.setFotoPerfil(null);

        assertThat(service.calcularArtista(usuario, perfil)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "telefone", "email", "cpf", "biografia", "cidade", "estado", "localizacao"})
    void artistaComTextoObrigatorioAusenteOuBlankFicaIncompleto(String campo) {
        Usuario usuario = usuarioCompleto(TipoUsuario.ARTISTA);
        PerfilArtista perfil = perfilArtistaCompleto();
        aplicarTexto(campo, null, usuario, perfil);
        assertThat(service.calcularArtista(usuario, perfil)).isFalse();

        aplicarTexto(campo, "   ", usuario, perfil);
        assertThat(service.calcularArtista(usuario, perfil)).isFalse();
    }

    @Test
    void artistaSemNascimentoOuCredencialFicaIncompleto() {
        Usuario usuario = usuarioCompleto(TipoUsuario.ARTISTA);
        PerfilArtista perfil = perfilArtistaCompleto();
        usuario.setDataNascimento(null);
        assertThat(service.calcularArtista(usuario, perfil)).isFalse();

        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setSenha(null);
        usuario.setGoogleId(null);
        assertThat(service.calcularArtista(usuario, perfil)).isFalse();
    }

    @Test
    void artistaGoogleDispensaSenhaLocal() {
        Usuario usuario = usuarioCompleto(TipoUsuario.ARTISTA);
        usuario.setSenha(null);
        usuario.setGoogleId("google-123");

        assertThat(service.calcularArtista(usuario, perfilArtistaCompleto())).isTrue();
    }

    @Test
    void artistaSemFuncoesFicaIncompletoEComUmaFuncaoFicaCompleto() {
        Usuario usuario = usuarioCompleto(TipoUsuario.ARTISTA);
        PerfilArtista perfil = perfilArtistaCompleto();
        com.portifolio.support.OfficialSchemaFixtures.funcoes(perfil, new HashSet<>());
        assertThat(service.calcularArtista(usuario, perfil)).isFalse();

        com.portifolio.support.OfficialSchemaFixtures.funcoes(perfil, perfilArtistaCompleto().getFuncoes());
        assertThat(service.calcularArtista(usuario, perfil)).isTrue();
    }

    @Test
    void contratanteCompletoNaoDependeDeNomeEmpresaNemCamposOpcionais() {
        Usuario usuario = usuarioCompleto(TipoUsuario.CONTRATANTE);
        PerfilContratante perfil = perfilContratanteCompleto();
        perfil.setNomeEmpresa(null);
        perfil.setBannerUrl(null);
        usuario.setFotoPerfil(null);
        assertThat(service.calcularContratante(usuario, perfil)).isTrue();

        perfil.setNomeEmpresa("");
        assertThat(service.calcularContratante(usuario, perfil)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "telefone", "email", "cpf", "localizacao"})
    void contratanteComTextoObrigatorioAusenteOuBlankFicaIncompleto(String campo) {
        Usuario usuario = usuarioCompleto(TipoUsuario.CONTRATANTE);
        PerfilContratante perfil = perfilContratanteCompleto();
        aplicarTextoContratante(campo, null, usuario, perfil);
        assertThat(service.calcularContratante(usuario, perfil)).isFalse();

        aplicarTextoContratante(campo, "  ", usuario, perfil);
        assertThat(service.calcularContratante(usuario, perfil)).isFalse();
    }

    @Test
    void contratanteSemBiografiaPermaneceCompleto() {
        Usuario usuario = usuarioCompleto(TipoUsuario.CONTRATANTE);
        PerfilContratante perfil = perfilContratanteCompleto();
        perfil.setBiografia(null);
        assertThat(service.calcularContratante(usuario, perfil)).isTrue();

        perfil.setBiografia("  ");
        assertThat(service.calcularContratante(usuario, perfil)).isTrue();
    }

    @Test
    void contratanteSemNascimentoOuCredencialFicaIncompleto() {
        Usuario usuario = usuarioCompleto(TipoUsuario.CONTRATANTE);
        PerfilContratante perfil = perfilContratanteCompleto();
        usuario.setDataNascimento(null);
        assertThat(service.calcularContratante(usuario, perfil)).isFalse();

        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setSenha(null);
        usuario.setGoogleId(null);
        assertThat(service.calcularContratante(usuario, perfil)).isFalse();
    }

    private Usuario usuarioCompleto(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário Completo");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setTelefone("11999999999");
        usuario.setEmail("completo@example.com");
        usuario.setSenha("hash-presente");
        usuario.setTipoUsuario(tipo);
        usuario.setCpf("12345678901");
        return usuario;
    }

    private PerfilArtista perfilArtistaCompleto() {
        PerfilArtista perfil = new PerfilArtista();
        perfil.setTipoPerfilArtistico(com.portifolio.model.enums.TipoPerfilArtistico.ARTISTA_SOLO);
        perfil.setRaioAtuacao(com.portifolio.model.enums.Abrangencia.LOCAL);
        perfil.setBiografia("Biografia");
        perfil.setLocalizacao("São Paulo, SP");
        perfil.setUrlPortfolio("https://portfolio.example");
        Funcao funcao = new Funcao(); funcao.setId(1L); funcao.setArea(com.portifolio.support.OfficialSchemaFixtures.area());
        Especializacao spec = new Especializacao(); spec.setId(1L); spec.setNome("Piano");
        funcao.setEspecializacoes(Set.of(spec));
        com.portifolio.support.OfficialSchemaFixtures.funcoes(perfil, Set.of(funcao));
        var area = perfil.getAreas().iterator().next();
        area.setNivelExperiencia(NivelExperiencia.SEM_EXPERIENCIA);
        area.setEspecializacoes(Set.of(spec));
        return perfil;
    }

    private PerfilContratante perfilContratanteCompleto() {
        PerfilContratante perfil = new PerfilContratante();
        perfil.setTipoPerfil("PESSOA_FISICA");
        perfil.setBiografia("Biografia");
        perfil.setLocalizacao("São Paulo, SP");
        return perfil;
    }

    private void aplicarTexto(String campo, String valor, Usuario usuario, PerfilArtista perfil) {
        switch (campo) {
            case "nome" -> usuario.setNome(valor);
            case "telefone" -> usuario.setTelefone(valor);
            case "email" -> usuario.setEmail(valor);
            case "cpf" -> usuario.setCpf(valor);
            case "biografia" -> perfil.setBiografia(valor);
            case "cidade" -> perfil.setCidade(valor);
            case "estado" -> perfil.setEstado(valor);
            case "localizacao" -> perfil.setLocalizacao(valor);
            case "portfolio" -> perfil.setUrlPortfolio(valor);
            default -> throw new IllegalArgumentException(campo);
        }
    }

    private void aplicarTextoContratante(
            String campo, String valor, Usuario usuario, PerfilContratante perfil) {
        switch (campo) {
            case "nome" -> usuario.setNome(valor);
            case "telefone" -> usuario.setTelefone(valor);
            case "email" -> usuario.setEmail(valor);
            case "cpf" -> usuario.setCpf(valor);
            case "biografia" -> perfil.setBiografia(valor);
            case "localizacao" -> perfil.setLocalizacao(valor);
            default -> throw new IllegalArgumentException(campo);
        }
    }

    @Test void experienciaFuncaoEEspecializacaoDevemSerDaAreaPrincipal() {
        var u = usuarioCompleto(TipoUsuario.ARTISTA);
        var p = perfilArtistaCompleto();
        var principal = p.getAreas().iterator().next();
        principal.setNivelExperiencia(null);
        assertThat(service.calcularArtista(u, p)).isFalse();
        principal.setNivelExperiencia(NivelExperiencia.INICIANTE);
        principal.setEspecializacoes(Set.of());
        assertThat(service.calcularArtista(u, p)).isFalse();
        Especializacao incompativel = new Especializacao(); incompativel.setId(2L);
        principal.setEspecializacoes(Set.of(incompativel));
        assertThat(service.calcularArtista(u, p)).isFalse();
        principal.setPrincipal(false);
        assertThat(service.calcularArtista(u, p)).isFalse();
    }

    @Test void umaEspecializacaoValidaNaoEncobreOutraIncompativel() {
        var u = usuarioCompleto(TipoUsuario.ARTISTA); var p = perfilArtistaCompleto();
        var area = p.getAreas().iterator().next();
        var invalida = new Especializacao(); invalida.setId(999L);
        var todas = new HashSet<>(area.getEspecializacoes()); todas.add(invalida); area.setEspecializacoes(todas);
        assertThat(service.calcularArtista(u, p)).isFalse();
    }

    @Test void secundariaComFuncaoDeOutraAreaInvalidaCompletude() {
        var u = usuarioCompleto(TipoUsuario.ARTISTA); var p = perfilArtistaCompleto();
        var secundaria = new PerfilArtistaArea(); secundaria.setArea(com.portifolio.support.OfficialSchemaFixtures.area((short)2));
        secundaria.setFuncoes(p.getFuncoes()); p.getAreas().add(secundaria);
        assertThat(service.calcularArtista(u, p)).isFalse();
    }

    @Test void exatamenteUmaPrincipalESecundariasPodemSerIncompletas() {
        var u = usuarioCompleto(TipoUsuario.ARTISTA); var p = perfilArtistaCompleto();
        var secundaria = new PerfilArtistaArea(); secundaria.setArea(com.portifolio.support.OfficialSchemaFixtures.area((short)2));
        p.getAreas().add(secundaria);
        assertThat(service.calcularArtista(u, p)).isTrue();
        secundaria.setPrincipal(true);
        assertThat(service.calcularArtista(u, p)).isFalse();
        secundaria.setPrincipal(false);
        p.getAreas().stream().filter(PerfilArtistaArea::isPrincipal).findFirst().orElseThrow().setFuncoes(Set.of());
        secundaria.setFuncoes(perfilArtistaCompleto().getFuncoes());
        assertThat(service.calcularArtista(u, p)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings={"ESTUDIO", "PRODUTORA_EMPRESA"})
    void tiposEmpresariaisDeArtistaExigemCnpjSemDispensarCpf(String tipo) {
        var u = usuarioCompleto(TipoUsuario.ARTISTA); var p = perfilArtistaCompleto();
        p.setTipoPerfilArtistico(TipoPerfilArtistico.valueOf(tipo));
        assertThat(service.calcularArtista(u, p)).isFalse();
        u.setCnpj("12345678000199"); assertThat(service.calcularArtista(u, p)).isTrue();
        u.setCpf(null); assertThat(service.calcularArtista(u, p)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings={"SETOR_PUBLICO", "SETOR_PRIVADO", "ONG"})
    void contratantesInstitucionaisExigemCnpjENomeEmpresa(String tipo) {
        var u = usuarioCompleto(TipoUsuario.CONTRATANTE); var p = perfilContratanteCompleto();
        p.setTipoPerfil(tipo); p.setNomeEmpresa("Instituição");
        assertThat(service.calcularContratante(u, p)).isFalse();
        u.setCnpj("12345678000199"); assertThat(service.calcularContratante(u, p)).isTrue();
        p.setNomeEmpresa("  "); assertThat(service.calcularContratante(u, p)).isFalse();
        p.setTipoContratante(null); assertThat(service.calcularContratante(u, p)).isFalse();
    }
}
