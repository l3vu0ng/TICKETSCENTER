package vn.ticketscenter.model.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;
import vn.ticketscenter.service.identity.IdentityNormalizer;

/** Identity state and profile invariants. */
@Entity
@Table(
    name = "[User]",
    schema = "dbo",
    uniqueConstraints = {
      @UniqueConstraint(name = "UQ_User_NormalizedEmail", columnNames = "normalizedEmail"),
      @UniqueConstraint(name = "UQ_User_NormalizedUserName", columnNames = "normalizedUserName")
    })
public class User {

  @Id
  @Column(name = "id", columnDefinition = "uniqueidentifier", updatable = false, nullable = false)
  private UUID id;

  @Nationalized
  @Column(name = "email", nullable = false, length = 254)
  private String email;

  @Nationalized
  @Column(name = "normalizedEmail", nullable = false, length = 254)
  private String normalizedEmail;

  @Nationalized
  @Column(name = "userName", nullable = false, length = 32)
  private String userName;

  @Nationalized
  @Column(name = "normalizedUserName", nullable = false, length = 32)
  private String normalizedUserName;

  @Nationalized
  @Column(name = "fullName", nullable = false, length = 120)
  private String fullName;

  @Nationalized
  @Column(name = "phone", length = 20)
  private String phone;

  @Nationalized
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserStatus status;

  @Column(name = "emailVerified", nullable = false)
  private boolean emailVerified;

  @Nationalized
  @Enumerated(EnumType.STRING)
  @Column(name = "platformRole", nullable = false, length = 20)
  private PlatformRole platformRole;

  /** Encoded password hash; never serialized as a DTO field. */
  @Nationalized
  @Column(name = "passwordHash", nullable = false)
  private String passwordHash;

  /** Incremented on password reset; stale sessions are rejected. */
  @Column(name = "authVersion", nullable = false)
  private int authVersion;

  @Column(name = "createdAt", nullable = false, updatable = false, columnDefinition = "datetime2")
  private Instant createdAt;

  @Version
  @Column(name = "version", nullable = false)
  private int version;

  /** DONG-02 supplies the persistent mapping; this transient map is not authorization data. */
  @Transient private Map<Object, OrganizationRole> organizationRoles = new HashMap<>();

  protected User() {}

  /** Factory: creates CUSTOMER ACTIVE unverified user. */
  public static User createCustomer(
      UUID id,
      String email,
      String normalizedEmail,
      String userName,
      String normalizedUserName,
      String fullName,
      String phone,
      String passwordHash,
      Instant now) {
    java.util.Objects.requireNonNull(id, "id");
    java.util.Objects.requireNonNull(now, "now");
    if (!IdentityNormalizer.email(email).equals(normalizedEmail)
        || !IdentityNormalizer.userName(userName).equals(normalizedUserName)) {
      throw new IllegalArgumentException("Identity normalization mismatch");
    }
    if (passwordHash == null || passwordHash.isBlank() || passwordHash.length() > 255) {
      throw new IllegalArgumentException("Encoded password hash required");
    }
    fullName = IdentityNormalizer.fullName(fullName);
    phone = IdentityNormalizer.phone(phone);
    User u = new User();
    u.id = id;
    u.email = email;
    u.normalizedEmail = normalizedEmail;
    u.userName = userName;
    u.normalizedUserName = normalizedUserName;
    u.fullName = fullName;
    u.phone = phone;
    u.status = UserStatus.ACTIVE;
    u.emailVerified = false;
    u.platformRole = PlatformRole.CUSTOMER;
    u.passwordHash = passwordHash;
    u.authVersion = 1;
    u.createdAt = now;
    return u;
  }

  public void updateProfile(String fullName, String phone) {
    String validatedName = IdentityNormalizer.fullName(fullName);
    String validatedPhone = IdentityNormalizer.phone(phone);
    this.fullName = validatedName;
    this.phone = validatedPhone;
  }

  /** Idempotent — safe to call again when already verified. */
  public void verifyEmail() {
    this.emailVerified = true;
  }

  public void assignRole(Object org, OrganizationRole role) {
    if (org == null || role == null) throw new IllegalArgumentException("org/role required");
    organizationRoles.put(org, role);
  }

  public void revokeRole(Object org) {
    if (org == null) throw new IllegalArgumentException("org required");
    organizationRoles.remove(org);
  }

  public boolean isEligibleBuyer() {
    return status == UserStatus.ACTIVE && emailVerified;
  }

  void updatePasswordHash(String newHash) {
    this.passwordHash = newHash;
    this.authVersion++;
  }

  void elevateToAdmin() {
    this.platformRole = PlatformRole.ADMIN;
    this.emailVerified = true;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  public String getUserName() {
    return userName;
  }

  public String getNormalizedUserName() {
    return normalizedUserName;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPhone() {
    return phone;
  }

  public UserStatus getStatus() {
    return status;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public PlatformRole getPlatformRole() {
    return platformRole;
  }

  public int getAuthVersion() {
    return authVersion;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Map<Object, OrganizationRole> getOrganizationRoles() {
    return Collections.unmodifiableMap(organizationRoles);
  }

  String getPasswordHash() {
    return passwordHash;
  }
}
