package vn.ticketscenter.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.dto.common.ApiError;
import vn.ticketscenter.exception.BusinessException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catches BusinessException and unhandled exceptions.
 * Returns safe error responses — never exposes SQL, stack traces or secrets.
 * Owner: Khánh (KHANH-02)
 */
public class ErrorHandlingFilter implements Filter {

    private static final Logger log = Logger.getLogger(ErrorHandlingFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        try {
            chain.doFilter(request, response);
        } catch (BusinessException e) {
            if (!resp.isCommitted()) {
                String correlationId = (String) req.getAttribute(
                        CorrelationIdFilter.ATTR_CORRELATION_ID);
                HttpResponses.error(resp, e.getHttpStatus(),
                        new ApiError(e.getCode(), e.getMessage(), correlationId));
            }
        } catch (Exception e) {
            String correlationId = (String) req.getAttribute(
                    CorrelationIdFilter.ATTR_CORRELATION_ID);
            log.log(Level.SEVERE, "Unhandled error [" + correlationId + "]", e);
            if (!resp.isCommitted()) {
                HttpResponses.error(resp, 500,
                        new ApiError("INTERNAL_ERROR",
                                "An unexpected error occurred.", correlationId));
            }
        }
    }
}
