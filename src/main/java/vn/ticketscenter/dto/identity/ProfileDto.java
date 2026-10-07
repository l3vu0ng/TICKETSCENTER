package vn.ticketscenter.dto.identity;

import vn.ticketscenter.dto.common.Page;

/** Profile DTO: user + memberships. MembershipDto owned by Đông. Owner: Khánh (KHANH-13) */
public record ProfileDto(UserDto user, Page<?> memberships) {}
