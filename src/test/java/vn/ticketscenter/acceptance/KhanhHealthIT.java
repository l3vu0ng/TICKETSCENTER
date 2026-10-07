package vn.ticketscenter.acceptance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for /health/live and /health/ready endpoints.
 * Requires TC_APP_BASE_URL to be set and the WAR to be deployed.
 *
 * Run with:
 *   mvn -B -Psqlserver-it -Dit.test=KhanhHealthIT verify
 */
@EnabledIfEnvironmentVariable(named = "TC_APP_BASE_URL", matches = ".+")
class KhanhHealthIT {

    private final String baseUrl = System.getenv("TC_APP_BASE_URL");
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void liveEndpoint_alwaysReturns200() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health/live"))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode(),
                "Expected 200 from /health/live, got: " + resp.statusCode() + " body: " + resp.body());
        assertTrue(resp.body().contains("\"status\""),
                "Expected status field in response: " + resp.body());
        assertTrue(resp.body().contains("UP"),
                "Expected UP in response: " + resp.body());
        String cacheControl = resp.headers().firstValue("Cache-Control").orElse("");
        assertTrue(cacheControl.contains("no-store"),
                "Expected Cache-Control: no-store, got: " + cacheControl);
    }

    @Test
    void liveEndpoint_responseIsJson() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health/live"))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.contains("application/json"),
                "Expected JSON Content-Type, got: " + contentType);
    }

    @Test
    void readyEndpoint_returnsEitherOkOrServiceUnavailable() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health/ready"))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertTrue(resp.statusCode() == 200 || resp.statusCode() == 503,
                "Expected 200 or 503 from /health/ready, got: " + resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"status\""),
                "Expected status field in response: " + body);
    }
}
