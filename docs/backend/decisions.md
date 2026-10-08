# Backend decisions

Updated 2026-10-08 for the Khánh M0 review. Validation details are in
[the M0 audit](../evidence/khanh/M0-AUDIT.md). Database and Đông integration are
deferred at the user's request; the entries below do not certify those integrations.

## Runtime and build

JDK 25, compiler release 25, UTF-8, WAR final name `ticketscenter`. Preview features
are unnecessary. Verified locally with Oracle JDK 25.0.4.1 and Maven 3.10.0.

| Component | Locked version |
|---|---|
| Tomcat / embedded container test | 11.0.25 |
| Servlet / JSP API, provided | 6.1.0 / 4.0.0 |
| JSTL API / GlassFish implementation | 3.0.1 / 3.0.1 |
| Hibernate ORM / Jakarta Persistence | 6.6.40.Final / 3.1.0 |
| SQL Server JDBC | 12.8.1.jre11 |
| HikariCP | 6.2.1 |
| Jackson / Java time | 2.18.1 |
| SLF4J API / JUL binding | 2.0.16 |
| Jakarta Mail / Angus | 2.1.3 / 2.0.3 |
| JUnit / Mockito | 5.11.3 / 5.20.0 |
| Bootstrap WebJar | 5.3.0, matches the checked-in prototype |
| Spotless / Google Java Format | 2.43.0 / 1.28.0 |
| Compiler / WAR plugins | 3.13.0 / 3.4.0 |
| Surefire / Failsafe | 3.5.2 / 3.5.2 |
| Resources / Clean plugins | 3.3.1 / 3.5.0 |

The formatter uses AOSP (four-space Java indentation), matching TEAM-CONTRACT §5.1.
Javadoc text is preserved. Configuration follows the
[Spotless Maven documentation](https://github.com/diffplug/spotless/tree/main/plugin-maven#google-java-format).

Hibernate 6.6 supports Java 25 starting with 6.6.40 and implements Persistence 3.1;
the earlier 6.6.3 / Persistence 3.2 pairing was replaced.
Source: [Hibernate compatibility table](https://hibernate.org/orm/releases/6.6/).
SQL mapping compatibility still requires SQL Server validation.

Bootstrap is served locally from its WebJar. Shared CSS uses the prototype's
white/red/black palette; no external font or script provider is needed.

## JSON and identity

Money and rates use BigDecimal. The shared serializer emits plain decimal strings
without implicit rounding. Money input is canonical `0` or an unsigned nonzero
integer of at most 19 digits; fractional rates are validated by their owning domain.
Instants are ISO-8601 UTC. Nullable DTO fields are preserved; `data:null` is valid.

Username is trimmed and lowercased with Locale.ROOT, ASCII `[a-z0-9_]{3,32}`.
Email is trimmed/lowercased without provider-specific rewrites. Full name is NFC,
1–120 characters. Phone is nullable, at most 20 characters, with digits, optional
leading +, spaces, parentheses and hyphens. Passwords are not normalized.
No hashing implementation is claimed complete before KHANH-05.

`0010_identity.sql` is preserved. New validation constraints and explicit normalized
column collation are supplied through `0011_identity_validation.sql`, to be registered
in Vương's manifest before use.

## Session and request foundation

Cookie sessions only, HttpOnly, SameSite=Lax; Secure for HTTPS/production. Lax allows
the top-level VNPAY Return navigation. Session timeout is validated at startup.
CSRF tokens are random, session-bound and compared in constant time. M1 login/reset
must rotate both session identity and CSRF.

No forwarded IP is trusted. Identity JSON is capped at 64 KiB, nesting 32 and strings
16 KiB; duplicate keys and caller-supplied identity/permission fields are rejected.
AuthRateLimiter is a bounded single-process component; M1 must wire the documented
account/source thresholds into the actual login/OTP flows.

## Transaction and pool proposal

One Hikari pool, maximum five connections total. A limited broker impersonates the
database user assigned to a server-selected PrincipalKind. That user must exist;
roles are not valid EXECUTE AS targets. No permission fallback exists.

The broker must have narrow IMPERSONATE permissions for those users and schema
metadata access needed for validation. It must not be sysadmin/db_owner. This proposal
requires Vương's review and real grant/DENY tests before integration is accepted.

A Hibernate session uses the borrowed JDBC connection for the whole resource-local
transaction. Nested calls join only if principal and actor match. Caught nested
failures still mark rollback-only. Cleanup reverts execution context and clears actor
session context; failures evict the connection.
Reference: [SQL Server EXECUTE AS / REVERT cookies](https://learn.microsoft.com/en-us/sql/t-sql/statements/revert-transact-sql).

JPA schema validation is lazy on the first transaction. Startup initializes a lazy
pool so liveness remains available during DB outages; readiness probes only connection
availability and does not certify schema correctness.

## Checks

- Default `verify`: unit tests, WAR and formatter check.
- `http-it`: packaged WAR on Tomcat 11, with an explicitly unavailable SQL endpoint.
- `sqlserver-it`: real SQL/HTTP integration; missing configuration fails, with no conditional skips.
- `browser-it`: reserved for Vương; shared-frame browser smoke is available through
  `http-it` with an explicitly configured Node/Playwright/browser runtime.
- `tc.build.directory`: optional output override for OneDrive locks; default remains `target`.
