# Backend implementation decisions — TicketsCenter

All decisions recorded here after compatibility spike. These lock the versions and choices
that all five members must use. No member may upgrade a dependency unilaterally.

## DECISION-001 — Java version

**Decision:** Use JDK 25. Maven compiler release=25. `--enable-preview` enabled for record patterns.

**Rationale:** Spec targets JDK 25; all members use JDK 25.

**Owner:** Khánh (KHANH-01)
**Date:** 2026-10-07

## DECISION-002 — Dependency versions (locked after KHANH-01 spike)

| Component | Version | Notes |
|---|---|---|
| Servlet API | 6.1.0 | Provided by Tomcat 11.0.25 |
| JSP API | 4.0.0 | Provided by Tomcat 11.0.25 |
| JSTL API | 3.0.1 | Bundled in WAR |
| JSTL Impl (GlassFish) | 3.0.1 | Bundled in WAR |
| Hibernate ORM | 6.6.3.Final | Tested with JDK 24 + SQL Server |
| Jakarta Persistence API | 3.2.0 | |
| MS SQL Server JDBC | 12.8.1.jre11 | Tested against SQL Server 2022 |
| HikariCP | 6.2.1 | Max pool=5 connections per PrincipalKind |
| Jackson | 2.18.1 | JavaTimeModule + no timestamps |
| Jakarta Mail API | 2.1.3 | |
| Angus Mail | 2.0.3 | Eclipse implementation |
| JUnit Jupiter | 5.11.3 | |
| Mockito | 5.14.2 | |

## DECISION-003 — Money representation

All monetary values use `java.math.BigDecimal` (scale 0 for VND, scale per domain for rates).
JSON serializes money as a string of digits (e.g., `"200000"`, `"0"`). Never use double/float.
SQL type: `decimal(19,0)` for VND amounts; `decimal(19,6)` for rates.

## DECISION-004 — JSON configuration

Jackson ObjectMapper:
- JavaTimeModule registered, WRITE_DATES_AS_TIMESTAMPS disabled
- Instants serialize as ISO-8601 UTC strings
- NON_NULL inclusion (null fields omitted)
- BigDecimal serialized as string via custom serializer (KHANH-02)

## DECISION-005 — Session and CSRF

- Servlet container session (HttpSession) with cookie-based tracking only
- CSRF token: SecureRandom, stored in session, compared constant-time
- Session cookie: HttpOnly=true, SameSite=Strict
- Session rotated on login and after reset grant is consumed

## DECISION-006 — Password hashing

BCrypt via jBCrypt library (to be confirmed by KHANH-05 after cost/timing test).
Salt generated per-password, algorithm version embedded in encoded string.
Minimum cost factor 12. Login timeout must be verified under load.

## DECISION-007 — Database connection pool

HikariCP single pool shared across all principals. Pool size TC_DB_POOL_SIZE (default 5)
shared — NOT 5 per principal. SESSION_CONTEXT set at connection borrow, cleared on return.
Connections not reused with stale actor context.

## DECISION-008 — Test profiles

- `mvn -B verify` — unit tests only (*Test), no DB required
- `mvn -B -Psqlserver-it verify` — SQL Server integration (*IT, acceptance/), requires TC_SQL_HOST + TC_TEST_DATABASE
- `mvn -B -Pbrowser-it verify` — browser E2E (*IT, browser/), tooling by Vương (VUONG-01)
- sqlserver-it profile MUST FAIL when DB env vars are missing, not silently skip
