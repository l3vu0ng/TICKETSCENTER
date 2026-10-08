package vn.ticketscenter.config;

import java.time.Clock;
import java.time.Instant;

public final class SystemClockProvider implements ClockProvider {
  private static final SystemClockProvider INSTANCE = new SystemClockProvider();
  private final Clock clock = Clock.systemUTC();

  private SystemClockProvider() {}

  public static SystemClockProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public Instant now() {
    return clock.instant();
  }
}
