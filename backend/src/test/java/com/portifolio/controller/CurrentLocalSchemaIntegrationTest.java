package com.portifolio.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.portifolio.model.PerfilArtista;
import com.portifolio.model.enums.*;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.security.JwtService;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integração adicional no banco descartável instalado; todas as linhas de teste são revertidas. */
@SpringBootTest(properties={"spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/palco_dev_manu05}?stringtype=unspecified",
        "app.vagas.auto-close.enabled=false"})
@ActiveProfiles("banco-oficial-local")
@AutoConfigureMockMvc @Transactional
@EnabledIfSystemProperty(named="palco.current-db-tests", matches="true")
class CurrentLocalSchemaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    @Autowired EntityManager em;
    @Autowired Environment env;

    @BeforeEach void exigeBancoDescartavel() {
        assertThat(db.queryForObject("select current_database()", String.class)).isEqualTo("palco_dev_manu05");
    }

    @Test void schemaRealCriaEntityManagerComValidateETaxonomiaDisponivel() throws Exception {
        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(em.getEntityManagerFactory().isOpen()).isTrue();
        assertThat(db.queryForObject("select count(*) from pg_tables where schemaname='public'", Long.class)).isEqualTo(43);
        assertThat(db.queryForList("select column_name from information_schema.columns where table_name='perfis_artistas'", String.class))
                .contains("cidade", "estado").doesNotContain("localizacao");
        assertThat(db.queryForList("select column_name from information_schema.columns where table_name='perfis_contratantes'", String.class))
                .contains("tipo_contratante").doesNotContain("cpf", "cnpj", "tipo_perfil");
        mvc.perform(get("/api/capacidades")).andExpect(status().isOk()).andExpect(jsonPath("$.taxonomia").value(true));
    }

    @Test void rf13PreservaAutorizacaoNoPostgresqlInstalado() throws Exception {
        mvc.perform(get("/api/talentos")).andExpect(status().isUnauthorized());
        long artista = usuario("ARTISTA", "ATIVA");
        mvc.perform(get("/api/talentos").header("Authorization", bearer(artista))).andExpect(status().isForbidden());
        long bloqueado = usuario("CONTRATANTE", "BLOQUEADA");
        mvc.perform(get("/api/talentos").header("Authorization", bearer(bloqueado))).andExpect(status().isForbidden());
        long contratante = usuario("CONTRATANTE", "ATIVA");
        mvc.perform(get("/api/talentos").header("Authorization", bearer(contratante)).param("cidade", "INEXISTENTE_"+UUID.randomUUID()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/talentos").header("Authorization", bearer(contratante)).param("vagaId", "9223372036854775807"))
                .andExpect(status().isNotFound());
    }

    @Test void perfilArtistaPersisteCidadeEstadoETiposNoBancoAtual() {
        long id = usuario("ARTISTA", "ATIVA");
        var p = new PerfilArtista(); p.setUsuario(usuarios.findById(id).orElseThrow());
        p.setCidade("Recife"); p.setEstado("PE"); p.setTipoPerfilArtistico(TipoPerfilArtistico.ARTISTA_SOLO); p.setRaioAtuacao(Abrangencia.LOCAL);
        em.persist(p); em.flush(); em.clear();
        var relido = em.find(PerfilArtista.class, id);
        assertThat(relido.getLocalizacao()).isEqualTo("Recife, PE");
        assertThat(relido.getEstado()).isEqualTo("PE");
        assertThat(relido.getRaioAtuacao()).isEqualTo(Abrangencia.LOCAL);
    }

    @Test void rf13ConsultaLocalizacaoETaxonomiaComGatilhosOriginais() throws Exception {
        long dono = usuario("CONTRATANTE", "ATIVA"), artista = usuario("ARTISTA", "ATIVA");
        String cidade = "CIDADE_"+UUID.randomUUID();
        db.update("update usuarios set cpf=? where id=?", String.format("%011d",artista), artista);
        db.update("insert into perfis_artistas(usuario_id,biografia,cidade,estado,url_portfolio,tipo_perfil_artistico,raio_atuacao) values (?,'Bio',?,'SP','https://example.test/portfolio','ARTISTA_SOLO','LOCAL')", artista,cidade);
        Short area = db.queryForObject("select min(id) from areas_artisticas", Short.class);
        if (area == null) {
            area = (short)32760;
            db.update("insert into areas_artisticas(id,nome) values (?,?)",area,"Área de teste "+UUID.randomUUID().toString().substring(0,8));
        }
        Long funcao = db.queryForObject("insert into funcoes(area_id,nome) values (?,?) returning id", Long.class,area,"Função "+UUID.randomUUID());
        Long spec = db.queryForObject("insert into especializacoes(nome) values (?) returning id", Long.class,"Especialização "+UUID.randomUUID());
        db.update("insert into funcao_especializacao values (?,?)",funcao,spec);
        db.update("insert into perfil_artista_area(perfil_artista_id,area_id,principal,nivel_experiencia) values (?,?,true,'INICIANTE')",artista,area);
        db.update("insert into perfil_artista_funcao values (?,?,?)",artista,area,funcao);
        db.update("insert into perfil_artista_especializacao values (?,?,?)",artista,area,spec);
        assertThat(db.queryForObject("select perfil_completo from usuarios where id=?",Boolean.class,artista)).isTrue();
        mvc.perform(get("/api/talentos").header("Authorization",bearer(dono)).param("cidade",cidade).param("estado","sp")
                        .param("areaId",area.toString()).param("funcaoIds",funcao.toString()).param("especializacaoIds",spec.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].artistaId").value(artista))
                .andExpect(jsonPath("$.content[0].localizacao").value(cidade+", SP"))
                .andExpect(jsonPath("$.content[0].quantidadeFuncoesCoincidentes").value(1))
                .andExpect(jsonPath("$.content[0].quantidadeEspecializacoesCoincidentes").value(1))
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist());
        String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                "usuarioId",artista,"biografia","Bio atualizada","cidade",cidade,"estado","SP",
                "urlPortfolio","https://example.test/portfolio","areaPrincipalId",area,"funcaoIds",java.util.List.of(funcao)));
        mvc.perform(put("/api/perfis-artistas/{id}",artista).header("Authorization",bearer(artista))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.localizacao").value(cidade+", SP"));
        em.flush(); em.clear();
        assertThat(db.queryForObject("select count(*) from perfil_artista_especializacao where perfil_artista_id=?",Long.class,artista)).isEqualTo(1);
        assertThat(db.queryForObject("select perfil_completo from usuarios where id=?",Boolean.class,artista)).isTrue();
    }

    private long usuario(String tipo,String status) {
        return db.queryForObject("insert into usuarios(username,nome,data_nascimento,telefone,email,senha,tipo_usuario,status_conta,email_verificado) values (('fixture_' || substring(replace(gen_random_uuid()::text,'-','') for 22)),'Teste transacional','1990-01-01','11999999999',?,'hash',?,?,true) returning id", Long.class,UUID.randomUUID()+"@schema.test",tipo,status);
    }
    private String bearer(long id) { return "Bearer "+jwt.gerarToken(usuarios.findById(id).orElseThrow()); }
}
