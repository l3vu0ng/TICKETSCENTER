# Test database runbook — VUONG-02, phần nền

Yêu cầu PowerShell 7, sqlcmd ODBC, SQL Server local; Maven/JDK25 do Khánh cung cấp khi đã tích hợp. Dùng database mới có tên `TicketsCenter_Test_<suffix>` và tài khoản migration riêng được phép DDL. Runner không tạo/drop/reset database, không chạm DB demo. Database test phải được người vận hành tạo và chọn rõ; TC_SQL_HOST/TC_TEST_DATABASE bắt buộc, không đoán server hoặc database mặc định.

```powershell
$env:TC_SQL_HOST = 'localhost'
$env:TC_TEST_DATABASE = 'TicketsCenter_Test_<suffix-thuc-te>'
# Bỏ qua trust chain chỉ cho SQL local có certificate tự ký; online cần certificate hợp lệ.
pwsh -NoProfile -File database/migrate.ps1 -Scope FinancialFoundation -TrustLocalCertificate
pwsh -NoProfile -File database/migrate.ps1 -Scope FinancialFoundation -TrustLocalCertificate
sqlcmd -S $env:TC_SQL_HOST -d $env:TC_TEST_DATABASE -E -C -I -f 65001 -b -i database/tests/vuong/VUONG-01.sql
sqlcmd -S $env:TC_SQL_HOST -d $env:TC_TEST_DATABASE -E -C -I -f 65001 -b -i database/tests/vuong/VUONG-02.sql
```

Expected: APPLIED lần đầu, UNCHANGED lần hai; history 1 dòng 0050; sáu bảng tài chính, fixture SQLtest rollback sạch. Đây là **schema tài chính độc lập**, không phải bộ migration toàn dự án. Không chạy 0050 trực tiếp rồi thêm history thủ công; runner quản lý cả DDL/history. Lỗi sau mutation rollback; không ghi history của file thất bại. Dữ liệu/chính sách payout vẫn chưa có caller nghiệp vụ.

```powershell
pwsh -NoProfile -File database/migrate.ps1 -PreflightOnly
pwsh -NoProfile -File database/tests/vuong/VUONG-02.ps1 -RepositoryRoot (Get-Location).Path
# Full hiện phải fail vì thiếu 0020/0030/0040/0100; 0700 đến từ task03.
# PreflightOnly kiểm file/checksum/order, không chứng minh SQL integration.
```

Authentication mặc định Integrated (`-E`). Nếu SQL login đã được cấu hình, dùng `-Authentication SqlLogin`, TC_TEST_LOGIN và SQLCMDPASSWORD nạp qua secret mechanism vào environment process. Không `-P`, không lưu password vào source/manifest/log. TC_TEST_LOGIN phải là login migration test, không runtime buyer/manager. Runner không đổi password hoặc cấu hình server.

Trên nền Khánh đã ghép (hoặc checkout kết hợp có SHA được ghi trong evidence):

```powershell
$env:TC_TRUST_LOCAL_CERTIFICATE = 'true' # chỉ localhost
$env:TC_MIGRATION_SCOPE = 'FinancialFoundation' # chủ động chọn kiểm phần nền
mvn -B -Psqlserver-it -Dit.test=VuongMigrationIT verify
```

VuongMigrationIT gọi runner hai lần và SQLtest01/02 bằng sqlcmd thật. Thiếu environment là failure, không skip. Khi bỏ TC_MIGRATION_SCOPE, scope Full được dùng và phải fail nếu thiếu miền. Không dùng Foundation PASS để tick VUONG-02 tổng hợp. Trên Windows test helper hiện kiểm Integrated authentication; kiểm JDBC/TransactionRunner/principal pool vẫn cần Khánh cung cấp.

Sau khi nhận đủ schema: owner cung cấp file+checksum+cột/keys → đăng ký manifest dependency → 0100 ghép FK → chạy Full trên DB mới → seed từ registry qua builder owner → inventory → repeat/checksum drift/fault/outer TX → evidence SHA tích hợp. Order.acceptedPayment và Refund.currentAttempt cần SP kiểm đúng aggregate, FK đơn không đủ. Không có lệnh reset demo; cleanup DB test riêng theo tên đã kiểm bởi người tạo.

Nền identity thật đã nhận từ Khánh `7b403f2`. Trên DB test **mới, rỗng**,
chọn `-Scope IdentityFinancialFoundation` hoặc đặt
`TC_MIGRATION_SCOPE=IdentityFinancialFoundation` cho `VuongMigrationIT`.
Expected: APPLIED 0010/0011/0050 lần đầu, cả ba UNCHANGED lần hai;
`database/tests/vuong/VUONG-02-identity.sql` kiểm owner/checksum/history,
ba bảng identity, năm constraint được trust và binary normalization collation.
Test này không seed người dùng hoặc nghiệm thu auth; không dùng schema identity
để coi FK các miền Đông/Liêm/Thái đã có. Full vẫn chặn thiếu schema/0100.
