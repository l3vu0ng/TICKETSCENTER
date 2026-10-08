package vn.ticketscenter.controller.common;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/** View paths are fixed by the owning modules; request input cannot select a JSP. */
public final class ViewSupport {
  private static final Set<String> ALLOWED_VIEWS =
      Set.of(
          "/WEB-INF/views/common/error.jsp",
          "/WEB-INF/views/identity/auth.jsp",
          "/WEB-INF/views/identity/profile.jsp",
          "/WEB-INF/views/identity/organization-requests.jsp",
          "/WEB-INF/views/organization/request.jsp",
          "/WEB-INF/views/organization/dashboard.jsp",
          "/WEB-INF/views/organization/members.jsp",
          "/WEB-INF/views/organization/events.jsp",
          "/WEB-INF/views/event/list.jsp",
          "/WEB-INF/views/event/detail.jsp",
          "/WEB-INF/views/event/editor.jsp",
          "/WEB-INF/views/sales/checkout.jsp",
          "/WEB-INF/views/sales/payment-result.jsp",
          "/WEB-INF/views/sales/orders.jsp",
          "/WEB-INF/views/sales/tickets.jsp",
          "/WEB-INF/views/fulfillment/refund-request.jsp");

  private ViewSupport() {}

  public static void forward(
      HttpServletRequest request, HttpServletResponse response, String viewPath, Object viewModel)
      throws ServletException, IOException {
    if (!ALLOWED_VIEWS.contains(viewPath))
      throw new IllegalArgumentException("View is not allowed");
    request.setAttribute("viewModel", viewModel);
    request.setAttribute("contextPath", request.getContextPath());
    defaults(request, "pageTitle", "TicketsCenter");
    defaults(request, "activeNav", "");
    defaults(request, "memberships", vn.ticketscenter.dto.common.Page.empty(1, 20));
    defaults(request, "notice", "");
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");
    request.getRequestDispatcher(viewPath).forward(request, response);
  }

  private static void defaults(HttpServletRequest request, String name, Object value) {
    if (request.getAttribute(name) == null) request.setAttribute(name, value);
  }
}
