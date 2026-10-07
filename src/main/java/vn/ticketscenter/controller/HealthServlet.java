package vn.ticketscenter.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.ticketscenter.controller.common.HttpResponses;

import java.io.IOException;
import java.util.Map;

/**
 * GET /health/live  — always 200 {"data":{"status":"UP"}} if WAR is running
 * GET /health/ready — 200 if DB OK, 503 otherwise; Cache-Control: no-store
 * Owner: Khánh (KHANH-01)
 */
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "no-store");
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "/";

        switch (pathInfo) {
            case "/live"  -> HttpResponses.data(resp, 200, Map.of("status", "UP"));
            case "/ready" -> handleReady(resp);
            default       -> HttpResponses.notFound(resp);
        }
    }

    private void handleReady(HttpServletResponse resp) throws IOException {
        // BLOCKED KHANH-03: PersistenceFactory not yet available.
        // Once KHANH-03 is complete, replace with a real lightweight DB probe.
        // For now: return 503 so readiness correctly reports not-ready.
        HttpResponses.data(resp, 503, Map.of(
                "status", "DOWN",
                "reason", "database_unavailable",
                "note", "BLOCKED: PersistenceFactory not yet initialized (KHANH-03)"));
    }
}
