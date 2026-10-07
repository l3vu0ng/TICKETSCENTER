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
 * Handles all authentication routes: /auth/*
 * Routes:
 *   GET  /auth            -> HTML view (login/register/otp/forgot/reset)
 *   GET  /auth/csrf       -> {"data":{"csrfToken":"..."}}
 *   POST /auth/register   -> 202 {"data":{"message":"..."}}
 *   POST /auth/login      -> 200 {"data":{"user":{...}}}
 *   POST /auth/logout     -> 200 {"data":{"loggedOut":true}}
 *   POST /auth/otp/send   -> 202 {"data":{"message":"...","resendAfterSeconds":60}}
 *   POST /auth/otp/verify -> 200 {"data":{...}}
 *   POST /auth/password/forgot -> 202 {"data":{"message":"..."}}
 *   POST /auth/password/reset  -> 200 {"data":{"message":"..."}}
 *
 * BLOCKED: Full implementation in KHANH-05 through KHANH-09.
 * Skeleton registered in web.xml to satisfy servlet mapping contract.
 *
 * Owner: Khánh (KHANH-05–12)
 */
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "/";

        switch (pathInfo) {
            case "/csrf" -> handleCsrf(req, resp);
            default -> {
                // TODO KHANH-12: forward to auth.jsp with view param
                HttpResponses.error(resp, HttpServletResponse.SC_NOT_IMPLEMENTED,
                        new ApiError("NOT_IMPLEMENTED",
                                "Auth UI not yet implemented. Task: KHANH-12.",
                                correlationId(req)));
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "/";

        switch (pathInfo) {
            case "/register"         -> handleNotImplemented(resp, req, "KHANH-05");
            case "/login"            -> handleNotImplemented(resp, req, "KHANH-06");
            case "/logout"           -> handleNotImplemented(resp, req, "KHANH-06");
            case "/otp/send"         -> handleNotImplemented(resp, req, "KHANH-08");
            case "/otp/verify"       -> handleNotImplemented(resp, req, "KHANH-09");
            case "/password/forgot"  -> handleNotImplemented(resp, req, "KHANH-08");
            case "/password/reset"   -> handleNotImplemented(resp, req, "KHANH-09");
            default -> HttpResponses.notFound(resp);
        }
    }

    private void handleCsrf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // TODO KHANH-07: generate and return real CSRF token bound to session
        resp.setHeader("Cache-Control", "no-store");
        HttpResponses.error(resp, HttpServletResponse.SC_NOT_IMPLEMENTED,
                new ApiError("NOT_IMPLEMENTED",
                        "CSRF token endpoint not yet implemented. Task: KHANH-07.",
                        correlationId(req)));
    }

    private void handleNotImplemented(HttpServletResponse resp, HttpServletRequest req,
                                      String task) throws IOException {
        HttpResponses.error(resp, HttpServletResponse.SC_NOT_IMPLEMENTED,
                new ApiError("NOT_IMPLEMENTED",
                        "Not yet implemented. Task: " + task + ".",
                        correlationId(req)));
    }

    private static String correlationId(HttpServletRequest req) {
        Object id = req.getAttribute(CorrelationIdFilter.ATTR_CORRELATION_ID);
        return id != null ? id.toString() : null;
    }
}
