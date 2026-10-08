package vn.ticketscenter.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import vn.ticketscenter.config.PersistenceFactory;
import vn.ticketscenter.controller.common.HttpResponses;

/** Liveness stays independent of database availability. */
public final class HealthServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setHeader("Cache-Control", "no-store");
    String path = request.getPathInfo();
    if ("/live".equals(path)) HttpResponses.data(response, 200, Map.of("status", "UP"));
    else if ("/ready".equals(path)) {
      var factory =
          (PersistenceFactory) getServletContext().getAttribute(PersistenceFactory.ATTRIBUTE);
      boolean ready = factory != null && factory.isReady();
      HttpResponses.data(response, ready ? 200 : 503, Map.of("status", ready ? "UP" : "DOWN"));
    } else HttpResponses.notFound(response);
  }
}
