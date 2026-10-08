package vn.ticketscenter.model.identity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for User domain methods. */
class UserTest {

    private User makeUser() {
        return User.createCustomer(
                UUID.randomUUID(),
                "buyer.a@example.test",
                "buyer.a@example.test",
                "buyer_a",
                "buyer_a",
                "Người mua A",
                null,
                "$2a$12$hashedpassword",
                Instant.parse("2026-10-06T03:00:00Z"));
    }

    @Test
    void newUser_isNotEligibleBuyer_emailNotVerified() {
        assertFalse(makeUser().isEligibleBuyer());
    }

    @Test
    void verifyEmail_makesUserEligible() {
        User u = makeUser();
        u.verifyEmail();
        assertTrue(u.isEligibleBuyer());
    }

    @Test
    void verifyEmail_isIdempotent() {
        User u = makeUser();
        u.verifyEmail();
        u.verifyEmail(); // second call — no exception
        assertTrue(u.isEmailVerified());
    }

    @Test
    void updateProfile_updatesNameAndPhone() {
        User u = makeUser();
        u.updateProfile("Tên mới", "+84912345678");
        assertEquals("Tên mới", u.getFullName());
        assertEquals("+84912345678", u.getPhone());
    }

    @Test
    void updateProfile_phoneNullable() {
        User u = makeUser();
        u.updateProfile("Tên mới", null);
        assertNull(u.getPhone());
    }

    @Test
    void updateProfile_blankName_throwsException() {
        User u = makeUser();
        assertThrows(IllegalArgumentException.class, () -> u.updateProfile("  ", null));
    }

    @Test
    void assignRole_addsRole() {
        User u = makeUser();
        Object fakeOrg = new Object();
        u.assignRole(fakeOrg, OrganizationRole.MANAGER);
        assertEquals(OrganizationRole.MANAGER, u.getOrganizationRoles().get(fakeOrg));
    }

    @Test
    void assignRole_replacesExistingRole() {
        User u = makeUser();
        Object org = new Object();
        u.assignRole(org, OrganizationRole.MANAGER);
        u.assignRole(org, OrganizationRole.CHECK_IN_STAFF);
        assertEquals(OrganizationRole.CHECK_IN_STAFF, u.getOrganizationRoles().get(org));
    }

    @Test
    void revokeRole_removesRole() {
        User u = makeUser();
        Object org = new Object();
        u.assignRole(org, OrganizationRole.MANAGER);
        u.revokeRole(org);
        assertFalse(u.getOrganizationRoles().containsKey(org));
    }

    @Test
    void twoOrgs_independentRoles() {
        User u = makeUser();
        Object orgA = new Object();
        Object orgB = new Object();
        u.assignRole(orgA, OrganizationRole.MANAGER);
        u.assignRole(orgB, OrganizationRole.CHECK_IN_STAFF);
        u.revokeRole(orgA);
        assertFalse(u.getOrganizationRoles().containsKey(orgA));
        assertEquals(OrganizationRole.CHECK_IN_STAFF, u.getOrganizationRoles().get(orgB));
    }

    @Test
    void newUser_statusIsActive_platformIsCustomer() {
        User u = makeUser();
        assertEquals(UserStatus.ACTIVE, u.getStatus());
        assertEquals(PlatformRole.CUSTOMER, u.getPlatformRole());
    }

    @Test
    void newUser_authVersionIs1() {
        assertEquals(1, makeUser().getAuthVersion());
    }

    @Test
    void disabledUser_isNotEligibleBuyer() {
        // Eligibility requires ACTIVE — disabled user can't buy
        User u = makeUser();
        u.verifyEmail();
        try {
            var status = User.class.getDeclaredField("status");
            status.setAccessible(true);
            status.set(u, UserStatus.DISABLED);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
        assertFalse(u.isEligibleBuyer());
    }

    @Test
    void invalidProfileDoesNotPartiallyMutateName() {
        User u = makeUser();
        assertThrows(
                IllegalArgumentException.class, () -> u.updateProfile("New name", "not-a-phone"));
        assertEquals("Người mua A", u.getFullName());
        assertThrows(IllegalArgumentException.class, () -> u.updateProfile("x".repeat(121), null));
    }
}
