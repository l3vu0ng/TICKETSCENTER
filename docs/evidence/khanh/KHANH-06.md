# KHANH-06 — Nền actor/session/authorization M0

Nguồn/nhánh/môi trường/lệnh: [M0-AUDIT](M0-AUDIT.md).
**PASS nền adapter độc lập; membership SQL DEFERRED, login/rotation thuộc M1.**

AuthService dùng session userId/authVersion rồi reread User ACTIVE/version qua AUTH_TECH.
AuthorizationService kiểm actor của transaction và current user, cùng EntityManager với guard.
Không có membership adapter thì deny. Cookie HttpOnly/SecureHTTPS/SameSiteLax/timeout/cookie-only.

Expected/actual: AuthFoundationTest4 PASS (stale/absent session/current EM/revocation/SYSTEM),
ActorContext negative cases trong HttpContractTest PASS.
Đây là adapter mock test, chưa chứng minh revoke–mutation race và grants SQL thật.
Chưa có SessionService/login/password verification/logout; routes M1 trả501.
