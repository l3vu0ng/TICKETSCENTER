package vn.ticketscenter.controller.common;

import java.util.Set;

/** Accepts only known internal destinations; encoded paths are never decoded twice. */
public final class ReturnToValidator {
  private static final Set<String> PATHS =
      Set.of(
          "/events",
          "/me",
          "/me/profile",
          "/me/orders",
          "/me/tickets",
          "/me/refund-requests",
          "/me/memberships",
          "/me/organization-requests",
          "/organizations",
          "/admin",
          "/checkin");

  private ReturnToValidator() {}

  public static String validate(String candidate) {
    return candidate != null && PATHS.contains(candidate) ? candidate : "/events";
  }
}
