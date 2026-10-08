package vn.ticketscenter.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vn.ticketscenter.support.HttpTestClient;
import vn.ticketscenter.support.SqlServerTestSupport;

class KhanhHttpIT {
  @Test
  void unimplementedAuthUsesSafeEnvelopeAndCorrelation() throws Exception {
    SqlServerTestSupport.config();
    var http = new HttpTestClient(SqlServerTestSupport.required("TC_APP_BASE_URL"));
    var response = http.get("/auth");
    assertEquals(501, response.statusCode());
    assertTrue(response.body().contains("\"error\""));
    assertFalse(response.body().contains("passwordHash"));
    assertTrue(response.headers().firstValue("X-Correlation-Id").isPresent());
  }
}
