package vn.ticketscenter.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

/** One-use reset permission bound to a hashed session identifier. */
@Entity
@Table(name = "ResetGrant", schema = "dbo")
public class ResetGrantRecord {
    @Id
    @Column(columnDefinition = "uniqueidentifier")
    private UUID id;

    @Column(nullable = false, columnDefinition = "uniqueidentifier")
    private UUID userId;

    @Nationalized
    @Column(nullable = false, length = 255)
    private String sessionBindingHash;

    @Column(nullable = false, columnDefinition = "datetime2")
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "datetime2")
    private Instant expiresAt;

    @Column(columnDefinition = "datetime2")
    private Instant consumedAt;

    protected ResetGrantRecord() {}

    public static ResetGrantRecord create(
            UUID id, UUID userId, String bindingHash, Instant now, Instant expiresAt) {
        if (bindingHash == null || bindingHash.isBlank() || bindingHash.length() > 255)
            throw new IllegalArgumentException("Session binding hash required");
        if (now == null || expiresAt == null || !expiresAt.isAfter(now))
            throw new IllegalArgumentException("Invalid reset grant lifetime");
        var result = new ResetGrantRecord();
        result.id = Objects.requireNonNull(id);
        result.userId = Objects.requireNonNull(userId);
        result.sessionBindingHash = bindingHash;
        result.createdAt = now;
        result.expiresAt = expiresAt;
        return result;
    }

    public UUID getId() {
        return id;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
