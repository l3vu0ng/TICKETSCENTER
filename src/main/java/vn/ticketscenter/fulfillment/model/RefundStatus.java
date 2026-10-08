package vn.ticketscenter.fulfillment.model;

/**
 * Domain state machine for a refund obligation.
 *
 * State transitions:
 * - REQUESTED -> APPROVED (via approve() or adoptEventCancellation())
 * - REQUESTED -> REJECTED (via reject(reason))
 * - APPROVED -> PROCESSING (via beginAttempt(attemptId))
 * - APPROVED (when amount is 0) -> COMPLETED (via completeWithoutTransfer())
 * - RETRYABLE -> PROCESSING (via beginAttempt(newAttemptId))
 * - PROCESSING -> COMPLETED (SUCCEEDED)
 * - PROCESSING -> RETRYABLE (FAILED)
 * - PROCESSING -> NEEDS_RECONCILIATION (UNKNOWN)
 */
public enum RefundStatus {
    REQUESTED,
    APPROVED,
    PROCESSING,
    NEEDS_RECONCILIATION,
    RETRYABLE,
    REJECTED,
    COMPLETED
}
