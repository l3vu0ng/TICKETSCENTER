package vn.ticketscenter.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.config.PersistenceFactory;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.model.identity.PlatformRole;
import vn.ticketscenter.support.*;
import vn.ticketscenter.transaction.PrincipalKind;

class KhanhTransactionIT {
  @Test
  void concurrentConnectionsKeepIndependentActorsAndCleanupOnReuse() throws Exception {
    try (var persistence = new PersistenceFactory(SqlServerTestSupport.config());
        var workers = Executors.newFixedThreadPool(2)) {
      var barrier = new CyclicBarrier(2);
      var runner = persistence.transactionRunner();
      var first =
          workers.submit(() -> checkActor(runner, "00000000-0000-0000-0000-000000000001", barrier));
      var second =
          workers.submit(() -> checkActor(runner, "00000000-0000-0000-0000-000000000002", barrier));
      assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
      runner.required(
          PrincipalKind.AUTH_TECH,
          ActorContext.system(),
          em -> {
            assertNull(
                em.createNativeQuery("SELECT CONVERT(nvarchar(36), SESSION_CONTEXT(N'actorId'))")
                    .getSingleResult());
            return null;
          });
    }
  }

  private int checkActor(
      vn.ticketscenter.transaction.TransactionRunner runner,
      String identity,
      CyclicBarrier barrier) {
    var actor = ActorContext.ofUser(UUID.fromString(identity), PlatformRole.CUSTOMER, 1, true);
    return runner.required(
        PrincipalKind.BUYER,
        actor,
        em -> {
          int connection =
              ((Number) em.createNativeQuery("SELECT @@SPID").getSingleResult()).intValue();
          ConcurrentTestSupport.meet(barrier);
          assertEquals(
              identity,
              em.createNativeQuery("SELECT CONVERT(nvarchar(36), SESSION_CONTEXT(N'actorId'))")
                  .getSingleResult());
          return connection;
        });
  }

  @Test
  void failureAfterStoredProcedureWriteRollsBackOuterTransaction() {
    try (var persistence = new PersistenceFactory(SqlServerTestSupport.config())) {
      var runner = persistence.transactionRunner();
      var id = UUID.fromString("00000000-0000-0000-0000-000000000003");
      String original =
          runner.required(
              PrincipalKind.AUTH_TECH,
              ActorContext.system(),
              em ->
                  (String)
                      em.createNativeQuery("SELECT fullName FROM dbo.[User] WHERE id=:id")
                          .setParameter("id", id)
                          .getSingleResult());
      assertThrows(
          TestRollback.class,
          () ->
              runner.required(
                  PrincipalKind.AUTH_TECH,
                  ActorContext.system(),
                  em -> {
                    em.createNativeQuery("EXEC dbo.tc_test_profile_write @id=:id, @name=:name")
                        .setParameter("id", id)
                        .setParameter("name", "M0 SP rollback")
                        .executeUpdate();
                    assertEquals(
                        1,
                        ((Number) em.createNativeQuery("SELECT @@TRANCOUNT").getSingleResult())
                            .intValue());
                    throw new TestRollback();
                  }));
      runner.required(
          PrincipalKind.AUTH_TECH,
          ActorContext.system(),
          em -> {
            assertEquals(
                original,
                em.createNativeQuery("SELECT fullName FROM dbo.[User] WHERE id=:id")
                    .setParameter("id", id)
                    .getSingleResult());
            return null;
          });
    }
  }

  private static class TestRollback extends RuntimeException {}
}
