package vn.ticketscenter.service.identity;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import vn.ticketscenter.config.ClockProvider;

/** Bounded per-process limiter; OTP attempt counts remain database-owned. */
public final class AuthRateLimiter {
  private record Counter(Instant expiresAt, int attempts) {}

  private final Map<String, Counter> counters = new HashMap<>();
  private final ClockProvider clock;
  private final int capacity;

  public AuthRateLimiter(ClockProvider clock, int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("Positive capacity required");
    this.clock = java.util.Objects.requireNonNull(clock);
    this.capacity = capacity;
  }

  public synchronized boolean allow(String key, int limit, Duration window) {
    if (key == null || key.length() > 512 || limit < 1 || window.isNegative() || window.isZero())
      throw new IllegalArgumentException("Invalid rate limit");
    Instant now = clock.now();
    counters.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    Counter counter = counters.get(key);
    if (counter == null) {
      if (counters.size() >= capacity) return false;
      counter = new Counter(now.plus(window), 0);
    }
    if (counter.attempts() >= limit) return false;
    counters.put(key, new Counter(counter.expiresAt(), counter.attempts() + 1));
    return true;
  }
}
