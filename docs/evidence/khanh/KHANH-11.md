# KHANH-11 — Layout và Fetch dùng chung

Nguồn/nhánh/môi trường/lệnh: [M0-AUDIT](M0-AUDIT.md).
**PASS khung M0 trên trình duyệt thật; nav membership/revocation DEFERRED.**

Thêm header/footer/notifications/error JSP, ViewSupport allowlist/defaults, Bootstrap local,
CSS palette prototype/wrap/focus/table overflow, Fetch envelopes/CSRF/error handling và UI states.
Sửa charset UTF-8 riêng cho từng jspf sau khi ảnh trình duyệt phát hiện chữ Việt bị hỏng.
Comment chỉ giữ mục đích, contract và lý do kỹ thuật cần thiết.

Expected/actual: Node client5test PASS, không tự retry POST, nullable data/encoded query an toàn.
WAR test route chỉ tồn tại ở test container. Browser Edge154/Playwright1.62.1 thật kiểm
320/375/768/1440, tên dài/XSS escaped, tiếng Việt, no overflow, Tab→skip link→brand,
focus outline, loading/empty/success/error aria-live/busy và error focus. Không console/CSP error.
Ảnh dùng DTO fixture test, không chứng minh danh tính/quyền production.

| Viewport | Actual |
|---|---|
|320|PASS — [ảnh](layout/layout-320.png)|
|375|PASS — [ảnh](layout/layout-375.png)|
|768|PASS — [ảnh](layout/layout-768.png)|
|1440|PASS — [ảnh](layout/layout-1440.png)|

Nav manager/check-in/revoke cần MembershipDto/query thật của Đông. Pagination/các form và
luồng M1 được kiểm khi có page thật; không dùng khung error để nghiệm thu24màn hình.
Smoke browser này tùy chọn qua http-it; profile browser-it chung vẫn do Vương cấu hình.
