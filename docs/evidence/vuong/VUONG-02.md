# VUONG-02 — manifest/fixture/runbook, phần nền

## Cập nhật M0 ngày08/10/2026

Nhánh `feature/vuong/vuong-02-migration-manifest-part-2` tiếp nối `efcdcbb`,
nhận VUONG-01 `a4a37a3` bằng merge local có Task-Id. Đọc routine main `baa15e7`
và hợp đồng gốc `04336c2`; 0100 thuộc VUONG-02 theo hợp đồng, không chuyển
thành VUONG-04 như bảng routine. Model-map cập nhật hai entity có typed relation
và bốn persistence mapping đã có, đánh dấu đúng compile riêng/runtime gate.

Kiểm mới runner trong checkout tạm chứa chính file đã commit: bảy ca
`pwsh -NoProfile -File database/tests/vuong/VUONG-02.ps1 -RepositoryRoot <checkout>`
đều PASS (missing domains, foundation, checksum drift, demo DB, missing env,
path traversal, missing declared dependency). Full preflight vẫn fail đúng
`Missing required migration`, không báo thành công khi thiếu0010/0020/0030/0040/0100.

Trong checkout nền Khánh `1edbced` + file Vương01/02/03, SQL Server localhost
17.0.1135.8, scope **FinancialSecurityFoundation**: `VuongMigrationIT` chạy thật
migration lần đầu/lặp lại, kiểm history và SQL01/02. Toàn bộ verify gồm62 unit
và3 IT quyền/migration, failure/error/skipped=0. SQL01 có20 rejection đúng mã,
test rows rollback về0. Database và login prefix riêng được dọn/kiểm master.
Log mới ở [VUONG-01-m0-validation.log](VUONG-01-m0-validation.log), phần
`sql-foundation-it.log`/`sql-foundation-validation.log`. Scope này có 0700 từ
task03; không thay manifest task02 trước khi nhận dependency đó.

**Chưa thể ghép 0100/seed toàn miền:** chưa có schema owner đủ/đã review trong
develop. Khánh cần giao0010 vào develop, Đông0020, Liêm0030, Thái0040; chốt
bảng/cột/PK thực, acceptedPaymentId/currentAttemptId và fixture registry.
Không tự tạo bảng owner hoặc FK tới tên/cột chưa bàn giao để báo xanh.
Full fresh-install M0 vẫn **BLOCKED**; chưa push/merge remote hoặc peer-review
chấp thuận. Các đoạn sau là evidence ngày07, không thay kết quả mới này.

---

Ngày 07/10/2026. **PARTIAL**: runner/registry và schema tài chính đã kiểm, fresh-install toàn miền/FK/seed còn **BLOCKED**. Hợp đồng và develop `04336c2` không đổi. Nhánh local `feature/vuong/vuong-02-migration-manifest-part-1` tiếp nối dependency VUONG-01 `be8fabc` (cùng ancestry develop), không merge/cherry-pick nền Khánh. Chỉ được ghép vào develop khi dependency/review/checks tương ứng đạt. Revision kiểm là commit chứa evidence và các file này; SHA được ghi ở Issue #5 sau commit.

## File, caller và đầu ra

- `database/migrate.ps1`, `database/migrations/manifest.csv`: checksum canonical UTF8/LF, owner/dependency, preflight, guard DB test, applied history, transaction/lock; dùng sqlcmd thật. Full mặc định đòi 0010–0050/0100/0700; FinancialFoundation phải chọn rõ, không đồng nghĩa Full đạt.
- `database/tests/vuong/VUONG-02.ps1`, `VUONG-02.sql`: bảy ca preflight/guard, assertions SQL history/schema/không fixture tồn dư.
- `support/TestFixtures.java`: UUID/clock registry dùng chung, không seed giả. `support/VuongSqlTestSupport.java`, `acceptance/VuongMigrationIT.java`: helper riêng Vương gọi sqlcmd/runner, không sửa hoặc tạo thay helper/JPA/TransactionRunner của Khánh.
- `database/README.md`, `docs/backend/database-runbook.md`, `fixture-registry.md`, `model-map.md`, `normalization.md`: cách tái lập, input owner còn thiếu, schema/FD và mapping tài chính có thật.

