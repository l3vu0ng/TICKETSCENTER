package vn.ticketscenter.dto.mail;

import java.time.Instant;

/** Receipt returned after a mail send. */
public record MailReceipt(String providerReference, Instant acceptedAt) {}
