package vn.ticketscenter.service.identity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.model.identity.UserStatus;
import vn.ticketscenter.repository.identity.UserRepository;
import vn.ticketscenter.transaction.PrincipalKind;
import vn.ticketscenter.transaction.TransactionRunner;

/** Revalidates account status and authVersion on every authenticated request. */
public final class AuthService {
  private final TransactionRunner transactions;
  private final UserRepository users;

  public AuthService(TransactionRunner transactions, UserRepository users) {
    this.transactions = transactions;
    this.users = users;
  }

  public ActorContext requireCurrentUser(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session == null
        || !(session.getAttribute("userId") instanceof UUID id)
        || !(session.getAttribute("authVersion") instanceof Integer version))
      throw BusinessException.unauthorized("Session is missing or expired");
    return transactions.required(
        PrincipalKind.AUTH_TECH,
        ActorContext.system(),
        em -> {
          var user =
              users
                  .findById(em, id)
                  .orElseThrow(
                      () -> BusinessException.unauthorized("Session is missing or expired"));
          if (user.getStatus() != UserStatus.ACTIVE || user.getAuthVersion() != version) {
            session.invalidate();
            throw BusinessException.unauthorized("Session is missing or expired");
          }
          return ActorContext.ofUser(id, user.getPlatformRole(), version, user.isEmailVerified());
        });
  }
}
