package vn.ticketscenter.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.filter.*;

class ErrorHandlingTest {
  @Test
  void sqlFailureProducesGenericErrorAndBoundedCorrelation() throws Exception {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    var output = new StringWriter();
    var correlation = new AtomicReference<String>();
    when(response.getWriter()).thenReturn(new PrintWriter(output));
    when(request.getHeader("X-Correlation-Id")).thenReturn("evil\r\nHeader: secret");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/ticketscenter/auth/login");
    when(request.getContextPath()).thenReturn("/ticketscenter");
    doAnswer(
            call -> {
              correlation.set(call.getArgument(1));
              return null;
            })
        .when(request)
        .setAttribute(eq("correlationId"), any());
    when(request.getAttribute("correlationId")).thenAnswer(call -> correlation.get());
    new CorrelationIdFilter()
        .doFilter(
            request,
            response,
            (req, resp) ->
                new ErrorHandlingFilter()
                    .doFilter(
                        req,
                        resp,
                        (r, s) -> {
                          throw new ServletException("password=secret");
                        }));
    verify(response).setStatus(500);
    assertTrue(output.toString().contains("INTERNAL_ERROR"));
    assertFalse(output.toString().contains("secret"));
    assertNotNull(java.util.UUID.fromString(correlation.get()));
  }
}
