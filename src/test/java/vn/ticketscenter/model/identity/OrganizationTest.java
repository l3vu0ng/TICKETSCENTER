package vn.ticketscenter.model.identity;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OrganizationTest {
    private Organization draft(User requester) {
        return Organization.draft(UUID.randomUUID(), " Organizer ", "contact@example.test", null, null,
                requester, Instant.parse("2026-10-06T03:00:00Z"));
    }

    @Test
    void requesterIsNullableOnlyUntilSubmission() {
        Organization organization = draft(null);
        assertThrows(IllegalStateException.class, organization::submitForApproval);
        assertEquals(OrganizationStatus.DRAFT, organization.getStatus());
    }

    @Test
    void approvalIsFinalAndRepeatDoesNotChangeIt() {
        Organization organization = draft(mock(User.class));
        assertEquals("Organizer", organization.getName());
        organization.submitForApproval();
        organization.approve();
        organization.approve();
        assertEquals(OrganizationStatus.APPROVED, organization.getStatus());
        assertThrows(IllegalStateException.class, () -> organization.reject("reason"));
        assertThrows(IllegalStateException.class, organization::submitForApproval);
    }

    @Test
    void rejectionRequiresAReasonAndPreservesTheFinalDecision() {
        Organization organization = draft(mock(User.class));
        organization.submitForApproval();
        assertThrows(IllegalArgumentException.class, () -> organization.reject(" "));
        assertEquals(OrganizationStatus.PENDING_APPROVAL, organization.getStatus());
        organization.reject(" reason ");
        organization.reject("reason");
        assertEquals("reason", organization.getRejectionReason());
        assertThrows(IllegalStateException.class, organization::approve);
        assertThrows(IllegalStateException.class, () -> organization.reject("different"));
        assertThrows(IllegalStateException.class, organization::submitForApproval);
    }
}
