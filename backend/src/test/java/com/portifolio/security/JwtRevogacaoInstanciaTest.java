package com.portifolio.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.portifolio.model.Usuario;
import com.portifolio.model.enums.TipoUsuario;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Prova os limites D15, sem pretender que uma blacklist local seja distribuída. */
class JwtRevogacaoInstanciaTest {
    @AfterEach void limparTransacaoSimulada() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test void logoutRevogaPorJtiSemGuardarJWTRaw() {
        JwtService jwt = instancia(); String token = jwt.gerarToken(usuario(1L));
        assertThat(jwt.tokenValido(token)).isTrue(); jwt.revogar(token);
        assertThat(jwt.tokenValido(token)).isFalse();
        Map<?, ?> blacklist = (Map<?, ?>) ReflectionTestUtils.getField(jwt, "revogados");
        assertThat(blacklist).hasSize(1);
        assertThat(blacklist.containsKey(token)).isFalse();
        assertThat(blacklist.containsKey(jwt.extrairClaims(token).getId())).isTrue();
    }

    @Test void todasRevogaSomenteEmitidosDoTitularENovoLoginContinuaValido() {
        JwtService jwt = instancia(); String a = jwt.gerarToken(usuario(1L)), b = jwt.gerarToken(usuario(2L));
        jwt.revogarTodosDoUsuario(1L);
        assertThat(jwt.tokenValido(a)).isFalse(); assertThat(jwt.tokenValido(b)).isTrue();
        assertThat(jwt.tokenValido(jwt.gerarToken(usuario(1L)))).isTrue();
    }

    @Test void outraInstanciaOuRestartNaoConheceRevogacaoAccessOnlyD15() {
        JwtService original = instancia(), nova = instancia(); String token = original.gerarToken(usuario(1L));
        original.revogarTodosDoUsuario(1L);
        assertThat(original.tokenValido(token)).isFalse();
        assertThat(nova.tokenValido(token)).as("limitação D15 demonstrada").isTrue();
    }

    @Test void revogacaoGlobalAguardaCommit() {
        JwtService jwt = instancia(); String token = jwt.gerarToken(usuario(1L)); iniciarTransacaoSimulada();
        jwt.revogarTodosDoUsuario(1L);
        assertThat(jwt.tokenValido(token)).isTrue();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(jwt.tokenValido(token)).isFalse();
    }

    @Test void rollbackNaoAplicaBlacklist() {
        JwtService jwt = instancia(); String token = jwt.gerarToken(usuario(1L)); iniciarTransacaoSimulada();
        jwt.revogarTodosDoUsuario(1L);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(jwt.tokenValido(token)).isTrue();
    }

    private void iniciarTransacaoSimulada() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
    private JwtService instancia() {
        JwtService jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "secret", "palco-test-only-hmac-key-2026-09-27-never-use-in-production-0000000000");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L); return jwt;
    }
    private Usuario usuario(Long id) {
        Usuario u = new Usuario(); u.setId(id); u.setNome("Fixture JWT");
        u.setEmail("fixture" + id + "@jwt.test"); u.setTipoUsuario(TipoUsuario.ARTISTA); return u;
    }
}
