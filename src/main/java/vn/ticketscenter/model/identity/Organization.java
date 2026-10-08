package vn.ticketscenter.model.identity;

import vn.ticketscenter.model.event.Event;
import vn.ticketscenter.model.order.Coupon;
import vn.ticketscenter.model.settlement.CommissionRule;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Organization {
    private UUID id;
    private String name;
    private String contactEmail;
    private String contactPhone;
    private String description;
    private User requester;
    private OrganizationStatus status;
    private String rejectionReason;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant decidedAt;
    private List<Event> events = new ArrayList<>();
    private List<Coupon> coupons = new ArrayList<>();
    private List<CommissionRule> commissionRules = new ArrayList<>();

    protected Organization() {
    }

    /** contactEmail must already have been normalized by Khánh's shared helper. */
    public static Organization draft(UUID id, String name, String contactEmail, String contactPhone,
                                     String description, User requester, Instant createdAt) {
        Organization result = new Organization();
        result.id = Objects.requireNonNull(id, "id");
        result.name = required(name, "name", 200);
        result.contactEmail = required(contactEmail, "contactEmail", 254);
        result.contactPhone = optional(contactPhone, "contactPhone", 32);
        result.description = optional(description, "description", 2000);
        result.requester = requester;
        result.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        result.status = OrganizationStatus.DRAFT;
        return result;
    }

    public void submitForApproval() {
        if (status != OrganizationStatus.DRAFT || requester == null) {
            throw new IllegalStateException("Submission requires a draft organization with a requester");
        }
        status = OrganizationStatus.PENDING_APPROVAL;
    }

    public void approve() {
        if (status == OrganizationStatus.APPROVED) {
            return;
        }
        requirePending();
        status = OrganizationStatus.APPROVED;
        rejectionReason = null;
    }

    public void reject(String reason) {
        String normalized = required(reason, "reason", 2000);
        if (status == OrganizationStatus.REJECTED && normalized.equals(rejectionReason)) {
            return;
        }
        requirePending();
        rejectionReason = normalized;
        status = OrganizationStatus.REJECTED;
    }

    private void requirePending() {
        if (status != OrganizationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only a pending organization can be decided");
        }
    }

    private static String required(String value, String field, int maximum) {
        String normalized = optional(value, field, maximum);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String optional(String value, String field, int maximum) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        if (normalized.length() > maximum) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return normalized;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public String getDescription() { return description; }
    public User getRequester() { return requester; }
    public OrganizationStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public List<Event> getEvents() { return List.copyOf(events); }
    public List<Coupon> getCoupons() { return List.copyOf(coupons); }
    public List<CommissionRule> getCommissionRules() { return List.copyOf(commissionRules); }
}
