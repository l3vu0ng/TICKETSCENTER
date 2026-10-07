package vn.ticketscenter.controller.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.ticketscenter.dto.common.ApiError;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Shared HTTP response helpers.
 * Success:  {"data": {...}}
 * Error:    {"error": {"code":"...","message":"...","correlationId":"..."}}
 * Owner: Khánh (KHANH-02)
 */
public final class HttpResponses {

    private static final ObjectMapper MAPPER = buildMapper();

    private HttpResponses() {}

    private static ObjectMapper buildMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return m;
    }

    public static void data(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(resp.getWriter(), Map.of("data", body));
    }

    public static void ok(HttpServletResponse resp, Object body) throws IOException {
        data(resp, HttpServletResponse.SC_OK, body);
    }

    public static void error(HttpServletResponse resp, int status, ApiError apiError) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        MAPPER.writeValue(resp.getWriter(), Map.of("error", apiError));
    }

    public static void notFound(HttpServletResponse resp) throws IOException {
        error(resp, HttpServletResponse.SC_NOT_FOUND,
                new ApiError("NOT_FOUND", "Resource not found", null));
    }

    public static void methodNotAllowed(HttpServletResponse resp) throws IOException {
        error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                new ApiError("METHOD_NOT_ALLOWED", "Method not allowed", null));
    }

    public static boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    public static String moneyToString(BigDecimal value) {
        if (value == null) return null;
        String plain = value.stripTrailingZeros().toPlainString();
        if (plain.contains(".")) {
            throw new IllegalArgumentException("Money must be a whole number, got: " + plain);
        }
        return plain;
    }
}
