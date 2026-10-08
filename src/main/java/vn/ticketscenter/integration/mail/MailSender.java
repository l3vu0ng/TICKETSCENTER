package vn.ticketscenter.integration.mail;

import vn.ticketscenter.dto.mail.MailMessage;
import vn.ticketscenter.dto.mail.MailReceipt;

/**
 * Mail sender SPI. ConfiguredMailSender is the production implementation. CapturingMailSender is
 * the test double.
 */
public interface MailSender {
  /**
   * Send an email. May throw checked/unchecked exceptions on failure. Must NOT update Order/Refund
   * state — only sends the message. Caller commits business state before calling send.
   */
  MailReceipt send(MailMessage message) throws Exception;
}
