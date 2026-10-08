package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.*;
import com.portifolio.model.enums.*;
import com.portifolio.repository.*;
import com.portifolio.security.JwtService;
import com.portifolio.support.OfficialPostgreSQLContainer;
import com.portifolio.support.OfficialSchemaFixtures;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class CandidatosVagaRf45IntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new OfficialPostgreSQLContainer();

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilArtistaRepository artistas;
    @Autowired PerfilContratanteRepository contratantes;
    @Autowired VagaRepository vagas;
    @Autowired CandidaturaRepository candidaturas;
    @Autowired SalaChatRepository salas;
    @Autowired ParticipanteChatRepository participantes;
    @Autowired JwtService jwt;
    @Autowired EntityManagerFactory entityManagerFactory;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 10, 1, 12, 0);

    @BeforeEach
    @AfterEach
    void limpar() {
        jdbc.execute("TRUNCATE salas_chat, usuarios RESTART IDENTITY CASCADE");
    }

    @Test
    void anonimoETokenInvalidoRecebem401() throws Exception {
        mockMvc.perform(get("/api/vagas/1/candidaturas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/vagas/1/candidaturas")
                .header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
    }

    @Test
    void artistaRecebe403MesmoComVagaInexistente() throws Exception {
        PerfilArtista artista = artista("artista", 25);
        Vaga vaga = vaga(dono("dono"));
        for (Long id : List.of(vaga.getId(), 999999L)) {
            mockMvc.perform(get("/api/vagas/{id}/candidaturas", id)
                    .header("Authorization", bearer(artista.getUsuario())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void outroContratanteNaoGanhaOwnershipComIdDoCliente() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        candidatura(vaga, artista("privado", 25), StatusCandidatura.PENDENTE);
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId())
                .header("Authorization", bearer(dono("intruso").getUsuario())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    void contratanteRecebe404ParaVagaInexistente() throws Exception {
        mockMvc.perform(get("/api/vagas/999999/candidaturas")
                .header("Authorization", bearer(dono("dono").getUsuario())))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaVaziaTemMetadadosPadrao() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId())
                .header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"PENDENTE", "EM_ANALISE"})
    void primeiraAtivaApareceComIdentificadoresPublicos(StatusCandidatura estado) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("publico", 25);
        Candidatura atual = candidatura(vaga, artista, estado);
        JsonNode item = pagina(vaga, dono, 0, 20).path("content").get(0);
        assertThat(item.path("candidaturaId").asLong()).isEqualTo(atual.getId());
        assertThat(item.path("artistaId").asLong()).isEqualTo(artista.getUsuarioId());
        assertThat(item.path("username").asText()).isEqualTo(artista.getUsuario().getUsername());
        assertThat(item.path("status").asText()).isEqualTo("ATIVA");
        mockMvc.perform(get("/api/perfis/publicos/ARTISTA/{id}", item.path("artistaId").asLong()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class,
            names = {"RETIRADA", "CANCELADA_POR_VAGA", "ACEITA", "REJEITADA", "BLOQUEADA"})
    void estadosHistoricosNaoSaoCandidatosAtuais(StatusCandidatura estado) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        candidatura(vaga, artista("historico", 25), estado);
        var antes = historico();
        assertThat(pagina(vaga, dono, 0, 1).path("totalElements").asLong()).isZero();
        assertThat(historico()).isEqualTo(antes);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"PENDENTE", "EM_ANALISE"})
    void retiradaMaisRecandidaturaMostraSoSegundaSemAlterarHistorico(StatusCandidatura estado) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("recandidato", 25);
        Candidatura antiga = candidatura(vaga, artista, StatusCandidatura.RETIRADA);
        Candidatura atual = candidatura(vaga, artista, estado);
        var antes = historico();
        JsonNode pagina = pagina(vaga, dono, 0, 1);
        assertThat(pagina.path("totalElements").asLong()).isOne();
        assertThat(ids(pagina)).containsExactly(atual.getId()).doesNotContain(antiga.getId());
        assertThat(historico()).isEqualTo(antes).hasSize(2);
        mockMvc.perform(get("/api/candidaturas/{id}", antiga.getId())
                .header("Authorization", bearer(artista.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETIRADA"));
        // A consulta não remove a primeira tentativa nem libera uma terceira.
        jdbc.update("update usuarios set perfil_completo = true where id = ?", artista.getUsuarioId());
        mockMvc.perform(post("/api/candidaturas").header("Authorization", bearer(artista.getUsuario()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmacao\":true,\"vagaId\":" + vaga.getId() + "}"))
                .andExpect(status().isConflict());
        assertThat(historico()).isEqualTo(antes);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"PENDENTE", "EM_ANALISE"})
    void duplicidadeAtivaLegadaExibeSomenteMaiorId(StatusCandidatura estado) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("duplicado-legado", 25);
        candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        Candidatura ultima = candidatura(vaga, artista, estado);
        var antes = historico();
        assertThat(ids(pagina(vaga, dono, 0, 1))).containsExactly(ultima.getId());
        assertThat(historico()).isEqualTo(antes);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class,
            names = {"RETIRADA", "CANCELADA_POR_VAGA", "ACEITA", "REJEITADA", "BLOQUEADA"})
    void ultimaTerminalLegadaNaoReativaTentativaAnterior(StatusCandidatura terminal) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("legado", 25);
        candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        candidatura(vaga, artista, terminal);
        var antes = historico();
        assertThat(ids(pagina(vaga, dono, 0, 1))).isEmpty();
        assertThat(historico()).isEqualTo(antes);
    }

    @Test
    void filtrosContagemEPaginasSaoDeterministicosSemOutrasVagasOuDuplicacao() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        List<Long> esperados = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PerfilArtista artista = artista("atual" + i, 25);
            candidatura(vaga, artista, StatusCandidatura.RETIRADA);
            esperados.add(candidatura(vaga, artista, StatusCandidatura.PENDENTE).getId());
        }
        candidatura(vaga, artista("retirado", 25), StatusCandidatura.RETIRADA);
        candidatura(vaga(dono), artista("outra-vaga", 25), StatusCandidatura.PENDENTE);
        candidatura(vaga(dono("outro")), artista("outro-dono", 25), StatusCandidatura.PENDENTE);
        List<Long> encontrados = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            JsonNode pagina = pagina(vaga, dono, page, 2);
            assertThat(pagina.path("totalElements").asLong()).isEqualTo(5);
            assertThat(pagina.path("totalPages").asInt()).isEqualTo(3);
            assertThat(ids(pagina(vaga, dono, page, 2))).isEqualTo(ids(pagina));
            encontrados.addAll(ids(pagina));
        }
        assertThat(encontrados).containsExactlyElementsOf(esperados.reversed()).doesNotHaveDuplicates();
        assertThat(ids(pagina(vaga, dono, 3, 2))).isEmpty();
    }

    @Test
    void limiteServerSidePermanece50() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        for (int i = 0; i < 51; i++) candidatura(vaga, artista("limite" + i, 25), StatusCandidatura.PENDENTE);
        JsonNode pagina = pagina(vaga, dono, 0, Integer.MAX_VALUE);
        assertThat(pagina.path("size").asInt()).isEqualTo(50);
        assertThat(ids(pagina)).hasSize(50);
        assertThat(pagina.path("totalElements").asLong()).isEqualTo(51);
        assertThat(pagina.path("hasNext").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,-1", "page,abc", "size,abc", "page,2147483648"})
    void paginacaoInvalidaRetorna400SemDetalhesInternos(String parametro, String valor) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId())
                .header("Authorization", bearer(dono.getUsuario())).param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void vinteCandidatosComFuncoesNaoGeramNMaisUm() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        Long funcao = jdbc.queryForObject("insert into funcoes(area_id,nome) values (1,'RF45 função') returning id", Long.class);
        jdbc.update("insert into vaga_funcao(vaga_id,funcao_id) values (?,?)", vaga.getId(), funcao);
        for (int i = 0; i < 20; i++) {
            PerfilArtista artista = artista("funcao" + i, 25);
            jdbc.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal) values (?,1,true)", artista.getUsuarioId());
            jdbc.update("insert into perfil_artista_funcao(perfil_artista_id,area_id,funcao_id) values (?,1,?)", artista.getUsuarioId(), funcao);
            candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        }
        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        pagina(vaga, dono, 0, 2);
        long pequena = stats.getPrepareStatementCount();
        stats.clear();
        JsonNode grande = pagina(vaga, dono, 0, 20);
        long consultas = stats.getPrepareStatementCount();
        assertThat(ids(grande)).hasSize(20);
        assertThat(grande.path("content").get(0).path("quantidadeFuncoesCoincidentes").asInt()).isOne();
        assertThat(consultas).isLessThanOrEqualTo(pequena + 1).isLessThanOrEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(ints = {14, 17})
    void candidatoMenorAutorizadoReutilizaPerfilPublicoEChatSemDadosPrivados(int idade) throws Exception {
        PerfilContratante dono = dono("dono");
        PerfilContratante intruso = dono("intruso");
        Vaga vaga = vaga(dono);
        PerfilArtista menor = artista("menor", idade);
        autorizar(menor.getUsuario());
        jdbc.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal,nivel_experiencia) values (?,1,true,'INICIANTE')", menor.getUsuarioId());
        candidatura(vaga, menor, StatusCandidatura.PENDENTE);
        var antes = historico();
        JsonNode item = pagina(vaga, dono, 0, 20).path("content").get(0);
        verificarPrivacidade(item.toString());
        String perfil = mockMvc.perform(get("/api/perfis/publicos/ARTISTA/{id}", item.path("artistaId").asLong()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        verificarPrivacidade(perfil);
        mockMvc.perform(get("/api/portfolio/publico/artistas/{id}/arquivos", menor.getUsuarioId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(post("/api/portfolio/videos").header("Authorization", bearer(menor.getUsuario()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://vimeo.com/123\"}"))
                .andExpect(status().isCreated());
        String videos = mockMvc.perform(get("/api/portfolio/publico/artistas/{id}/videos", menor.getUsuarioId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].embedUrl").value("https://player.vimeo.com/video/123"))
                .andReturn().getResponse().getContentAsString();
        verificarPrivacidade(videos);
        long primeira = abrirSala(dono.getUsuario(), item.path("artistaId").asLong());
        assertThat(abrirSala(dono.getUsuario(), menor.getUsuarioId())).isEqualTo(primeira);
        assertThat(abrirSala(menor.getUsuario(), dono.getUsuarioId())).isEqualTo(primeira);
        assertThat(salas.count()).isOne();
        assertThat(participantes.findUsuarioIdsBySalaId(primeira))
                .containsExactlyInAnyOrder(dono.getUsuarioId(), menor.getUsuarioId());
        mockMvc.perform(get("/api/chat/salas/{id}/mensagens", primeira)
                .header("Authorization", bearer(intruso.getUsuario()))).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/chat/salas").header("Authorization", bearer(intruso.getUsuario()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioDestinoId\":" + menor.getUsuarioId() + "}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(historico()).isEqualTo(antes);
        // A autorização é reconsultada também ao reabrir uma sala existente.
        jdbc.update("update responsaveis_legais set consentimento_revogado = true where usuario_id = ?", menor.getUsuarioId());
        negarPerfilEChat(dono, menor);
    }

    @Test
    void candidatoAdultoListadoPodeAbrirEReutilizarChatExistente() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("adulto", 25);
        candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        JsonNode item = pagina(vaga, dono, 0, 20).path("content").get(0);
        verificarPrivacidade(item.toString());
        long sala = abrirSala(dono.getUsuario(), item.path("artistaId").asLong());
        assertThat(abrirSala(dono.getUsuario(), artista.getUsuarioId())).isEqualTo(sala);
        assertThat(salas.count()).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sem", "pendente", "revogado", "inativa", "abaixo14"})
    void menorSemAutorizacaoVigenteNaoTemPerfilPublicoNemAberturaDeChat(String situacao) throws Exception {
        PerfilContratante dono = dono("dono");
        PerfilArtista menor = artista("menor", "abaixo14".equals(situacao) ? 13 : 16);
        candidatura(vaga(dono), menor, StatusCandidatura.PENDENTE);
        if (!"sem".equals(situacao)) autorizar(menor.getUsuario());
        if ("pendente".equals(situacao)) jdbc.update("update responsaveis_legais set data_consentimento = null where usuario_id = ?", menor.getUsuarioId());
        if ("revogado".equals(situacao)) jdbc.update("update responsaveis_legais set consentimento_revogado = true where usuario_id = ?", menor.getUsuarioId());
        if ("inativa".equals(situacao)) jdbc.update("update usuarios set status_conta = 'BLOQUEADA' where id = ?", menor.getUsuarioId());
        negarPerfilEChat(dono, menor);
        assertThat(salas.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"ACEITA", "REJEITADA"})
    void consultaNaoCriaTransicaoFormalNemPermitePutDoContratante(StatusCandidatura destino) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("artista", 25);
        Candidatura candidatura = candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        var antes = historico();
        pagina(vaga, dono, 0, 20);
        mockMvc.perform(put("/api/candidaturas/{id}", candidatura.getId())
                .header("Authorization", bearer(dono.getUsuario())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"vagaId\":" + vaga.getId() + ",\"artistaId\":" + artista.getUsuarioId()
                        + ",\"status\":\"" + destino.name() + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(historico()).isEqualTo(antes);
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }

    @Test
    void todasERetiradasUsamUltimaTentativaSemRessuscitarAnterior() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista reaplicante = artista("reaplicante", 25);
        candidatura(vaga, reaplicante, StatusCandidatura.RETIRADA);
        Candidatura ativa = candidatura(vaga, reaplicante, StatusCandidatura.PENDENTE);
        Candidatura retirada = candidatura(vaga, artista("retirada", 25), StatusCandidatura.RETIRADA);
        Candidatura legado = candidatura(vaga, artista("legado", 25), StatusCandidatura.ACEITA);
        var antes = historico();
        assertThat(ids(consulta(vaga, dono))).containsExactly(legado.getId(), retirada.getId(), ativa.getId());
        assertThat(ids(consulta(vaga, dono, "status", "ATIVAS"))).containsExactly(ativa.getId());
        assertThat(ids(consulta(vaga, dono, "status", "RETIRADAS"))).containsExactly(retirada.getId());
        assertThat(historico()).isEqualTo(antes).hasSize(4);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"ACEITA", "REJEITADA", "BLOQUEADA", "CANCELADA_POR_VAGA"})
    void todasPreservaLegadoSemInventarSelecaoFormal(StatusCandidatura estado) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        candidatura(vaga, artista("legado", 25), estado);
        var antes = historico();
        JsonNode item = consulta(vaga, dono).path("content").get(0);
        assertThat(item.path("status").isNull()).isTrue();
        assertThat(item.path("registroLegado").asBoolean()).isTrue();
        assertThat(item.toString()).doesNotContain("statusLegado", estado.name());
        assertThat(item.path("statusVaga").asText()).isEqualTo("ABERTA");
        assertThat(historico()).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "\\", "' OR 1=1 --"})
    void buscaEscapaCuringasETrataTentativaDeInjecaoComoTexto(String termo) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista literal = artista("literal", 25);
        jdbc.update("update usuarios set nome=? where id=?", "Nome " + termo + " Público", literal.getUsuarioId());
        Candidatura esperada = candidatura(vaga, literal, StatusCandidatura.PENDENTE);
        candidatura(vaga, artista("nao-corresponde", 25), StatusCandidatura.PENDENTE);
        assertThat(ids(consulta(vaga, dono, "busca", termo))).containsExactly(esperada.getId());
    }

    @Test
    void buscaUsaSomenteNomePublicoCaseInsensitive() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("nome", 25);
        jdbc.update("update usuarios set nome='Artista ALFA',email='privado@example.test',username='userPrivado' where id=?",
                artista.getUsuarioId());
        candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        assertThat(consulta(vaga, dono, "busca", "aLfA").path("totalElements").asLong()).isOne();
        assertThat(ids(consulta(vaga, dono, "busca", "privado@example.test"))).isEmpty();
        assertThat(ids(consulta(vaga, dono, "busca", "userPrivado"))).isEmpty();
        assertThat(ids(consulta(vaga, dono, "busca", "Resumo profissional"))).isEmpty();
    }

    @Test
    void periodoUsaDataRealDaUltimaTentativaComFimInclusivo() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        Candidatura inicio = candidatura(vaga, artista("inicio", 25), StatusCandidatura.PENDENTE);
        Candidatura fim = candidatura(vaga, artista("fim", 25), StatusCandidatura.RETIRADA);
        Candidatura fora = candidatura(vaga, artista("fora", 25), StatusCandidatura.PENDENTE);
        jdbc.update("update candidaturas set data_candidatura=? where id=?", DATA.toLocalDate().atStartOfDay(), inicio.getId());
        jdbc.update("update candidaturas set data_candidatura=? where id=?", DATA.toLocalDate().atTime(23, 59, 59), fim.getId());
        jdbc.update("update candidaturas set data_candidatura=? where id=?", DATA.toLocalDate().plusDays(1).atStartOfDay(), fora.getId());
        PerfilArtista reaplicante = artista("reaplicante", 25);
        candidatura(vaga, reaplicante, StatusCandidatura.RETIRADA);
        Candidatura atual = candidatura(vaga, reaplicante, StatusCandidatura.PENDENTE);
        jdbc.update("update candidaturas set data_candidatura=? where id=?", DATA.plusDays(2), atual.getId());
        assertThat(ids(consulta(vaga, dono, "dataInicio", "2026-10-01", "dataFim", "2026-10-01")))
                .containsExactly(fim.getId(), inicio.getId());
        assertThat(ids(consulta(vaga, dono, "dataInicio", "2026-10-02", "dataFim", "2026-10-03")))
                .containsExactly(atual.getId(), fora.getId());
    }

    @ParameterizedTest
    @CsvSource({"status,ACEITA", "status,REJEITADA", "status,EM_ANALISE", "status,APROVADAS",
            "dataInicio,2026-02-30", "dataFim,invalida", "areaId,0", "funcaoId,-1", "especializacaoId,0",
            "usuarioId,1", "ownerId,1", "contratanteId,1", "cpf,123", "email,teste", "sort,id",
            "experiencia,INICIANTE", "afirmativa,true", "somenteFavoritas,invalido"})
    void rejeitaFiltrosForaDoContratoSemConsultaGlobal(String nome, String valor) throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId())
                .header("Authorization", bearer(dono.getUsuario())).param(nome, valor))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.trace").doesNotExist());
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }

    @Test
    void intervaloInvertidoFiltroRepetidoEBuscaLongaSao400() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId()).header("Authorization", bearer(dono.getUsuario()))
                .param("dataInicio", "2026-10-02").param("dataFim", "2026-10-01")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId()).header("Authorization", bearer(dono.getUsuario()))
                .param("status", "ATIVAS", "TODAS")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId()).header("Authorization", bearer(dono.getUsuario()))
                .param("busca", "a".repeat(151))).andExpect(status().isBadRequest());
    }

    @Test
    void taxonomiaCombinaAreaSecundariaFuncaoEEspecializacaoNaMesmaCadeia() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        long[] taxonomia = taxonomia(2);
        PerfilArtista artista = artista("secundaria", 25);
        classificar(artista, taxonomia);
        Candidatura esperada = candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        candidatura(vaga, artista("sem-taxonomia", 25), StatusCandidatura.PENDENTE);
        assertThat(ids(consulta(vaga, dono, "areaId", "2"))).containsExactly(esperada.getId());
        assertThat(ids(consulta(vaga, dono, "funcaoId", "" + taxonomia[0]))).containsExactly(esperada.getId());
        assertThat(ids(consulta(vaga, dono, "especializacaoId", "" + taxonomia[1]))).containsExactly(esperada.getId());
        JsonNode pagina = consulta(vaga, dono, "areaId", "2", "funcaoId", "" + taxonomia[0],
                "especializacaoId", "" + taxonomia[1]);
        assertThat(ids(pagina)).containsExactly(esperada.getId());
        assertThat(pagina.path("content").get(0).path("areaIds").toString()).contains("2");
        assertThat(pagina.path("content").get(0).path("especializacaoIds").toString()).contains("" + taxonomia[1]);
    }

    @Test
    void areaSemFuncaoContinuaRepresentavelNoFiltro() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("area", 25);
        jdbc.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal) values (?,1,true)", artista.getUsuarioId());
        Candidatura esperada = candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        assertThat(ids(consulta(vaga, dono, "areaId", "1"))).containsExactly(esperada.getId());
    }

    @Test
    void taxonomiaInexistenteOuIncompativelNaoSeTornaFiltroSilencioso() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        long[] t = taxonomia(2);
        for (String campo : List.of("areaId", "funcaoId", "especializacaoId")) {
            mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId())
                    .header("Authorization", bearer(dono.getUsuario())).param(campo, "30000"))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId()).header("Authorization", bearer(dono.getUsuario()))
                .param("areaId", "1").param("funcaoId", "" + t[0])).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId()).header("Authorization", bearer(dono.getUsuario()))
                .param("areaId", "1").param("especializacaoId", "" + t[1])).andExpect(status().isUnprocessableEntity());
        long[] outra = taxonomia(1);
        mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId()).header("Authorization", bearer(dono.getUsuario()))
                .param("funcaoId", "" + t[0]).param("especializacaoId", "" + outra[1])).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void favoritosSaoDoOwnerECombinamComStatusNomeEPeriodoSemNotificar() throws Exception {
        PerfilContratante dono = dono("dono");
        PerfilContratante outro = dono("outro");
        Vaga vaga = vaga(dono);
        PerfilArtista favorito = artista("favorito", 25);
        PerfilArtista alheio = artista("alheio", 25);
        PerfilArtista retirada = artista("retirada", 25);
        Candidatura esperada = candidatura(vaga, favorito, StatusCandidatura.PENDENTE);
        candidatura(vaga, alheio, StatusCandidatura.PENDENTE);
        candidatura(vaga, retirada, StatusCandidatura.RETIRADA);
        favoritar(dono, favorito);
        favoritar(dono, retirada);
        favoritar(outro, alheio);
        JsonNode filtrada = consulta(vaga, dono, "somenteFavoritas", "true", "status", "ATIVAS",
                "busca", "favorito", "dataInicio", "2026-10-01", "dataFim", "2026-10-01");
        assertThat(ids(filtrada)).containsExactly(esperada.getId());
        assertThat(filtrada.path("content").get(0).path("favorito").asBoolean()).isTrue();
        JsonNode todas = consulta(vaga, dono);
        assertThat(todas.path("content").get(1).path("favorito").asBoolean()).isFalse();
        assertThat(consulta(vaga, dono, "somenteFavoritas", "true").path("totalElements").asLong()).isEqualTo(2);
        assertThat(todas.toString()).doesNotContain("ownerId", "usuarioId", "quantidadeFavoritos");
        assertThat(jdbc.queryForObject("select count(*) from notificacoes", Long.class)).isZero();
    }

    @Test
    void consentimentoRevogadoExcluiAntesDaContagemEPaginacao() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista menor = artista("menor", 16);
        autorizar(menor.getUsuario());
        Candidatura menorCand = candidatura(vaga, menor, StatusCandidatura.PENDENTE);
        Candidatura adulta = candidatura(vaga, artista("adulto", 25), StatusCandidatura.PENDENTE);
        assertThat(consulta(vaga, dono).path("totalElements").asLong()).isEqualTo(2);
        jdbc.update("update responsaveis_legais set consentimento_revogado=true where usuario_id=?", menor.getUsuarioId());
        JsonNode pagina = consulta(vaga, dono, "size", "1");
        assertThat(pagina.path("totalElements").asLong()).isOne();
        assertThat(ids(pagina)).containsExactly(adulta.getId());
        mockMvc.perform(post("/api/vagas/{id}/candidaturas/{candidaturaId}/conversa", vaga.getId(), menorCand.getId())
                .header("Authorization", bearer(dono.getUsuario()))).andExpect(status().isUnprocessableEntity());
        assertThat(salas.count()).isZero();
    }

    @Test
    void conversaRf45DerivaDestinoLegitimoEReutilizaSalaRf35() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("adulto", 25);
        Candidatura candidato = candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        var antes = historico();
        JsonNode item = consulta(vaga, dono).path("content").get(0);
        assertThat(item.path("perfilUrl").asText()).isEqualTo("/api/perfis/publicos/ARTISTA/" + artista.getUsuarioId());
        String acao = item.path("conversaUrl").asText();
        JsonNode sala = mapper.readTree(mockMvc.perform(post(acao).header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode repetida = mapper.readTree(mockMvc.perform(post(acao).header("Authorization", bearer(dono.getUsuario())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(repetida.path("salaId")).isEqualTo(sala.path("salaId"));
        assertThat(abrirSala(dono.getUsuario(), artista.getUsuarioId())).isEqualTo(sala.path("salaId").asLong());
        assertThat(salas.count()).isOne();
        assertThat(participantes.findUsuarioIdsBySalaId(sala.path("salaId").asLong()))
                .containsExactlyInAnyOrder(dono.getUsuarioId(), candidato.getArtista().getUsuarioId());
        assertThat(historico()).isEqualTo(antes);
    }

    @Test
    void conversaRf45NegaTerceirosETrocaDeVagaCandidatura() throws Exception {
        PerfilContratante dono = dono("dono");
        PerfilContratante intruso = dono("intruso");
        Vaga vaga = vaga(dono);
        PerfilArtista artista = artista("candidato", 25);
        Candidatura legitima = candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        Candidatura outra = candidatura(vaga(dono), artista("outro", 25), StatusCandidatura.PENDENTE);
        String acao = "/api/vagas/" + vaga.getId() + "/candidaturas/" + legitima.getId() + "/conversa";
        mockMvc.perform(post(acao)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(acao).header("Authorization", bearer(artista.getUsuario()))).andExpect(status().isForbidden());
        mockMvc.perform(post(acao).header("Authorization", bearer(intruso.getUsuario()))).andExpect(status().isForbidden());
        for (long id : List.of(outra.getId(), 999999L)) {
            mockMvc.perform(post("/api/vagas/{id}/candidaturas/{candidaturaId}/conversa", vaga.getId(), id)
                    .header("Authorization", bearer(dono.getUsuario()))).andExpect(status().isNotFound());
        }
        assertThat(salas.count()).isZero();
    }

    @Test
    void cinquentaCandidatosComDuasAreasEspecializacoesEFavoritosTemConsultasLimitadas() throws Exception {
        PerfilContratante dono = dono("dono");
        Vaga vaga = vaga(dono);
        long[] t = taxonomia(2);
        for (int i = 0; i < 55; i++) {
            PerfilArtista artista = artista("lote" + i, 25);
            classificar(artista, t);
            favoritar(dono, artista);
            candidatura(vaga, artista, StatusCandidatura.PENDENTE);
        }
        var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        consulta(vaga, dono, "size", "10", "areaId", "2", "especializacaoId", "" + t[1], "somenteFavoritas", "true");
        long pequena = stats.getPrepareStatementCount();
        stats.clear();
        JsonNode pagina = consulta(vaga, dono, "size", "50", "areaId", "2", "especializacaoId", "" + t[1], "somenteFavoritas", "true");
        long grande = stats.getPrepareStatementCount();
        assertThat(pagina.path("totalElements").asLong()).isEqualTo(55);
        assertThat(ids(pagina)).hasSize(50).doesNotHaveDuplicates();
        System.out.printf("RF45 queries: 10=%d; 50=%d; total=55%n", pequena, grande);
        // Os lotes de especializações crescem por conjuntos de áreas, não por candidato.
        assertThat(grande).isLessThanOrEqualTo(pequena + 3).isLessThanOrEqualTo(14);
        JsonNode segunda = consulta(vaga, dono, "size", "50", "page", "1", "areaId", "2", "somenteFavoritas", "true");
        assertThat(ids(segunda)).hasSize(5).doesNotContainAnyElementsOf(ids(pagina));
    }

    private JsonNode consulta(Vaga vaga, PerfilContratante dono, String... params) throws Exception {
        var request = get("/api/vagas/{id}/candidaturas", vaga.getId()).header("Authorization", bearer(dono.getUsuario()));
        for (int i = 0; i < params.length; i += 2) request.param(params[i], params[i + 1]);
        return mapper.readTree(mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private long[] taxonomia(int area) {
        String nome = "RF45 " + java.util.UUID.randomUUID();
        long funcao = jdbc.queryForObject("insert into funcoes(area_id,nome) values (?,?) returning id", Long.class, area, nome);
        long especializacao = jdbc.queryForObject("insert into especializacoes(nome) values (?) returning id", Long.class, nome);
        jdbc.update("insert into funcao_especializacao(funcao_id,especializacao_id) values (?,?)", funcao, especializacao);
        return new long[]{funcao, especializacao, area};
    }

    private void classificar(PerfilArtista artista, long[] t) {
        jdbc.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal) values (?,1,true),(?,?,false)",
                artista.getUsuarioId(), artista.getUsuarioId(), t[2]);
        jdbc.update("insert into perfil_artista_funcao(perfil_artista_id,area_id,funcao_id) values (?,?,?)",
                artista.getUsuarioId(), t[2], t[0]);
        jdbc.update("insert into perfil_artista_especializacao(perfil_artista_id,area_id,especializacao_id) values (?,?,?)",
                artista.getUsuarioId(), t[2], t[1]);
    }

    private void favoritar(PerfilContratante dono, PerfilArtista artista) {
        jdbc.update("insert into itens_salvos(usuario_id,tipo_alvo,alvo_id) values (?,'PERFIL_ARTISTA',?)",
                dono.getUsuarioId(), artista.getUsuarioId());
    }

    private void negarPerfilEChat(PerfilContratante dono, PerfilArtista menor) throws Exception {
        mockMvc.perform(get("/api/perfis/publicos/ARTISTA/{id}", menor.getUsuarioId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/portfolio/publico/artistas/{id}/arquivos", menor.getUsuarioId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/portfolio/publico/artistas/{id}/videos", menor.getUsuarioId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/chat/salas").header("Authorization", bearer(dono.getUsuario()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioDestinoId\":" + menor.getUsuarioId() + "}"))
                .andExpect(status().isUnprocessableEntity());
    }

    private void verificarPrivacidade(String json) {
        assertThat(json).doesNotContain("email", "telefone", "dataNascimento", "senha", "token",
                "cpf", "cnpj", "responsavel", "consentimento", "experiencia", "Experiencia",
                "afirmativa", "enderecoCompleto", "privado@example.test", "hash-privado");
    }

    private List<java.util.Map<String, Object>> historico() {
        return jdbc.queryForList("select * from candidaturas order by id");
    }

    private List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>();
        pagina.path("content").forEach(item -> ids.add(item.path("candidaturaId").asLong()));
        return ids;
    }

    private JsonNode pagina(Vaga vaga, PerfilContratante dono, int page, int size) throws Exception {
        return mapper.readTree(mockMvc.perform(get("/api/vagas/{id}/candidaturas", vaga.getId())
                .header("Authorization", bearer(dono.getUsuario()))
                .param("status", "ATIVAS")
                .param("page", Integer.toString(page)).param("size", Integer.toString(size)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long abrirSala(Usuario ator, long destinoId) throws Exception {
        return mapper.readTree(mockMvc.perform(post("/api/chat/salas")
                .header("Authorization", bearer(ator)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioDestinoId\":" + destinoId + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .path("salaId").asLong();
    }

    private String bearer(Usuario usuario) { return "Bearer " + jwt.gerarToken(usuario); }

    private Usuario usuario(String nome, TipoUsuario tipo, int idade) {
        Usuario usuario = OfficialSchemaFixtures.usuario();
        usuario.setNome(nome);
        usuario.setEmail(nome + "@rf45.test");
        usuario.setTelefone("11999999999");
        usuario.setDataNascimento(LocalDate.now().minusYears(idade));
        usuario.setSenha("hash-privado");
        usuario.setTipoUsuario(tipo);
        usuario.setStatusConta(StatusConta.ATIVA);
        usuario.setEmailVerificado(true);
        return usuarios.saveAndFlush(usuario);
    }

    private PerfilContratante dono(String nome) {
        PerfilContratante perfil = new PerfilContratante();
        perfil.setUsuario(usuario(nome, TipoUsuario.CONTRATANTE, 30));
        perfil.setTipoPerfil("PESSOA_FISICA");
        return contratantes.saveAndFlush(perfil);
    }

    private PerfilArtista artista(String nome, int idade) {
        PerfilArtista perfil = new PerfilArtista();
        perfil.setUsuario(usuario(nome, TipoUsuario.ARTISTA, idade));
        perfil.setTipoPerfilArtistico(TipoPerfilArtistico.ARTISTA_SOLO);
        perfil.setBiografia("Resumo profissional");
        perfil.setLocalizacao("São Paulo, SP");
        perfil.setUrlPortfolio("https://portfolio.example/publico");
        perfil.setUltimaAtualizacao(DATA);
        return artistas.saveAndFlush(perfil);
    }

    private void autorizar(Usuario usuario) {
        ResponsavelLegal responsavel = new ResponsavelLegal();
        responsavel.setNomeResponsavel("Responsável privado");
        responsavel.setEmailResponsavel("privado@example.test");
        responsavel.setTelefoneResponsavel("11988887777");
        responsavel.setDataConsentimento(DATA);
        responsavel.setConsentimentoRevogado(false);
        usuario.setResponsavelLegal(responsavel);
        usuarios.saveAndFlush(usuario);
    }

    private Vaga vaga(PerfilContratante dono) {
        Vaga vaga = new Vaga();
        vaga.setContratante(dono);
        vaga.setArea(OfficialSchemaFixtures.area());
        vaga.setAbrangencia(Abrangencia.LOCAL);
        vaga.setTitulo("RF45");
        vaga.setDescricao("Descrição");
        vaga.setRequisitos("Requisitos");
        vaga.setCidade("São Paulo");
        vaga.setEstado("SP");
        vaga.setTipoContrato("Freelance");
        vaga.setFormaRemuneracao(FormaRemuneracao.A_COMBINAR);
        vaga.setStatus(StatusVaga.ABERTA);
        return vagas.saveAndFlush(vaga);
    }

    private Candidatura candidatura(Vaga vaga, PerfilArtista artista, StatusCandidatura estado) {
        Candidatura candidatura = new Candidatura();
        candidatura.setVaga(vaga);
        candidatura.setArtista(artista);
        candidatura.setStatus(estado);
        candidatura.setDataCandidatura(DATA.plusSeconds(candidaturas.count()));
        return candidaturas.saveAndFlush(candidatura);
    }
}
