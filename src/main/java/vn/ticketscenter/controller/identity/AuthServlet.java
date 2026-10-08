package vn.ticketscenter.controller.identity;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import vn.ticketscenter.controller.common.HttpResponses;
import vn.ticketscenter.dto.common.ApiError;
import vn.ticketscenter.service.identity.CsrfTokens;

/** M0 exposes CSRF; account flows are completed in M1. */
public final class AuthServlet extends HttpServlet {
  private static final Set<String> MUTATIONS =
      Set.of(
          "/register",
          "/login",
          "/logout",
          "/otp/send",
          "/otp/verify",
          "/password/forgot",
          "/password/reset");

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String path = request.getPathInfo();
    if ("/csrf".equals(path)) {
      response.setHeader("Cache-Control", "no-store");
      HttpResponses.data(
          response, 200, Map.of("csrfToken", CsrfTokens.getOrCreate(request.getSession(true))));
    } else if (path == null || path.equals("/")) unavailable(request, response);
    else HttpResponses.notFound(response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (request.getPathInfo() != null && MUTATIONS.contains(request.getPathInfo()))
      unavailable(request, response);
    else HttpResponses.notFound(response);
  }

  private static void unavailable(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    HttpResponses.error(
        response,
        501,
        new ApiError(
            "NOT_IMPLEMENTED",
            "Authentication flow is not available yet.",
            (String) request.getAttribute("correlationId")));
  }
}
