package vn.ticketscenter.transaction;

/**
 * Maps use-case roles to DB connection principals. Khánh defines; Vương maps to real DB logins
 * (VUONG-03).
 */
public enum PrincipalKind {
    BUYER,
    MANAGER,
    CHECK_IN,
    PLATFORM_ADMIN,
    AUTH_TECH,
    WORKER_TECH
}
