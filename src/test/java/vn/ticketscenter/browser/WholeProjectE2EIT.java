package vn.ticketscenter.browser;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import vn.ticketscenter.controller.common.ViewSupport;
import vn.ticketscenter.dto.common.ApiError;

/** M0 browser/packaging smoke only. Business E2E is added as M1-M3 contracts arrive. */
class WholeProjectE2EIT {
    @TempDir Path directory;

    @Test
    @Timeout(120)
    void deployPackagedWarAndCheckBrowserFoundation() throws Exception {
        assertEquals("test", System.getenv("TC_APP_ENV"), "M0 smoke requires test configuration");
        assertEquals(
                "127.0.0.1",
                System.getenv("TC_SQL_HOST"),
                "Use the isolated unavailable SQL endpoint");
        assertEquals(
                "1",
                System.getenv("TC_SQL_PORT"),
                "M0 browser smoke must not access an existing database");
        assertTrue(
                System.getenv().getOrDefault("TC_DATABASE", "").endsWith("_test"),
                "Test-only database name required");
        Path war =
                Path.of(System.getProperty("tc.war.path", "target/ticketscenter.war"))
                        .toAbsolutePath();
        assertTrue(Files.isRegularFile(war), "Package the WAR before browser-it");
        java.net.URLConnection.setDefaultUseCaches("jar", false);
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(directory.toString());
        Files.createDirectories(directory.resolve("webapps"));
        tomcat.setPort(0);
        tomcat.getConnector();
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        var context = tomcat.addWebapp("/ticketscenter", war.toString());
        ((StandardJarScanner) context.getJarScanner()).setScanClassPath(false);
        // Fixture route is registered in this test container only, never shipped in the WAR.
        Tomcat.addServlet(
                context,
                "vuong-m0-layout",
                new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                            throws java.io.IOException, jakarta.servlet.ServletException {
                        request.setAttribute("pageTitle", "M0 browser fixture");
                        request.setAttribute(
                                "error",
                                new ApiError(
                                        "M0_TEST",
                                        "<script>window.unescaped=true</script>",
                                        "m0-fixture"));
                        ViewSupport.forward(
                                request, response, "/WEB-INF/views/common/error.jsp", null);
                    }
                });
        context.addServletMappingDecoded("/test-m0-layout", "vuong-m0-layout");
        try {
            tomcat.start();
            assertTrue(context.getState().isAvailable(), "Packaged WAR startup failed");
            assertTrue(
                    tomcat.getConnector().getState().isAvailable(),
                    "Test HTTP connector startup failed");
            BrowserTestSupport.run(
                    "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/ticketscenter");
        } finally {
            try {
                tomcat.stop();
            } finally {
                tomcat.destroy();
            }
        }
        assertFalse(
                Thread.getAllStackTraces().keySet().stream()
                        .anyMatch(
                                thread ->
                                        thread.isAlive()
                                                && thread.getName().startsWith("ticketscenter")),
                "Persistence pool thread survived undeploy");
    }
}
