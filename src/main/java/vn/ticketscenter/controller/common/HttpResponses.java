package vn.ticketscenter.controller.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import vn.ticketscenter.dto.common.ApiError;

/** Shared JSON envelopes; decimals retain their exact precision as strings. */
public final class HttpResponses {
    private static final ObjectMapper MAPPER = buildMapper();

    private HttpResponses() {}

    private static ObjectMapper buildMapper() {
        var decimals = new SimpleModule();
        decimals.addSerializer(
                BigDecimal.class,
                new JsonSerializer<BigDecimal>() {
                    @Override
                    public void serialize(
                            BigDecimal value, JsonGenerator output, SerializerProvider provider)
                            throws IOException {
                        output.writeString(value.toPlainString());
                    }
                });
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(decimals)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static ObjectMapper jsonMapper() {
        return MAPPER.copy();
    }

    public static void data(HttpServletResponse response, int status, Object body)
            throws IOException {
        // Map.of rejects null; an absent current hold must still produce data:null.
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("data", body);
        write(response, status, envelope);
    }

    public static void ok(HttpServletResponse response, Object body) throws IOException {
        data(response, 200, body);
    }

    public static void error(HttpServletResponse response, int status, ApiError error)
            throws IOException {
        write(response, status, Map.of("error", error));
    }

    private static void write(HttpServletResponse response, int status, Object body)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(), body);
    }

    public static void notFound(HttpServletResponse response) throws IOException {
        error(
                response,
                404,
                new ApiError(
                        "NOT_FOUND", "Resource not found", response.getHeader("X-Correlation-Id")));
    }

    public static void methodNotAllowed(HttpServletResponse response) throws IOException {
        error(
                response,
                405,
                new ApiError(
                        "METHOD_NOT_ALLOWED",
                        "Method not allowed",
                        response.getHeader("X-Correlation-Id")));
    }

    public static boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept == null) return false;
        double json = -1, html = -1;
        for (String range : accept.toLowerCase(Locale.ROOT).split(",")) {
            String[] parts = range.strip().split(";");
            double quality = 1;
            for (int i = 1; i < parts.length; i++) {
                String parameter = parts[i].strip();
                if (parameter.startsWith("q=")) {
                    try {
                        quality = Double.parseDouble(parameter.substring(2));
                    } catch (NumberFormatException ex) {
                        quality = 0;
                    }
                    if (!Double.isFinite(quality) || quality < 0 || quality > 1) quality = 0;
                }
            }
            if (parts[0].equals("application/json")) json = Math.max(json, quality);
            if (parts[0].equals("text/html")) html = Math.max(html, quality);
        }
        return json > 0 && json >= html;
    }

    public static String moneyToString(BigDecimal value) {
        if (value == null) return null;
        String result = value.stripTrailingZeros().toPlainString();
        if (!result.matches("0|[1-9][0-9]{0,18}"))
            throw new IllegalArgumentException("Invalid money amount");
        return result;
    }
}
