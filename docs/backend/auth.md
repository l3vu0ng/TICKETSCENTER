# Auth foundation

AuthService obtains userId/authVersion from the server session, rereads User through
AUTH_TECH and rejects missing, disabled or stale identities. A stale session is invalidated.
ActorContext cannot be constructed as a SYSTEM actor with user identity/admin privileges.

AuthorizationService has the exact public three-argument organization guard. It checks
the supplied actor against the active transaction and rereads the account. Membership
access is delegated to an injected guard on that same EntityManager. The current
ServletContext binding has no Đông adapter and denies membership access; integration
with MembershipAuthorizationRepository is deferred, not treated as authorized access.

M1 must implement login/password verification, rotate session ID and CSRF, then store
UUID userId and integer authVersion. It must wire account/source throttling, OTP/reset
and logout. The rate limiter supplied here is bounded per process; it does not replace
OTP failedAttempts or database locks.

Cookies: HttpOnly, SameSite=Lax, Secure for HTTPS/production, cookie-only tracking,
validated inactivity timeout. Forwarded headers are not trusted by this foundation.
