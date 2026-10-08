package vn.ticketscenter.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.support.HttpTestClient;
import vn.ticketscenter.support.SqlServerTestSupport;

class KhanhHealthIT {
    private static HttpTestClient http;

    @BeforeAll
    static void setup() {
        SqlServerTestSupport.config();
        http = new HttpTestClient(SqlServerTestSupport.required("TC_APP_BASE_URL"));
    }

    @Test
    void liveIsUpAndNeverLeaksConfiguration() throws Exception {
        var response = http.get("/health/live");
        assertEquals(200, response.statusCode());
        assertEquals("{\"data\":{\"status\":\"UP\"}}", response.body());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
        assertTrue(
                response.headers()
                        .firstValue("Content-Type")
                        .orElseThrow()
                        .startsWith("application/json"));
    }

    @Test
    void readyMatchesExplicitExpectedDatabaseState() throws Exception {
        String expected = System.getProperty("TC_EXPECT_READY", "UP");
        assertTrue(
                expected.equals("UP") || expected.equals("DOWN"),
                "TC_EXPECT_READY must be UP or DOWN");
        var response = http.get("/health/ready");
        assertEquals(expected.equals("UP") ? 200 : 503, response.statusCode());
        assertEquals(
                expected,
                HttpResponses.jsonMapper()
                        .readTree(response.body())
                        .path("data")
                        .path("status")
                        .asText());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
    }
}
