package com.portifolio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portifolio.model.Usuario;
import com.portifolio.model.enums.StatusConta;
import com.portifolio.repository.UsuarioRepository;
import com.portifolio.support.EmailVerificationTestConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers @SpringBootTest @AutoConfigureMockMvc @Import(EmailVerificationTestConfig.class)
class ConfirmacoesConcorrenciaRf26Rf27IntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer()
            .withUrlParam("stringtype", "unspecified");
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired EmailVerificationTestConfig.CapturingEmailVerificationSender emails;
    @Autowired EmailVerificationTestConfig.CapturingGuardianConsentSender convites;
    private final ObjectMapper json = new ObjectMapper();
    @AfterEach void limpar() { emails.limpar(); convites.limpar(); }

    @Test void duasConfirmacoesDoMesmoEmailConsomemTokenUmaVez() throws Exception {
        Usuario u = cadastrar(25); String token = emails.token(u.getEmail());
        var r = disputar("usuarios", u.getId(), () -> confirmar(token), () -> confirmar(token));
        assertThat(r.stream().map(x -> x.getResponse().getStatus())).containsExactlyInAnyOrder(200, 404);
        Usuario finalizado = usuarios.findById(u.getId()).orElseThrow();
        assertThat(finalizado.getStatusConta()).isEqualTo(StatusConta.ATIVA);
        assertThat(finalizado.getEmailVerificado()).isTrue(); assertThat(finalizado.getTokenVerificacao()).isNull();
    }

    @Test void reenvioConcorrenteComConfirmacaoNaoDeixaDoisTokensValidos() throws Exception {
        Usuario u = cadastrar(25); String antigo = emails.token(u.getEmail());
        u.setUltimoReenvioVerificacao(LocalDateTime.now().minusMinutes(2)); usuarios.saveAndFlush(u);
        var r = disputar("usuarios", u.getId(), () -> confirmar(antigo), () -> reenviarEmail(u.getEmail()));
        int confirmacao = r.getFirst().getResponse().getStatus();
        assertThat(confirmacao).isIn(200, 404); assertThat(r.get(1).getResponse().getStatus()).isEqualTo(200);
        Usuario atual = usuarios.findById(u.getId()).orElseThrow();
        if (confirmacao == 200) {
            assertThat(atual.getStatusConta()).isEqualTo(StatusConta.ATIVA); assertThat(atual.getTokenVerificacao()).isNull();
        } else {
            assertThat(atual.getStatusConta()).isEqualTo(StatusConta.PENDENTE_VERIFICACAO_EMAIL);
            assertThat(emails.token(u.getEmail())).isNotEqualTo(antigo);
            confirmar(emails.token(u.getEmail())).andExpect(status().isOk());
        }
        confirmar(antigo).andExpect(status().isNotFound());
    }

    @Test void duasAutorizacoesDisputamLockFisicoEProduzemUmConsentimento() throws Exception {
        Usuario u = pendente(); String token = convites.token(u.getEmailResponsavel());
        var r = disputar("responsaveis_legais", u.getResponsavelLegal().getId(),
                () -> decidir(token, "AUTORIZAR"), () -> decidir(token, "AUTORIZAR"));
        assertThat(r.stream().map(x -> x.getResponse().getStatus())).containsExactlyInAnyOrder(200, 404);
        Usuario atual = usuarios.findById(u.getId()).orElseThrow();
        assertThat(atual.getStatusConta()).isEqualTo(StatusConta.ATIVA);
        assertThat(atual.getResponsavelLegal().getDataConsentimento()).isNotNull();
        assertThat(atual.getResponsavelLegal().getTokenConsentimento()).isNull();
    }

    @Test void autorizarERecusarDisputamLockFisicoComDecisaoUnica() throws Exception {
        Usuario u = pendente(); String token = convites.token(u.getEmailResponsavel());
        var r = disputar("responsaveis_legais", u.getResponsavelLegal().getId(),
                () -> decidir(token, "AUTORIZAR"), () -> decidir(token, "RECUSAR"));
        assertThat(r.stream().map(x -> x.getResponse().getStatus())).containsExactlyInAnyOrder(200, 404);
        Usuario atual = usuarios.findById(u.getId()).orElseThrow();
        boolean autorizou = r.getFirst().getResponse().getStatus() == 200;
        assertThat(atual.getStatusConta()).isEqualTo(autorizou ? StatusConta.ATIVA : StatusConta.BLOQUEADA);
        assertThat(atual.getResponsavelLegal().getConsentimentoRevogado()).isEqualTo(!autorizou);
        assertThat(atual.getResponsavelLegal().getTokenConsentimento()).isNull();
    }

    @Test void reenvioDoResponsavelConcorrenteComDecisaoNaoReabreConsentimento() throws Exception {
        Usuario u = pendente(); String antigo = convites.token(u.getEmailResponsavel());
        var resp = u.getResponsavelLegal(); String[] registro = resp.getTokenConsentimento().split(":");
        jdbc.update("update responsaveis_legais set token_consentimento=? where id=?",
                "v1:" + registro[1] + ":" + Instant.now().minusSeconds(120).getEpochSecond() + ":0", resp.getId());
        var r = disputar("responsaveis_legais", resp.getId(), () -> decidir(antigo, "AUTORIZAR"),
                () -> reenviarResponsavel(u.getEmail()));
        int decisao = r.getFirst().getResponse().getStatus();
        assertThat(decisao).isIn(200, 404); assertThat(r.get(1).getResponse().getStatus()).isEqualTo(200);
        Usuario atual = usuarios.findById(u.getId()).orElseThrow();
        if (decisao == 200) {
            assertThat(atual.getStatusConta()).isEqualTo(StatusConta.ATIVA);
            assertThat(atual.getResponsavelLegal().getTokenConsentimento()).isNull();
        } else {
            assertThat(atual.getStatusConta()).isEqualTo(StatusConta.PENDENTE_CONSENTIMENTO);
            assertThat(convites.token(u.getEmailResponsavel())).isNotEqualTo(antigo);
            decidir(convites.token(u.getEmailResponsavel()), "AUTORIZAR").andExpect(status().isOk());
        }
        decidir(antigo, "AUTORIZAR").andExpect(status().isNotFound());
    }

    private List<MvcResult> disputar(String tabela, Long id, Callable<org.springframework.test.web.servlet.ResultActions> a,
            Callable<org.springframework.test.web.servlet.ResultActions> b) throws Exception {
        // Identificador é limitado a duas tabelas oficiais constantes; não vem de request/fixture externa.
        String lock = switch(tabela) { case "usuarios" -> "select id from usuarios where id=? for update";
            case "responsaveis_legais" -> "select id from responsaveis_legais where id=? for update";
            default -> throw new IllegalArgumentException("Tabela não permitida"); };
        CountDownLatch bloqueado = new CountDownLatch(1), liberar = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(3)) {
            var holder = pool.submit(() -> new TransactionTemplate(manager).execute(tx -> {
                jdbc.queryForObject(lock, Long.class, id); bloqueado.countDown();
                try { if (!liberar.await(20, TimeUnit.SECONDS)) throw new AssertionError("Barreira não liberada"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return true;
            }));
            assertThat(bloqueado.await(10, TimeUnit.SECONDS)).isTrue();
            var primeira = pool.submit(() -> a.call().andReturn()); var segunda = pool.submit(() -> b.call().andReturn());
            try {
                long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); int esperas;
                do {
                    esperas = jdbc.queryForObject("select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock'", Integer.class);
                    if (esperas < 2) Thread.sleep(25);
                } while (esperas < 2 && System.nanoTime() < limite);
                assertThat(esperas).as("duas transações realmente esperando lock PostgreSQL").isGreaterThanOrEqualTo(2);
            } finally { liberar.countDown(); }
            holder.get(10, TimeUnit.SECONDS);
            return List.of(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));
        } finally { liberar.countDown(); }
    }

    private Usuario cadastrar(int idade) throws Exception {
        var p = new HashMap<String,Object>(); com.portifolio.support.CadastroFixtures.identificar(p);
        String email = "corrida-" + UUID.randomUUID() + "@palco.test";
        p.put("nome", "Pessoa da concorrência"); p.put("email", email); p.put("senha", "Palco@2026");
        p.put("dataNascimento", LocalDate.now().minusYears(idade).toString()); p.put("telefone", "11999999999");
        p.put("tipoUsuario", "ARTISTA"); p.put("tipoPerfilArtistico", "ARTISTA_SOLO"); p.put("areaPrincipalId", 1);
        if (idade < 18) { p.put("nomeResponsavel", "Responsável"); p.put("telefoneResponsavel", "11988887777");
            p.put("emailResponsavel", "resp-" + UUID.randomUUID() + "@palco.test"); }
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andExpect(status().isCreated());
        return usuarios.findByEmail(email).orElseThrow();
    }
    private Usuario pendente() throws Exception {
        Usuario u = cadastrar(16); confirmar(emails.token(u.getEmail())).andExpect(status().isOk());
        return usuarios.findById(u.getId()).orElseThrow();
    }
    private org.springframework.test.web.servlet.ResultActions confirmar(String token) throws Exception { return requisitar("confirm-email", Map.of("token", token)); }
    private org.springframework.test.web.servlet.ResultActions reenviarEmail(String email) throws Exception { return requisitar("resend-confirmation", Map.of("email", email)); }
    private org.springframework.test.web.servlet.ResultActions decidir(String token, String decisao) throws Exception { return requisitar("guardian-decision", Map.of("token", token, "decisao", decisao)); }
    private org.springframework.test.web.servlet.ResultActions reenviarResponsavel(String email) throws Exception { return requisitar("resend-guardian-invite", Map.of("email", email)); }
    private org.springframework.test.web.servlet.ResultActions requisitar(String rota, Object payload) throws Exception {
        return mvc.perform(post("/api/auth/" + rota).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)));
    }
}
