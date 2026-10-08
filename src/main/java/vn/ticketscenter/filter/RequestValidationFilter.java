package vn.ticketscenter.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.exception.BusinessException;

/** Limits identity JSON before the controller can mutate data. */
public final class RequestValidationFilter implements Filter {
  private static final int LIMIT = 65536;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var req = (HttpServletRequest) request;
    String path = req.getRequestURI().substring(req.getContextPath().length());
    if ("POST".equals(req.getMethod()) && (path.startsWith("/auth/") || path.startsWith("/me/"))) {
      String contentType = req.getContentType();
      if (contentType == null
          || !contentType.toLowerCase(Locale.ROOT).split(";")[0].strip().equals("application/json"))
        throw new BusinessException("UNSUPPORTED_MEDIA_TYPE", "JSON body required", 415);
      if (req.getContentLengthLong() > LIMIT)
        throw new BusinessException("BODY_TOO_LARGE", "Request body is too large", 413);
      byte[] body = req.getInputStream().readNBytes(LIMIT + 1);
      if (body.length > LIMIT)
        throw new BusinessException("BODY_TOO_LARGE", "Request body is too large", 413);
      try {
        var mapper = HttpResponses.jsonMapper();
        mapper
            .getFactory()
            .setStreamReadConstraints(
                com.fasterxml.jackson.core.StreamReadConstraints.builder()
                    .maxNestingDepth(32)
                    .maxStringLength(16384)
                    .build());
        mapper.enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        mapper.enable(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        var json = mapper.readTree(body);
        if (json == null || !json.isObject()) throw new IllegalArgumentException();
        for (String reserved :
            java.util.List.of(
                "actor",
                "actorId",
                "userId",
                "role",
                "platformRole",
                "principal",
                "now",
                "emailVerified",
                "authVersion")) if (json.has(reserved)) throw new IllegalArgumentException();
      } catch (IOException | IllegalArgumentException ex) {
        throw BusinessException.badRequest("INVALID_JSON", "Invalid JSON body");
      }
      req.setAttribute("jsonBody", new String(body, StandardCharsets.UTF_8));
      req = new CachedRequest(req, body);
    }
    chain.doFilter(req, response);
  }

  private static final class CachedRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    CachedRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() {
      var input = new java.io.ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return input.read();
        }

        @Override
        public boolean isFinished() {
          return input.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException("Synchronous request");
        }
      };
    }

    @Override
    public java.io.BufferedReader getReader() {
      return new java.io.BufferedReader(
          new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
  }
}
