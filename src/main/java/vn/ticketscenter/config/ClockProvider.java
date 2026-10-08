package vn.ticketscenter.config;

import java.time.Instant;

/** Server clock shared by services and deterministic tests. */
public interface ClockProvider {
    Instant now();
}
