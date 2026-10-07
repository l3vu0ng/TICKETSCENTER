package vn.ticketscenter.dto.mail;

import java.time.Instant;

/**
 * Receipt returned after a mail send.
 * Owner: Khánh (KHANH-08)
 */
public record MailReceipt(String providerReference, Instant acceptedAt) {}
