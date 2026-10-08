package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.portifolio.model.Usuario;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import com.portifolio.service.PerfilCompletoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.LongStream;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** PostgreSQL database05 integral: somente fixtures de usuário/perfil, nunca catálogo. */
@Testcontainers
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class PerfilTaxonomiaRf54Rf08IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired EntityManagerFactory emf;
    @MockitoSpyBean PerfilCompletoService completo;
    @MockitoSpyBean com.portifolio.security.AuthenticatedUserResolver resolver;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    ObjectMapper json = new ObjectMapper();

    @Test void seteAreasECatalogoOficialReduzidoNaoSaoPreenchidos() throws Exception {
        mvc.perform(get("/api/areas")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(7));
        assertThat(db.queryForList("select nome from areas_artisticas order by id", String.class)).containsExactly(
                "Artes Cênicas", "Música", "Dança", "Artes Visuais", "Audiovisual", "Arte e Tecnologia", "Artes Literárias");
        assertThat(db.queryForObject("select count(*) from funcoes", Integer.class)).isEqualTo(4);
        assertThat(db.queryForObject("select count(*) from especializacoes", Integer.class)).isEqualTo(4);
        mvc.perform(post("/api/funcoes").header("Authorization", token(artista()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Função inventada\",\"areaId\":1}"))
                .andExpect(status().isForbidden());
        assertThat(db.queryForObject("select count(*) from funcoes", Integer.class)).isEqualTo(4);
    }

    @Test void cascatasPublicasSomenteRelacoesExistentesEPaginadas() throws Exception {
        mvc.perform(get("/api/funcoes").param("areaId", "2").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[0].areaId").value(2)).andExpect(jsonPath("$.hasMore").value(true));
        mvc.perform(get("/api/funcoes").param("areaId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/funcoes")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/talentos/especializacoes").param("areaId", "2").param("funcaoIds", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3));
        mvc.perform(get("/api/talentos/especializacoes").param("areaId", "4").param("funcaoIds", "3"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/funcoes").param("areaId", "99")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/funcoes").param("areaId", "2").param("size", "51")).andExpect(status().isBadRequest());
    }

    @Test void jwtDeterminaTitularEIdsNaoConcedemAutoridade() throws Exception {
        var dono = artista(); var terceiro = artista(); var payload = payload(true);
        mvc.perform(put("/api/perfis-artistas/{id}", dono.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload))).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfis-artistas/{id}", dono.getId()).header("Authorization", token(terceiro))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isForbidden());
        payload.put("usuarioId", terceiro.getId());
        editar(dono, payload).andExpect(status().isForbidden());
        payload.remove("usuarioId"); editar(dono, payload).andExpect(status().isOk());
    }

    @Test void multiplasAreasExperienciaECompletudeSemFotoPortfolioRaioOuDisponibilidade() throws Exception {
        var dono = artista(); var payload = payload(true); payload.put("disponivelOportunidades", false);
        editar(dono, payload).andExpect(status().isOk()).andExpect(jsonPath("$.areas.length()").value(2))
                .andExpect(jsonPath("$.areaPrincipalId").value(2)).andExpect(jsonPath("$.funcaoIds.length()").value(4))
                .andExpect(jsonPath("$.areas[0].nivelExperiencia").value("INICIANTE"))
                .andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.responsaveisLegais").doesNotExist()).andExpect(jsonPath("$.condicoesAfirmativas").doesNotExist());
        assertThat(usuarios.findById(dono.getId()).orElseThrow().getPerfilCompleto()).isTrue();
        assertThat(db.queryForObject("select disponivel_oportunidades from perfis_artistas where usuario_id=?",
                Boolean.class, dono.getId())).isFalse();
        assertThat(db.queryForObject("select raio_atuacao from perfis_artistas where usuario_id=?", String.class, dono.getId())).isNull();
        mvc.perform(get("/api/perfis/publicos/ARTISTA/{id}", dono.getId())).andExpect(status().isOk());
    }

    @Test void trocaPrincipalNaoViolaUniqueEPreservaVinculos() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk());
        editar(dono, payload(false)).andExpect(status().isOk()).andExpect(jsonPath("$.areaPrincipalId").value(4));
        assertThat(db.queryForObject("select count(*) from perfil_artista_area where perfil_artista_id=? and principal",
                Integer.class, dono.getId())).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from perfil_artista_funcao where perfil_artista_id=?",
                Integer.class, dono.getId())).isEqualTo(4);
    }

    @ParameterizedTest @ValueSource(strings = {"duas", "nenhuma", "areaDuplicada", "funcaoDuplicada", "specDuplicada", "cruzada", "specIncompativel"})
    void payloadInvalidoPreservaTodoEstadoAnterior(String caso) throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk());
        var antes = estado(dono); var invalido = payload(true);
        @SuppressWarnings("unchecked") var areas = (List<Map<String, Object>>) invalido.get("areas");
        switch (caso) {
            case "duas" -> areas.get(1).put("principal", true);
            case "nenhuma" -> areas.get(0).put("principal", false);
            case "areaDuplicada" -> areas.get(1).put("areaId", 2);
            case "funcaoDuplicada" -> areas.get(0).put("funcaoIds", List.of(3, 3));
            case "specDuplicada" -> areas.get(0).put("especializacaoIds", List.of(3, 3));
            case "cruzada" -> areas.get(1).put("funcaoIds", List.of(3));
            case "specIncompativel" -> areas.get(1).put("especializacaoIds", List.of(3));
        }
        invalido.put("biografia", "Não deve persistir");
        editar(dono, invalido).andExpect(status().isUnprocessableEntity());
        assertThat(estado(dono)).isEqualTo(antes);
    }

    @ParameterizedTest @ValueSource(strings = {"funcaoIds", "especializacaoIds"})
    void sextaOpcaoNaoPersisteNemPreencheCatalogo(String campo) throws Exception {
        var dono = artista(); var invalido = payload(true);
        @SuppressWarnings("unchecked") var areas = (List<Map<String, Object>>) invalido.get("areas");
        areas.get(0).put(campo, LongStream.rangeClosed(1, 6).boxed().toList());
        editar(dono, invalido).andExpect(status().isUnprocessableEntity());
        assertThat(db.queryForObject("select count(*) from perfil_artista_area where perfil_artista_id=?", Integer.class, dono.getId())).isZero();
        assertThat(db.queryForObject("select count(*) from funcoes", Integer.class)).isEqualTo(4);
    }

    @ParameterizedTest @ValueSource(strings = {"areaId", "funcaoIds", "especializacaoIds"})
    void idsInexistentesRejeitadosSemEfeito(String campo) throws Exception {
        var dono = artista(); var invalido = payload(true);
        @SuppressWarnings("unchecked") var areas = (List<Map<String, Object>>) invalido.get("areas");
        areas.get(0).put(campo, campo.equals("areaId") ? 99 : List.of(99999));
        editar(dono, invalido).andExpect(status().isNotFound());
        assertThat(db.queryForObject("select biografia from perfis_artistas where usuario_id=?", String.class, dono.getId())).isNull();
    }

    @Test void removerAreaNaoAfetaOutrasAreasOuSeusTimestamps() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk());
        var timestamp = db.queryForObject("select ultima_atualizacao from perfil_artista_area where perfil_artista_id=? and area_id=2",
                LocalDateTime.class, dono.getId());
        var request = payload(true);
        @SuppressWarnings("unchecked") var areas = (List<Map<String, Object>>) request.get("areas");
        request.put("areas", List.of(areas.get(0)));
        editar(dono, request).andExpect(status().isOk());
        assertThat(db.queryForObject("select count(*) from perfil_artista_area where perfil_artista_id=?", Integer.class, dono.getId())).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from perfil_artista_funcao where perfil_artista_id=? and area_id=4", Integer.class, dono.getId())).isZero();
        assertThat(db.queryForObject("select count(*) from perfil_artista_especializacao where perfil_artista_id=? and area_id=4", Integer.class, dono.getId())).isZero();
        assertThat(db.queryForObject("select ultima_atualizacao from perfil_artista_area where perfil_artista_id=? and area_id=2",
                LocalDateTime.class, dono.getId())).isEqualTo(timestamp);
    }

    @Test void removerFuncaoPodaApenasSpecsIncompativeisEPreservaExperiencia() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk());
        var request = payload(true);
        @SuppressWarnings("unchecked") var areas = (List<Map<String, Object>>) request.get("areas");
        areas.get(0).put("funcaoIds", List.of(4)); areas.get(0).remove("especializacaoIds");
        areas.get(0).remove("nivelExperiencia");
        editar(dono, request).andExpect(status().isOk()).andExpect(jsonPath("$.areas[0].especializacaoIds.length()").value(1))
                .andExpect(jsonPath("$.areas[0].especializacaoIds[0]").value(4))
                .andExpect(jsonPath("$.areas[0].nivelExperiencia").value("INICIANTE"));
    }

    @Test void contratoPlanoPreservaSecundariasESuasSelecoes() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk());
        var request = payload(true); request.remove("areas"); request.put("areaPrincipalId", 2); request.put("funcaoIds", List.of(4));
        editar(dono, request).andExpect(status().isOk()).andExpect(jsonPath("$.areas.length()").value(2));
        assertThat(db.queryForList("select funcao_id from perfil_artista_funcao where perfil_artista_id=? and area_id=4 order by funcao_id",
                Long.class, dono.getId())).containsExactly(1L, 2L);
    }

    @Test void falhaDepoisDoFlushFazRollbackInclusiveDaTrocaDePrincipal() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk()); var antes = estado(dono);
        doThrow(new IllegalStateException("Falha controlada após persistência")).when(completo).recalcular(any(Usuario.class));
        try { assertThatThrownBy(() -> editar(dono, payload(false)))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Falha controlada após persistência"); }
        finally { reset(completo); }
        assertThat(estado(dono)).isEqualTo(antes);
    }

    @Test void edicoesConcorrentesMantemUmaPrincipalELimites() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk());
        var invalido = payload(true);
        @SuppressWarnings("unchecked") var areas = (List<Map<String, Object>>) invalido.get("areas");
        areas.get(0).put("funcaoIds", LongStream.rangeClosed(1, 6).boxed().toList());
        var inicio = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(3);
        try {
            var primeira = pool.submit(() -> { inicio.await(); return editar(dono, payload(false)).andReturn().getResponse().getStatus(); });
            var segunda = pool.submit(() -> { inicio.await(); return editar(dono, payload(true)).andReturn().getResponse().getStatus(); });
            var sexta = pool.submit(() -> { inicio.await(); return editar(dono, invalido).andReturn().getResponse().getStatus(); });
            inicio.countDown();
            assertThat(primeira.get(45, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(segunda.get(45, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(sexta.get(45, TimeUnit.SECONDS)).isEqualTo(422);
        } finally { pool.shutdownNow(); }
        assertThat(db.queryForObject("select count(*) from perfil_artista_area where perfil_artista_id=? and principal", Integer.class, dono.getId())).isEqualTo(1);
        assertThat(db.queryForList("select count(*) from perfil_artista_funcao where perfil_artista_id=? group by area_id", Long.class, dono.getId()))
                .allMatch(n -> n <= 5);
        assertThat(db.queryForList("select count(*) from perfil_artista_especializacao where perfil_artista_id=? group by area_id", Long.class, dono.getId()))
                .allMatch(n -> n <= 5);
    }

    @Test void consultaProfissionalNaoMultiplicaQueriesPorFuncaoOuEspecializacao() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk());
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        mvc.perform(get("/api/perfis-artistas/{id}", dono.getId()).header("Authorization", token(dono)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.areas.length()").value(2));
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(6);
    }

    @Test void todasSeteAreasPermitidasSemLimiteArtificialDeSecundarias() throws Exception {
        var dono = artista(); var request = payload(true);
        @SuppressWarnings("unchecked") var originais = (List<Map<String, Object>>) request.get("areas");
        var todas = new ArrayList<>(originais);
        for (int id : List.of(1, 3, 5, 6, 7)) todas.add(area(id, false, List.of()));
        request.put("areas", todas);
        editar(dono, request).andExpect(status().isOk()).andExpect(jsonPath("$.areas.length()").value(7));
        assertThat(usuarios.findById(dono.getId()).orElseThrow().getPerfilCompleto()).isTrue();
    }

    @Test void removerPrincipalExigeEscolhaExplicitaEntreAreasRemanescentes() throws Exception {
        var dono = artista(); editar(dono, payload(true)).andExpect(status().isOk()); var antes = estado(dono);
        var request = payload(true); request.put("areas", List.of(area(4, false, List.of(1, 2))));
        editar(dono, request).andExpect(status().isUnprocessableEntity()); assertThat(estado(dono)).isEqualTo(antes);
        request.put("areas", List.of(area(4, true, List.of(1, 2))));
        editar(dono, request).andExpect(status().isOk()).andExpect(jsonPath("$.areaPrincipalId").value(4));
        assertThat(db.queryForObject("select count(*) from perfil_artista_area where perfil_artista_id=? and area_id=2",
                Integer.class, dono.getId())).isZero();
        assertThat(usuarios.findById(dono.getId()).orElseThrow().getPerfilCompleto()).isTrue();
    }

    @Test void criarPerfilUsaJwtSemExigirIdentificadorNoPayload() throws Exception {
        var dono = artista(); db.update("delete from perfis_artistas where usuario_id=?", dono.getId());
        var request = payload(true); request.put("tipoPerfilArtistico", "ARTISTA_SOLO");
        mvc.perform(post("/api/perfis-artistas").header("Authorization", token(dono)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(dono.getId()));
        mvc.perform(post("/api/perfis-artistas").header("Authorization", token(dono)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andExpect(status().isConflict());
    }

    @Test void esperaDoLockNaoSobrescreveDadosDaContaAtualizadosSimultaneamente() throws Exception {
        var dono = artista(); var lido = new CountDownLatch(1); var pool = Executors.newSingleThreadExecutor();
        var chamada = new java.util.concurrent.atomic.AtomicReference<Future<Integer>>();
        doAnswer(invocacao -> { var resultado = invocacao.callRealMethod(); lido.countDown(); return resultado; })
                .when(resolver).usuarioAtual();
        try {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                usuarios.findByIdForUpdate(dono.getId()).orElseThrow();
                db.update("update usuarios set telefone='11911112222' where id=?", dono.getId());
                chamada.set(pool.submit(() -> editar(dono, payload(true)).andReturn().getResponse().getStatus()));
                try { assertThat(lido.await(15, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
                assertThat(chamada.get().isDone()).isFalse();
            });
            assertThat(chamada.get().get(45, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(usuarios.findById(dono.getId()).orElseThrow().getTelefone()).isEqualTo("11911112222");
        } finally { reset(resolver); pool.shutdownNow(); }
    }

    private Usuario artista() {
        var suffix = UUID.randomUUID().toString().replace("-", "");
        Long id = db.queryForObject("""
                insert into usuarios(username,nome,email,senha,tipo_usuario,status_conta,data_nascimento,telefone,cpf,email_verificado)
                values (?,'Artista RF54',?,'hash','ARTISTA','ATIVA','1990-01-01','11999999999',?,true) returning id
                """, Long.class, "rf54_" + suffix.substring(0, 22), suffix + "@rf54.test",
                String.format("%011d", System.nanoTime() % 100000000000L));
        db.update("insert into perfis_artistas(usuario_id,tipo_perfil_artistico) values (?,'ARTISTA_SOLO')", id);
        return usuarios.findById(id).orElseThrow();
    }

    private Map<String, Object> payload(boolean musicaPrincipal) {
        var request = new LinkedHashMap<String, Object>();
        request.put("biografia", "Biografia RF08"); request.put("cidade", "Recife"); request.put("estado", "PE");
        request.put("areas", List.of(area(2, musicaPrincipal, List.of(3, 4)), area(4, !musicaPrincipal, List.of(1, 2))));
        return request;
    }
    private Map<String, Object> area(int id, boolean principal, List<Integer> ids) {
        var a = new LinkedHashMap<String, Object>(); a.put("areaId", id); a.put("principal", principal);
        a.put("nivelExperiencia", "INICIANTE"); a.put("funcaoIds", ids); a.put("especializacaoIds", ids); return a;
    }
    private String token(Usuario u) { return "Bearer " + jwt.gerarToken(u); }
    private org.springframework.test.web.servlet.ResultActions editar(Usuario u, Map<String, Object> payload) throws Exception {
        return mvc.perform(put("/api/perfis-artistas/{id}", u.getId()).header("Authorization", token(u))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)));
    }
    private String estado(Usuario u) throws Exception {
        return mvc.perform(get("/api/perfis-artistas/{id}", u.getId()).header("Authorization", token(u)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
