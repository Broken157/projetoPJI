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
                .param("contratanteId", dono.getUsuarioId().toString())
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
        assertThat(item.path("status").asText()).isEqualTo(estado.name());
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
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
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
        assertThat(encontrados).containsExactlyElementsOf(esperados).doesNotHaveDuplicates();
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
