# Công cụ nghiệm thu — phần nền VUONG-15/M0

Routine nguồn: main `90800f7`; hợp đồng nghiệp vụ/ownership không đổi từ
`04336c2`. Tooling nhận foundation Khánh `7b403f2` trên nhánh riêng từ
develop `d2282d7`. Chưa mặc nhiên coi dependency đã được review/merge develop.
Phần tài chính VUONG-01/02/03 nằm trên các nhánh task riêng và có gate riêng;
không thêm class của Đông để làm chúng compile trên nhánh tooling này.

## Build, test và browser

`mvn -B clean verify` chạy unit/package/formatter, không chứng minh SQL/browser.
`mvn -B -Pbrowser-it clean verify` dùng profile Khánh đã cấp; không đổi pom.xml.
JUnit khởi động WAR đã đóng gói trên Tomcat11.0.25, cổng loopback ngẫu nhiên,
tạo route fixture chỉ trong container test, chạy Node/Playwright, rồi undeploy.
Timeout hữu hạn cho test/process/browser; browser lỗi hoặc thiếu runtime làm
profile fail. Không dùng assumption/skip hoặc trả thành công giả.

Playwright Node khóa1.62.1 cùng lockfile, Node>=20; CI dùng Node24. Cài tại
`src/test/browser`, không cài dependency sản phẩm hoặc đóng Node/browser vào WAR:

```powershell
npm ci --prefix src/test/browser
npm --prefix src/test/browser run install-browser
$env:TC_APP_ENV = 'test'
$env:TC_SQL_HOST = '127.0.0.1'
$env:TC_SQL_PORT = '1'
$env:TC_DATABASE = 'ticketscenter_browser_test'
$env:TC_APP_BASE_URL = 'http://127.0.0.1:8080/ticketscenter'
$env:TC_OTP_HMAC_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
mvn -B -Pbrowser-it clean verify
```

Các biến SQL trên cố ý chỉ vào endpoint không tồn tại, được test guard trước
Tomcat startup. Đây là browser/packaging smoke M0; không kết nối DB demo và
không chứng minh readiness SQL UP. Cấu hình môi trường khác bị từ chối.

Máy đã cài browser có thể chọn `TC_BROWSER_EXECUTABLE` (ví dụ Edge) và
`TC_BROWSER_NODE`; `TC_PLAYWRIGHT_MODULE` là override runtime kiểm thử, phải
cùng phiên bản pin. Lượt kiểm cuối dùng dependency từ npm ci/lockfile,
không cần module override. Linux CI cài Chromium bằng CLI Playwright.
Hướng dẫn tương thích và cài browser: [Playwright](https://playwright.dev/java/docs/browsers).

Máy Windows hiện tại có lỗi JDK25 AF_UNIX: Selector.open tối giản và Tomcat
NIO đều báo Invalid argument: connect. Đã đối chiếu src.zip: PipeImpl dùng
TCP dự phòng khi không bind được Unix socket. Khi gặp đúng lỗi này, một
process-only option trỏ Unix socket tới đường dẫn **chưa tồn tại** cho phép
fallback TCP; không tạo thư mục đó, không đổi cấu hình Java/hệ thống toàn cục:

```powershell
$socketFallback = Join-Path $env:TEMP ('tc-uds-unavailable-' + [Guid]::NewGuid().ToString('N'))
if (Test-Path -LiteralPath $socketFallback) { throw 'Expected absent socket directory' }
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$socketFallback"
# Chỉ process kiểm thử hiện tại; bỏ biến sau khi kiểm.
```

## Phạm vi bằng chứng

Browser thật kiểm live200, render JSP/assets/CSP/JS không lỗi, text được escape,
không overflow ở320/375/768/1440, Tab tới skip-link/brand và outline, bốn UI
states, session CSRF403, cùng-session token, login M0 trả501 rõ ràng.
Screenshot/log ở `target/browser-evidence`, Failsafe report ở target; không
lưu token/cookie/trace phiên. Không nhận register/pay/refund/payout đã hoạt động.

Điểm giao Khánh: main-content chưa có tabindex, nhấn Enter skip-link hiện không
chuyển focus tới main. Smoke M0 giữ đúng contract Tab/outline đã giao, không
chứng minh skip-target focus đã đạt. Cần sửa/review layout cùng Khánh trước
nghiệm thu accessibility toàn UI ở mốc sau; phiên này không sửa layout owner.

## CI và các gate còn thiếu

`.github/workflows/verify.yml` là workflow VUONG-15: build/unit/formatter/WAR
và browser foundation trên PR/develop/feature Vương; quyền contents:read,
timeout, artifact report/screenshots, không deploy hoặc báo PASS cố định.
SQL/whole-project E2E cần runner/test DB/fixture/principal và artifact owner.
Chưa khai báo một job SQL giả hoặc nhận việc không chạy là PASS.

Các workflow có sẵn từ develop do thành viên khác giao vẫn giữ nguyên. Chúng
đang tham chiếu Day06IT/07IT/08IT/09IT/11IT và FeaturePackageStructureTest chưa
có, dùng tên DB/biến môi trường khác runbook và có summary PASS cố định. Cần
owner phối hợp nối test target/manifest/secret và sửa summary trước nghiệm thu
CI toàn nhóm. Workflow mới không chứng minh các workflow đó đã xanh.

VUONG-15 chỉ giao tooling M0. WholeProjectIT SQL/runtime và E2E luồng nghiệp vụ
M1–M4 sẽ bổ sung khi đủ artifact chuẩn. VUONG-01/02/03 cần schema Đông/Liêm/Thái,
FK/seed và mapping JDBC thật; không đóng M0 toàn nhóm từ browser smoke.
