package vn.ticketscenter.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.service.identity.CsrfTokens;

public final class CsrfFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        // The contracted VNPAY IPN uses GET; it has no POST exemption.
        if ("POST".equals(req.getMethod())
                && !CsrfTokens.matches(req.getSession(false), req.getHeader("X-CSRF-Token")))
            throw BusinessException.forbidden("Invalid CSRF token");
        chain.doFilter(request, response);
    }
}