API/Servlet/UI không thay đổi. SQL mới là SchemaMigrationHistory kỹ thuật của runner, không thêm lớp nghiệp vụ. 0100 và seeds liên miền chưa tạo vì thiếu schema/PK/caller, không bịa FK/seed. Manifest chỉ chứa SHA thật của 0050; không đăng ký placeholder cho file chưa giao.

## Môi trường và kết quả

SQL Server localhost 17.0.1135.8 Developer, Windows Integrated; PowerShell7/sqlcmd ODBC; database riêng `TicketsCenter_Test_Vuong_b196ce73f3d8_migration`, lúc đầu 0 bảng. Sau apply/repeat: history 1 dòng 0050, sáu bảng tài chính + history, 0 dòng nghiệp vụ. DB fault riêng đã xóa sau kiểm; DB demo không dùng. Maven3.9.16/JDK25+36 trên archive Khánh `1edbced` với file Vương overlay; baseline develop vẫn thiếu dependency. Log: [VUONG-02-validation.log](VUONG-02-validation.log).

| Ca/lệnh thực chạy | Expected | Actual | Exit / gate |
|---|---|---|---|
| `pwsh -File database/tests/vuong/VUONG-02.ps1 -RepositoryRoot E:/TICKETSCENTER` trước runner | Không có runner thì fail | Expected migration runner | 1 / RED |
| Cùng test sau triển khai | Full thiếu schema, checksum drift, demo DB, thiếu env, traversal, dependency thiếu phải fail; financial preflight pass | 7/7 assertion đạt, không kết nối SQL cho các ca này | 0 / PASS guard |
| `pwsh -File database/migrate.ps1 -Scope FinancialFoundation -Server localhost -Database <testDb> -TrustLocalCertificate` hai lần | APPLIED rồi UNCHANGED | Đúng; history 1 dòng | 0 / PASS SQL thật |
| `sqlcmd ... -E -C -I -b -i database/tests/vuong/VUONG-02.sql` | Đủ sáu bảng/history một dòng/không business fixtures | Đúng | 0 / PASS SQL thật |
| Sửa hash history trong DB test rồi chạy lại; sau ca phục hồi hash thật | Phải chặn applied checksum khác | Applied checksum mismatch | 1 / expected failure |
| Private copy migration có THROW sau mutation, hash private copy cập nhật thật | SQL lỗi không để tài chính/history applied dở dang | Runner exit1; assertions DB cho thấy 0 financial tables/0 history rows | 1 rồi 0 / PASS rollback thật |
| Maven `-Psqlserver-it -Dit.test=VuongMigrationIT verify`, không env | Fail rõ, không skip | 36 unit pass; 1 IT fail BLOCKED missing TC_SQL_HOST, skipped0 | 1 / expected BLOCKED |
| Cùng IT, TC_SQL_HOST=localhost/TC_TEST_DATABASE test/TC_TRUST_LOCAL_CERTIFICATE=true/TC_MIGRATION_SCOPE=FinancialFoundation | Unit+IT thật pass | 36 unit, 1 IT, 0 failures/errors/skipped; runner repeat và SQLtest01/02 thật | 0 / PASS phần nền |
| Cùng IT có DB nhưng bỏ TC_MIGRATION_SCOPE (Full) | Không được xanh khi thiếu miền | Missing required migration 0010_identity.sql | 1 / BLOCKED Full |

Fault và checksum test chỉ sửa private copy/DB test rồi kiểm/phục hồi; không sửa migration/schema của owner khác, không sửa history DB chung. Registry/normalization là phân tích phạm vi đã nhận, không đếm là integration.

## Dependency và người nhận

- Khánh: build/error/helper vào develop, 0010/schema identity, TransactionRunner; PrincipalKind đã có trên feature. Review registry và runner, cung cấp cấu hình JDBC/principal để kiểm runtime qua JPA.
- Đông/Liêm/Thái: 0020/0030/0040, PK/FK/cột đúng và builder/domain fixtures. Khi nhận đủ, Vương tạo 0100, seeds, inventory và chạy Full/fresh DB/checksum/repeat.
- Vương: VUONG-01 model/JPA và VUONG-03 grants còn partial. Order.acceptedPayment/Refund.currentAttempt cần SP kiểm đúng aggregate ngoài FK.
- Browser/HTTP E2E chưa chạy vì thiếu Tomcat/caller/schema; không báo PASS từ sqlcmd hoặc unit. Chưa review chấp thuận, chưa push/merge/deploy. Checklist VUONG-02 tổng thể giữ unchecked.
