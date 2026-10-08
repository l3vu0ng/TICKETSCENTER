package vn.ticketscenter.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.ticketscenter.service.identity.AuthService;

public final class AuthenticationFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var req = (HttpServletRequest) request;
    String path = req.getRequestURI().substring(req.getContextPath().length());
    if (path.equals("/me") || path.startsWith("/me/")) {
      var auth = (AuthService) req.getServletContext().getAttribute("authService");
      req.setAttribute("actorContext", auth.requireCurrentUser(req));
    }
    chain.doFilter(request, response);
  }
}
