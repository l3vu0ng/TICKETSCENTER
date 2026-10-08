package vn.ticketscenter.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;
import vn.ticketscenter.model.identity.OtpPurpose;

/** Technical OTP state; only a keyed digest is persisted. */
@Entity
@Table(name = "OtpRecord", schema = "dbo")
public class OtpRecord {
  @Id
  @Column(columnDefinition = "uniqueidentifier")
  private UUID id;

  @Column(nullable = false, columnDefinition = "uniqueidentifier")
  private UUID userId;

  @Nationalized
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OtpPurpose purpose;

  @Nationalized
  @Column(nullable = false, length = 255)
  private String codeHmac;

  @Column(nullable = false, columnDefinition = "datetime2")
  private Instant createdAt;

  @Column(nullable = false, columnDefinition = "datetime2")
  private Instant expiresAt;

  @Column(nullable = false, columnDefinition = "datetime2")
  private Instant lastSentAt;

  @Column(nullable = false)
  private int failedAttempts;

  @Column(columnDefinition = "datetime2")
  private Instant consumedAt;

  @Column(columnDefinition = "datetime2")
  private Instant invalidatedAt;

  protected OtpRecord() {}

  public static OtpRecord create(
      UUID id, UUID userId, OtpPurpose purpose, String digest, Instant now) {
    var result = new OtpRecord();
    result.id = java.util.Objects.requireNonNull(id);
    result.userId = java.util.Objects.requireNonNull(userId);
    result.purpose = java.util.Objects.requireNonNull(purpose);
    if (digest == null || digest.isBlank() || digest.length() > 255)
      throw new IllegalArgumentException("OTP digest required");
    result.codeHmac = digest;
    result.createdAt = java.util.Objects.requireNonNull(now);
    result.lastSentAt = now;
    result.expiresAt = now.plusSeconds(300);
    return result;
  }

  public UUID getId() {
    return id;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }
}
