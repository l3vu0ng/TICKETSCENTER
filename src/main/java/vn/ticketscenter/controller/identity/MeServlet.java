package vn.ticketscenter.controller.identity;

import jakarta.servlet.http.*;
import java.io.IOException;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.dto.common.ApiError;

/** Personal queries are integrated with their owning modules in M1–M3. */
public final class MeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpResponses.error(
                response,
                501,
                new ApiError(
                        "NOT_IMPLEMENTED",
                        "Personal account views are not available yet.",
                        (String) request.getAttribute("correlationId")));
    }
}
