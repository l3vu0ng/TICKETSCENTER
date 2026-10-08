package vn.ticketscenter.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public final class SecurityHeadersFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var req = (HttpServletRequest) request;
    var resp = (HttpServletResponse) response;
    resp.setHeader("X-Content-Type-Options", "nosniff");
    resp.setHeader("X-Frame-Options", "DENY");
    resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    resp.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
    if (req.isSecure()) resp.setHeader("Strict-Transport-Security", "max-age=31536000");
    String path = req.getRequestURI().substring(req.getContextPath().length());
    if (path.equals("/auth")
        || path.startsWith("/auth/")
        || path.equals("/me")
        || path.startsWith("/me/")) resp.setHeader("Cache-Control", "no-store");
    chain.doFilter(request, response);
  }
}
