package vn.ticketscenter.fulfillment.model;

/**
 * Outcome of a ticket check-in scan.
 */
public enum CheckInResult {
    SUCCESS,
    INVALID_CODE,
    WRONG_EVENT,
    ALREADY_USED,
    OUTSIDE_WINDOW,
    INACTIVE_TICKET,
    EVENT_CANCELLED
}
