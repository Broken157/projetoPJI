package com.portifolio.support;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** PostgreSQL descartavel inicializado pelo database05 completo, sem SQL historico. */
public class OfficialPostgreSQLContainer extends PostgreSQLContainer<OfficialPostgreSQLContainer> {
    public static final String SOURCE_ZIP_SHA256 =
            "6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf";
    public static final String WORKING_TREE_SHA256 =
            "c68460169fcd2538fefd34a109ee3ea7640e553ce1882dd225d88d655229c134";
    private static final String TEMP_DATABASE_PREFIX = "palco_test_";
    private static final String CONTAINER_SQL_ROOT = "/opt/palco/database05";
    private static final long INIT_TIMEOUT_SECONDS = 120;
    private final String localAdminUrl;
    private final String localDatabaseName;
    private boolean localStarted;

    public OfficialPostgreSQLContainer() {
        super("postgres:18-alpine");
        localAdminUrl = configuredValue("palco.test.local-postgres-url", "PALCO_TEST_LOCAL_POSTGRES_URL");
        localDatabaseName = localAdminUrl == null ? null
                : TEMP_DATABASE_PREFIX + UUID.randomUUID().toString().replace("-", "");
        withDatabaseName("palco_test_manu05");
        withUrlParam("stringtype", "unspecified");
    }

    @Override
    public void start() {
        if (localAdminUrl == null) {
            super.start();
            return;
        }
        if (localStarted) return;
        validateLocalAdminUrl();
        assertTemporaryDatabaseName(localDatabaseName);
        try (Connection connection = DriverManager.getConnection(
                        localAdminUrl, localUsername(), localPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("create database " + quoteIdentifier(localDatabaseName)
                    + " template template0 encoding 'UTF8'");
            localStarted = true;
            initializeDatabase05();
        } catch (Exception error) {
            try {
                dropLocalDatabase();
            } catch (RuntimeException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw new IllegalStateException("Falha ao iniciar database05 no PostgreSQL local descartavel", error);
        }
    }

    @Override public void stop() {
        if (localAdminUrl == null) super.stop();
        else dropLocalDatabase();
    }

    @Override public boolean isRunning() {
        return localAdminUrl == null ? super.isRunning() : localStarted;
    }

    @Override public String getDockerImageName() {
        return localAdminUrl == null ? super.getDockerImageName() : "postgres:18-alpine";
    }

    @Override public String getJdbcUrl() {
        if (localAdminUrl == null) return super.getJdbcUrl();
        int queryStart = localAdminUrl.indexOf('?');
        String baseUrl = queryStart < 0 ? localAdminUrl : localAdminUrl.substring(0, queryStart);
        return baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1)
                + localDatabaseName + "?stringtype=unspecified";
    }

    @Override public String getUsername() {
        return localAdminUrl == null ? super.getUsername() : localUsername();
    }

    @Override public String getPassword() {
        return localAdminUrl == null ? super.getPassword() : localPassword();
    }

    @Override public String getDatabaseName() {
        return localAdminUrl == null ? super.getDatabaseName() : localDatabaseName;
    }

    /** Nenhum fallback para database/, dump ou migration de outro snapshot. */
    public static Path database05Path() {
        if (configuredValue("palco.test.database04-path", "PALCO_TEST_DATABASE04_PATH") != null)
            throw new IllegalStateException("Configuracao legada database04 recusada; use PALCO_TEST_DATABASE05_PATH ou -Dpalco.test.database05-path.");
        String configured = configuredValue("palco.test.database05-path", "PALCO_TEST_DATABASE05_PATH");
        if (configured != null) return requireSnapshotDirectory(Path.of(configured));
        for (Path root : workingDirectoryAncestors()) {
            Path candidate = root.resolve("database05/palco-database");
            if (Files.isDirectory(candidate)) return requireSnapshotDirectory(candidate);
        }
        throw new IllegalStateException("Pacote database05 completo nao encontrado. Informe "
                + "PALCO_TEST_DATABASE05_PATH ou -Dpalco.test.database05-path.");
    }

    /** Fingerprint de nomes e bytes dos 46 arquivos; sem alteracao de nenhum arquivo oficial. */
    public static String workingTreeSha256(Path root) throws Exception {
        List<Path> files;
        try (var paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .sorted(java.util.Comparator.comparing(
                            path -> root.relativize(path).toString().replace('\\', '/')))
                    .toList();
        }
        if (files.size() != 46)
            throw new IllegalStateException("database05 deve conter 46 arquivos; encontrados " + files.size());
        StringBuilder manifest = new StringBuilder();
        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            String hash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
            manifest.append(relative).append('\t').append(hash).append('\n');
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(manifest.toString().getBytes(StandardCharsets.UTF_8)));
    }

