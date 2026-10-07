package vn.ticketscenter.controller.identity;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.dto.common.ApiError;
import vn.ticketscenter.filter.CorrelationIdFilter;

import java.io.IOException;

/**
 * Handles /me/* routes.
 * BLOCKED: Full implementation in KHANH-13 (M1-M3).
 * Skeleton registered in web.xml to satisfy servlet mapping contract.
 *
 * Owner: Khánh (KHANH-13)
 */
public class MeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpResponses.error(resp, HttpServletResponse.SC_NOT_IMPLEMENTED,
                new ApiError("NOT_IMPLEMENTED",
                        "MeServlet not yet implemented. Task: KHANH-13.",
                        correlationId(req)));
    }

    private static String correlationId(HttpServletRequest req) {
        Object id = req.getAttribute(CorrelationIdFilter.ATTR_CORRELATION_ID);
        return id != null ? id.toString() : null;
    }
}
