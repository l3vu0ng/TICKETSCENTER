package vn.ticketscenter.support;

import java.time.Instant;
import java.util.UUID;

/** Registry of reserved test identities, not a claim that domain rows were seeded. */
public final class TestFixtures {
    public static final Instant BASE_TIME = Instant.parse("2026-10-06T03:00:00Z");
    public static final UUID BUYER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID BUYER_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID BUYER_UNVERIFIED = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID MANAGER_A_CHECKIN_B = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final UUID LAST_MANAGER_A = UUID.fromString("00000000-0000-0000-0000-000000000005");
    public static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000006");
    public static final UUID DISABLED_USER = UUID.fromString("00000000-0000-0000-0000-000000000007");
    public static final UUID ORGANIZATION_A = UUID.fromString("90000000-0000-0000-0000-000000000001");
    public static final UUID ORGANIZATION_B = UUID.fromString("90000000-0000-0000-0000-000000000002");
    public static final UUID EVENT_A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID SEATED_ZONE_A = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID STANDING_ZONE_A = UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID SEAT_A1 = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID ORDER_A = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID COMMISSION_A = UUID.fromString("60000000-0000-0000-0000-000000000001");
    public static final UUID SETTLEMENT_A = UUID.fromString("70000000-0000-0000-0000-000000000001");
    public static final UUID REFUND_ATTEMPT_A = UUID.fromString("80000000-0000-0000-0000-000000000001");
    public static final UUID PAYOUT_A = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private TestFixtures() { }
}
