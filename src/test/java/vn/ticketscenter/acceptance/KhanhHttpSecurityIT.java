package vn.ticketscenter.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.support.HttpTestClient;
import vn.ticketscenter.support.SqlServerTestSupport;

class KhanhHttpSecurityIT {
  @Test
  void csrfIsSessionBoundAndMalformedBodyNeverReachesMutation() throws Exception {
    SqlServerTestSupport.config();
    String base = SqlServerTestSupport.required("TC_APP_BASE_URL");
    var first = new HttpTestClient(base);
    var second = new HttpTestClient(base);
    var tokenResponse = first.get("/auth/csrf");
    assertEquals(200, tokenResponse.statusCode());
    String token =
        HttpResponses.jsonMapper()
            .readTree(tokenResponse.body())
            .path("data")
            .path("csrfToken")
            .asText();
    assertEquals(403, first.post("/auth/login", "{}", null).statusCode());
    assertEquals(403, second.post("/auth/login", "{}", token).statusCode());
    assertEquals(400, first.post("/auth/login", "{", token).statusCode());
    assertEquals(413, first.post("/auth/login", "x".repeat(65537), token).statusCode());
    assertEquals(501, first.post("/auth/login", "{}", token).statusCode());
  }
}
