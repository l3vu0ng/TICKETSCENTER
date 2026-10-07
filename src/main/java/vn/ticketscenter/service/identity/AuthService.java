package vn.ticketscenter.service.identity;

import jakarta.servlet.http.HttpServletRequest;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.exception.BusinessException;

/**
 * Verifies session and returns a DB-backed ActorContext.
 * BLOCKED: Full implementation in KHANH-06.
 * Skeleton provided for compile-time contract satisfaction.
 * Owner: Khánh (KHANH-06)
 */
public class AuthService {

    private static final AuthService INSTANCE = new AuthService();
    private AuthService() {}
    public static AuthService getInstance() { return INSTANCE; }

    /**
     * Returns ActorContext for the current session.
     * Verifies User ACTIVE and authVersion match DB state.
     * Throws BusinessException(401) if session is missing or stale.
     *
     * BLOCKED KHANH-06: implementation pending session/DB infrastructure.
     */
    public ActorContext requireCurrentUser(HttpServletRequest request) {
        // TODO KHANH-06
        throw BusinessException.unauthorized(
                "Authentication not yet implemented. Task: KHANH-06.");
    }
}
