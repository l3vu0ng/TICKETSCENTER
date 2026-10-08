package vn.ticketscenter.support;

import java.time.Duration;
import java.time.Instant;
import vn.ticketscenter.config.ClockProvider;

/**
 * Test-only mutable clock. Set the time before each test assertion. Base time: 2026-10-06T03:00:00Z
 * (per TEAM-CONTRACT §6). NEVER use in production code.
 */
public class MutableClock implements ClockProvider {

  /** Base test time per TEAM-CONTRACT §6 fixture registry. */
  public static final Instant BASE_TIME = Instant.parse("2026-10-06T03:00:00Z");

  private Instant current;

  public MutableClock() {
    this.current = BASE_TIME;
  }

  public MutableClock(Instant initial) {
    this.current = initial;
  }

  @Override
  public Instant now() {
    return current;
  }

  public void set(Instant t) {
    this.current = t;
  }

  public void advance(Duration d) {
    this.current = current.plus(d);
  }

  public void rewind(Duration d) {
    this.current = current.minus(d);
  }
}
