package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.PerfilContratante;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.model.enums.TipoPerfilArtistico;
import com.portifolio.model.enums.TipoUsuario;
import com.portifolio.repository.PerfilArtistaRepository;
import com.portifolio.repository.PerfilContratanteRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.security.MenorAutorizadoPolicy;
import com.portifolio.support.OfficialPostgreSQLContainer;
import com.portifolio.support.OfficialSchemaFixtures;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.portifolio.controller.DescobertaPublicaRf37IntegrationTest$SqlInspector")
public class DescobertaPublicaRf37IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilArtistaRepository artistas;
    @Autowired PerfilContratanteRepository contratantes;
    @Autowired JwtService jwt;
    @Autowired Clock clock;
    @Autowired MenorAutorizadoPolicy menorPolicy;
    @Autowired PlatformTransactionManager transactionManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String ROTA = "/api/perfis/publicos";

    @BeforeEach @AfterEach
    void limpar() {
        jdbc.execute("TRUNCATE salas_chat, usuarios RESTART IDENTITY CASCADE");
        SqlInspector.sql.get().clear();
    }

    @Test
    void visitanteEAutenticadoRecebemMesmosDadosPublicosComFavoritoPrivadoSeparado() throws Exception {
        var artista = artista("adulto", 30);
        var contratante = contratante("entidade", 30);
        JsonNode anonimo = pagina();
        assertThat(ids(anonimo)).containsExactly(artista.getUsuarioId(), contratante.getUsuarioId());
        for (Usuario ator : List.of(artista.getUsuario(), contratante.getUsuario())) {
            var autenticado = pagina(get(ROTA).header("Authorization", "Bearer " + jwt.gerarToken(ator)));
            assertThat(autenticado.path("content").get(0).path("favorito").asBoolean()).isFalse();
            autenticado.path("content").forEach(item -> ((com.fasterxml.jackson.databind.node.ObjectNode)item).remove("favorito"));
            assertThat(autenticado).isEqualTo(anonimo);
        }
        assertThat(anonimo.path("content").get(0).path("tipo").asText()).isEqualTo("ARTISTA");
        assertThat(anonimo.path("content").get(1).path("tipo").asText()).isEqualTo("CONTRATANTE");
    }

    @Test
    void TokenInvalidoNaoTornaDescobertaPrivadaERotasPrivadasContinuam401() throws Exception {
        artista("publico", 30);
        assertThat(pagina(get(ROTA).header("Authorization", "Bearer invalido"))).isEqualTo(pagina());
        for (String rota : List.of("/api/usuarios/me", "/api/talentos", "/api/salvos", "/api/chat/salas")) {
            mvc.perform(get(rota)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post(ROTA)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @EnumSource(value = TipoUsuario.class, names = {"ARTISTA", "CONTRATANTE"})
    void tipoFiltraSemMisturarPerfis(TipoUsuario tipo) throws Exception {
        var artista = artista("adulto", 30);
        var contratante = contratante("entidade", 30);
        assertThat(ids(pagina("tipo", tipo.name()))).containsExactly(
                tipo == TipoUsuario.ARTISTA ? artista.getUsuarioId() : contratante.getUsuarioId());
    }

    @ParameterizedTest @EnumSource(value = TipoUsuario.class, names = {"ADMIN", "MODERADOR"})
    void papelInternoComPerfilLegadoNaoAparece(TipoUsuario tipo) throws Exception {
        var perfil = artista("interno", 30);
        jdbc.update("update usuarios set tipo_usuario = ?::tipo_usuario_enum where id = ?", tipo.name(), perfil.getUsuarioId());
        assertThat(ids(pagina())).isEmpty();
        mvc.perform(get(ROTA + "/ARTISTA/{id}", perfil.getUsuarioId())).andExpect(status().isNotFound());
    }

    @ParameterizedTest @EnumSource(value = StatusConta.class, names = "ATIVA", mode = EnumSource.Mode.EXCLUDE)
    void estadosNaoAptosSaemAntesDaContagem(StatusConta estado) throws Exception {
        artista("inapto", 30);
        contratante("inapta", 30);
        jdbc.update("update usuarios set status_conta = ?::status_conta_enum", estado.name());
        JsonNode pagina = pagina();
        assertThat(ids(pagina)).isEmpty();
        assertThat(pagina.path("totalElements").asLong()).isZero();
    }

    @ParameterizedTest @ValueSource(ints = {14, 17})
    void menorAutorizadoTemCardRestritoENavegaAoRF10(int idade) throws Exception {
        var menor = artista("menor", idade);
        autorizar(menor.getUsuarioId(), true, false);
        area(menor.getUsuarioId(), 1);
        assertThat(autorizado(menor.getUsuarioId())).isTrue();
        JsonNode item = pagina().path("content").get(0);
        whitelist(item);
        assertThat(item.path("usuarioId").asLong()).isEqualTo(menor.getUsuarioId());
        JsonNode detalhe = mapper.readTree(mvc.perform(get(ROTA + "/{tipo}/{id}",
                item.path("tipo").asText(), item.path("usuarioId").asLong()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(detalhe.toString()).doesNotContain("nivelExperiencia", "dataNascimento", "responsavel", "consentimento", "email", "telefone");
    }

    @ParameterizedTest @CsvSource({"13,false,false", "14,false,false", "17,false,false", "14,true,true", "17,true,true"})
    void menorSemConsentimentoOuRevogadoNaoAparece(int idade, boolean consentiu, boolean revogado) throws Exception {
        var menor = artista("privado", idade);
        autorizar(menor.getUsuarioId(), consentiu, revogado);
        assertThat(ids(pagina())).isEmpty();
        assertThat(autorizado(menor.getUsuarioId())).isFalse();
        mvc.perform(get(ROTA + "/ARTISTA/{id}", menor.getUsuarioId())).andExpect(status().isNotFound());
    }

    @Test
    void menorSemResponsavelEContratanteMenorContinuamPrivados() throws Exception {
        artista("sem_responsavel", 17);
        var menorContratante = contratante("contratante_menor", 17);
        autorizar(menorContratante.getUsuarioId(), true, false);
        assertThat(ids(pagina())).isEmpty();
    }

    @Test
    void menorAutorizadoComContaBloqueadaNaoAparece() throws Exception {
        var menor = artista("bloqueado", 17);
        autorizar(menor.getUsuarioId(), true, false);
        jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=?", menor.getUsuarioId());
        assertThat(ids(pagina())).isEmpty();
    }

    @ParameterizedTest @CsvSource({"14,0,true", "14,1,false", "18,0,true", "18,1,false", "0,1,false"})
    void limitesDeAniversarioUsamORelogioDaPolitica(int anos, int dias, boolean esperado) throws Exception {
        var perfil = artista("aniversario", 30);
        LocalDate nascimento = LocalDate.now(clock).minusYears(anos).plusDays(dias);
        jdbc.update("update usuarios set data_nascimento=? where id=?", nascimento, perfil.getUsuarioId());
        if (anos == 14) autorizar(perfil.getUsuarioId(), true, false);
        assertThat(ids(pagina()).contains(perfil.getUsuarioId())).isEqualTo(esperado);
        mvc.perform(get(ROTA + "/ARTISTA/{id}", perfil.getUsuarioId()))
                .andExpect(esperado ? status().isOk() : status().isNotFound());
    }

    @Test
    void revogacaoPosteriorRetiraDescobertaEDetalheSemRemoverDados() throws Exception {
        var menor = artista("revogado_depois", 17);
        autorizar(menor.getUsuarioId(), true, false);
        assertThat(ids(pagina())).containsExactly(menor.getUsuarioId());
        jdbc.update("update responsaveis_legais set consentimento_revogado=true where usuario_id=?", menor.getUsuarioId());
        assertThat(ids(pagina())).isEmpty();
        mvc.perform(get(ROTA + "/ARTISTA/{id}", menor.getUsuarioId())).andExpect(status().isNotFound());
        assertThat(artistas.existsById(menor.getUsuarioId())).isTrue();
    }

    @Test
    void adultoSemPerfilCompletoENaoAssociadoAoBancoEhPublico() throws Exception {
        var artista = artista("incompleto", 30);
        jdbc.update("update usuarios set perfil_completo=false where id=?", artista.getUsuarioId());
        assertThat(ids(pagina())).containsExactly(artista.getUsuarioId());
        mvc.perform(get(ROTA + "/ARTISTA/{id}", artista.getUsuarioId())).andExpect(status().isOk());
    }

    @Test
    void usuarioSemPerfilCorrespondenteNaoAparece() throws Exception {
        usuario("sem_perfil", TipoUsuario.ARTISTA, 30);
        usuario("sem_perfil_contratante", TipoUsuario.CONTRATANTE, 30);
        assertThat(ids(pagina())).isEmpty();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void disponibilidadeNaoMudaDescoberta(boolean disponibilidade) throws Exception {
        var perfil = artista("disponivel", 30);
        jdbc.update("update perfis_artistas set disponivel_oportunidades=? where usuario_id=?", disponibilidade, perfil.getUsuarioId());
        assertThat(ids(pagina())).containsExactly(perfil.getUsuarioId());
        whitelist(pagina().path("content").get(0));
    }

    @Test
    void DentroEForaDoBancoTemMesmaVisibilidadeSemVinculoExpostoOuMutacoes() throws Exception {
        var dentro = artista("dentro", 30);
        var fora = artista("fora", 30);
        var dono = contratante("dono", 30);
        jdbc.update("insert into banco_talentos(contratante_id,artista_id,data_adicao) values (?,?,now())", dono.getUsuarioId(), dentro.getUsuarioId());
        var antes = jdbc.queryForList("select * from banco_talentos");
        assertThat(ids(pagina("tipo", "ARTISTA"))).containsExactly(dentro.getUsuarioId(), fora.getUsuarioId());
        for (JsonNode item : pagina().path("content")) whitelist(item);
        assertThat(jdbc.queryForList("select * from banco_talentos")).isEqualTo(antes);
        for (String tabela : List.of("salas_chat", "itens_salvos", "candidaturas", "notificacoes")) {
            assertThat(jdbc.queryForObject("select count(*) from " + tabela, Long.class)).isZero();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"João", "JOÃO", "Palco"})
    void nomePublicoUsaCampoPersistidoRF10(String texto) throws Exception {
        var perfil = artista("nome_artistico", 30);
        jdbc.update("update usuarios set nome='João do Palco' where id=?", perfil.getUsuarioId());
        assertThat(ids(pagina("q", texto))).containsExactly(perfil.getUsuarioId());
        assertThat(pagina("q", texto).path("content").get(0).path("nomeExibicao").asText()).isEqualTo("João do Palco");
    }

    @Test
    void acentosSaoPreservadosSemExtensaoOuUnaccent() throws Exception {
        var perfil = artista("acentuado", 30);
        jdbc.update("update usuarios set nome='João' where id=?", perfil.getUsuarioId());
        assertThat(ids(pagina("q", "Joao"))).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"@Artista_37", "Artista_37", "@ARTISTA_37"})
    void usernameComESemArrobaPreservaValorArmazenado(String texto) throws Exception {
        var perfil = artista("identidade", 30);
        jdbc.update("update usuarios set username='Artista_37' where id=?", perfil.getUsuarioId());
        JsonNode pagina = pagina("q", texto);
        assertThat(ids(pagina)).containsExactly(perfil.getUsuarioId());
        assertThat(pagina.path("content").get(0).path("username").asText()).isEqualTo("Artista_37");
        assertThat(jdbc.queryForObject("select username from usuarios where id=?", String.class, perfil.getUsuarioId())).isEqualTo("Artista_37");
    }

    @Test
    void entidadePublicaTemNomeEmpresaEFallbackSemBuscarNomeOculto() throws Exception {
        var entidade = contratante("nome_pessoal", 30);
        jdbc.update("update usuarios set username='entidade' where id=?", entidade.getUsuarioId());
        jdbc.update("update perfis_contratantes set nome_empresa='Estúdio Lua' where usuario_id=?", entidade.getUsuarioId());
        assertThat(ids(pagina("q", "Estúdio"))).containsExactly(entidade.getUsuarioId());
        assertThat(ids(pagina("q", "nome_pessoal"))).isEmpty();
        JsonNode item = pagina("q", "Lua").path("content").get(0);
        assertThat(item.path("nomeExibicao").asText()).isEqualTo("Estúdio Lua");
        mvc.perform(get(ROTA + "/{tipo}/{id}", item.path("tipo").asText(), item.path("usuarioId").asLong()))
                .andExpect(status().isOk());
        jdbc.update("update perfis_contratantes set nome_empresa='   ' where usuario_id=?", entidade.getUsuarioId());
        assertThat(ids(pagina("q", "nome_pessoal"))).containsExactly(entidade.getUsuarioId());
    }

    @Test
    void cidadeEUfSaoPublicasComparadasSemDistinguirMaiusculas() throws Exception {
        var perfil = artista("local", 30);
        assertThat(ids(pagina("cidade", "  cAMPINAS  ", "estado", "sp"))).containsExactly(perfil.getUsuarioId());
        assertThat(ids(pagina("cidade", "Camp"))).isEmpty();
        assertThat(ids(pagina("estado", "RJ"))).isEmpty();
    }

    @Test
    void areaOficialUsaExistsSemDuplicarCardECombinaFiltros() throws Exception {
        var perfil = artista("musico", 30);
        artista("outro", 30);
        contratante("musica_entidade", 30);
        area(perfil.getUsuarioId(), 1);
        area(perfil.getUsuarioId(), 2);
        assertThat(ids(pagina("areaId", "2"))).containsExactly(perfil.getUsuarioId());
        assertThat(ids(pagina("areaId", "2", "tipo", "ARTISTA", "cidade", "Campinas", "estado", "SP", "q", "musico")))
                .containsExactly(perfil.getUsuarioId());
        mvc.perform(get(ROTA).param("areaId", "99")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void buscaLivreNaoPercorreBiografiaOuOutrosCamposPrivados() throws Exception {
        var perfil = artista("limitado", 30);
        jdbc.update("update usuarios set nome='Público', username='publico', email='oculto@rf37.test', token_recuperacao='segredo-token' where id=?", perfil.getUsuarioId());
        jdbc.update("update perfis_artistas set biografia='biografia escondida da pesquisa', nome_integrantes='integrante secreto' where usuario_id=?", perfil.getUsuarioId());
        for (String texto : List.of("oculto", "segredo-token", "biografia", "integrante", "11999999999", "INICIANTE")) {
            assertThat(ids(pagina("q", texto))).isEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"' OR 1=1 --", "%", "_", "\\", "x'; DROP TABLE usuarios;--"})
    void textoMaliciosoOuCuringaEhLiteralEBindado(String texto) throws Exception {
        artista("literal", 30);
        SqlInspector.sql.get().clear();
        assertThat(ids(pagina("q", texto))).isEmpty();
        assertThat(usuarios.count()).isOne();
        assertThat(SqlInspector.sql.get().get(0)).contains(" like ?").doesNotContain("OR 1=1", "DROP TABLE");
    }

    @Test
    void avatarReutilizaFotoEFallbackOpacoSemPersistir() throws Exception {
        var perfil = artista("avatar", 30);
        JsonNode primeiro = pagina();
        assertThat(primeiro).isEqualTo(pagina());
        assertThat(primeiro.path("content").get(0).path("avatarUrl").asText()).containsPattern("seed=[0-9a-f]{64}");
        assertThat(jdbc.queryForObject("select foto_perfil_url from usuarios where id=?", String.class, perfil.getUsuarioId())).isNull();
        jdbc.update("update usuarios set foto_perfil_url='https://cdn.example/avatar.jpg' where id=?", perfil.getUsuarioId());
        assertThat(pagina().path("content").get(0).path("avatarUrl").asText()).isEqualTo("https://cdn.example/avatar.jpg");
    }

    @Test
    void paginaVaziaTem200EMetadadosSem404() throws Exception {
        JsonNode pagina = pagina("q", "sem resultado");
        assertThat(ids(pagina)).isEmpty();
        assertThat(pagina.path("page").asInt()).isZero();
        assertThat(pagina.path("size").asInt()).isEqualTo(20);
        assertThat(pagina.path("totalElements").asLong()).isZero();
        assertThat(pagina.path("totalPages").asInt()).isZero();
        assertThat(pagina.path("first").asBoolean()).isTrue();
        assertThat(pagina.path("last").asBoolean()).isTrue();
    }

    @Test
    void paginasEstaveisSemDuplicatasComDesempatePorIdENaoPublicaveisForaDoCount() throws Exception {
        var perfis = IntStream.range(0, 5).mapToObj(i -> artista("pessoa" + i, 30)).toList();
        jdbc.update("update usuarios set nome='Mesmo nome'");
        var excluido = artista("nao_publicavel", 30);
        jdbc.update("update usuarios set status_conta='BLOQUEADA' where id=?", excluido.getUsuarioId());
        JsonNode primeira = pagina("size", "2");
        JsonNode meio = pagina("size", "2", "page", "1");
        JsonNode ultima = pagina("size", "2", "page", "2");
        assertThat(primeira).isEqualTo(pagina("size", "2"));
        List<Long> ids = new ArrayList<>(ids(primeira)); ids.addAll(ids(meio)); ids.addAll(ids(ultima));
        assertThat(ids).containsExactlyElementsOf(perfis.stream().map(PerfilArtista::getUsuarioId).toList()).doesNotHaveDuplicates();
        assertThat(primeira.path("totalElements").asLong()).isEqualTo(5);
        assertThat(primeira.path("totalPages").asInt()).isEqualTo(3);
        assertThat(meio.path("hasNext").asBoolean()).isTrue();
        assertThat(meio.path("hasPrevious").asBoolean()).isTrue();
        assertThat(ultima.path("last").asBoolean()).isTrue();
        assertThat(ids(pagina("size", "2", "page", "3"))).isEmpty();
    }

    @Test
    void limiteCinquentaAplicadoNoBancoSemFetchDeExperienciaOuNMaisUm() throws Exception {
        IntStream.range(0, 51).forEach(i -> area(artista("volume" + i, 30).getUsuarioId(), 1));
        SqlInspector.sql.get().clear();
        assertThat(ids(pagina("size", "1", "areaId", "1"))).hasSize(1);
        int pequenas = SqlInspector.sql.get().size();
        SqlInspector.sql.get().clear();
        JsonNode pagina = pagina("size", "50", "areaId", "1");
        assertThat(ids(pagina)).hasSize(50);
        assertThat(pagina.path("totalElements").asLong()).isEqualTo(51);
        // Uma validação do catálogo + count + página; custo constante, não por perfil.
        assertThat(SqlInspector.sql.get()).hasSize(pequenas).hasSize(3);
        assertThat(SqlInspector.sql.get().getLast()).contains("fetch first ? rows only");
        for (String sql : SqlInspector.sql.get()) {
            String select = sql.substring(0, sql.indexOf(" from "));
            assertThat(select).doesNotContain("data_nascimento", "email", "telefone", "senha", "consentimento", "nivel_experiencia", "responsavel");
            assertThat(sql).doesNotContain("nivel_experiencia", "perfil_artista_funcao", "perfil_artista_especializacao", "banco_talentos");
        }
    }

    @ParameterizedTest @CsvSource({"page,-1", "page,texto", "page,2147483647", "size,0", "size,-1", "size,51", "size,texto", "tipo,ADMIN", "tipo,MODERADOR", "tipo,INVALIDO", "estado,S", "estado,SPP", "estado,1A", "areaId,0", "areaId,-1", "areaId,abc", "q,@", "disponivel,false", "funcaoIds,1", "recomendados,true"})
    void parametrosInvalidosOuAvancadosRetornam400Sanitizado(String parametro, String valor) throws Exception {
        String body = mvc.perform(get(ROTA).param(parametro, valor)).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(body).path("status").asInt()).isEqualTo(400);
        assertThat(body).doesNotContain("SQLException", "stackTrace", "org.hibernate", "SELECT ");
    }

    @Test
    void contratanteComAreaTemErroExplicitoETextoLimitado() throws Exception {
        mvc.perform(get(ROTA).param("tipo", "CONTRATANTE").param("areaId", "1")).andExpect(status().isBadRequest());
        mvc.perform(get(ROTA).param("q", "x".repeat(151))).andExpect(status().isBadRequest());
        mvc.perform(get(ROTA).param("cidade", "x".repeat(101))).andExpect(status().isBadRequest());
        mvc.perform(get(ROTA).param("q", "nome\nprivado")).andExpect(status().isBadRequest());
    }

    private JsonNode pagina(String... filtros) throws Exception {
        var request = get(ROTA);
        for (int i = 0; i < filtros.length; i += 2) request.param(filtros[i], filtros[i + 1]);
        return pagina(request);
    }
    private JsonNode pagina(MockHttpServletRequestBuilder request) throws Exception {
        return mapper.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>();
        pagina.path("content").forEach(item -> ids.add(item.path("usuarioId").asLong()));
        return ids;
    }
    private void whitelist(JsonNode item) {
        List<String> campos = new ArrayList<>(); item.fieldNames().forEachRemaining(campos::add);
        var esperados = new ArrayList<>(List.of("usuarioId", "tipo", "username", "nomeExibicao", "avatarUrl", "cidade", "estado"));
        if (item.has("tipoPerfil")) { esperados.add("tipoPerfil"); assertThat(item.path("tipo").asText()).isEqualTo("ARTISTA"); }
        if (item.has("tipoContratante")) { esperados.add("tipoContratante"); assertThat(item.path("tipo").asText()).isEqualTo("CONTRATANTE"); }
        if (item.has("favorito")) { esperados.add("favorito"); assertThat(item.path("tipo").asText()).isEqualTo("ARTISTA"); }
        assertThat(campos).containsExactlyInAnyOrderElementsOf(esperados);
        assertThat(item.toString()).doesNotContain("hash-privado", "rf37.test", "11999999999", "responsavel-privado", "INICIANTE");
    }
    private Usuario usuario(String chave, TipoUsuario tipo, int idade) {
        Usuario usuario = OfficialSchemaFixtures.usuario();
        usuario.setNome(chave); usuario.setUsername(chave); usuario.setEmail(chave + "@rf37.test");
        usuario.setTelefone("11999999999"); usuario.setSenha("hash-privado");
        usuario.setDataNascimento(LocalDate.now(clock).minusYears(idade));
        usuario.setTipoUsuario(tipo); usuario.setStatusConta(StatusConta.ATIVA); usuario.setEmailVerificado(true);
        return usuarios.saveAndFlush(usuario);
    }
    private PerfilArtista artista(String chave, int idade) {
        PerfilArtista perfil = new PerfilArtista(); perfil.setUsuario(usuario(chave, TipoUsuario.ARTISTA, idade));
        perfil.setTipoPerfilArtistico(TipoPerfilArtistico.ARTISTA_SOLO); perfil.setCidade("Campinas"); perfil.setEstado("SP");
        perfil.setDisponivelOportunidades(true); perfil.setBiografia("Biografia pública");
        return artistas.saveAndFlush(perfil);
    }
    private PerfilContratante contratante(String chave, int idade) {
        PerfilContratante perfil = new PerfilContratante(); perfil.setUsuario(usuario(chave, TipoUsuario.CONTRATANTE, idade));
        perfil.setTipoPerfil("PESSOA_FISICA"); perfil.setCidade("Campinas"); perfil.setEstado("SP");
        return contratantes.saveAndFlush(perfil);
    }
    private void autorizar(Long id, boolean consentiu, boolean revogado) {
        jdbc.update("""
                insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,data_consentimento,consentimento_revogado)
                values (?,'responsavel-privado','11988887777','responsavel@rf37.test',?,?)
                """, id, consentiu ? java.time.LocalDateTime.now(clock) : null, revogado);
    }
    private void area(Long id, int area) {
        jdbc.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal,nivel_experiencia) values (?,?,false,'INICIANTE')", id, area);
    }
    private boolean autorizado(Long id) {
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status ->
                menorPolicy.autorizado(usuarios.findById(id).orElseThrow())));
    }
    public static class SqlInspector implements StatementInspector {
        public static final ThreadLocal<List<String>> sql = ThreadLocal.withInitial(ArrayList::new);
        @Override public String inspect(String statement) { sql.get().add(statement); return statement; }
    }
}
