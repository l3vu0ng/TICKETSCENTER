package vn.ticketscenter.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.model.settlement.PayoutResult;
import vn.ticketscenter.model.settlement.TransferStatus;
import vn.ticketscenter.persistence.audit.AuditLogRecord;
import vn.ticketscenter.persistence.settlement.*;

/** Real SQL Server mapping test; inputs required, no silent skip or owner table substitutes. */
class VuongFinancialMappingIT {
    private static final UUID SETTLEMENT = UUID.fromString("70000000-0000-0000-0000-000000000071");
    private static final UUID ORDER = UUID.fromString("40000000-0000-0000-0000-000000000071");
    private static final UUID PAYOUT = UUID.fromString("a0000000-0000-0000-0000-000000000071");
    private static final UUID AUDIT = UUID.fromString("b0000000-0000-0000-0000-000000000071");
    private static final Instant TIME = Instant.parse("2026-10-08T03:00:00.123456700Z");

    @Test
    void validatesAndReadsFinancialRowsWithExactMoneyUtcAndUnicode() {
        String url = required("TC_JDBC_URL");
        // A second DB_NAME check below protects against URL spelling/case ambiguity.
        assertTrue(
                url.toLowerCase(java.util.Locale.ROOT).contains("databasename=ticketscenter_test_"),
                "Dedicated test database required");
        var registry =
                new StandardServiceRegistryBuilder()
                        .applySetting(
                                "hibernate.connection.driver_class",
                                "com.microsoft.sqlserver.jdbc.SQLServerDriver")
                        .applySetting("hibernate.connection.url", url)
                        .applySetting("hibernate.connection.username", required("TC_JDBC_USER"))
                        .applySetting("hibernate.connection.password", required("TC_JDBC_PASSWORD"))
                        .applySetting("hibernate.hbm2ddl.auto", "validate")
                        .applySetting("hibernate.jdbc.time_zone", "UTC")
                        .applySetting("hibernate.show_sql", "false")
                        .build();
        try (var factory =
                        new MetadataSources(registry)
                                .addAnnotatedClass(SettlementOrderSnapshotRecord.class)
                                .addAnnotatedClass(SettlementTransferLogRecord.class)
                                .addAnnotatedClass(MockPayoutProviderRecord.class)
                                .addAnnotatedClass(AuditLogRecord.class)
                                .buildMetadata()
                                .buildSessionFactory();
                Session session = factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                session.doWork(
                        connection -> {
                            try (var query = connection.createStatement();
                                    var row = query.executeQuery("SELECT DB_NAME()")) {
                                assertTrue(row.next());
                                assertTrue(
                                        row.getString(1)
                                                .matches("TicketsCenter_Test_[A-Za-z0-9_]+"));
                            }
                            try (var insert =
                                    connection.prepareStatement(
                                            """
                            INSERT dbo.Settlement(id,eventId,status,grossRevenue,totalRefund,totalCommission,paidAmount,pendingAmount)
                            VALUES(?,NEWID(),'CONFIRMED',500000,100000,50000,0,100000)
                            """)) {
                                insert.setString(1, SETTLEMENT.toString());
                                insert.executeUpdate();
                            }
                            execute(
                                    connection,
                                    "INSERT dbo.SettlementOrderSnapshot VALUES(?,?,500000,100000,50000,350000)",
                                    SETTLEMENT,
                                    ORDER);
                            execute(
                                    connection,
                                    "INSERT dbo.SettlementTransferLog VALUES(?,?,100000,'PENDING',N'chi thử','2026-10-08T03:00:00.1234567',NULL)",
                                    PAYOUT,
                                    SETTLEMENT);
                            execute(
                                    connection,
                                    "INSERT dbo.MockPayoutProviderLedger VALUES(?,?,100000,'SUCCEEDED',N'chi thử','2026-10-08T03:00:00.1234567')",
                                    PAYOUT,
                                    SETTLEMENT);
                            execute(
                                    connection,
                                    "INSERT dbo.AuditLog VALUES(?,NULL,'SYSTEM','MAPPING_TEST','Settlement',?,N'kiểm tra không có bí mật','2026-10-08T03:00:00.1234567')",
                                    AUDIT,
                                    SETTLEMENT);
                        });
                var snapshot =
                        session.find(
                                SettlementOrderSnapshotRecord.class,
                                new SettlementOrderSnapshotRecord.Key(SETTLEMENT, ORDER));
                assertEquals(new BigDecimal("350000"), snapshot.getNetAmount());
                assertEquals(ORDER, snapshot.getId().getOrderId());
                var transfer = session.find(SettlementTransferLogRecord.class, PAYOUT);
                assertEquals(new BigDecimal("100000"), transfer.getAmount());
                assertEquals(TransferStatus.PENDING, transfer.getStatus());
                assertEquals("chi thử", transfer.getReference());
                assertEquals(TIME, transfer.getCreatedAt());
                assertNull(transfer.getCompletedAt());
                var provider = session.find(MockPayoutProviderRecord.class, PAYOUT);
                assertEquals(PayoutResult.SUCCEEDED, provider.getStatus());
                assertEquals(TIME, provider.getAcceptedAt());
                var audit = session.find(AuditLogRecord.class, AUDIT);
                assertNull(audit.getActorId());
                assertEquals(AuditLogRecord.Source.SYSTEM, audit.getSource());
                assertEquals("kiểm tra không có bí mật", audit.getDetail());
                assertEquals(TIME, audit.getCreatedAt());
            } finally {
                if (transaction.isActive()) transaction.rollback();
            }
            // Verify rollback through another connection, not the previous session cache.
            try (var fresh = factory.openSession()) {
                assertNull(fresh.find(AuditLogRecord.class, AUDIT));
                assertNull(fresh.find(SettlementTransferLogRecord.class, PAYOUT));
                assertNull(fresh.find(MockPayoutProviderRecord.class, PAYOUT));
                assertNull(
                        fresh.find(
                                SettlementOrderSnapshotRecord.class,
                                new SettlementOrderSnapshotRecord.Key(SETTLEMENT, ORDER)));
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static void execute(java.sql.Connection connection, String sql, UUID first, UUID second)
            throws java.sql.SQLException {
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setString(1, first.toString());
            insert.setString(2, second.toString());
            insert.executeUpdate();
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        assertTrue(value != null && !value.isBlank(), "BLOCKED: missing " + name);
        return value;
    }
}
