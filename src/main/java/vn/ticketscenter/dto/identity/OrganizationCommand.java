package vn.ticketscenter.dto.identity;


public record OrganizationCommand(
        String name,
        String contactEmail,
        String contactPhone,
        String description) {
}
