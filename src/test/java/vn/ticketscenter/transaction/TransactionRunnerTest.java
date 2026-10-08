package vn.ticketscenter.transaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.model.identity.PlatformRole;

class TransactionRunnerTest {
    private final EntityManager em = mock(EntityManager.class);
    private final EntityTransaction tx = mock(EntityTransaction.class);
    private final TransactionRunner.Scope scope = mock(TransactionRunner.Scope.class);
    private final TransactionRunner.ScopeFactory scopes =
            mock(TransactionRunner.ScopeFactory.class);
    private final ActorContext actor =
            ActorContext.ofUser(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    PlatformRole.CUSTOMER,
                    1,
                    true);
    private final TransactionRunner runner = new TransactionRunner(scopes);

    @BeforeEach
    void setup() {
        when(scope.entityManager()).thenReturn(em);
        when(scopes.open(any(), any())).thenReturn(scope);
        when(em.getTransaction()).thenReturn(tx);
        when(tx.isActive()).thenReturn(true);
    }

    @Test
    void nestedSuccessUsesSameEntityManagerAndCommitsOnce() {
        assertEquals(
                "done",
                runner.required(
                        PrincipalKind.BUYER,
                        actor,
                        current ->
                                runner.required(
                                        PrincipalKind.BUYER,
                                        actor,
                                        nested -> {
                                            assertSame(current, nested);
                                            return "done";
                                        })));
        var order = inOrder(tx, em, scope);
        order.verify(tx).begin();
        order.verify(em).flush();
        order.verify(tx).commit();
        order.verify(scope).close();
        verify(scopes, times(1)).open(any(), any());
        assertThrows(IllegalStateException.class, TransactionContext::entityManager);
    }

    @Test
    void failureAfterMutationRollsBackAndCloses() {
        var entity = new Object();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        runner.required(
                                PrincipalKind.BUYER,
                                actor,
                                current -> {
                                    current.persist(entity);
                                    throw new IllegalArgumentException("test failure");
                                }));
        verify(tx).rollback();
        verify(tx, never()).commit();
        verify(scope).close();
        assertThrows(IllegalStateException.class, TransactionContext::entityManager);
    }

    @Test
    void caughtNestedFailureStillRollsBackOuterTransaction() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        runner.required(
                                PrincipalKind.BUYER,
                                actor,
                                current -> {
                                    assertThrows(
                                            IllegalArgumentException.class,
                                            () ->
                                                    runner.required(
                                                            PrincipalKind.BUYER,
                                                            actor,
                                                            nested -> {
                                                                throw new IllegalArgumentException();
                                                            }));
                                    return null;
                                }));
        verify(tx).rollback();
        verify(tx, never()).commit();
    }

    @Test
    void principalChangeIsRollbackOnlyEvenIfCaught() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        runner.required(
                                PrincipalKind.BUYER,
                                actor,
                                current -> {
                                    assertThrows(
                                            IllegalStateException.class,
                                            () ->
                                                    runner.required(
                                                            PrincipalKind.MANAGER,
                                                            actor,
                                                            nested -> null));
                                    return null;
                                }));
        verify(tx).rollback();
    }

    @Test
    void flushFailureRollsBack() {
        doThrow(new IllegalStateException()).when(em).flush();
        assertThrows(
                IllegalStateException.class,
                () -> runner.required(PrincipalKind.BUYER, actor, current -> null));
        verify(tx).rollback();
        verify(scope).close();
    }

    @Test
    void actorCannotUseAdminOrWorkerPrincipal() {
        assertThrows(
                IllegalArgumentException.class,
                () -> runner.required(PrincipalKind.PLATFORM_ADMIN, actor, current -> null));
        assertThrows(
                IllegalArgumentException.class,
                () -> runner.required(PrincipalKind.WORKER_TECH, actor, current -> null));
        verifyNoInteractions(scopes);
    }
}
