# KHANH-07 — Nền bảo vệ request M0

Nguồn/nhánh/môi trường/lệnh: [M0-AUDIT](M0-AUDIT.md).
**PASS CSRF/validation độc lập; auth nghiệp vụ và SQL assertions DEFERRED.**

Thêm runtime random session CSRF/constant-time check/rotation helper, JSON64KiB/depth32/
string16KiB/duplicate/trailing guards, reserved authority fields, bounded limiter/clock,
returnTo allowlist, CSP/no-store. IPN contract là GET, không miễn CSRF cho POST/payments.

Expected/actual: RequestSecurityTest5 PASS; WAR thực csrf200, thiếu/sai/chéo-session403,
malformed400,65537bytes413, JSON hợp lệ tới auth501 (chưa mutation).
Rate window boundary/unsafe redirects/chunked body/forged authority đều bị kiểm.

Ngưỡng login/OTP/source wiring và IPN signature/business mutation thuộc task M1/M2.
Không có DB integration nên chưa assert mutation DB/OTP counter bằng bộ thử này.
