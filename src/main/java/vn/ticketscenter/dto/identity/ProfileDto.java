package vn.ticketscenter.dto.identity;

import vn.ticketscenter.dto.common.Page;

/** Profile DTO: user + memberships. MembershipDto owned by Đông. */
public record ProfileDto(UserDto user, Page<?> memberships) {}
