package vn.ticketscenter.exception;

import jakarta.servlet.http.HttpServletResponse;

/** Domain error with stable HTTP code. Message is safe to expose. */
public class BusinessException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public BusinessException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BusinessException badRequest(String code, String msg) {
        return new BusinessException(code, msg, HttpServletResponse.SC_BAD_REQUEST);
    }

    public static BusinessException unauthorized(String msg) {
        return new BusinessException("UNAUTHORIZED", msg, HttpServletResponse.SC_UNAUTHORIZED);
    }

    public static BusinessException forbidden(String msg) {
        return new BusinessException("FORBIDDEN", msg, HttpServletResponse.SC_FORBIDDEN);
    }

    public static BusinessException notFound(String msg) {
        return new BusinessException("NOT_FOUND", msg, HttpServletResponse.SC_NOT_FOUND);
    }

    public static BusinessException conflict(String code, String msg) {
        return new BusinessException(code, msg, HttpServletResponse.SC_CONFLICT);
    }

    public static BusinessException tooManyRequests(String msg) {
        return new BusinessException("TOO_MANY_REQUESTS", msg, 429);
    }

    public static BusinessException serviceUnavailable(String msg) {
        return new BusinessException(
                "SERVICE_UNAVAILABLE", msg, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
