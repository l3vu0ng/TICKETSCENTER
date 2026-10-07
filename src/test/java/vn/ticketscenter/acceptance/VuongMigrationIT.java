package vn.ticketscenter.acceptance;

import org.junit.jupiter.api.Test;
import vn.ticketscenter.support.VuongSqlTestSupport;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VuongMigrationIT {
    @Test
    void appliesAndRepeatsTheDeclaredMigrationScopeWithoutDuplicatingHistory() throws Exception {
        String server = VuongSqlTestSupport.requiredEnvironment("TC_SQL_HOST");
        String database = VuongSqlTestSupport.requiredEnvironment("TC_TEST_DATABASE");
        // Full is the acceptance default. Foundation must be explicitly selected and reported.
        String scope = System.getenv().getOrDefault("TC_MIGRATION_SCOPE", "Full");
        List<String> command = new ArrayList<>(List.of("pwsh", "-NoProfile", "-File", "database/migrate.ps1",
                "-Scope", scope, "-Server", server, "-Database", database));
        if ("true".equals(System.getenv("TC_TRUST_LOCAL_CERTIFICATE"))) command.add("-TrustLocalCertificate");
        assertTrue(VuongSqlTestSupport.run(command).contains("MIGRATIONS PASS (" + scope + ")"));
        assertTrue(VuongSqlTestSupport.run(command).contains("UNCHANGED 0050_settlements_audit.sql"));
        assertTrue(VuongSqlTestSupport.sqlScript("database/tests/vuong/VUONG-02.sql").contains("VUONG-02 PASS"));
        assertTrue(VuongSqlTestSupport.sqlScript("database/tests/vuong/VUONG-01.sql").contains("VUONG-01 PASS"));
    }
}
