package vn.ticketscenter.dto.identity;

import java.util.UUID;
import vn.ticketscenter.model.identity.PlatformRole;
import vn.ticketscenter.model.identity.User;
import vn.ticketscenter.model.identity.UserStatus;

/** Safe user DTO — no secrets. */
public record UserDto(
        UUID id,
        String userName,
        String fullName,
        String email,
        String phone,
        UserStatus status,
        boolean emailVerified,
        PlatformRole platformRole) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getUserName(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getStatus(),
                u.isEmailVerified(),
                u.getPlatformRole());
    }
}
