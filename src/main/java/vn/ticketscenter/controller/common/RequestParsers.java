package vn.ticketscenter.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import vn.ticketscenter.dto.common.PageRequest;
import vn.ticketscenter.exception.BusinessException;

/**
 * Parses and validates common request parameters. All methods throw BusinessException(400) on
 * invalid input — before any DB query.
 */
public final class RequestParsers {

  private RequestParsers() {}

  public static UUID uuid(String value, String field) {
    if (value == null || value.isBlank()) {
      throw BusinessException.badRequest("INVALID_" + field.toUpperCase(), field + " is required");
    }
    try {
      if (!value.matches(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
        throw new IllegalArgumentException();
      }
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw BusinessException.badRequest(
          "INVALID_" + field.toUpperCase(), field + " must be a valid UUID");
    }
  }

  public static PageRequest page(HttpServletRequest req, Set<String> allowedSorts) {
    int page = parseIntParam(req, "page", PageRequest.MIN_PAGE);
    int pageSize = parseIntParam(req, "pageSize", PageRequest.DEFAULT_PAGE_SIZE);
    String sort = req.getParameter("sort");

    if (page < PageRequest.MIN_PAGE) {
      throw BusinessException.badRequest("INVALID_PAGE", "page must be >= 1");
    }
    if (pageSize < 1 || pageSize > PageRequest.MAX_PAGE_SIZE) {
      throw BusinessException.badRequest(
          "INVALID_PAGE_SIZE", "pageSize must be between 1 and " + PageRequest.MAX_PAGE_SIZE);
    }
    if (sort != null && !sort.isBlank() && !allowedSorts.contains(sort.trim())) {
      throw BusinessException.badRequest("INVALID_SORT", "sort value not allowed: " + sort);
    }
    try {
      return new PageRequest(page, pageSize, (sort == null || sort.isBlank()) ? null : sort.trim());
    } catch (IllegalArgumentException ex) {
      throw BusinessException.badRequest("INVALID_PAGE", "Page offset is too large");
    }
  }

  /**
   * Parse a money value from a request parameter string. Valid: non-negative whole number string,
   * no sign, no exponent, no decimal, max 19 digits.
   */
  public static BigDecimal money(String value, String field) {
    if (value == null || value.isBlank()) {
      throw BusinessException.badRequest("INVALID_" + field.toUpperCase(), field + " is required");
    }
    String v = value;
    if (!v.matches("0|[1-9][0-9]{0,18}")) {
      throw BusinessException.badRequest(
          "INVALID_" + field.toUpperCase(),
          field + " must be a non-negative integer string with max 19 digits");
    }
    return new BigDecimal(v);
  }

  private static int parseIntParam(HttpServletRequest req, String name, int defaultValue) {
    String raw = req.getParameter(name);
    if (raw == null || raw.isBlank()) return defaultValue;
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      throw BusinessException.badRequest(
          "INVALID_" + name.toUpperCase(), name + " must be an integer");
    }
  }
}
