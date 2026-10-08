package vn.ticketscenter.persistence.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import vn.ticketscenter.model.identity.Organization;
import vn.ticketscenter.model.identity.OrganizationRole;
import vn.ticketscenter.model.identity.User;

/** Persistence representation of User.organizationRoles and inactive membership history. */
@Entity
@Table(name = "UserOrganizationRole", uniqueConstraints = @UniqueConstraint(
        name = "UQ_UserOrganizationRole_User_Organization", columnNames = {"userId", "organizationId"}))
public class UserOrganizationRole {
    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uniqueidentifier")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizationId", nullable = false, updatable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private OrganizationRole role;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "createdAt", nullable = false, updatable = false, columnDefinition = "datetime2")
    private Instant createdAt;

    @Column(name = "updatedAt", nullable = false, columnDefinition = "datetime2")
    private Instant updatedAt;

    protected UserOrganizationRole() {
    }

    public UserOrganizationRole(UUID id, User user, Organization organization, OrganizationRole role,
                                boolean active, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.user = Objects.requireNonNull(user, "user");
        this.organization = Objects.requireNonNull(organization, "organization");
        this.role = Objects.requireNonNull(role, "role");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Organization getOrganization() { return organization; }
    public OrganizationRole getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
