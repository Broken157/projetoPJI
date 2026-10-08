package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.*;
import com.portifolio.model.enums.*;
import com.portifolio.repository.*;
import com.portifolio.security.JwtService;
import com.portifolio.support.OfficialSchemaFixtures;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Filtros novos RF37 com as quatro Funções/Especializações do seed oficial, sem expandi-lo. */
@Testcontainers
@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.portifolio.controller.DescobertaFiltrosRf37IntegrationTest$SqlInspector")
@AutoConfigureMockMvc
class DescobertaFiltrosRf37IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilArtistaRepository artistas;
    @Autowired PerfilContratanteRepository contratantes;
    @Autowired JwtService jwt;
    @Autowired Clock clock;
    ObjectMapper mapper = new ObjectMapper();
    static final String ROTA = "/api/perfis/publicos";

    @BeforeEach void limpar() {
        db.execute("truncate salas_chat,usuarios restart identity cascade");
        SqlInspector.sql.get().clear();
    }

    @Test void principalSecundariaFuncaoEEspecializacaoConsideramQualquerAreaValida() throws Exception {
        var multi = artista("multi", 30); profissional(multi.getUsuarioId());
        var visual = artista("visual", 30); area(visual.getUsuarioId(), 4, true, List.of(1), List.of(1));
        assertThat(ids(pagina("areaId", "2"))).containsExactly(multi.getUsuarioId());
        assertThat(ids(pagina("areaId", "4"))).containsExactly(multi.getUsuarioId(), visual.getUsuarioId());
        assertThat(ids(pagina("funcaoId", "4"))).containsExactly(multi.getUsuarioId());
        assertThat(ids(pagina("especializacaoId", "4"))).containsExactly(multi.getUsuarioId());
        assertThat(ids(pagina("areaId", "4", "funcaoId", "1", "especializacaoId", "1")))
                .containsExactly(multi.getUsuarioId(), visual.getUsuarioId());
        assertThat(ids(pagina("funcaoId", "3", "especializacaoId", "3"))).containsExactly(multi.getUsuarioId());
        assertThat(ids(pagina("areaId", "2", "especializacaoId", "3"))).containsExactly(multi.getUsuarioId());
    }

    @Test void especializacaoDeveSerSelecionadaECompativelComFuncaoNaMesmaArea() throws Exception {
        var valido = artista("valido", 30); area(valido.getUsuarioId(), 2, true, List.of(4), List.of(4));
        var cruzado = artista("cruzado", 30);
        area(cruzado.getUsuarioId(), 2, true, List.of(4), List.of());
        area(cruzado.getUsuarioId(), 4, false, List.of(1), List.of(4));
        var incompativel = artista("incompativel", 30); area(incompativel.getUsuarioId(), 2, true, List.of(3), List.of(4));
        assertThat(ids(pagina("especializacaoId", "4"))).containsExactly(valido.getUsuarioId());
        assertThat(ids(pagina("funcaoId", "4", "especializacaoId", "4"))).containsExactly(valido.getUsuarioId());
        assertThat(ids(pagina("areaId", "2", "funcaoId", "4", "especializacaoId", "4"))).containsExactly(valido.getUsuarioId());
    }

    @ParameterizedTest @CsvSource({"areaId,99", "funcaoId,99999", "especializacaoId,99999"})
    void idsInexistentesGeram422EmVezDePaginaVazia(String parametro, String valor) throws Exception {
        mvc.perform(get(ROTA).param(parametro, valor)).andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest @CsvSource({"4,3,3", "2,1,1", "2,3,1", "2,3,4", "4,,4"})
    void cadeiaImpossivelRejeitadaMesmoComIdsIndividuaisExistentes(String area, String funcao, String spec) throws Exception {
        var request = get(ROTA).param("areaId", area).param("especializacaoId", spec);
        if (funcao != null) request.param("funcaoId", funcao);
        mvc.perform(request).andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest @EnumSource(TipoPerfilArtistico.class)
    void seisTiposFisicosDeArtistaSaoFiltradosSemAliases(TipoPerfilArtistico tipo) throws Exception {
        var alvo = artista("alvo", 30); alvo.setTipoPerfilArtistico(tipo); artistas.saveAndFlush(alvo);
        var outro = artista("outro", 30); outro.setTipoPerfilArtistico(tipo == TipoPerfilArtistico.ARTISTA_SOLO
                ? TipoPerfilArtistico.BANDA : TipoPerfilArtistico.ARTISTA_SOLO); artistas.saveAndFlush(outro);
        var pagina = pagina("tipo", "ARTISTA", "tipoPerfil", tipo.name());
        assertThat(ids(pagina)).containsExactly(alvo.getUsuarioId());
        assertThat(pagina.path("content").get(0).path("tipoPerfil").asText()).isEqualTo(tipo.name());
    }

    @Test void criteriosArtistaisUsamAndETextoNomeOuUsername() throws Exception {
        var alvo = artista("palco_alvo", 30); profissional(alvo.getUsuarioId());
        db.update("update usuarios set nome='Nome Público' where id=?", alvo.getUsuarioId());
        var outro = artista("palco_outro", 30); profissional(outro.getUsuarioId());
        db.update("update perfis_artistas set cidade='Recife',estado='PE' where usuario_id=?", outro.getUsuarioId());
        assertThat(ids(pagina("q", "@PALCO_ALVO", "estado", "sp", "tipoPerfil", "ARTISTA_SOLO", "areaId", "4")))
                .containsExactly(alvo.getUsuarioId());
        assertThat(ids(pagina("q", "NOME PÚBLICO", "cidade", "Campinas", "estado", "SP", "funcaoId", "3", "especializacaoId", "3")))
                .containsExactly(alvo.getUsuarioId());
        assertThat(ids(pagina("q", "palco_alvo", "cidade", "Recife"))).isEmpty();
    }

    @ParameterizedTest @EnumSource(TipoContratante.class)
    void quatroTiposDeContratanteCombinamTextoCidadeEstadoSemExigirVagaOuBanco(TipoContratante tipo) throws Exception {
        var alvo = contratante("entidade_alvo", tipo);
        var outro = contratante("entidade_outro", tipo == TipoContratante.ONG ? TipoContratante.PESSOA_FISICA : TipoContratante.ONG);
        db.update("update perfis_contratantes set cidade='Recife',estado='PE' where usuario_id=?", outro.getUsuarioId());
        var pagina = pagina("tipo", "CONTRATANTE", "tipoContratante", tipo.name(), "q", "@ENTIDADE_ALVO", "cidade", "campinas", "estado", "sp");
        assertThat(ids(pagina)).containsExactly(alvo.getUsuarioId());
        assertThat(pagina.path("content").get(0).path("tipoContratante").asText()).isEqualTo(tipo.name());
        assertThat(ids(pagina("q", "entidade_alvo", "tipoContratante", tipo.name()))).containsExactly(alvo.getUsuarioId());
        assertThat(ids(pagina("q", "entidade_alvo", "cidade", "Recife"))).isEmpty();
        assertThat(db.queryForObject("select count(*) from vagas", Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from banco_talentos", Integer.class)).isZero();
        privado(pagina.path("content").get(0));
    }

    @Test void contratantesTemPaginacaoUnicaCountCorretoEContaBloqueadaExcluida() throws Exception {
        var perfis = IntStream.range(0, 5).mapToObj(i -> contratante("entidade_" + i, TipoContratante.ONG)).toList();
        var bloqueado = contratante("entidade_bloqueada", TipoContratante.ONG);
        db.update("update usuarios set status_conta='BLOQUEADA' where id=?", bloqueado.getUsuarioId());
        var primeira = pagina("tipo", "CONTRATANTE", "tipoContratante", "ONG", "size", "2");
        var meio = pagina("tipo", "CONTRATANTE", "tipoContratante", "ONG", "size", "2", "page", "1");
        var ultima = pagina("tipo", "CONTRATANTE", "tipoContratante", "ONG", "size", "2", "page", "2");
        var todos = new ArrayList<>(ids(primeira)); todos.addAll(ids(meio)); todos.addAll(ids(ultima));
        assertThat(todos).containsExactlyElementsOf(perfis.stream().map(PerfilContratante::getUsuarioId).toList()).doesNotHaveDuplicates();
        assertThat(primeira.path("totalElements").asLong()).isEqualTo(5);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void bancoAtivoBloqueadoC01SemInferirQuantidadeDeMembros(boolean ativo) throws Exception {
        var vazio = contratante("vazio", TipoContratante.PESSOA_FISICA);
        var comMembro = contratante("com_membro", TipoContratante.PESSOA_FISICA);
        var membro = artista("membro", 30);
        db.update("insert into banco_talentos(contratante_id,artista_id) values (?,?)", comMembro.getUsuarioId(), membro.getUsuarioId());
        assertThat(ids(pagina("tipo", "CONTRATANTE"))).containsExactly(comMembro.getUsuarioId(), vazio.getUsuarioId());
        SqlInspector.sql.get().clear();
        mvc.perform(get(ROTA).param("tipo", "CONTRATANTE").param("bancoTalentosAtivo", String.valueOf(ativo)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("C01")));
        assertThat(SqlInspector.sql.get()).isEmpty();
    }

    @Test void favoritoContratanteBloqueadoC06SemUsarPerfilArtista() throws Exception {
        var ator = artista("ator", 30); contratante("entidade", TipoContratante.ONG);
        mvc.perform(get(ROTA).param("tipo", "CONTRATANTE").param("somenteFavoritos", "true"))
                .andExpect(status().isUnauthorized());
        SqlInspector.sql.get().clear();
        mvc.perform(autenticado(ator.getUsuario()).param("tipo", "CONTRATANTE").param("somenteFavoritos", "true"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("C06")));
        assertThat(SqlInspector.sql.get()).noneMatch(sql -> sql.contains("itens_salvos"));
        var publico = pagina(autenticado(ator.getUsuario()).param("tipo", "CONTRATANTE"));
        assertThat(publico.path("content").get(0).has("favorito")).isFalse();
    }

    @Test void favoritoArtistaPertenceAoJwtSemVazarColecaoOuNotificarDono() throws Exception {
        var a = artista("usuario_a", 30); var b = artista("usuario_b", 30);
        var alvoA = artista("alvo_a", 30); var alvoB = artista("alvo_b", 30);
        profissional(alvoA.getUsuarioId()); profissional(alvoB.getUsuarioId());
        favorito(a.getUsuarioId(), alvoA.getUsuarioId()); favorito(b.getUsuarioId(), alvoB.getUsuarioId());
        var filtro = get(ROTA).param("tipo", "ARTISTA").param("somenteFavoritos", "true");
        mvc.perform(filtro).andExpect(status().isUnauthorized());
        mvc.perform(get(ROTA).header("Authorization", "Bearer inválido").param("tipo", "ARTISTA").param("somenteFavoritos", "true"))
                .andExpect(status().isUnauthorized());
        var paginaA = pagina(autenticado(a.getUsuario()).param("tipo", "ARTISTA").param("somenteFavoritos", "true").param("areaId", "4").param("cidade", "Campinas"));
        assertThat(ids(paginaA)).containsExactly(alvoA.getUsuarioId());
        assertThat(paginaA.path("totalElements").asLong()).isEqualTo(1);
        assertThat(paginaA.path("content").get(0).path("favorito").asBoolean()).isTrue();
        assertThat(ids(pagina(autenticado(b.getUsuario()).param("tipo", "ARTISTA").param("somenteFavoritos", "true"))))
                .containsExactly(alvoB.getUsuarioId());
        var publico = pagina("q", "alvo", "somenteFavoritos", "false");
        publico.path("content").forEach(item -> assertThat(item.has("favorito")).isFalse());
        var proprio = pagina(autenticado(a.getUsuario()).param("q", "alvo"));
        assertThat(proprio.path("content").get(0).path("favorito").asBoolean()).isTrue();
        assertThat(proprio.path("content").get(1).path("favorito").asBoolean()).isFalse();
        assertThat(db.queryForObject("select count(*) from notificacoes", Integer.class)).isZero();
    }

    @Test void contextoMistoDeFavoritosNaoIgnoraContratanteSilenciosamente() throws Exception {
        var ator = artista("ator", 30);
        mvc.perform(get(ROTA).param("somenteFavoritos", "true")).andExpect(status().isUnauthorized());
        mvc.perform(autenticado(ator.getUsuario()).param("somenteFavoritos", "true")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest @CsvSource({"usuarioId,1", "ownerId,1", "favoritosUsuarioId,1", "sort,email", "sort,nomeExibicao", "experienciaMinima,INICIANTE", "raio,LOCAL", "disponivel,false", "condicaoAfirmativa,MULHER", "categoriaAfirmativa,PCD", "recomendados,true", "funcaoId,0", "especializacaoId,-1", "funcaoId,texto", "tipoPerfil,EMPRESA", "tipoContratante,EMPRESA", "bancoTalentosAtivo,invalido"})
    void parametrosPrivadosAliasesEFormatosInvalidosNaoSaoIgnorados(String parametro, String valor) throws Exception {
        mvc.perform(get(ROTA).param(parametro, valor)).andExpect(status().isBadRequest());
    }

    @Test void menorAutorizadoComFiltrosProfissionaisNaoExpoeExperienciaOuResponsavel() throws Exception {
        var menor = artista("jovem", 15); profissional(menor.getUsuarioId());
        db.update("""
                insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,data_consentimento)
                values (?,'Responsável privado','11988887777','responsavel@privado.test',?)
                """, menor.getUsuarioId(), LocalDateTime.now(clock));
        db.update("update perfis_artistas set disponivel_oportunidades=false where usuario_id=?", menor.getUsuarioId());
        var pagina = pagina("tipo", "ARTISTA", "funcaoId", "1", "especializacaoId", "1");
        assertThat(ids(pagina)).containsExactly(menor.getUsuarioId()); privado(pagina.path("content").get(0));
        assertThat(pagina.toString()).doesNotContain("INICIANTE", "Responsável privado", "11988887777", "responsavel@privado.test");
    }

    @Test void paginasComMultiplasAreasFuncoesSpecsTemCountUnicoENaoHaNMaisUm() throws Exception {
        var perfis = IntStream.range(0, 52).mapToObj(i -> artista(String.format("volume_%03d", i), 30)).toList();
        perfis.forEach(p -> profissional(p.getUsuarioId()));
        SqlInspector.sql.get().clear();
        var pequena = pagina("areaId", "2", "funcaoId", "3", "especializacaoId", "3", "size", "1");
        int consultas = SqlInspector.sql.get().size();
        SqlInspector.sql.get().clear();
        var grande = pagina("areaId", "2", "funcaoId", "3", "especializacaoId", "3", "size", "50");
        assertThat(ids(grande)).hasSize(50).doesNotHaveDuplicates();
        assertThat(grande.path("totalElements").asLong()).isEqualTo(52);
        assertThat(SqlInspector.sql.get()).hasSize(consultas); assertThat(consultas).isLessThanOrEqualTo(5);
        assertThat(SqlInspector.sql.get().getLast()).contains("fetch first ? rows only");
        var ultima = pagina("areaId", "2", "funcaoId", "3", "especializacaoId", "3", "size", "50", "page", "1");
        var todos = new ArrayList<>(ids(grande)); todos.addAll(ids(ultima));
        assertThat(todos).containsExactlyElementsOf(perfis.stream().map(PerfilArtista::getUsuarioId).toList()).doesNotHaveDuplicates();
        assertThat(pequena.path("totalElements").asLong()).isEqualTo(52);
        assertThat(db.queryForObject("select count(*) from funcoes", Integer.class)).isEqualTo(4);
        assertThat(db.queryForObject("select count(*) from especializacoes", Integer.class)).isEqualTo(4);
    }

    @Test void estadoFavoritoEmLoteNaoCrescePorPerfilEPaginacaoJaFiltraColecao() throws Exception {
        var ator = artista("ator", 30);
        var perfis = IntStream.range(0, 52).mapToObj(i -> artista(String.format("volume_%03d", i), 30)).toList();
        perfis.forEach(p -> { profissional(p.getUsuarioId()); favorito(ator.getUsuarioId(), p.getUsuarioId()); });
        SqlInspector.sql.get().clear();
        pagina(autenticado(ator.getUsuario()).param("tipo", "ARTISTA").param("q", "volume").param("somenteFavoritos", "true").param("size", "1"));
        int pequena = SqlInspector.sql.get().size(); SqlInspector.sql.get().clear();
        var grande = pagina(autenticado(ator.getUsuario()).param("tipo", "ARTISTA").param("q", "volume").param("somenteFavoritos", "true").param("size", "50"));
        assertThat(SqlInspector.sql.get()).hasSize(pequena); assertThat(pequena).isLessThanOrEqualTo(5);
        assertThat(ids(grande)).hasSize(50).doesNotHaveDuplicates();
        assertThat(grande.path("totalElements").asLong()).isEqualTo(52);
        grande.path("content").forEach(item -> assertThat(item.path("favorito").asBoolean()).isTrue());
        var segunda = pagina(autenticado(ator.getUsuario()).param("tipo", "ARTISTA").param("q", "volume").param("somenteFavoritos", "true").param("size", "50").param("page", "1"));
        var todos = new ArrayList<>(ids(grande)); todos.addAll(ids(segunda));
        assertThat(todos).containsExactlyElementsOf(perfis.stream().map(PerfilArtista::getUsuarioId).toList()).doesNotHaveDuplicates();
    }

    private Usuario usuario(String nome, TipoUsuario tipo, int idade) {
        var usuario = OfficialSchemaFixtures.usuario(); usuario.setNome(nome); usuario.setUsername(nome);
        usuario.setEmail(nome + "@rf37.test"); usuario.setTipoUsuario(tipo); usuario.setDataNascimento(LocalDate.now(clock).minusYears(idade));
        usuario.setSenha("hash-privado"); usuario.setTelefone("11999999999"); usuario.setStatusConta(StatusConta.ATIVA); usuario.setEmailVerificado(true);
        return usuarios.saveAndFlush(usuario);
    }
    private PerfilArtista artista(String nome, int idade) {
        var p = new PerfilArtista(); p.setUsuario(usuario(nome, TipoUsuario.ARTISTA, idade));
        p.setTipoPerfilArtistico(TipoPerfilArtistico.ARTISTA_SOLO); p.setCidade("Campinas"); p.setEstado("SP"); return artistas.saveAndFlush(p);
    }
    private PerfilContratante contratante(String nome, TipoContratante tipo) {
        var p = new PerfilContratante(); p.setUsuario(usuario(nome, TipoUsuario.CONTRATANTE, 30));
        p.setTipoContratante(tipo); p.setCidade("Campinas"); p.setEstado("SP"); return contratantes.saveAndFlush(p);
    }
    private void profissional(Long id) {
        area(id, 2, true, List.of(3, 4), List.of(3, 4)); area(id, 4, false, List.of(1, 2), List.of(1, 2));
    }
    private void area(Long id, int area, boolean principal, List<Integer> funcs, List<Integer> specs) {
        db.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal,nivel_experiencia) values (?,?,?,'INICIANTE')", id, area, principal);
        funcs.forEach(f -> db.update("insert into perfil_artista_funcao(perfil_artista_id,area_id,funcao_id) values (?,?,?)", id, area, f));
        specs.forEach(s -> db.update("insert into perfil_artista_especializacao(perfil_artista_id,area_id,especializacao_id) values (?,?,?)", id, area, s));
    }
    private void favorito(Long usuario, Long alvo) {
        db.update("insert into itens_salvos(usuario_id,tipo_alvo,alvo_id) values (?,'PERFIL_ARTISTA',?)", usuario, alvo);
    }
    private MockHttpServletRequestBuilder autenticado(Usuario u) { return get(ROTA).header("Authorization", "Bearer " + jwt.gerarToken(u)); }
    private JsonNode pagina(String... filtros) throws Exception {
        var request = get(ROTA); for(int i=0;i<filtros.length;i+=2) request.param(filtros[i], filtros[i+1]); return pagina(request);
    }
    private JsonNode pagina(MockHttpServletRequestBuilder request) throws Exception {
        return mapper.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private List<Long> ids(JsonNode pagina) {
        var ids = new ArrayList<Long>(); pagina.path("content").forEach(item -> ids.add(item.path("usuarioId").asLong())); return ids;
    }
    private void privado(JsonNode item) {
        var campos = new ArrayList<String>();
        item.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).contains("usuarioId", "tipo", "username", "nomeExibicao", "avatarUrl", "cidade", "estado")
                .isSubsetOf("usuarioId", "tipo", "username", "nomeExibicao", "avatarUrl",
                "cidade", "estado", "tipoPerfil", "tipoContratante", "favorito");
    }
    public static class SqlInspector implements StatementInspector {
        static final ThreadLocal<List<String>> sql = ThreadLocal.withInitial(ArrayList::new);
        @Override public String inspect(String statement) { sql.get().add(statement); return statement; }
    }
}
