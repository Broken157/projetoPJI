package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.Usuario;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DenunciaRf14Rf18IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    private final ObjectMapper json = new ObjectMapper();
    private Usuario artista, contratante;

    @BeforeEach void fixtures() {
        jdbc.execute("truncate usuarios,funcoes,especializacoes restart identity cascade");
        jdbc.execute("""
                insert into usuarios(id,username,nome,data_nascimento,telefone,email,senha,tipo_usuario,status_conta) values
                (1,'denuncia_artista','Artista teste','1990-01-01','11900000001','artista@denuncia.invalid','hash','ARTISTA','ATIVA'),
                (2,'denuncia_contratante','Contratante teste','1990-01-01','11900000002','contratante@denuncia.invalid','hash','CONTRATANTE','ATIVA'),
                (3,'denuncia_menor','Perfil privado',current_date-interval '16 years','11900000003','menor@denuncia.invalid','hash','ARTISTA','ATIVA'),
                (4,'denuncia_bloqueado','Perfil bloqueado','1990-01-01','11900000004','bloqueado@denuncia.invalid','hash','ARTISTA','BLOQUEADA');
                insert into perfis_artistas(usuario_id,tipo_perfil_artistico,raio_atuacao,cidade,estado) values
                (1,'ARTISTA_SOLO','LOCAL','São Paulo','SP'),(3,'ARTISTA_SOLO','LOCAL','Privada','SP'),(4,'ARTISTA_SOLO','LOCAL','São Paulo','SP');
                insert into perfis_contratantes(usuario_id,nome_empresa,tipo_contratante) values(2,'Empresa teste','SETOR_PRIVADO');
                insert into vagas(id,contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,tipo_contrato,abrangencia,status,endereco_completo) values
                (10,2,1,'Vaga pública','Descrição','Requisitos','São Paulo','SP','Projeto','LOCAL','ABERTA','Endereço privado'),
                (11,2,1,'Vaga privada','Descrição','Requisitos','São Paulo','SP','Projeto','LOCAL','RASCUNHO',null);
                """);
        artista = usuarios.findById(1L).orElseThrow();
        contratante = usuarios.findById(2L).orElseThrow();
    }

    private String token(Usuario usuario) { return "Bearer " + jwt.gerarToken(usuario); }
    private ResultActions enviar(Usuario usuario, String tipo, long alvo, String motivo, String descricao) throws Exception {
        return enviarJson(usuario, json.writeValueAsString(Map.of("tipoAlvo", tipo, "alvoId", alvo, "motivo", motivo, "descricao", descricao)));
    }
    private ResultActions enviarJson(Usuario usuario, String body) throws Exception {
        return mvc.perform(post("/api/denuncias").header("Authorization", token(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private long total() {
        return jdbc.queryForObject("select (select count(*) from denuncias_plagio)+(select count(*) from reportes_usuario)", Long.class);
    }

    @Test void artistaRegistraVagaComIdEDataReaisSemInventarStatus() throws Exception {
        enviar(artista,"VAGA",10,"Conteúdo impróprio","Detalhes reais")
                .andExpect(status().isCreated()).andExpect(header().string("Location","/api/denuncias/CONTEUDO/1"))
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.categoria").value("CONTEUDO"))
                .andExpect(jsonPath("$.dataRegistro").isNotEmpty()).andExpect(jsonPath("$.status").isEmpty());
        assertThat(jdbc.queryForObject("select denunciante_id from reportes_usuario",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select descricao_adicional from reportes_usuario",String.class)).isEqualTo("Detalhes reais");
        assertThat(jdbc.queryForObject("select count(*) from moderacao_conteudo",Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from notificacoes",Long.class)).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"PLAGIO DE IMAGEM","PLAGIO DE AUDIO","COPIA DE BIOGRAFIA","OUTRO"})
    void contratanteRegistraTiposOficiaisDePlagio(String motivo) throws Exception {
        enviar(contratante,"PERFIL_ARTISTA",1,motivo,"Descrição da violação")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.motivo").value(motivo)).andExpect(jsonPath("$.categoria").value("PLAGIO"));
        assertThat(jdbc.queryForObject("select denunciante_id from denuncias_plagio",Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select tipo_violacao::text from denuncias_plagio",String.class)).isEqualTo(motivo.replace(' ', '_'));
    }

    @Test void perfilPublicoContratanteTambemEhAlvoDePlagio() throws Exception {
        enviar(artista,"PERFIL_CONTRATANTE",2,"COPIA DE BIOGRAFIA","Biografia copiada")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipoAlvo").value("PERFIL_CONTRATANTE"));
    }

    @Test void identidadeInjetadaNaoMudaDenuncianteNemStatus() throws Exception {
        enviarJson(artista,"""
                {"tipoAlvo":"VAGA","alvoId":10,"motivo":"Outro","descricao":"Contexto",
                 "denuncianteId":2,"usuarioId":2,"status":"PROCEDENTE","id":999}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.status").isEmpty());
        assertThat(jdbc.queryForObject("select denunciante_id from reportes_usuario",Long.class)).isEqualTo(1);
    }

    @Test void anonimoBloqueadoEmTodosEndpoints() throws Exception {
        mvc.perform(get("/api/denuncias")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/denuncias/CONTEUDO/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/denuncias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoAlvo\":\"VAGA\",\"alvoId\":10,\"motivo\":\"Outro\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(total()).isZero();
    }

    @Test void contaNaoAtivaBloqueada() throws Exception {
        jdbc.update("update usuarios set status_conta='PENDENTE_VERIFICACAO_EMAIL' where id=1");
        enviar(artista,"VAGA",10,"Outro","Contexto").andExpect(status().isUnauthorized());
        mvc.perform(get("/api/denuncias").header("Authorization",token(artista))).andExpect(status().isUnauthorized());
        assertThat(total()).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"VAGA","PERFIL_ARTISTA","PERFIL_CONTRATANTE"})
    void recursoInexistenteNaoCriaDenuncia(String tipo) throws Exception {
        enviar(artista,tipo,99999,"OUTRO","Contexto").andExpect(status().isNotFound());
        assertThat(total()).isZero();
    }

    @Test void idsNaoRevelamPerfisPrivadosOuVagaAlheiaPrivada() throws Exception {
        enviar(artista,"VAGA",11,"Outro","Contexto").andExpect(status().isNotFound());
        enviar(contratante,"PERFIL_ARTISTA",3,"OUTRO","Contexto").andExpect(status().isNotFound());
        enviar(contratante,"PERFIL_ARTISTA",4,"OUTRO","Contexto").andExpect(status().isNotFound());
        enviar(contratante,"PERFIL_CONTRATANTE",1,"OUTRO","Contexto").andExpect(status().isNotFound());
        assertThat(total()).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"Assédio","Plágio","INVALIDO",""})
    void perfilNaoAceitaMotivoForaDoEnumNemDenunciaGeral(String motivo) throws Exception {
        enviar(contratante,"PERFIL_ARTISTA",1,motivo,"Contexto").andExpect(status().isBadRequest());
        assertThat(total()).isZero();
    }

    @Test void camposInvalidosNaoPersistem() throws Exception {
        enviar(artista,"VAGA",10," ","Contexto").andExpect(status().isBadRequest());
        enviar(artista,"VAGA",10,"x".repeat(151),"Contexto").andExpect(status().isBadRequest());
        enviar(artista,"VAGA",0,"Outro","Contexto").andExpect(status().isBadRequest());
        enviar(contratante,"PERFIL_ARTISTA",1,"OUTRO"," ").andExpect(status().isBadRequest());
        enviar(contratante,"PERFIL_ARTISTA",1,"OUTRO","x".repeat(2001)).andExpect(status().isBadRequest());
        enviarJson(artista,"{\"tipoAlvo\":\"COMUNIDADE\",\"alvoId\":10,\"motivo\":\"Outro\"}").andExpect(status().isBadRequest());
        assertThat(total()).isZero();
    }

    @Test void listaEDetalheSaoPrivadosMesmoParaDonoDoRecurso() throws Exception {
        enviar(artista,"VAGA",10,"Outro","Relato privado").andExpect(status().isCreated());
        mvc.perform(get("/api/denuncias").header("Authorization",token(contratante)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/denuncias/CONTEUDO/1").header("Authorization",token(contratante))).andExpect(status().isNotFound());
        var response=mvc.perform(get("/api/denuncias/CONTEUDO/1").header("Authorization",token(artista)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descricao").value("Relato privado")).andReturn();
        assertThat(response.getResponse().getContentAsString()).doesNotContain("denuncianteId","telefone","email","Endereco","senha","medidasAdotadas");
        mvc.perform(get("/api/denuncias/PLAGIO/9999").header("Authorization",token(artista))).andExpect(status().isNotFound());
    }

    @Test void paginacaoLimitadaEstavelESeparadaPorCategoria() throws Exception {
        enviar(artista,"VAGA",10,"Outro","Contexto").andExpect(status().isCreated());
        enviar(artista,"PERFIL_CONTRATANTE",2,"OUTRO","Contexto").andExpect(status().isCreated());
        jdbc.update("update reportes_usuario set data_reporte='2026-01-01 12:00:00'");
        jdbc.update("update denuncias_plagio set data_registro='2026-01-01 12:00:00'");
        mvc.perform(get("/api/denuncias?size=1").header("Authorization",token(artista)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].categoria").value("CONTEUDO")).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/denuncias?size=1&page=1").header("Authorization",token(artista)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].categoria").value("PLAGIO"))
                .andExpect(jsonPath("$.hasNext").value(false)).andExpect(jsonPath("$.hasPrevious").value(true));
        mvc.perform(get("/api/denuncias?size=5000").header("Authorization",token(artista)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
        mvc.perform(get("/api/denuncias?size=0").header("Authorization",token(artista))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/denuncias?page=-1").header("Authorization",token(artista))).andExpect(status().isBadRequest());
    }

    @Test void motivoLivreRf18EhParametrizadoSemExecutarSql() throws Exception {
        String texto="Outro'); delete from usuarios; --";
        enviar(artista,"VAGA",10,texto,"Contexto").andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select motivo_reporte from reportes_usuario",String.class)).isEqualTo(texto);
        assertThat(jdbc.queryForObject("select count(*) from usuarios",Integer.class)).isEqualTo(4);
    }
}
