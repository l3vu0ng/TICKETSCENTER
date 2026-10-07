package vn.ticketscenter.config;
import java.time.Clock;
import java.time.Instant;
/** Production UTC clock. Owner: Khánh (KHANH-02) */
public class SystemClockProvider implements ClockProvider {
    private static final SystemClockProvider INSTANCE = new SystemClockProvider();
    private SystemClockProvider() {}
    public static SystemClockProvider getInstance() { return INSTANCE; }
    @Override public Instant now() { return Instant.now(Clock.systemUTC()); }
}
