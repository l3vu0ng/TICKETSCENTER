package vn.ticketscenter.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vn.ticketscenter.support.VuongSqlTestSupport;

class AuthorizationMatrixIT {
    @Test
    void enforcesFinancialFoundationAndEffectivePermissionSources() throws Exception {
        assertTrue(
                VuongSqlTestSupport.sqlScript("database/tests/vuong/VUONG-03.sql")
                        .contains("VUONG-03 PASS"));
    }
}
