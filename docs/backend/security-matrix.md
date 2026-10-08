# Database principal matrix — VUONG-03, financial foundation

Contract `04336c2`, spec §14.10; PrincipalKind thật từ feature Khánh `1edbced` ở `transaction/PrincipalKind.java`. Vương không tạo enum/TransactionRunner thay thế. Actor/use case đã xác minh chọn principal; request không chọn login/role, transaction giữ một connection/principal. Shared runtime principals không tự có row security: Service/SP phải lọc owner/org và recheck membership dưới khóa.

| PrincipalKind | Role / login / user chuẩn | Quyền **đã cấp** trong 0700 | Quyền **chưa cấp**, chờ owner/caller/guard |
|---|---|---|---|
| BUYER | tc_buyer / tc_buyer_login / tc_buyer_user | CONNECT qua user/public; không đọc/DML tài chính | V01/V02 và order/ticket/refund/membership đúng owner; SP02/03/05/06/07/08; SP09 chỉ 0đ có guard DB |
| MANAGER | tc_manager / tc_manager_login / tc_manager_user | Không đọc/DML bảng tài chính nền | Draft Event/Zone/Seat/Coupon/map role; V02–V10/F05/F10 theo scope; SP04; không approve/payout |
| CHECK_IN | tc_checkin / tc_checkin_login / tc_checkin_user | DENY SELECT/INSERT/UPDATE/DELETE trên sáu bảng tài chính 0050 | SP04/Event tối thiểu/V05/F07; DENY V03/V04/V07/V08/F05/F10 khi object có thật; không V06 có paidAmount |
| PLATFORM_ADMIN | tc_platform_admin / tc_admin_login / tc_admin_user | SELECT CommissionRule/Settlement/snapshot/transfer/AuditLog; không db_owner hoặc broad schema permission | SP01/10/12/13/14/15/16; policy mutations sau guard TR03; payout ledger quyền hẹp ở task08 |
| AUTH_TECH | tc_auth_tech / login,user cần Khánh chốt | Role riêng chưa cấp quyền | Identity/auth/session repository tối thiểu; không dùng ADMIN cho login/filter |
| WORKER_TECH | tc_worker_tech / login,user cần Khánh chốt | Role riêng chưa cấp quyền | SP07/09/11/17, SP10 chỉ canceled guard; ledger refund hẹp; **không SP16/payout ledger** |

0700 chỉ cấp trên object tài chính đã có và fail nếu thiếu 0050. Không grant blanket `EXECUTE/SELECT ON SCHEMA::dbo`, không DML tài chính rộng để SP chạy. Future object permissions dùng migration mới (0710...), đăng ký đúng checksum/dependency/caller; không sửa 0700 đã áp dụng chung. Mỗi SP owner chứng minh ownership chain/module signing cần thiết và guard direct-SP; GRANT EXECUTE không đủ chứng minh an toàn use case.

## Provisioning local và kiểm

`database/security/provision-principals.ps1` dùng Windows Integrated của người vận hành migration, encrypted connection; TrustLocalCertificate chỉ localhost. SQL passwords từ environment **process** `TC_BUYER_PASSWORD, TC_MANAGER_PASSWORD, TC_CHECKIN_PASSWORD, TC_ADMIN_PASSWORD` qua cơ chế secret; không command argument/file/SQLCMD macro/log. Typed SqlParameter giữ password khỏi nối chuỗi caller; script SQL dùng QUOTENAME/escape literal cho CREATE LOGIN theo SQL Server. CHECK_POLICY/CHECK_EXPIRATION được bật; độ mạnh có hiệu lực theo password policy thực của host, không suy ra từ flag.

```powershell
pwsh -NoProfile -File database/migrate.ps1 -Scope FinancialSecurityFoundation -Server $env:TC_SQL_HOST -Database $env:TC_TEST_DATABASE -TrustLocalCertificate
# Nạp bốn secret vào process bằng secret mechanism trước lệnh sau, không ghi value tại đây.
pwsh -NoProfile -File database/security/provision-principals.ps1 -Server $env:TC_SQL_HOST -Database $env:TC_TEST_DATABASE -TrustLocalCertificate
```

Default login/user names trong bảng. Cho test local trên server dùng chung, `-TestLoginPrefix tc_vuongtest_<12hex>_` chỉ đổi tên login (user/role trong DB test vẫn chuẩn), tránh đụng login vận hành. Bốn create login/user/role membership cùng một transaction; thiếu secret fail trước SQL; collision không rotate/replace credential và rollback những user/login mới của lần chạy. Login/provisioning không tự chạy trong Maven IT hoặc runner migration, không đưa credential vào manifest.

```powershell
$env:TC_MIGRATION_SCOPE = 'FinancialSecurityFoundation'
$env:TC_TRUST_LOCAL_CERTIFICATE = 'true' # local test only
mvn -B -Psqlserver-it -Dit.test=VuongMigrationIT,DatabasePrincipalsIT,AuthorizationMatrixIT verify
sqlcmd -S $env:TC_SQL_HOST -d $env:TC_TEST_DATABASE -E -C -I -b -i database/tests/vuong/VUONG-03.sql
```

DatabasePrincipalsIT đòi login-backed user thật, đúng role, không role thừa/sysadmin; AuthorizationMatrixIT EXECUTE AS từng user, kiểm 229 và competing grant source. SQLtest không tạo user buyer giả để vượt thiếu provisioning. Direct login connection được kiểm riêng bằng SqlClient trong evidence03; IT là kiểm DB/principal, không phải JPA/session/HTTP scope.

## Azure và input còn thiếu

`azure-users.sql` là artifact contained-user dùng typed @User/@Role/@Password, EngineEdition=5 guard, no CREATE LOGIN. **BLOCKED Azure:** chưa có account/config và approved Azure authentication caller. Wrapper `-Environment Azure` fail rõ trước SQL; không dùng Windows SSPI local rồi báo đã provisioning online. Azure script chưa được nghiệm thu trên Azure.

Khánh nhận mapping principal, xử lý TransactionRunner/factory/pool/auth/actor: reset SESSION_CONTEXT khi mượn/trả, không dùng context làm authorization; pool budget tổng. Mỗi owner Đông/Liêm/Thái giao catalog/guards/permissions trước mở use case. Vương kiểm lại caller DB/login thật, grant/revoke/deny, direct-SP sai context và HTTP owner/org trên SHA develop đã ghép. Hiện chưa có V03/V04/V07/V08/F05/F10 hoặc SP nên các gate đó còn BLOCKED, không mô phỏng object để tick.
