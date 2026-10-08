package vn.ticketscenter.fulfillment.model;

/**
 * Transfer outcome returned by the provider adapter or gateway reconciliation.
 */
public enum RefundTransferResult {
    SUCCEEDED,
    FAILED,
    UNKNOWN
}
