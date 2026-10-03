package com.portifolio;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Executa contratos SQL do database04 completo; nao aplica correcoes de snapshots anteriores. */
@Testcontainers
class ValidatedOfficialSqlIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new com.portifolio.support.OfficialPostgreSQLContainer();

    @BeforeAll static void fixtures() throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement()) {
            // DML de isolamento desta classe, depois do init/seed oficial comprovado pelo bootstrap.
            s.execute("delete from usuarios");
            s.execute("select setval('vagas_id_seq',1,false)");
            s.execute("insert into usuarios(id,username,nome,data_nascimento,telefone,email,senha,tipo_usuario,status_conta) values (1,'sql_contratante','Contratante','1990-01-01','11999999999','c@sql.test','hash','CONTRATANTE','PENDENTE_VERIFICACAO_EMAIL'),(2,'sql_artista','Artista','1990-01-01','11999999999','a@sql.test','hash','ARTISTA','PENDENTE_VERIFICACAO_EMAIL')");
            s.execute("insert into perfis_contratantes(usuario_id,tipo_contratante) values(1,'PESSOA_FISICA')");
            s.execute("insert into perfis_artistas(usuario_id,tipo_perfil_artistico,raio_atuacao) values(2,'ARTISTA_SOLO','LOCAL')");
            s.execute("insert into vagas(contratante_id,area_id,titulo,descricao,requisitos,cidade,estado,tipo_contrato,abrangencia,status,data_publicacao) select 1,1,'Vaga '||n,'Descricao','Requisitos','Sao Paulo','SP','Projeto','LOCAL','ABERTA',timestamp '2026-09-14 12:00:00' from generate_series(1,56) n");
        }
    }

    @Test void usuarioPreservaEstadoPendenteExplicito() throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("select status_conta from usuarios where id=1")) {
            assertThat(r.next()).isTrue(); assertThat(r.getString(1)).isEqualTo("PENDENTE_VERIFICACAO_EMAIL");
        }
    }

    @Test void buscaDatabase04LimitaCemEPaginaSemRepetir() throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement()) {
            try (ResultSet r=s.executeQuery("select count(*),min(vaga_id),max(vaga_id) from fn_buscar_vagas(p_limit=>100)")) {
                r.next(); assertThat(r.getInt(1)).isEqualTo(56); assertThat(r.getLong(2)).isEqualTo(1); assertThat(r.getLong(3)).isEqualTo(56);
            }
            try (ResultSet r=s.executeQuery("select count(*),max(vaga_id) from fn_buscar_vagas(p_limit=>100,p_cursor_data_publicacao=>timestamp '2026-09-14 12:00:00',p_cursor_id=>7::bigint)")) {
                r.next(); assertThat(r.getInt(1)).isEqualTo(6); assertThat(r.getLong(2)).isEqualTo(6);
            }
        }
    }

    static Stream<Arguments> limites() {
        return Stream.of(new Object[]{"image/jpeg",5242880},new Object[]{"image/jpg",5242880},
                new Object[]{"image/png",5242880},new Object[]{"application/pdf",10485760},new Object[]{"audio/mpeg",20971520})
                .flatMap(v -> Stream.of(1,(int)v[1],0,-1,(int)v[1]+1)
                        .map(size -> Arguments.of(v[0],size)));
    }

    @ParameterizedTest @MethodSource("limites")
    void database04NaoImpoeChecksHistoricosDeTamanho(String mime,int tamanho) throws Exception {
        try (Connection c=connection(); PreparedStatement s=c.prepareStatement("insert into portfolio_arquivos(artista_id,url_arquivo,nome_original,tamanho_bytes,tipo_mime) values(2,'audit','audit',?,?)")) {
            s.setInt(1,tamanho); s.setString(2,mime);
            assertThat(s.executeUpdate()).isEqualTo(1);
        }
    }

    @Test void database04NaoImpoeCheckHistoricoDeMime() throws Exception {
        try (Connection c=connection(); Statement s=c.createStatement()) {
            assertThat(s.executeUpdate("insert into portfolio_arquivos(artista_id,url_arquivo,nome_original,tamanho_bytes,tipo_mime) values(2,'audit','audit',1,'video/mp4')"))
                    .isEqualTo(1);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
    }
}
