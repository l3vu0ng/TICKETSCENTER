package vn.ticketscenter.model.event;

import vn.ticketscenter.model.identity.Organization;
import vn.ticketscenter.model.settlement.CommissionRule;
import vn.ticketscenter.model.settlement.Settlement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Event {
    private UUID id;
    private Organization organization;
    private String title;
    private String description;
    private EventCategory category;
    private String venueName;
    private String venueAddress;
    private String coverImageUrl;
    private Instant saleStart;
    private Instant saleEnd;
    private Instant startTime;
    private Instant endTime;
    private EventStatus status;
    private String rejectionReason;
    private CommissionRule commissionRule;
    private List<Zone> zones = new ArrayList<>();
    private Settlement settlement;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant decidedAt;

    protected Event() {
    }

    public static Event draft(UUID id, Organization organization, EventDetails details,
                              EventSchedule schedule, Instant createdAt) {
        Event result = new Event();
        result.id = Objects.requireNonNull(id, "id");
        result.organization = Objects.requireNonNull(organization, "organization");
        result.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        result.status = EventStatus.DRAFT;
        result.updateDetails(details);
        result.configureSchedule(schedule);
        return result;
    }

    public void updateDetails(EventDetails details) {
        requireEditable();
        Objects.requireNonNull(details, "details");
        title = details.title();
        description = details.description();
        category = details.category();
        venueName = details.venueName();
        venueAddress = details.venueAddress();
        coverImageUrl = details.coverImageUrl();
    }

    public void configureSchedule(EventSchedule schedule) {
        requireEditable();
        Objects.requireNonNull(schedule, "schedule");
        saleStart = schedule.saleStart();
        saleEnd = schedule.saleEnd();
        startTime = schedule.startTime();
        endTime = schedule.endTime();
    }

    public void addZone(Zone zone) {
        requireEditable();
        Objects.requireNonNull(zone, "zone");
        if (!id.equals(zone.getEvent().getId())) {
            throw new IllegalArgumentException("Zone belongs to another event");
        }
        if (zones.stream().anyMatch(current -> current.getId().equals(zone.getId()))) {
            throw new IllegalStateException("Zone is already attached");
        }
        zone.attachTo(this);
        zones.add(zone);
    }

    public void removeZone(Zone zone) {
        requireEditable();
        Objects.requireNonNull(zone, "zone");
        Zone owned = zones.stream().filter(current -> current.getId().equals(zone.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Zone does not belong to this event"));
        if (owned.hasAllocation()) {
            throw new IllegalStateException("Allocated inventory cannot be removed");
        }
        // Physical deletion and checks for historical references belong to the transactional repository.
        zones.remove(owned);
    }

    public void submitForApproval() {
        requireEditable();
        requireComplete();
        status = EventStatus.PENDING_APPROVAL;
        rejectionReason = null;
    }

    public void publish(CommissionRule rule, Instant now) {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(now, "now");
        if (status == EventStatus.PUBLISHED && commissionRule.getId().equals(rule.getId())) {
            return;
        }
        if (status != EventStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Publication requires a pending event");
        }
        requireComplete();
        if (rule.getOrganization() == null || !organization.getId().equals(rule.getOrganization().getId())
                || !rule.isEffectiveAt(now)) {
            throw new IllegalArgumentException("Commission rule must be effective and belong to the event organization");
        }
        commissionRule = rule;
        status = EventStatus.PUBLISHED;
        rejectionReason = null;
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank() || reason.strip().length() > 2000) {
            throw new IllegalArgumentException("A rejection reason of at most 2000 characters is required");
        }
        String normalized = reason.strip();
        if (status == EventStatus.REJECTED && normalized.equals(rejectionReason)) {
            return;
        }
        if (status != EventStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only a pending event can be rejected");
        }
        status = EventStatus.REJECTED;
        rejectionReason = normalized;
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status == EventStatus.CANCELLED) {
            return;
        }
        if (status != EventStatus.PUBLISHED || !now.isBefore(startTime)) {
            throw new IllegalStateException("Cancellation requires a published event before startTime");
        }
        status = EventStatus.CANCELLED;
    }

    public boolean isSaleActive(Instant now) {
        Objects.requireNonNull(now, "now");
        return status == EventStatus.PUBLISHED && !now.isBefore(saleStart) && now.isBefore(saleEnd);
    }

    public boolean isCheckInOpen(Instant now) {
        Objects.requireNonNull(now, "now");
        return status == EventStatus.PUBLISHED && !now.isBefore(startTime.minusSeconds(3600)) && now.isBefore(endTime);
    }

    private void requireComplete() {
        new EventSchedule(saleStart, saleEnd, startTime, endTime);
        if (coverImageUrl == null || coverImageUrl.isBlank() || category == null
                || zones.isEmpty() || zones.stream().anyMatch(zone -> !zone.isConfigured())) {
            throw new IllegalStateException("Approval requires a cover, category and configured zones");
        }
    }

    private void requireEditable() {
        if (status != EventStatus.DRAFT && status != EventStatus.REJECTED) {
            throw new IllegalStateException("Only draft or rejected events can be edited");
        }
    }

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public EventCategory getCategory() { return category; }
    public String getVenueName() { return venueName; }
    public String getVenueAddress() { return venueAddress; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public Instant getSaleStart() { return saleStart; }
    public Instant getSaleEnd() { return saleEnd; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public EventStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public CommissionRule getCommissionRule() { return commissionRule; }
    public List<Zone> getZones() { return List.copyOf(zones); }
    public Settlement getSettlement() { return settlement; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
