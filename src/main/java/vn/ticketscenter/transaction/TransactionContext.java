package vn.ticketscenter.transaction;

import jakarta.persistence.EntityManager;
import java.util.Objects;
import vn.ticketscenter.dto.common.ActorContext;

/** Thread-confined state for one resource-local transaction. */
public final class TransactionContext {
  private static final ThreadLocal<TransactionContext> CURRENT = new ThreadLocal<>();
  final PrincipalKind principal;
  final ActorContext actor;
  final EntityManager entityManager;
  boolean rollbackOnly;

  TransactionContext(PrincipalKind principal, ActorContext actor, EntityManager entityManager) {
    this.principal = principal;
    this.actor = actor;
    this.entityManager = entityManager;
  }

  static TransactionContext current() {
    return CURRENT.get();
  }

  static void bind(TransactionContext context) {
    CURRENT.set(context);
  }

  static void clear() {
    CURRENT.remove();
  }

  public static EntityManager entityManager() {
    TransactionContext context = CURRENT.get();
    if (context == null) throw new IllegalStateException("No active transaction");
    return context.entityManager;
  }

  public static ActorContext actor() {
    TransactionContext context = CURRENT.get();
    if (context == null) throw new IllegalStateException("No active transaction");
    return context.actor;
  }

  void requireSame(PrincipalKind requestedPrincipal, ActorContext requestedActor) {
    if (principal != requestedPrincipal || !Objects.equals(actor, requestedActor)) {
      rollbackOnly = true;
      throw new IllegalStateException("Nested transaction cannot change principal or actor");
    }
  }
}
