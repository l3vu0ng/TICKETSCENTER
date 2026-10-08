# VUONG-03 — principal và quyền DB nền

## Cập nhật M0 ngày08/10/2026 — kiểm lại trên SQL Server thật

Nhánh `feature/vuong/vuong-03-database-principals-part-2` tiếp nối `305425e`,
nhận Model VUONG-01 `a4a37a3` và cập nhật mapping VUONG-02 `98f8c5b` bằng
merge local có Task-Id. Routine main `baa15e7` không đổi chuẩn quyền/owner.
Không sửa migration 0050/0700 đã áp dụng hoặc mở rộng quyền runtime.

Thực chạy mới trên SQL Server localhost `17.0.1135.8` bằng hai database test
và hai prefix login ngẫu nhiên riêng: provisioning tạo đủ bốn SQL login/user
đúng role, password sinh trong process environment/SqlParameter và không lưu
vào source/log/URL. Kết nối SqlClient riêng bằng từng SQL login: admin SELECT
CommissionRule thành công; buyer/manager/check-in bị lỗi229. Đây là SQL login
thật, không chỉ EXECUTE AS dưới migration principal.

`DatabasePrincipalsIT` kiểm SID/login, membership đúng role, không role thừa/
sysadmin; `AuthorizationMatrixIT` chạy SQL03 để kiểm hiệu lực DENY dù có competing
GRANT, GRANT rồi REVOKE, admin đọc và các lệnh denied. `VuongMigrationIT`
kiểm apply/repeat/history và SQL01/02. Lượt cuối:
`mvn -B -Psqlserver-it -Dit.test=VuongMigrationIT,DatabasePrincipalsIT,AuthorizationMatrixIT verify`
trên archive Khánh `1edbced` + toàn file Vương01/02/03:
**62 unit +3 IT, failure/error/skipped=0; WAR tạo**. Scope chỉ
FinancialSecurityFoundation, chưa phải toàn dự án tích hợp.

Test business rows rollback về0. Mỗi lượt dọn database test và năm login
có prefix riêng (bốn runtime + một mapping probe); master xác nhận CLEANUP
PASS. Mapping probe dùng user test riêng, chỉ SELECT/INSERT trên năm bảng
cho test Hibernate; không cấp thêm quyền cho runtime roles hoặc db_owner.
Lượt có `VuongFinancialMappingIT` fail kết nối JDBC do TCP SQL Server tắt;
không nhận đó là PASS runtime JPA. Không bật TCP/restart SQL Server trong phiên.
Log chung: [VUONG-01-m0-validation.log](VUONG-01-m0-validation.log).

**Bàn giao M0:** bốn role/login/provisioning và các kiểm quyền nền có thật.
Khánh còn giao TransactionRunner/pool/principal context và foundation vào
develop; chủ SQL module giao object/caller/guard để thêm migration quyền hẹp.
DENY các view/function chưa tồn tại, worker limits, buyer SP09 0đ, admin SP16,
scope HTTP và Azure vẫn cần input/test riêng; không tick đạt từ SELECT bảng nền.
Chưa push/merge remote hoặc peer review chấp thuận. Các phần dưới là lịch sử
ngày07; Model/persistence Java nêu “chưa có” ở đó đã được bổ sung trong task01.

---

Ngày 07/10/2026; **PARTIAL**, toàn module/runtime/HTTP/Azure **BLOCKED**. Nhánh `feature/vuong/vuong-03-database-principals-part-1` tiếp nối VUONG-02 `efcdcbb` và VUONG-01 `be8fabc` local, cùng ancestry develop `04336c2`. Hợp đồng không đổi. Commit chứa các file/evidence là revision quyền được kiểm; Issue #5 ghi SHA sau commit. Chưa push/merge/PR hoặc review chấp thuận.

## File và giới hạn quyền

- `0700_roles_grants.sql`: bốn runtime role + hai TECH role; admin SELECT financial foundation, check-in explicit DENY sáu bảng 0050; không broad schema SELECT/DML/EXECUTE hoặc db_owner/sysadmin. Auth/worker role chưa có module grants, không mở payout cho worker.
- `security/local-logins.sql`, `provision-principals.ps1`: bốn login/user/role mapping thật, password từ process environment qua typed SqlParameter, QUOTENAME/literal escape ở SQL CREATE LOGIN. Bốn principal cùng outer transaction; missing secret/demo guard/collision fail, không rotate credentials. Local wrapper dùng Windows Integrated của người cấp DDL. Flags CHECK_POLICY/EXPIRATION có thật nhưng policy host quyết định độ mạnh.
- `security/azure-users.sql`: contained-user artifact có engine guard. Wrapper Azure **fail rõ trước SQL** vì chưa có approved Azure auth caller/config. Không có bằng chứng Azure thành công.
- `tests/vuong/VUONG-03.sql`, `security/DatabasePrincipalsIT.java`, `AuthorizationMatrixIT.java`: real permission rejection, role membership/login SID/extra role/sysadmin assertions; không tạo buyer bằng quyền migration giả.
- `docs/backend/security-matrix.md`: principal→role/login/user, effective và pending permission/caller. Manifest thêm checksum thật 0700, dependency0050; runner thêm scope explicit FinancialSecurityFoundation. VUONG-02 guard regression thích nghi hai file thật và vẫn 7/7 pass.

API/Servlet/UI không đổi. PrincipalKind từ Khánh feature có thật; không tạo bản sao hoặc sửa file Khánh. TransactionRunner/JPA pool/session context chưa có và chưa kiểm, SQLcmd/SqlClient không thay bằng chứng runtime JPA. Module V03/V04/V07/V08/F05/F10/SP chưa có nên chưa cấp/kiểm các quyền đó bằng object giả; forward permission migrations phải đi cùng object/caller/guard.

