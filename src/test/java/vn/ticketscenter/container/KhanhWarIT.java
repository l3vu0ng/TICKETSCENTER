package vn.ticketscenter.container;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipFile;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.controller.common.ViewSupport;
import vn.ticketscenter.dto.common.ApiError;
import vn.ticketscenter.dto.identity.UserDto;
import vn.ticketscenter.model.identity.PlatformRole;
import vn.ticketscenter.model.identity.UserStatus;
import vn.ticketscenter.support.HttpTestClient;

/** Deploys the packaged WAR twice; the test route exists only in this container. */
class KhanhWarIT {
  @TempDir Path directory;

  @Test
  void deployRedeployHealthCsrfAndEscapedJsp() throws Exception {
    // Standalone Tomcat does not install the server listener that disables JAR URL caching.
    java.net.URLConnection.setDefaultUseCaches("jar", false);
    Path war =
        Path.of(System.getProperty("tc.war.path", "target/ticketscenter.war")).toAbsolutePath();
    assertTrue(Files.isRegularFile(war), "Package the WAR before running http-it");
    try (var zip = new ZipFile(war.toFile())) {
      assertTrue(
          zip.getEntry("WEB-INF/classes/vn/ticketscenter/transaction/TransactionRunner.class")
              != null);
      assertFalse(
          zip.stream()
              .anyMatch(
                  entry ->
                      entry.getName().contains(".env")
                          || entry.getName().contains("KhanhWarIT")
                          || entry
                              .getName()
                              .matches("WEB-INF/lib/(jakarta.servlet-api|tomcat-embed-).*")));
    }
    for (int deploy = 0; deploy < 2; deploy++) {
      Tomcat tomcat = new Tomcat();
      Path baseDirectory = directory.resolve("deploy-" + deploy);
      Files.createDirectories(baseDirectory.resolve("webapps"));
      tomcat.setBaseDir(baseDirectory.toString());
      tomcat.setPort(0);
      tomcat.getConnector();
      var context = tomcat.addWebapp("/ticketscenter", war.toString());
      ((StandardJarScanner) context.getJarScanner()).setScanClassPath(false);
      Tomcat.addServlet(
          context,
          "layout-test",
          new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws java.io.IOException, jakarta.servlet.ServletException {
              request.setAttribute("pageTitle", "Layout test");
              request.setAttribute(
                  "error", new ApiError("TEST", "<script>alert(1)</script>", "test-correlation"));
              if ("true".equals(request.getParameter("signedIn"))) {
                request.setAttribute(
                    "currentUser",
                    new UserDto(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "buyer.a",
                        "Khách kiểm thử <script>alert(1)</script> " + "TênDài".repeat(20),
                        "buyer.a@example.test",
                        null,
                        UserStatus.ACTIVE,
                        true,
                        PlatformRole.CUSTOMER));
              }
              ViewSupport.forward(request, response, "/WEB-INF/views/common/error.jsp", null);
            }
          });
      context.addServletMappingDecoded("/test-layout", "layout-test");
      try {
        tomcat.start();
        assertTrue(context.getState().isAvailable(), "WAR startup failed");
        String base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/ticketscenter";
        var http = new HttpTestClient(base);
        var live = http.get("/health/live");
        assertEquals(200, live.statusCode());
        assertEquals("{\"data\":{\"status\":\"UP\"}}", live.body());
        var ready = http.get("/health/ready");
        assertEquals(503, ready.statusCode(), "http-it uses an unavailable test SQL endpoint");
        assertEquals("{\"data\":{\"status\":\"DOWN\"}}", ready.body());
        assertEquals(200, http.get("/health/live").statusCode());
        var tokenResponse = http.get("/auth/csrf");
        assertEquals(200, tokenResponse.statusCode());
        String token =
            HttpResponses.jsonMapper()
                .readTree(tokenResponse.body())
                .path("data")
                .path("csrfToken")
                .asText();
        assertEquals(43, token.length());
        assertEquals(403, http.post("/auth/login", "{}", null).statusCode());
        assertEquals(403, new HttpTestClient(base).post("/auth/login", "{}", token).statusCode());
        assertEquals(400, http.post("/auth/login", "{", token).statusCode());
        assertEquals(413, http.post("/auth/login", "x".repeat(65537), token).statusCode());
        assertEquals(501, http.post("/auth/login", "{}", token).statusCode());
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var layout =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/test-layout"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, layout.statusCode(), layout.body());
        assertTrue(layout.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(layout.body().contains("<script>alert(1)</script>"));
        assertTrue(layout.body().contains("/ticketscenter/assets/css/app.css"));
        assertTrue(layout.body().contains("main-content"));
        assertTrue(layout.body().contains("Sự kiện"));
        assertTrue(layout.body().contains("TicketsCenter · Sự kiện và vé của bạn"));
        var missing = http.get("/auth/unknown");
        assertEquals(404, missing.statusCode());
        assertEquals(
            missing.headers().firstValue("X-Correlation-Id").orElseThrow(),
            HttpResponses.jsonMapper()
                .readTree(missing.body())
                .path("error")
                .path("correlationId")
                .asText());
        assertEquals(200, http.get("/webjars/bootstrap/5.3.0/css/bootstrap.min.css").statusCode());
        if (deploy == 0 && System.getenv("TC_BROWSER_NODE") != null) {
          var browserCheck =
              new ProcessBuilder(
                      System.getenv("TC_BROWSER_NODE"),
                      "src/test/js/shared-layout.browser.cjs",
                      base)
                  .redirectErrorStream(true)
                  .start();
          if (!browserCheck.waitFor(45, java.util.concurrent.TimeUnit.SECONDS)) {
            browserCheck.destroyForcibly();
            fail("Layout browser check timed out");
          }
          String browserOutput =
              new String(
                  browserCheck.getInputStream().readAllBytes(),
                  java.nio.charset.StandardCharsets.UTF_8);
          assertEquals(0, browserCheck.exitValue(), browserOutput);
          System.out.print(browserOutput);
        }
      } finally {
        tomcat.stop();
        tomcat.destroy();
      }
      assertFalse(
          Thread.getAllStackTraces().keySet().stream()
              .anyMatch(thread -> thread.isAlive() && thread.getName().startsWith("ticketscenter")),
          "Persistence pool thread survived undeploy");
    }
  }
}
