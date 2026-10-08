package vn.ticketscenter.fulfillment.model;

/**
 * Purpose of the refund obligation.
 *
 * - CUSTOMER_REFUND: Ticket-based refund requested by buyer or resulting from event cancellation.
 * - PAYMENT_COMPENSATION: Gateway payment compensation (e.g. late/duplicate capture, hold expired),
 *   tied directly to a Payment without tickets.
 */
public enum RefundPurpose {
    CUSTOMER_REFUND,
    PAYMENT_COMPENSATION
}
