package vn.ticketscenter.config;
import java.time.Instant;
/** Provides current UTC time. HTTP MUST NOT provide 'now'. Owner: Khánh (KHANH-02) */
public interface ClockProvider { Instant now(); }