    public static void validateSnapshot(Path root) throws Exception {
        String actual = workingTreeSha256(root);
        if (!WORKING_TREE_SHA256.equals(actual))
            throw new IllegalStateException("Integridade do database05 diverge: " + actual);
    }

    @Override protected void runInitScriptIfRequired() {
        try {
            initializeDatabase05();
        } catch (Exception error) {
            throw new IllegalStateException("init.sql completo do database05 falhou; nenhum patch SQL aplicado", error);
        }
    }

    private void initializeDatabase05() throws Exception {
        Path root = database05Path();
        validateSnapshot(root);
        Path logDirectory = Path.of(System.getProperty(
                "palco.test.init-log-directory", "target-maven/database05-init"));
        Files.createDirectories(logDirectory);
        Path log = logDirectory.resolve(getDatabaseName() + "_" + UUID.randomUUID() + ".log");
        if (localAdminUrl == null) initializeInsideContainer(root, log);
        else initializeWithLocalPsql(root, log);

        // init.sql ja inclui seed.sql oficial. Nenhum seed/patch historico e executado.
        System.out.println("[database05] init.sql e seed.sql oficiais OK; log: " + log.toAbsolutePath());
    }

    private void initializeWithLocalPsql(Path root, Path log) throws Exception {
        URI uri = postgresUri(getJdbcUrl());
        List<String> command = new ArrayList<>(List.of(psqlExecutable(), "-X", "--no-password",
                "-a", "-v", "ON_ERROR_STOP=1", "-v", "VERBOSITY=verbose",
                "--host=" + uri.getHost(), "--port=" + (uri.getPort() < 0 ? 5432 : uri.getPort()),
                "--username=" + getUsername(), "--dbname=" + getDatabaseName(), "-f", "init.sql"));
        ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("PGPASSWORD", getPassword());
        builder.environment().put("PGCLIENTENCODING", "UTF8");
        Process process = builder.start();
        if (!process.waitFor(INIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("psql excedeu " + INIT_TIMEOUT_SECONDS + " segundos; log: " + log);
        }
        if (process.exitValue() != 0)
            throw new IllegalStateException("init.sql terminou com codigo " + process.exitValue()
                    + "; log: " + log.toAbsolutePath() + "\n" + Files.readString(log));
    }

    private void initializeInsideContainer(Path root, Path log) throws Exception {
        copyFileToContainer(MountableFile.forHostPath(root.toString()), CONTAINER_SQL_ROOT);
        var result = execInContainer("sh", "-c", "cd \"$1\" && shift && exec psql \"$@\"",
                "--", CONTAINER_SQL_ROOT, "-X", "--no-password", "-a",
                "-v", "ON_ERROR_STOP=1", "-v", "VERBOSITY=verbose",
                "--username=" + getUsername(), "--dbname=" + getDatabaseName(), "-f", "init.sql");
        String output = result.getStdout() + System.lineSeparator() + result.getStderr();
        Files.writeString(log, output, StandardCharsets.UTF_8);
        if (result.getExitCode() != 0)
            throw new IllegalStateException("init.sql no Testcontainer terminou com codigo "
                    + result.getExitCode() + "; log: " + log.toAbsolutePath() + "\n" + output);
    }

    private static String psqlExecutable() {
        String configured = configuredValue("palco.test.psql-path", "PALCO_TEST_PSQL_PATH");
        if (configured != null) return requireReadableFile(Path.of(configured), "psql").toString();
        String executable = isWindows() ? "psql.exe" : "psql";
        String pathValue = System.getenv("PATH");
        if (pathValue != null) {
            for (String directory : pathValue.split(java.io.File.pathSeparator)) {
                if (directory.isBlank()) continue;
                Path candidate = Path.of(directory).resolve(executable);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                    return candidate.toAbsolutePath().normalize().toString();
            }
        }
        if (isWindows()) {
            Path root = Path.of(System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files"), "PostgreSQL");
            if (Files.isDirectory(root)) {
                try (var versions = Files.list(root)) {
                    var candidates = versions.map(version -> version.resolve("bin/psql.exe"))
                            .filter(Files::isRegularFile).sorted(java.util.Comparator.reverseOrder()).toList();
                    if (!candidates.isEmpty()) return candidates.getFirst().toString();
                } catch (java.io.IOException error) {
                    throw new IllegalStateException("Falha ao procurar psql em " + root, error);
                }
            }
        }
        throw new IllegalStateException("psql nao encontrado. Informe PALCO_TEST_PSQL_PATH.");
    }

    private void validateLocalAdminUrl() {
        URI uri = postgresUri(localAdminUrl);
        if (!databaseName(uri).equals("postgres")
                || !List.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost()))
            throw new IllegalArgumentException("Testes locais exigem banco administrativo postgres em loopback.");
    }

    private String localUsername() {
        String configured = configuredValue("palco.test.local-postgres-user", "PALCO_TEST_LOCAL_POSTGRES_USER");
        return configured == null ? "postgres" : configured;
    }

    private String localPassword() {
        String configured = configuredValue("palco.test.local-postgres-password", "PALCO_TEST_LOCAL_POSTGRES_PASSWORD");
        return configured == null ? "" : configured;
    }

    private void dropLocalDatabase() {
        if (!localStarted) return;
        assertTemporaryDatabaseName(localDatabaseName);
        try (Connection connection = DriverManager.getConnection(localAdminUrl, localUsername(), localPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("drop database if exists " + quoteIdentifier(localDatabaseName) + " with (force)");
            localStarted = false;
        } catch (Exception error) {
            throw new IllegalStateException("Falha ao remover PostgreSQL local descartavel " + localDatabaseName, error);
        }
    }

    private static List<Path> workingDirectoryAncestors() {
        List<Path> roots = new ArrayList<>();
        Path current = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; current != null && depth < 8; depth++, current = current.getParent()) roots.add(current);
        return roots;
    }

    private static Path requireSnapshotDirectory(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized) || !Files.isRegularFile(normalized.resolve("init.sql")))
            throw new IllegalStateException("Pacote database05 ilegivel: " + normalized);
        return normalized;
    }

    private static Path requireReadableFile(Path path, String description) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || !Files.isReadable(normalized))
            throw new IllegalStateException(description + " nao encontrado ou ilegivel: " + normalized);
        return normalized;
    }

    private static String configuredValue(String property, String environment) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static URI postgresUri(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://"))
            throw new IllegalArgumentException("URL PostgreSQL invalida: " + jdbcUrl);
        return URI.create(jdbcUrl.substring("jdbc:".length()));
    }

    private static String databaseName(URI uri) {
        String path = uri.getPath();
        if (path == null || path.length() < 2) throw new IllegalArgumentException("URL sem banco: " + uri);
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static void assertTemporaryDatabaseName(String name) {
        if (name == null || !name.startsWith(TEMP_DATABASE_PREFIX)
                || !name.substring(TEMP_DATABASE_PREFIX.length()).matches("[0-9a-f]{32}"))
            throw new IllegalStateException("Operacao recusada fora de banco temporario palco_test_<UUID>: " + name);
    }

    private static String quoteIdentifier(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
