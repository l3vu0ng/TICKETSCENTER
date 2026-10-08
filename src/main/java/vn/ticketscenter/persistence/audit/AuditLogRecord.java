package vn.ticketscenter.persistence.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/** Append-only audit projection. SQL/service must sanitize detail before insert. */
@Entity @Immutable @Table(name = "AuditLog", schema = "dbo")
public class AuditLogRecord {
    public enum Source { USER, SYSTEM }
    @Id @Column(nullable = false, columnDefinition = "uniqueidentifier") private UUID id;
    @Column(columnDefinition = "uniqueidentifier") private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Source source;
    @Column(nullable = false, length = 80) private String action;
    @Column(nullable = false, length = 80) private String aggregateType;
    @Column(nullable = false, columnDefinition = "uniqueidentifier") private UUID aggregateId;
    @Nationalized @Column(columnDefinition = "nvarchar(max)") private String detail;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(nullable = false, columnDefinition = "datetime2(7)") private Instant createdAt;
    protected AuditLogRecord() { }
    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public Source getSource() { return source; }
    public String getAction() { return action; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
