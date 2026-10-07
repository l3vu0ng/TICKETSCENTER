package vn.ticketscenter.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executes real sqlcmd assertions; no substitute JDBC/domain implementation. */
public final class VuongSqlTestSupport {
    private VuongSqlTestSupport() { }

    public static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        assertTrue(value != null && !value.isBlank(), "BLOCKED: missing " + name);
        return value;
    }

    public static List<String> connectionArguments() {
        String server = requiredEnvironment("TC_SQL_HOST");
        String database = requiredEnvironment("TC_TEST_DATABASE");
        assertTrue(database.matches("TicketsCenter_Test_[A-Za-z0-9_]+"), "Dedicated test database required");
        List<String> command = new ArrayList<>(List.of("sqlcmd", "-S", server, "-d", database,
                "-E", "-b", "-I", "-f", "65001", "-l", "5", "-t", "30"));
        if ("true".equals(System.getenv("TC_TRUST_LOCAL_CERTIFICATE"))) {
            String localName = System.getenv("COMPUTERNAME");
            assertTrue("localhost".equalsIgnoreCase(server) || ".".equals(server)
                    || "127.0.0.1".equals(server) || server.equalsIgnoreCase(localName),
                    "Certificate override limited to local SQL Server");
            command.add("-C");
        }
        return command;
    }

    public static String run(List<String> command) throws IOException, InterruptedException {
        Path log = Files.createTempFile("ticketscenter-vuong-it-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "SQL test command timed out");
            String output = Files.readString(log, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(log);
        }
    }

    public static String sqlScript(String path) throws IOException, InterruptedException {
        List<String> command = connectionArguments();
        command.addAll(List.of("-i", path));
        return run(command);
    }
}
