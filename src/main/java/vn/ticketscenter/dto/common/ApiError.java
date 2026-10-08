package vn.ticketscenter.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Standard error envelope body: {"code":"...","message":"...","correlationId":"..."} */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, String correlationId) {
  public static ApiError of(String code, String message, String correlationId) {
    return new ApiError(code, message, correlationId);
  }
}
