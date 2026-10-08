package vn.ticketscenter.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.logging.Logger;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.controller.common.ViewSupport;
import vn.ticketscenter.dto.common.ApiError;
import vn.ticketscenter.exception.BusinessException;

public final class ErrorHandlingFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(ErrorHandlingFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var resp = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } catch (BusinessException ex) {
            render(req, resp, ex.getHttpStatus(), ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            // SQL exceptions can contain connection details; log only type and correlation.
            LOG.severe(
                    "Unhandled "
                            + ex.getClass().getSimpleName()
                            + " ["
                            + req.getAttribute("correlationId")
                            + "]");
            render(req, resp, 500, "INTERNAL_ERROR", "An unexpected error occurred.");
        }
    }

    private static void render(
            HttpServletRequest req,
            HttpServletResponse resp,
            int status,
            String code,
            String message)
            throws IOException, ServletException {
        if (resp.isCommitted()) return;
        resp.resetBuffer();
        var error = new ApiError(code, message, (String) req.getAttribute("correlationId"));
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean json =
                !"GET".equals(req.getMethod())
                        || path.startsWith("/health/")
                        || path.equals("/auth/csrf")
                        || path.equals("/me/hold")
                        || HttpResponses.wantsJson(req);
        if (json) HttpResponses.error(resp, status, error);
        else {
            resp.setStatus(status);
            req.setAttribute("error", error);
            ViewSupport.forward(req, resp, "/WEB-INF/views/common/error.jsp", error);
        }
    }
}