## Môi trường và actual

SQL Server localhost `17.0.1135.8`, Developer; mixed authentication được đọc xác nhận (`IsIntegratedSecurityOnly=0`), PowerShell7/.NET SqlClient/sqlcmd ODBC. Database test `TicketsCenter_Test_Vuong_b196ce73f3d8_migration`, chỉ financial schema/history; password test ngẫu nhiên có dấu quote để kiểm escaping, không lưu/in value. Login có prefix riêng `tc_vuongtest_<12hex>_` tránh tên vận hành trên server; user/role vẫn tên chuẩn. Maven JDK25+36/3.9.16 dùng archive Khánh `1edbced` overlay file Vương, không phải develop tích hợp.

| Ca/lệnh thực chạy | Expected | Actual | Gate |
|---|---|---|---|
| SQLtest03 trước roles/users | Fail rõ phần nền chưa có | THROW51301, exit1 | RED |
| Roles DDL rồi provisioning chưa có secrets | Roles thật, không tạo login khi thiếu secret | Roles tạo; required secret unavailable, exit1 | PASS guard |
| Provisioning lần đầu | SqlConnectionStringBuilder cần setter CLR thật trong PowerShell | DataSource keyword adapter lỗi; sửa bằng explicit .NET setters sau probe connection | Lỗi sửa trước commit |
| Provisioning bốn runtime identities, version cuối có outer transaction | Tạo đủ login/user/role, không log secrets | Đủ bốn mapping; không role thừa/sysadmin | PASS local thật |
| Kết nối SqlClient riêng bằng từng SQL login/password | Admin đọc CommissionRule; buyer/manager/check-in bị từ chối | Cả4 đăng nhập thật; admin read pass, ba login read fail229 | PASS login thật |
| SQLtest03 / AuthorizationMatrixIT | Admin đọc; 11 denied calls; GRANT rồi REVOKE mất quyền; competing grant vẫn bị tc_checkin DENY | Đúng mã229; disposable probe user được xóa | PASS effective permissions |
| Fault DB riêng, user manager đã tồn tại, provisioning tạo buyer trước rồi fail ở manager | Không để buyer/login mới; không xóa user cũ | Rollback tất cả mới, user manager cũ giữ nguyên; fault DB xóa | PASS atomic provisioning |
| Probe password policy | Không suy ra strength từ CHECK_POLICY=ON | Host chấp nhận password ngắn; không dùng ca này để chứng minh rollback/strength; test DB/login của probe đã dọn | Giới hạn môi trường được ghi rõ |
| Demo DB / existing login-user / Azure wrapper | Fail trước mutation hoặc giữ existing credential; Azure BLOCKED | Cả3 bị chặn đúng, không thay credential | PASS guard / BLOCKED Azure |
| `pwsh -File database/tests/vuong/VUONG-02.ps1 -RepositoryRoot E:/TICKETSCENTER` sau manifest0700 | Giữ guard cũ | 7/7 assertion pass | PASS regression |
| Maven `-B -Psqlserver-it -Dit.test=VuongMigrationIT,DatabasePrincipalsIT,AuthorizationMatrixIT verify`, scope FinancialSecurityFoundation + localhost/test DB | Build/WAR, 36 unit, 3 IT thật, skipped0 | **36 unit + 3 IT / 0 failure/error/skipped**, WAR tạo; migration repeat và SQL01/02/03 thật | PASS kết hợp nền |

Sau migration scope security, history có2 entries (0050/0700), sáu bảng tài chính/0 business rows và bốn login-backed users đúng role. Kết quả/nhật ký đã lọc: [VUONG-03-validation.log](VUONG-03-validation.log). Không có money/provider/mock transaction nào được nhận là tích hợp thật; chỉ cấu trúc ledger mô phỏng được constraint test.

Sau tất cả checks: đối chiếu SHA256 của19 source/test/SQL/manifest files local với checkout đã kiểm, tất cả trùng. Đã dọn toàn bộ database test riêng do phiên tạo và bốn login prefix riêng; assertions master xác nhận không còn target test/logins. Không xóa/reset database demo hoặc login vận hành. File `application.properties` xuất hiện untracked ngoài thay đổi của phiên; không đọc secrets, không sửa/stage/commit file đó.

## Bàn giao và checklist chưa đủ

- Khánh: nhận matrix PrincipalKind, nền/helper/0010 vào develop, TransactionRunner/factory/pool; reset SESSION_CONTEXT, chọn principal từ actor/use case, không request; kiểm connection reuse/principal/DB rights qua JPA.
- Đông/Liêm/Thái: giao schema/SQL module/guards để cấp hẹp và kiểm direct-SP/HTTP wrong owner/org. SP09 buyer chỉ0đ, SP10/17 worker chỉ canceled, SP16 chỉADMIN phải được owner chứng minh trong DB.
- Vương: VUONG-01 Java model/persistence đủ quan hệ và VUONG-02 cross-FK/seed còn chờ. Thêm DENY V03/V04/V07/V08/F05/F10 và catalog module grants bằng migration mới sau khi object có thật; không tick từ quyền đọc bảng nền.
- Azure/Tomcat/session/auth/browser/cross-org/GRANT module/worker không payout chưa có đủ input/test; ghi BLOCKED, không giả xanh. VUONG-01/02/03 tổng hợp trên Issue #5 giữ unchecked.
