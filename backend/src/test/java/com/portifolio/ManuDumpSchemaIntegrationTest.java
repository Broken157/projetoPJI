package com.portifolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.portifolio.support.OfficialPostgreSQLContainer;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Nome historico mantido; a unica fonte ativa desta classe agora e o database04. */
@Testcontainers
@SpringBootTest
class ManuDumpSchemaIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new OfficialPostgreSQLContainer();
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;

    @Test void database04CompletoTemIntegridadeVerificada() throws Exception {
        assertThat(OfficialPostgreSQLContainer.database04Path().resolve("init.sql")).isRegularFile();
        OfficialPostgreSQLContainer.validateSnapshot(OfficialPostgreSQLContainer.database04Path());
        assertThat(OfficialPostgreSQLContainer.SOURCE_ZIP_SHA256)
                .isEqualTo("52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b");
    }

    @Test void entityManagerFactorySobeComValidateEQuarentaETresTabelas() throws Exception {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(entityManagerFactory.isOpen()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getURL()).isEqualTo(postgres.getJdbcUrl());
        }
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo(postgres.getDatabaseName());
        assertThat(jdbc.queryForObject("select count(*) from pg_tables where schemaname='public'", Integer.class))
                .isEqualTo(43);
    }

    @Test void perfisArtistasMantemEstruturaDoDatabase04() {
        assertThat(columns("perfis_artistas")).contains("usuario_id", "cidade", "estado", "url_portfolio",
                "tipo_perfil_artistico", "raio_atuacao", "banner_url", "ultima_atualizacao")
                .doesNotContain("localizacao");
    }

    @Test void contratantesEUsuariosMantemEstruturaDoDatabase04() {
        assertThat(columns("perfis_contratantes")).contains("usuario_id", "tipo_contratante", "cidade", "estado")
                .doesNotContain("localizacao", "cpf", "cnpj", "tipo_perfil");
        assertThat(columns("usuarios")).contains("username", "foto_perfil_url", "status_conta", "cpf", "cnpj");
    }

    @Test void initCompletoCarregaObjetosDoDatabase04() {
        assertThat(jdbc.queryForObject("select count(*) from pg_type t join pg_namespace n on n.oid=t.typnamespace "
                + "where n.nspname='public' and t.typtype='e'", Integer.class)).isEqualTo(24);
        assertThat(jdbc.queryForObject("select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace "
                + "where n.nspname='public' and p.prokind='f'", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace "
                + "where n.nspname='public' and p.prokind='p'", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("select count(*) from pg_trigger where not tgisinternal", Integer.class))
                .isEqualTo(3);
    }

    @Test void timestampDeVagaPertenceAoSnapshotSemMigrationAdicional() {
        var definition = jdbc.queryForMap("select data_type,column_default,is_nullable from information_schema.columns "
                + "where table_schema='public' and table_name='vagas' and column_name='ultima_atualizacao'");
        assertThat(definition).containsEntry("data_type", "timestamp without time zone").containsEntry("is_nullable", "YES");
        assertThat(definition.get("column_default").toString()).containsIgnoringCase("current_timestamp");
    }

    private List<String> columns(String table) {
        return jdbc.queryForList("select column_name from information_schema.columns "
                + "where table_schema='public' and table_name=? order by ordinal_position", String.class, table);
    }
}
