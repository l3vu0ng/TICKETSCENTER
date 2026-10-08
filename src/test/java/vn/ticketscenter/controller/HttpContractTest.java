package vn.ticketscenter.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.ticketscenter.controller.common.*;
import vn.ticketscenter.dto.common.Page;
import vn.ticketscenter.exception.BusinessException;

class HttpContractTest {
  @ParameterizedTest
  @ValueSource(strings = {"0", "200000", "9999999999999999999"})
  void acceptsExactCanonicalMoney(String value) {
    assertEquals(new BigDecimal(value), RequestParsers.money(value, "amount"));
  }

  @ParameterizedTest
  @ValueSource(strings = {" 1", "1 ", "01", "-1", "+1", "1.5", "1e6", "10000000000000000000", "１"})
  void rejectsNonCanonicalMoney(String value) {
    assertEquals(
        400,
        assertThrows(BusinessException.class, () -> RequestParsers.money(value, "amount"))
            .getHttpStatus());
  }

  @Test
  void rejectsAbbreviatedUuidAndPageOverflow() {
    assertThrows(BusinessException.class, () -> RequestParsers.uuid("1-1-1-1-1", "id"));
    var request = mock(HttpServletRequest.class);
    when(request.getParameter("page")).thenReturn("2147483647");
    assertThrows(BusinessException.class, () -> RequestParsers.page(request, Set.of("createdAt")));
  }

  @Test
  void rejectsUnsafeSortBeforeQuery() {
    var request = mock(HttpServletRequest.class);
    when(request.getParameter("sort")).thenReturn("id; DROP TABLE");
    assertThrows(BusinessException.class, () -> RequestParsers.page(request, Set.of("createdAt")));
  }

  @Test
  void serializesNullMoneyAndUtcWithoutLosingPrecision() throws Exception {
    var response = mock(HttpServletResponse.class);
    var output = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(output));
    HttpResponses.data(response, 200, null);
    assertEquals("{\"data\":null}", output.toString());
    String json =
        HttpResponses.jsonMapper()
            .writeValueAsString(
                Map.of(
                    "money",
                    new BigDecimal("9999999999999999999"),
                    "rate",
                    new BigDecimal("2.500000"),
                    "time",
                    Instant.parse("2026-10-06T03:00:00Z")));
    assertTrue(json.contains("\"money\":\"9999999999999999999\""));
    assertTrue(json.contains("\"rate\":\"2.500000\""));
    assertTrue(json.contains("\"time\":\"2026-10-06T03:00:00Z\""));
  }

  @Test
  void pageCopiesItemsAndSerializesEmptyContract() throws Exception {
    var source = new ArrayList<>(List.of("value"));
    var page = Page.of(source, 1, 20, 1);
    source.clear();
    assertEquals(List.of("value"), page.items());
    assertThrows(UnsupportedOperationException.class, () -> page.items().clear());
    assertEquals(
        "{\"items\":[],\"page\":1,\"pageSize\":20,\"total\":0}",
        HttpResponses.jsonMapper().writeValueAsString(Page.empty(1, 20)));
  }

  @Test
  void acceptUsesExplicitJsonAndQuality() {
    var request = mock(HttpServletRequest.class);
    for (String value :
        List.of(
            "*/*", "text/html", "application/json;q=0", "application/json;q=.5,text/html;q=1")) {
      when(request.getHeader("Accept")).thenReturn(value);
      assertFalse(HttpResponses.wantsJson(request), value);
    }
    when(request.getHeader("Accept")).thenReturn("application/json");
    assertTrue(HttpResponses.wantsJson(request));
  }
}
