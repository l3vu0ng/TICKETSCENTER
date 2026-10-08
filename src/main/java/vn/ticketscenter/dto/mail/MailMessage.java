package vn.ticketscenter.dto.mail;

/**
 * Outbound email message. textBody and htmlBody are rendered by MailContentService. idempotencyKey
 * used for dedup where provider supports it.
 */
public record MailMessage(
    String to, String subject, String textBody, String htmlBody, String idempotencyKey) {}
