package vn.ticketscenter.browser;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Runs the pinned Node Playwright client; absence/error fails the browser profile. */
public final class BrowserTestSupport {
    private BrowserTestSupport() {}

    public static void run(String baseUrl) throws Exception {
        assertEquals("test", System.getenv("TC_APP_ENV"), "browser-it requires TC_APP_ENV=test");
        assertTrue(
                baseUrl.matches("http://127\\.0\\.0\\.1:[0-9]+/ticketscenter"),
                "Only the isolated test container may be used");
        Path output =
                Path.of(System.getProperty("tc.browser.output", "target/browser-evidence"))
                        .toAbsolutePath();
        Files.createDirectories(output);
        String node = System.getenv().getOrDefault("TC_BROWSER_NODE", "node");
        Path log = output.resolve("m0-browser.log");
        Process process =
                new ProcessBuilder(
                                node, "src/test/browser/m0-smoke.cjs", baseUrl, output.toString())
                        .redirectErrorStream(true)
                        .redirectOutput(log.toFile())
                        .start();
        try {
            assertTrue(process.waitFor(60, TimeUnit.SECONDS), "Browser smoke exceeded 60 seconds");
            String result = Files.readString(log, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), result);
            assertTrue(
                    result.contains("M0 BROWSER PASS"),
                    "Browser smoke produced no success assertion");
            System.out.print(result);
        } finally {
            if (process.isAlive()) {
                process.descendants().forEach(child -> child.destroyForcibly());
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }
}
