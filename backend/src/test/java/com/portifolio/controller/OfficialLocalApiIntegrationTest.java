package com.portifolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Usa somente a instância oficial. Todos os dados de teste são revertidos na transação. */
@SpringBootTest(properties={"spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/palco_dev_manu05}?stringtype=unspecified",
        "app.vagas.auto-close.enabled=false"})
@ActiveProfiles("banco-oficial-local")
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(com.portifolio.support.EmailVerificationTestConfig.class)
@Transactional
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="palco.official-db-tests", matches="true")
class OfficialLocalApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    final ObjectMapper mapper=new ObjectMapper();
    record Conta(long id,String email,String bearer,Cookie cookie) {}
    String json(Object body) throws Exception { return mapper.writeValueAsString(body); }
    JsonNode body(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
    Cookie cookie(MvcResult result) {
        String header=result.getResponse().getHeader("Set-Cookie");
        return new Cookie("palco_refresh",header.split(";",2)[0].substring("palco_refresh=".length()));
    }
    Conta conta(String tipo,boolean remember) throws Exception {
        String email="integracao-"+UUID.randomUUID()+"@palco.test";
        long id=body(mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nome","Teste transacional PALCO","email",email,"senha","Palco@2026!",
                    "dataNascimento","1990-01-01","telefone","11999999999","tipoUsuario",tipo))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        MvcResult login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email",email,"senha","Palco@2026!","rememberMe",remember))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
        return new Conta(id,email,"Bearer "+body(login).get("token").asText(),remember?cookie(login):null);
    }
    long vaga(Conta dono) throws Exception {
        return body(mvc.perform(post("/api/vagas").header("Authorization",dono.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("titulo","Vaga transacional PALCO","descricao","Apresentação musical",
                "requisitos","Portfólio atualizado","valorMinimo",100,"formaPagamento","PIX",
                "cidade","São Paulo","estado","SP","modeloTrabalho","PRESENCIAL","tipoContrato","Projeto",
                "dataLimiteCandidatura",LocalDate.now().plusDays(30).toString()))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ABERTA")).andReturn()).get("id").asLong();
    }
    @Test void cookieSeguroRotacaoRevogacaoEHashPersistido() throws Exception {
        Conta a=conta("ARTISTA",true);
        assertThat(jdbc.queryForObject("select token_hash from refresh_tokens where usuario_id=? and ativo",String.class,a.id()))
            .hasSize(64).doesNotContain(a.cookie().getValue());
        MvcResult rotated=mvc.perform(post("/api/auth/refresh").cookie(a.cookie()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
        String header=rotated.getResponse().getHeader("Set-Cookie");
        assertThat(header).contains("HttpOnly","Secure","SameSite=Strict","Path=/api/auth");
        Cookie next=cookie(rotated);
        assertThat(next.getValue()).isNotEqualTo(a.cookie().getValue());
        mvc.perform(post("/api/auth/refresh").cookie(a.cookie())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").header("Authorization",a.bearer())).andExpect(status().isOk());
        String bearer="Bearer "+body(rotated).get("token").asText();
        mvc.perform(get("/api/usuarios/me").header("Authorization",bearer)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").cookie(next).header("Authorization",bearer)).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").cookie(next)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").header("Authorization",bearer)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").header("Authorization",a.bearer())).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where usuario_id=? and ativo",Long.class,a.id())).isZero();
    }
    @Test void semRememberNaoPersisteRefreshEBodyNaoSubstituiCookie() throws Exception {
        Conta a=conta("ARTISTA",false);
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where usuario_id=?",Long.class,a.id())).isZero();
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("refreshToken","token-em-json")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").header("Authorization",a.bearer())).andExpect(status().isNoContent());
        mvc.perform(get("/api/usuarios/me").header("Authorization",a.bearer())).andExpect(status().isUnauthorized());
    }
    @Test void origemExternaNaoConsomeRefresh() throws Exception {
        Conta a=conta("ARTISTA",true);
        mvc.perform(post("/api/auth/refresh").cookie(a.cookie()).header("Origin","https://externo.invalid"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh").cookie(a.cookie())).andExpect(status().isOk());
    }
    @Test void perfilBasicoPrivacidadeEAutoridadeDoJwt() throws Exception {
        Conta a=conta("ARTISTA",false),b=conta("ARTISTA",false);
        Map<String,Object> update=Map.of("usuarioId",a.id(),"biografia","Bio real de teste","localizacao","São Paulo/SP");
        mvc.perform(put("/api/perfis-artistas/"+a.id()).header("Authorization",b.bearer())
            .contentType(MediaType.APPLICATION_JSON).content(json(update))).andExpect(status().isForbidden());
        mvc.perform(put("/api/perfis-artistas/"+a.id()).header("Authorization",a.bearer())
            .contentType(MediaType.APPLICATION_JSON).content(json(update))).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/me").header("Authorization",a.bearer())).andExpect(jsonPath("$.perfilCompleto").value(false));
        JsonNode publicProfile=body(mvc.perform(get("/api/perfis/publicos/ARTISTA/"+a.id())).andExpect(status().isOk()).andReturn());
        assertThat(publicProfile.get("biografia").asText()).isEqualTo("Bio real de teste");
        for(String key:List.of("cpf","cnpj","telefone","email","enderecoCompleto","dataNascimento","responsavelLegal","senha"))
            assertThat(publicProfile.has(key)).as(key).isFalse();
        var minor=usuarios.findById(b.id()).orElseThrow();minor.setDataNascimento(LocalDate.now().minusYears(16));usuarios.saveAndFlush(minor);
        mvc.perform(get("/api/perfis/publicos/ARTISTA/"+b.id())).andExpect(status().isNotFound());
    }
    @Test void salvarDuplicidadeContadorPaginacaoERemocaoPropria() throws Exception {
        Conta artist=conta("ARTISTA",false),owner=conta("CONTRATANTE",false),other=conta("CONTRATANTE",false);
        String payload=json(Map.of("tipoAlvo","PERFIL_ARTISTA","alvoId",artist.id()));
        mvc.perform(post("/api/salvos").header("Authorization",owner.bearer()).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.quantidadeSalvos").value(1));
        mvc.perform(post("/api/salvos").header("Authorization",owner.bearer()).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeSalvos").value(1));
        mvc.perform(get("/api/salvos?size=1").header("Authorization",owner.bearer())).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].alvoId").value(artist.id()));
        mvc.perform(delete("/api/salvos/PERFIL_ARTISTA/"+artist.id()).header("Authorization",other.bearer())).andExpect(status().isNoContent());
        mvc.perform(get("/api/salvos/estado?tipoAlvo=PERFIL_ARTISTA&alvoId="+artist.id()).header("Authorization",owner.bearer()))
            .andExpect(jsonPath("$.salvo").value(true));
        mvc.perform(delete("/api/salvos/PERFIL_ARTISTA/"+artist.id()).header("Authorization",owner.bearer())).andExpect(status().isNoContent());
    }
    @Test void vagaCandidaturaCompletaSemDuplicidadeERetiradaLogica() throws Exception {
        Conta owner=conta("CONTRATANTE",false),artist=conta("ARTISTA",false),other=conta("ARTISTA",false);
        long id=vaga(owner);
        Map<String,Object> candidatura=Map.of("vagaId",id,"mensagemApresentacao","Apresentação","linkPortfolioCandidatura","https://example.com/portfolio");
        mvc.perform(post("/api/candidaturas").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(candidatura))).andExpect(status().isUnprocessableEntity());
        // Exercita a condição persistida requerida pelo RF, somente na fixture revertida deste teste.
        var user=usuarios.findById(artist.id()).orElseThrow();user.setPerfilCompleto(true);usuarios.saveAndFlush(user);
        long cid=body(mvc.perform(post("/api/candidaturas").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(candidatura))).andExpect(status().isCreated()).andExpect(jsonPath("$.artistaId").value(artist.id())).andReturn()).get("id").asLong();
        mvc.perform(post("/api/candidaturas").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(candidatura))).andExpect(status().isConflict());
        mvc.perform(delete("/api/candidaturas/"+cid).header("Authorization",other.bearer())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/candidaturas/"+cid).header("Authorization",artist.bearer())).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select status::text from candidaturas where id=?",String.class,cid)).isEqualTo("retirada");
        mvc.perform(get("/api/vagas/"+id+"/candidaturas").header("Authorization",owner.bearer())).andExpect(status().isOk());
        mvc.perform(patch("/api/vagas/"+id+"/status").header("Authorization",owner.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("acao","ENCERRAR")))).andExpect(status().isOk());
        mvc.perform(patch("/api/vagas/"+id+"/status").header("Authorization",owner.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("acao","REABRIR")))).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/vagas/"+id)).andExpect(status().isNotFound());
    }
    @Test void agendaIntervalosAdjacentesConflitoEProtecaoDeTitular() throws Exception {
        Conta artist=conta("ARTISTA",false),other=conta("ARTISTA",false),contractor=conta("CONTRATANTE",false);
        Map<String,Object> event=new HashMap<>(Map.of("titulo","Agenda transacional","tipo","Compromisso","localizacao","São Paulo",
            "inicio","2090-01-01T10:00:00","fim","2090-01-01T11:00:00"));
        long id=body(mvc.perform(post("/api/agenda").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(event))).andExpect(status().isCreated()).andReturn()).get("id").asLong();
        mvc.perform(post("/api/agenda").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(event))).andExpect(status().isConflict());
        event.put("inicio","2090-01-01T11:00:00");event.put("fim","2090-01-01T12:00:00");
        mvc.perform(post("/api/agenda").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(event))).andExpect(status().isCreated());
        mvc.perform(delete("/api/agenda/"+id).header("Authorization",other.bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/agenda").header("Authorization",contractor.bearer())).andExpect(status().isForbidden());
        event.put("fim","2090-01-01T11:00:00");
        mvc.perform(put("/api/agenda/"+id).header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(event))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/agenda/"+id).header("Authorization",artist.bearer())).andExpect(status().isNoContent());
    }
    @Test void chatPersisteOriginalERecusaNaoParticipante() throws Exception {
        Conta artist=conta("ARTISTA",false),owner=conta("CONTRATANTE",false),other=conta("ARTISTA",false);
        long room=body(mvc.perform(post("/api/chat/salas").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("usuarioDestinoId",owner.id())))).andExpect(status().isCreated()).andReturn()).get("salaId").asLong();
        long mid=body(mvc.perform(post("/api/chat/salas/"+room+"/mensagens").header("Authorization",artist.bearer())
            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("texto","Mensagem original de teste"))))
            .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        mvc.perform(get("/api/chat/salas/"+room+"/mensagens").header("Authorization",other.bearer())).andExpect(status().isNotFound());
        mvc.perform(patch("/api/chat/mensagens/"+mid).header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("texto","Alteração")))).andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("select texto_mensagem from mensagens_chat where id=?",String.class,mid)).isEqualTo("Mensagem original de teste");
        mvc.perform(get("/api/chat/salas?size=1").header("Authorization",owner.bearer())).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].participanteNome").value("Teste transacional PALCO"));
    }
    @Test void rascunhoAusenteNuncaEhConvertidoEmPublicacao() throws Exception {
        Conta owner=conta("CONTRATANTE",false);
        long before=jdbc.queryForObject("select count(*) from vagas",Long.class);
        mvc.perform(post("/api/vagas").header("Authorization",owner.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("titulo","Rascunho","descricao","Descrição","requisitos","Requisitos",
                "valorMinimo",100,"formaPagamento","PIX","cidade","São Paulo","estado","SP",
                "modeloTrabalho","PRESENCIAL","tipoContrato","Projeto","status","RASCUNHO"))))
            .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("select count(*) from vagas",Long.class)).isEqualTo(before);
    }
    @Test void responsavelDeMenorPersisteNasColunasExistentes() throws Exception {
        String email="menor-"+UUID.randomUUID()+"@palco.test";
        Map<String,Object> payload=new HashMap<>(Map.of("nome","Menor transacional","email",email,"senha","Palco@2026!",
            "dataNascimento",LocalDate.now().minusYears(16).toString(),"telefone","11999999999","tipoUsuario","ARTISTA",
            "nomeResponsavel","Responsável transacional","telefoneResponsavel","11888888888","emailResponsavel","responsavel@palco.test"));
        long id=body(mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(payload)))
            .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        assertThat(jdbc.queryForObject("select nome_responsavel from usuarios where id=?",String.class,id)).isEqualTo("Responsável transacional");
        assertThat(jdbc.queryForObject("select email_responsavel from usuarios where id=?",String.class,id)).isEqualTo("responsavel@palco.test");
        mvc.perform(get("/api/perfis/publicos/ARTISTA/"+id)).andExpect(status().isNotFound());
    }
    @Test void portfolioArquivoRealLinksSpotifyEPropriedade() throws Exception {
        Conta artist=conta("ARTISTA",false),other=conta("ARTISTA",false);
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        var file=new org.springframework.mock.web.MockMultipartFile("arquivo","teste.png","image/png",bytes.toByteArray());
        long id=body(mvc.perform(multipart("/api/portfolio/arquivos").file(file).header("Authorization",artist.bearer()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("IMAGEM")).andReturn()).get("id").asLong();
        mvc.perform(get("/api/portfolio/arquivos/"+id+"/conteudo").header("Authorization",artist.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/portfolio/arquivos/"+id+"/conteudo").header("Authorization",other.bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/portfolio/publico/arquivos/"+id+"/conteudo")).andExpect(status().isOk());
        mvc.perform(multipart("/api/portfolio/arquivos").file(new org.springframework.mock.web.MockMultipartFile("arquivo","bloqueado.docx","application/octet-stream",new byte[]{1}))
            .header("Authorization",artist.bearer())).andExpect(status().isUnprocessableEntity());
        long link=body(mvc.perform(post("/api/portfolio/videos").header("Authorization",artist.bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("url","https://open.spotify.com/track/0123456789abcdefghijAB?si=tracking"))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.provedor").value("SPOTIFY")).andReturn()).get("id").asLong();
        assertThat(jdbc.queryForObject("select tipo_midia::text from embeds_externos where id=?",String.class,link)).isEqualTo("audio");
        mvc.perform(get("/api/portfolio/me/videos?size=1").header("Authorization",artist.bearer())).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].urlOriginal").value("https://open.spotify.com/track/0123456789abcdefghijAB"));
        mvc.perform(delete("/api/portfolio/arquivos/"+id).header("Authorization",other.bearer())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/portfolio/arquivos/"+id).header("Authorization",artist.bearer())).andExpect(status().isNoContent());
    }
    @Test void comunidadePrivadaNaoPermiteDescobertaPorTerceiro() throws Exception {
        Conta owner=conta("ARTISTA",false),other=conta("ARTISTA",false);
        Long id=jdbc.queryForObject("insert into comunidades(criador_id,nome,descricao,categoria_artistica,privacidade) values (?,?,?,?, 'privada') returning id",
            Long.class,owner.id(),"Comunidade transacional","Descrição","Música");
        mvc.perform(get("/api/comunidades/"+id).header("Authorization",owner.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/comunidades/"+id).header("Authorization",other.bearer())).andExpect(status().isNotFound());
    }
    @Test void catalogosAusentesSaoBloqueadosERestantePermaneceDisponivel() throws Exception {
        Conta contractor=conta("CONTRATANTE",false);
        mvc.perform(get("/api/capacidades")).andExpect(status().isOk()).andExpect(jsonPath("$.taxonomia").value(false));
        mvc.perform(get("/api/talentos").header("Authorization",contractor.bearer())).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/areas")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/vagas?size=1")).andExpect(status().isOk());
        mvc.perform(get("/api/comunidades?size=1").header("Authorization",contractor.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard").header("Authorization",contractor.bearer())).andExpect(status().isOk());
        mvc.perform(delete("/api/usuarios/me").header("Authorization",contractor.bearer())).andExpect(status().isForbidden());
    }
}
