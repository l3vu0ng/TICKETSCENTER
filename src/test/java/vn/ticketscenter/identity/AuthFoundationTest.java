package vn.ticketscenter.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.servlet.http.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.model.identity.*;
import vn.ticketscenter.repository.identity.UserRepository;
import vn.ticketscenter.service.identity.*;
import vn.ticketscenter.support.MutableClock;
import vn.ticketscenter.transaction.*;

class AuthFoundationTest {
    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final EntityManager em = mock(EntityManager.class);
    private final EntityTransaction tx = mock(EntityTransaction.class);
    private final UserRepository users = mock(UserRepository.class);

    private TransactionRunner runner() {
        when(em.getTransaction()).thenReturn(tx);
        when(tx.isActive()).thenReturn(true);
        return new TransactionRunner(
                (principal, actor) ->
                        new TransactionRunner.Scope() {
                            public EntityManager entityManager() {
                                return em;
                            }

                            public void close() {
                                em.close();
                            }
                        });
    }

    private User user() {
        return User.createCustomer(
                id,
                "buyer.a@example.test",
                "buyer.a@example.test",
                "buyer_a",
                "buyer_a",
                "Buyer A",
                null,
                "test-encoded-hash",
                MutableClock.BASE_TIME);
    }

    @Test
    void currentSessionReadsUserAndRejectsStaleVersion() {
        var user = user();
        when(users.findById(em, id)).thenReturn(Optional.of(user));
        var request = mock(HttpServletRequest.class);
        var session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(id);
        when(session.getAttribute("authVersion")).thenReturn(1);
        var auth = new AuthService(runner(), users);
        assertEquals(id, auth.requireCurrentUser(request).actorId());
        verify(users).findById(em, id);
        when(session.getAttribute("authVersion")).thenReturn(2);
        assertEquals(
                401,
                assertThrows(BusinessException.class, () -> auth.requireCurrentUser(request))
                        .getHttpStatus());
        verify(session).invalidate();
    }

    @Test
    void missingSessionDoesNotOpenDatabaseTransaction() {
        var request = mock(HttpServletRequest.class);
        assertThrows(
                BusinessException.class,
                () -> new AuthService(runner(), users).requireCurrentUser(request));
        verifyNoInteractions(users);
    }

    @Test
    void membershipGuardReceivesCurrentEntityManagerAndRevocationIsEffective() {
        var user = user();
        when(users.findById(em, id)).thenReturn(Optional.of(user));
        var guard = mock(AuthorizationService.MembershipGuard.class);
        var actor = ActorContext.ofUser(id, PlatformRole.CUSTOMER, 1, false);
        var roles = java.util.Set.of(OrganizationRole.MANAGER);
        when(guard.allows(em, id, id, roles)).thenReturn(true, false);
        var authorization = new AuthorizationService(users, guard);
        var runner = runner();
        runner.required(
                PrincipalKind.MANAGER,
                actor,
                current -> {
                    authorization.requireOrganizationRole(actor, id, roles);
                    return null;
                });
        assertThrows(
                BusinessException.class,
                () ->
                        runner.required(
                                PrincipalKind.MANAGER,
                                actor,
                                current -> {
                                    authorization.requireOrganizationRole(actor, id, roles);
                                    return null;
                                }));
    }

    @Test
    void systemCannotBecomeAdminOrBorrowUserTransaction() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ActorContext(
                                null,
                                PlatformRole.ADMIN,
                                0,
                                false,
                                vn.ticketscenter.dto.common.ActorType.SYSTEM));
        var authorization = new AuthorizationService(users, null);
        var actor = ActorContext.ofUser(id, PlatformRole.CUSTOMER, 1, false);
        assertThrows(
                BusinessException.class,
                () ->
                        runner().required(
                                        PrincipalKind.MANAGER,
                                        actor,
                                        current -> {
                                            authorization.requireAdmin(ActorContext.system());
                                            return null;
                                        }));
    }
}
