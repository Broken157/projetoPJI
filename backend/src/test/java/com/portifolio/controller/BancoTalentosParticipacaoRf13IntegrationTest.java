package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.repository.BancoTalentosRepository;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.support.OfficialPostgreSQLContainer;
import com.portifolio.support.OfficialSchemaFixtures;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class BancoTalentosParticipacaoRf13IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired Clock clock;
    @Autowired Environment environment;
    @MockitoSpyBean BancoTalentosRepository banco;
    private final ObjectMapper mapper = new ObjectMapper();
    private long artista, outroArtista, contratante, outroContratante;
    private String token;

    @BeforeEach
    void dados() {
        limpar();
        artista = artista("artista", 30);
        outroArtista = artista("colega", 30);
        contratante = contratante("contratante", 30);
        outroContratante = contratante("outro_contratante", 30);
        token = bearer(artista);
    }

    @AfterEach
    void limpar() {
        db.execute("truncate salas_chat, usuarios, funcoes, especializacoes restart identity cascade");
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "POST"})
    void anonimoOuTokenInvalidoNaoEntraNemConsulta(String metodo) throws Exception {
        mvc.perform(request(metodo, contratante)).andExpect(status().isUnauthorized());
        mvc.perform(request(metodo, contratante).header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
        assertThat(vinculos()).isEmpty();
    }

    @ParameterizedTest @CsvSource({"CONTRATANTE,GET", "CONTRATANTE,POST", "ADMIN,GET", "ADMIN,POST", "MODERADOR,GET", "MODERADOR,POST"})
    void papelNaoArtistaNaoInsereTerceiros(String papel, String metodo) throws Exception {
        long ator = usuario("interno", papel, 30);
        mvc.perform(request(metodo, contratante).header("Authorization", bearer(ator)))
                .andExpect(status().isForbidden());
        assertThat(vinculos()).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"PENDENTE_VERIFICACAO_EMAIL", "PENDENTE_TIPO_PERFIL", "PENDENTE_CONSENTIMENTO", "BLOQUEADA"})
    void JwtAntigoNaoContornaEstadoAtualDaConta(String estado) throws Exception {
        db.update("update usuarios set status_conta=?::status_conta_enum where id=?", estado, artista);
        mvc.perform(entrada(contratante)).andExpect(status().isUnauthorized());
        mvc.perform(consulta(contratante)).andExpect(status().isUnauthorized());
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void perfilIncompletoRecebe422SemEfeitos() throws Exception {
        db.update("update usuarios set perfil_completo=false where id=?", artista);
        mvc.perform(entrada(contratante)).andExpect(status().isUnprocessableEntity());
        assertThat(vinculos()).isEmpty();
        assertThat(quantidade("notificacoes")).isZero();
    }

    @Test
    void perfilArtistaAusenteNaoPermiteEntradaMesmoComFlagLegadaTrue() throws Exception {
        db.update("delete from perfis_artistas where usuario_id=?", artista);
        db.update("update usuarios set perfil_completo=true where id=?", artista);
        mvc.perform(entrada(contratante)).andExpect(status().isUnprocessableEntity());
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void contratanteInexistenteTemErroGenericoIgualAoInacessivel() throws Exception {
        JsonNode inexistente = erro404(999999);
        db.update("update usuarios set status_conta='BLOQUEADA' where id=?", contratante);
        JsonNode privado = erro404(contratante);
        assertThat(privado.path("mensagem")).isEqualTo(inexistente.path("mensagem"));
        assertThat(privado.toString()).doesNotContain("SQL", "status_conta", "@rf13.test", "stacktrace");
        assertThat(vinculos()).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"ARTISTA", "ADMIN", "MODERADOR"})
    void alvoDeOutroPapelNaoRecebeParticipacaoMesmoComPerfilContratanteLegado(String papel) throws Exception {
        db.update("update usuarios set tipo_usuario=?::tipo_usuario_enum where id=?", papel, contratante);
        erro404(contratante);
        mvc.perform(consulta(contratante)).andExpect(status().isNotFound());
        assertThat(vinculos()).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"PENDENTE_VERIFICACAO_EMAIL", "PENDENTE_TIPO_PERFIL", "PENDENTE_CONSENTIMENTO", "BLOQUEADA"})
    void alvoInaptoNaoRecebeParticipacao(String estado) throws Exception {
        db.update("update usuarios set status_conta=?::status_conta_enum where id=?", estado, contratante);
        erro404(contratante);
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void alvoSemPerfilOuMenorNaoPublicavelRecebe404() throws Exception {
        db.update("delete from perfis_contratantes where usuario_id=?", contratante);
        erro404(contratante);
        db.update("update usuarios set data_nascimento=? where id=?", LocalDate.now(clock).minusYears(17), outroContratante);
        erro404(outroContratante);
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void consultaFalseEntrada201EConsultaTrueUsamAChaveOficial() throws Exception {
        assertThat(estado(contratante).path("participante").asBoolean()).isFalse();
        JsonNode criada = mapper.readTree(mvc.perform(entrada(contratante)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertThat(criada).isEqualTo(estado(contratante));
        assertThat(criada.path("contratanteId").asLong()).isEqualTo(contratante);
        assertThat(criada.path("participante").asBoolean()).isTrue();
        assertThat(vinculos()).hasSize(1);
        assertThat(vinculos().getFirst()).containsEntry("contratante_id", contratante).containsEntry("artista_id", artista);
        assertThat(vinculos().getFirst().get("data_adicao")).isNotNull();
    }

    @Test
    void atorVemDoJwtEArtistaIdDeTerceiroNoPayloadEQueryNaoConcedeAutoridade() throws Exception {
        mvc.perform(entrada(contratante).param("artistaId", Long.toString(outroArtista))
                        .param("contratanteId", Long.toString(outroContratante))
                        .content("{\"confirmado\":true,\"artistaId\":" + outroArtista + ",\"usuarioId\":" + outroArtista + "}"))
                .andExpect(status().isCreated());
        assertThat(vinculos()).hasSize(1);
        assertThat(vinculos().getFirst()).containsEntry("artista_id", artista).containsEntry("contratante_id", contratante);
        mvc.perform(get(rota(contratante)).header("Authorization", bearer(outroArtista)).param("artistaId", Long.toString(artista)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participante").value(false));
        assertThat(estado(contratante).path("participante").asBoolean()).isTrue();
    }

    @Test
    void bancosIndependentesPermitemMesmoArtistaEmDoisContratantes() throws Exception {
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        assertThat(estado(outroContratante).path("participante").asBoolean()).isFalse();
        mvc.perform(entrada(outroContratante)).andExpect(status().isCreated());
        assertThat(vinculos()).hasSize(2).allSatisfy(v -> assertThat(v.get("artista_id")).isEqualTo(artista));
    }

    @Test
    void repeticao200NaoRegravaDataNemCriaSegundaRelacaoOuNotificacaoImpropria() throws Exception {
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        var antes = vinculos();
        for (int i = 0; i < 3; i++) mvc.perform(entrada(contratante)).andExpect(status().isOk());
        assertThat(vinculos()).isEqualTo(antes);
        assertThat(quantidade("notificacoes")).isZero();
    }

    @RepeatedTest(3)
    void concorrenciaRealNoPostgresProduzUma201Uma200EUmaLinhaSem500() throws Exception {
        CyclicBarrier inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var chamada = (java.util.concurrent.Callable<Integer>) () -> {
                inicio.await(10, TimeUnit.SECONDS);
                return mvc.perform(entrada(contratante)).andReturn().getResponse().getStatus();
            };
            var primeira = executor.submit(chamada);
            var segunda = executor.submit(chamada);
            assertThat(List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 200);
        }
        assertThat(vinculos()).hasSize(1);
        assertThat(quantidade("notificacoes")).isZero();
    }

    @Test
    void entradaNaoCriaCandidaturaSalvoChatOuOutrasRelacoes() throws Exception {
        List<String> tabelas = db.queryForList("select tablename from pg_tables where schemaname='public' order by tablename", String.class);
        var antes = contagens(tabelas);
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        var depois = contagens(tabelas);
        assertThat(depois.remove("banco_talentos")).isEqualTo(1L);
        antes.remove("banco_talentos");
        assertThat(depois).isEqualTo(antes);
    }

    @Test
    void naoFabricaTipoSemanticoDeNotificacaoParaContornarDatabase04() throws Exception {
        assertThat(db.queryForList("select enumlabel::text from pg_enum join pg_type on pg_type.oid=enumtypid where typname='tipo_notificacao_enum'", String.class))
                .containsExactlyInAnyOrder("CANDIDATURA", "MENSAGEM", "CONVITE", "EDITAL", "SALVO");
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        assertThat(quantidade("notificacoes")).isZero();
        // Critério RF13 de notificação na primeira entrada permanece pendente, não é dado como aprovado.
    }

    @Test
    void indisponibilidadeNaoImpedeEntradaNemApagaVinculoOuDescoberta() throws Exception {
        db.update("update perfis_artistas set disponivel_oportunidades=false where usuario_id=?", artista);
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        var antes = vinculos();
        assertThat(estado(contratante).path("participante").asBoolean()).isTrue();
        mvc.perform(get("/api/perfis/publicos").param("q", "@artista").param("tipo", "ARTISTA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].usuarioId").value(artista));
        assertThat(vinculos()).isEqualTo(antes);
    }

    @Test
    void consultaNaoExigeCompletudeNemApagaHistoricoOuMutaTabelas() throws Exception {
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        db.update("update usuarios set perfil_completo=false where id=?", artista);
        var antes = vinculos();
        var tabelas = db.queryForList("select tablename from pg_tables where schemaname='public' order by tablename", String.class);
        var contagensAntes = contagens(tabelas);
        for (int i = 0; i < 3; i++) assertThat(estado(contratante).path("participante").asBoolean()).isTrue();
        assertThat(vinculos()).isEqualTo(antes);
        assertThat(contagens(tabelas)).isEqualTo(contagensAntes);
    }

    @Test
    void alterarDisponibilidadeDepoisDaEntradaPreservaRelacaoEPerfilPublico() throws Exception {
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        var antes = vinculos();
        db.update("update perfis_artistas set disponivel_oportunidades=false where usuario_id=?", artista);
        assertThat(estado(contratante).path("participante").asBoolean()).isTrue();
        mvc.perform(get("/api/perfis/publicos/ARTISTA/" + artista)).andExpect(status().isOk());
        assertThat(vinculos()).isEqualTo(antes);
    }

    @Test
    void alvoPosteriormentePrivadoNaoApagaRelacaoHistorica() throws Exception {
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        var antes = vinculos();
        db.update("update usuarios set status_conta='BLOQUEADA' where id=?", contratante);
        mvc.perform(consulta(contratante)).andExpect(status().isNotFound());
        erro404(contratante);
        assertThat(vinculos()).isEqualTo(antes);
    }

    @ParameterizedTest @ValueSource(ints = {14, 17})
    void menorAutorizadoParticipaSemNovoConsentimentoEComRespostaRestrita(int idade) throws Exception {
        db.update("update usuarios set data_nascimento=? where id=?", LocalDate.now(clock).minusYears(idade), artista);
        autorizar(artista, true, false);
        var consentimento = db.queryForMap("select * from responsaveis_legais where usuario_id=?", artista);
        JsonNode resposta = mapper.readTree(mvc.perform(entrada(contratante)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        whitelist(resposta);
        whitelist(estado(contratante));
        assertThat(vinculos()).hasSize(1);
        assertThat(db.queryForMap("select * from responsaveis_legais where usuario_id=?", artista)).isEqualTo(consentimento);
    }

    @ParameterizedTest @CsvSource({"false,false", "true,true"})
    void menorAtivoSemConsentimentoVigenteNaoEntraNemConsulta(boolean consentiu, boolean revogado) throws Exception {
        db.update("update usuarios set data_nascimento=? where id=?", LocalDate.now(clock).minusYears(17), artista);
        autorizar(artista, consentiu, revogado);
        mvc.perform(entrada(contratante)).andExpect(status().isUnprocessableEntity());
        mvc.perform(consulta(contratante)).andExpect(status().isUnprocessableEntity());
        assertThat(vinculos()).isEmpty();
    }

    @ParameterizedTest @ValueSource(ints = {13, 17})
    void menorSemResponsavelOuAbaixoDaIdadeMinimaNaoEntra(int idade) throws Exception {
        db.update("update usuarios set data_nascimento=? where id=?", LocalDate.now(clock).minusYears(idade), artista);
        if (idade == 13) autorizar(artista, true, false);
        mvc.perform(entrada(contratante)).andExpect(status().isUnprocessableEntity());
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void revogacaoPosteriorBloqueiaJwtAntigoSemRemoverVinculo() throws Exception {
        db.update("update usuarios set data_nascimento=? where id=?", LocalDate.now(clock).minusYears(17), artista);
        autorizar(artista, true, false);
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        var antes = vinculos();
        db.update("update responsaveis_legais set consentimento_revogado=true where usuario_id=?", artista);
        mvc.perform(entrada(outroContratante)).andExpect(status().isUnprocessableEntity());
        mvc.perform(consulta(contratante)).andExpect(status().isUnprocessableEntity());
        assertThat(vinculos()).isEqualTo(antes);
    }

    @ParameterizedTest @ValueSource(strings = {"DELETE", "PUT", "PATCH"})
    void nenhumMetodoDeRemocaoOuEdicaoFoiLiberado(String metodo) throws Exception {
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
        var antes = vinculos();
        mvc.perform(request(metodo, contratante).header("Authorization", token)).andExpect(status().isForbidden());
        assertThat(vinculos()).isEqualTo(antes);
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"confirmado\":null}", "{\"artistaId\":1}"})
    void confirmacaoObrigatoriaAusenteRecebe400SemParticipacao(String body) throws Exception {
        mvc.perform(entrada(contratante).content(body)).andExpect(status().isBadRequest());
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void confirmacaoNegativaRecebe422SemParticipacao() throws Exception {
        mvc.perform(entrada(contratante).content("{\"confirmado\":false}")).andExpect(status().isUnprocessableEntity());
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void completudeConsomeFlagOficialSemFotoPortfolioRaioOuNovaFormula() throws Exception {
        db.update("update usuarios set foto_perfil_url=null where id=?", artista);
        db.update("update perfis_artistas set url_portfolio=null, raio_atuacao=null where usuario_id=?", artista);
        db.update("update usuarios set perfil_completo=true where id=?", artista);
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
    }

    @Test
    void contratantePublicavelNaoPrecisaTerPerfilCompletoParaReceberArtista() throws Exception {
        db.update("update usuarios set perfil_completo=false where id=?", contratante);
        mvc.perform(get("/api/perfis/publicos/CONTRATANTE/" + contratante)).andExpect(status().isOk());
        mvc.perform(entrada(contratante)).andExpect(status().isCreated());
    }

    @Test
    void respostaDeAdultoTambemUsaSomenteWhitelist() throws Exception {
        whitelist(estado(contratante));
        JsonNode resposta = mapper.readTree(mvc.perform(entrada(contratante)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        whitelist(resposta);
    }

    @Test
    void falhaDepoisDoInsertReverteVinculoNaTransacao() throws Exception {
        doAnswer(invocacao -> {
            invocacao.callRealMethod();
            throw new UnprocessableEntityException("Falha controlada de teste.");
        }).when(banco).adicionar(anyLong(), anyLong());
        mvc.perform(entrada(contratante)).andExpect(status().isUnprocessableEntity());
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void parametrosMalformadosNaoExecutamSqlNemContornamAlvo() throws Exception {
        mvc.perform(post("/api/talentos/contratantes/1 OR 1=1/participacao").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmado\":true}"))
                .andExpect(status().isBadRequest());
        erro404(-1);
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void tokenComPapelAntigoNaoPrevaleceSobrePapelPersistido() throws Exception {
        db.update("update usuarios set tipo_usuario='CONTRATANTE' where id=?", artista);
        mvc.perform(entrada(contratante)).andExpect(status().isForbidden());
        assertThat(vinculos()).isEmpty();
    }

    @Test
    void database04ContinuaValidateEChaveCompostaReal() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(db.queryForList("""
                select a.attname::text from pg_index i
                join pg_class t on t.oid=i.indrelid
                join lateral unnest(i.indkey) with ordinality k(attnum, ordem) on true
                join pg_attribute a on a.attrelid=t.oid and a.attnum=k.attnum
                where t.relname='banco_talentos' and i.indisprimary order by k.ordem
                """, String.class)).containsExactly("contratante_id", "artista_id");
    }

    private String rota(long alvo) { return "/api/talentos/contratantes/" + alvo + "/participacao"; }
    private MockHttpServletRequestBuilder request(String metodo, long alvo) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(metodo), rota(alvo))
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmado\":true}");
    }
    private MockHttpServletRequestBuilder entrada(long alvo) { return request("POST", alvo).header("Authorization", token); }
    private MockHttpServletRequestBuilder consulta(long alvo) { return get(rota(alvo)).header("Authorization", token); }
    private JsonNode estado(long alvo) throws Exception {
        return mapper.readTree(mvc.perform(consulta(alvo)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode erro404(long alvo) throws Exception {
        return mapper.readTree(mvc.perform(entrada(alvo)).andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString());
    }
    private List<Map<String, Object>> vinculos() { return db.queryForList("select * from banco_talentos order by contratante_id,artista_id"); }
    private long quantidade(String tabela) { return db.queryForObject("select count(*) from " + tabela, Long.class); }
    private Map<String, Long> contagens(List<String> tabelas) {
        Map<String, Long> resultado = new java.util.LinkedHashMap<>();
        tabelas.forEach(tabela -> resultado.put(tabela, quantidade(tabela)));
        return resultado;
    }
    private void whitelist(JsonNode node) {
        List<String> campos = new ArrayList<>(); node.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder("contratanteId", "participante");
        assertThat(node.toString()).doesNotContain("responsavel", "consentimento", "experiencia", "senha", "@rf13.test");
    }
    private String bearer(long id) { return "Bearer " + jwt.gerarToken(usuarios.findById(id).orElseThrow()); }
    private long usuario(String chave, String papel, int idade) {
        return db.queryForObject("""
                insert into usuarios(nome,username,data_nascimento,telefone,email,senha,tipo_usuario,status_conta,email_verificado)
                values (?,?,?,'11999999999',?,'hash-ficticio',?::tipo_usuario_enum,'ATIVA',true) returning id
                """, Long.class, chave, chave, LocalDate.now(clock).minusYears(idade), chave + "@rf13.test", papel);
    }
    private long artista(String chave, int idade) {
        long id = usuario(chave, "ARTISTA", idade);
        db.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,disponivel_oportunidades) values (?,'ARTISTA_SOLO',true)", id);
        OfficialSchemaFixtures.completarArtista(db, id);
        return id;
    }
    private long contratante(String chave, int idade) {
        long id = usuario(chave, "CONTRATANTE", idade);
        db.update("insert into perfis_contratantes(usuario_id,tipo_contratante) values (?,'PESSOA_FISICA')", id);
        return id;
    }
    private void autorizar(long id, boolean consentiu, boolean revogado) {
        db.update("""
                insert into responsaveis_legais(usuario_id,nome_responsavel,telefone_responsavel,email_responsavel,data_consentimento,consentimento_revogado)
                values (?,'responsavel-privado','11988887777','responsavel@rf13.test',?,?)
                """, id, consentiu ? java.time.LocalDateTime.now(clock) : null, revogado);
    }
}
