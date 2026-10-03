package com.portifolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portifolio.support.OfficialPostgreSQLContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Valida bootstrap e contratos reais do pacote, independentemente do contexto JPA. */
@Testcontainers
class Database04BootstrapIntegrationTest {
    @Container static OfficialPostgreSQLContainer postgres = new OfficialPostgreSQLContainer();

    @Test void pacoteCompletoOficialNaoFoiAlterado() throws Exception {
        assertThat(OfficialPostgreSQLContainer.workingTreeSha256(OfficialPostgreSQLContainer.database04Path()))
                .isEqualTo(OfficialPostgreSQLContainer.WORKING_TREE_SHA256);
    }

    @Test void initCompletoCriaObjetosDoSnapshot() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("select current_database(), "
                    + "(select count(*) from pg_tables where schemaname='public'), "
                    + "(select count(*) from pg_type t join pg_namespace n on n.oid=t.typnamespace "
                    + "where n.nspname='public' and t.typtype='e'), "
                    + "(select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace "
                    + "where n.nspname='public' and p.prokind='f'), "
                    + "(select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace "
                    + "where n.nspname='public' and p.prokind='p'), "
                    + "(select count(*) from pg_trigger where not tgisinternal)")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo(postgres.getDatabaseName());
                assertThat(result.getInt(2)).isEqualTo(43);
                assertThat(result.getInt(3)).isEqualTo(24);
                assertThat(result.getInt(4)).isEqualTo(12);
                assertThat(result.getInt(5)).isEqualTo(12);
                assertThat(result.getInt(6)).isEqualTo(3);
            }
        }
    }

    @Test void usuariosExigeUsernameSemDefaultOuTriggerProvisorio() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate("insert into usuarios "
                    + "(nome,data_nascimento,telefone,email,senha,tipo_usuario,status_conta) values "
                    + "('Bootstrap','1990-01-01','11999999999','bootstrap@database04.test',"
                    + "'hash','ARTISTA','PENDENTE_VERIFICACAO_EMAIL')"))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState()).isEqualTo("23502");
            try (var result = statement.executeQuery("select is_nullable,column_default "
                    + "from information_schema.columns where table_schema='public' "
                    + "and table_name='usuarios' and column_name='username'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("NO");
                assertThat(result.getString(2)).isNull();
            }
        }
    }

    @Test void contratanteUsaCidadeEEstadoSemColunaLegada() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement();
                var result = statement.executeQuery("select "
                        + "count(*) filter (where column_name in ('cidade','estado')), "
                        + "count(*) filter (where column_name='localizacao') "
                        + "from information_schema.columns where table_schema='public' "
                        + "and table_name='perfis_contratantes'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(2);
            assertThat(result.getInt(2)).isZero();
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Test void seedOficialCompletoCarregaCatalogoEContaReservada() throws Exception {
        try (var connection = connection(); var statement = connection.createStatement();
                var result = statement.executeQuery("select "
                        + "(select count(*) from areas_artisticas), "
                        + "(select nome from areas_artisticas where id=1), "
                        + "(select nome from areas_artisticas where id=2), "
                        + "(select count(*) from usuarios), "
                        + "(select username from usuarios where id=0), "
                        + "(select count(*) from funcoes), "
                        + "(select count(*) from especializacoes)")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(7);
            assertThat(result.getString(2)).isEqualTo("Artes Cênicas");
            assertThat(result.getString(3)).isEqualTo("Música");
            assertThat(result.getInt(4)).isEqualTo(7);
            assertThat(result.getString(5)).isEqualTo("usuario.removido");
            assertThat(result.getInt(6)).isEqualTo(4);
            assertThat(result.getInt(7)).isEqualTo(4);
        }
    }
}
