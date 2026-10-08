package vn.ticketscenter.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.config.PersistenceFactory;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.model.identity.*;
import vn.ticketscenter.support.SqlServerTestSupport;
import vn.ticketscenter.transaction.PrincipalKind;

class KhanhIdentityMappingIT {
  @Test
  void identityRoundTripUsesRealSqlServerAndRollsBackTestWrite() {
    try (var persistence = new PersistenceFactory(SqlServerTestSupport.config())) {
      var runner = persistence.transactionRunner();
      var id = UUID.fromString("00000000-0000-0000-0000-000000000003");
      assertThrows(
          TestRollback.class,
          () ->
              runner.required(
                  PrincipalKind.AUTH_TECH,
                  ActorContext.system(),
                  em -> {
                    var user = em.find(User.class, id);
                    assertNotNull(user, "VUONG-02 must provide buyer_unverified fixture");
                    String original = user.getFullName();
                    user.updateProfile("M0 mapping test", null);
                    em.flush();
                    em.clear();
                    assertEquals("M0 mapping test", em.find(User.class, id).getFullName());
                    assertEquals(UserStatus.ACTIVE, em.find(User.class, id).getStatus());
                    assertEquals(PlatformRole.CUSTOMER, em.find(User.class, id).getPlatformRole());
                    assertNotNull(em.find(User.class, id).getCreatedAt());
                    throw new TestRollback(original);
                  }));
      runner.required(
          PrincipalKind.AUTH_TECH,
          ActorContext.system(),
          em -> {
            assertNotEquals("M0 mapping test", em.find(User.class, id).getFullName());
            return null;
          });
    }
  }

  private static class TestRollback extends RuntimeException {
    TestRollback(String original) {
      super(original);
    }
  }
}
