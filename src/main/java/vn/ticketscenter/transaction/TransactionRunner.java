package vn.ticketscenter.transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.Objects;
import java.util.function.Function;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.dto.common.ActorType;
import vn.ticketscenter.model.identity.PlatformRole;

/** Joins nested calls and marks the outer transaction rollback-only on failure. */
public final class TransactionRunner {
    public interface Scope extends AutoCloseable {
        EntityManager entityManager();

        @Override
        void close();
    }

    @FunctionalInterface
    public interface ScopeFactory {
        Scope open(PrincipalKind principal, ActorContext actor);
    }

    private final ScopeFactory scopes;

    public TransactionRunner(ScopeFactory scopes) {
        this.scopes = Objects.requireNonNull(scopes);
    }

    public <T> T required(
            PrincipalKind principal, ActorContext actor, Function<EntityManager, T> work) {
        Objects.requireNonNull(principal);
        Objects.requireNonNull(actor);
        Objects.requireNonNull(work);
        TransactionContext nested = TransactionContext.current();
        if (nested != null) {
            nested.requireSame(principal, actor);
            try {
                return work.apply(nested.entityManager);
            } catch (RuntimeException | Error ex) {
                nested.rollbackOnly = true;
                throw ex;
            }
        }
        validatePrincipal(principal, actor);
        try (Scope scope = scopes.open(principal, actor)) {
            EntityManager em = scope.entityManager();
            EntityTransaction transaction = em.getTransaction();
            TransactionContext context = new TransactionContext(principal, actor, em);
            transaction.begin();
            TransactionContext.bind(context);
            try {
                T result = work.apply(em);
                if (result instanceof EntityManager)
                    throw new IllegalStateException("EntityManager must remain in its scope");
                if (context.rollbackOnly || transaction.getRollbackOnly()) {
                    throw new IllegalStateException("Transaction is rollback-only");
                }
                em.flush();
                transaction.commit();
                return result;
            } catch (RuntimeException | Error ex) {
                try {
                    if (transaction.isActive()) transaction.rollback();
                } catch (RuntimeException rollbackFailure) {
                    ex.addSuppressed(rollbackFailure);
                }
                throw ex;
            } finally {
                TransactionContext.clear();
            }
        }
    }

    private static void validatePrincipal(PrincipalKind principal, ActorContext actor) {
        if (principal == PrincipalKind.WORKER_TECH && actor.type() != ActorType.SYSTEM
                || principal != PrincipalKind.WORKER_TECH
                        && principal != PrincipalKind.AUTH_TECH
                        && actor.type() != ActorType.USER
                || principal == PrincipalKind.PLATFORM_ADMIN
                        && actor.platformRole() != PlatformRole.ADMIN) {
            throw new IllegalArgumentException("Actor cannot use this database principal");
        }
    }
}
