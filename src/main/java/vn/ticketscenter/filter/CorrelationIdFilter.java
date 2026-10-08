package vn.ticketscenter.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Assigns a correlation ID to every request for log tracing. Accepts X-Correlation-Id only if
 * bounded safe ASCII; otherwise generates a UUID. Never logs request bodies or sensitive headers.
 */
public class CorrelationIdFilter implements Filter {

  public static final String HEADER = "X-Correlation-Id";
  public static final String ATTR_CORRELATION_ID = "correlationId";

  private static final int MAX_LEN = 64;
  private static final Pattern SAFE = Pattern.compile("[a-zA-Z0-9\\-_]{1,64}");

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    String id = sanitize(req.getHeader(HEADER));
    req.setAttribute(ATTR_CORRELATION_ID, id);
    resp.setHeader(HEADER, id);

    chain.doFilter(request, response);
  }

  private static String sanitize(String candidate) {
    if (candidate != null && candidate.length() <= MAX_LEN && SAFE.matcher(candidate).matches()) {
      return candidate;
    }
    return UUID.randomUUID().toString();
  }
}
