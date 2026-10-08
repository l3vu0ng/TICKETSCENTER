# DONG-02 — source schema M0 và audit tiến độ

Ngày kiểm 2026-10-08 (Asia/Bangkok). Trạng thái task: **BLOCKED / chưa nghiệm thu SQL/JPA**.

## Revision / nguồn / ownership

- Nhánh `feature/dong/dong-02-event-schema` rẽ từ origin/develop mới nhất.
- testedRevision: `d2282d702445911e65d174a1e853273c4164bf96` + diff DONG-02 trước checkpoint.
- Source spec/diagram/TEAM/CONVENTIONS/API-MAP/COVERAGE/GIT-WORKFLOW/dong.md và các task dependency đã được đọc đầy đủ trong phiên trước. Kiểm diff lại với baseline 04336c2: các hợp đồng bắt buộc này không đổi. Đọc lại DONG-02 và KHANH-04/VUONG-01/02/03; đọc M0 PROJECT-ROUTINE mới trên main eedda3f.
- Chỉ tạo file sở hữu Đông. Registry/manifest/database README/0100, build/test harness và các model thành viên khác chưa được sửa, ghép hoặc thay bằng stub.
- DONG-01 checkpoint local `68f311062f46b4a094d0376052e9552528fcd38d` giữ trên nhánh `feature/dong/dong-01-domain-contracts-rebuild`. Chưa ở develop; không cherry-pick/merge để coi dependency đã nghiệm thu.

## Tiến độ M0 theo artifact trên refs đã fetch

Đây là audit các remote refs và checkpoint Đông trong workspace này. Không suy ra trạng thái từ tên commit hoặc tài liệu task; không quan sát được working tree riêng chưa push của thành viên khác.

| Thành viên | Source có bằng chứng tồn tại | Phần chưa đủ để nghiệm thu |
|---|---|---|
| Khánh | Feature `1edbced` có pom WAR/dependency, common types/clock, User và 0010_identity.sql | Chưa ghép develop; chưa thấy TransactionRunner/layout đầy đủ. docs/evidence/khanh chỉ .gitkeep; claim 15 unit PASS trong commit chưa được xác minh ở đây. User.assignRole/revokeRole/map còn dùng Object thay Organization |
| Đông | DONG-01 checkpoint 68f3110 có model/DTO/API/test source, độc lập 20 source compile và 12 assertions đã chạy. DONG-02 source trong diff này | Full compile/unit, JPA mapping, SQL execution, registry/manifest/cross keys còn BLOCKED |
| Liêm | Chưa thấy LIEM-01/02 artifacts trên các remote refs hiện có | 7 models, DTO/worker SPI, 0030_sales.sql chưa có bằng chứng trên develop/main |
| Thái | 5 enum từ 8babf1f được ghép vào develop/main | Chưa có Refund/createCompensation, DTO/schema 0040. Package fulfillment/model còn khác contract model/fulfillment |
| Vương | Cấu hình CI được thêm vào develop qua 709ab73/d2282d7 | Chưa thấy financial models/0050/manifest/0100/fixture/grants/evidence M0. Có workflow không đồng nghĩa tests/SQL đã chạy đạt |

Refs kiểm: origin/develop `d2282d7`; origin/main `eedda3f`; origin/enhance/refund_func `d2282d7`; origin/feature/khanh/khanh-01-war-foundation `1edbced`. PROJECT-ROUTINE mới trên main đã đồng bộ mã task M0 của Thái/Vương với task cá nhân; develop còn bản routine cũ.

Lưu ý evidence CI: workflow 02-unit-integration-tests.yml có summary PASS literal trong step `if: always()`; không dùng dòng này để kết luận PASS. Workflow gọi Day06IT/Day07IT/... trong khi task cá nhân dùng named IT và baseline chưa có test profiles/artifacts tương ứng. Không có run log/report đã được xác minh trong audit này. M0 chưa đạt hai gate compile toàn dự án và fresh database đủ FK.

## Source Đông đã tạo

- 0020_organizations_events.sql: sáu bảng, C02/03/04/05/10, CHECK kỹ thuật, năm FK nội miền NO ACTION. Atomic standalone, savepoint trong transaction caller, fail rõ khi objects đã tồn tại.
- event-categories.sql: seed theo registry JSON do Vương cung cấp trên connection; bind qua SESSION_CONTEXT, không tự sinh UUID. Validate registry, khóa range update/insert, từ chối đổi ID/code mapping; giữ references khi chạy lại.
- DONG-02.sql: 18 ca constraint lỗi được kiểm bằng THROW/error number/constraint name; các ca hợp lệ equality/quota3/giá0/default, multirow atomicity và outer/savepoint rollback. Chưa chạy.
- UserOrganizationRole.java / JpaMembershipAuthorizationRepository.java: đã được thêm trong checkpoint 968d52e nhưng chưa compile được; đã rút khỏi source trong lần sửa CI bên dưới. Mã còn trong lịch sử Git; phần mapping/query vẫn BLOCKED.
- dong-schema.md: data dictionary, giới hạn kỹ thuật, FK proposal cho Vương, manifest/registry transport và các phần thiếu.

## Môi trường / expected / actual

Windows + PowerShell, OpenJDK 25.0.2 tìm thấy ngoài PATH. SQL Server ScriptDom từ SSMS 22, assembly version 17.0.0.0; parser TSql160Parser. Không cấu hình TC_SQL_HOST/TC_TEST_DATABASE. sqlcmd có sẵn; không có test DB/fixture/runner đã được xác nhận. Thử đọc service inventory bị quyền hệ điều hành chặn; không suy ra database đang chạy hay đã kết nối được.

| Check | Expected | Actual | Exit / status |
|---|---|---|---|
| ScriptDom parse ba SQL scripts | 0 syntax error | Cả migration/seed/test đều 0 parse error | 0; PASS chỉ syntax |
| Rà cấu trúc migration | 6 tables; 5 FK nội miền; không FK sang User/rule | 6 tables; 5 FK; 0 cross-domain FK target | 0; PASS chỉ cấu trúc source |
| Prepared negative SQL cases | 18 case và fail nếu chạy thiếu | Static count 18; SQL source kiểm @passed=18 | PASS source count; chưa runtime |
| javac current main source | Compile với actual dependencies | 47 errors, thiếu Jakarta và User/Organization/role/DTO/interface | 1; BLOCKED |
| Maven DongSchemaIT | Compile/run test bằng profile/harness thật | mvn không được nhận diện; DongSchemaIT chưa tạo vì thiếu mapping/harness | 1; BLOCKED |
| SQL schema/constraints/rollback/seed reapply | SQL Server thật, dữ liệu trước/sau và fixture | Chưa chạy vì thiếu test target/registry/manifest | BLOCKED; không có DB before/after |

## Lệnh kiểm chứng

Parse SQL tĩnh đã chạy:

```powershell
Add-Type -Path 'C:\Program Files\Microsoft SQL Server Management Studio 22\Release\Common7\IDE\Extensions\Application\Microsoft.SqlServer.TransactSql.ScriptDom.dll'
$taskParser = [Microsoft.SqlServer.TransactSql.ScriptDom.TSql160Parser]::new($true)
foreach ($taskFile in @('database/migrations/0020_organizations_events.sql',
    'database/seeds/event-categories.sql','database/tests/dong/DONG-02.sql')) {
    $taskErrors = [System.Collections.Generic.List[Microsoft.SqlServer.TransactSql.ScriptDom.ParseError]]::new()
    $taskReader = [System.IO.StringReader]::new((Get-Content -Raw -LiteralPath $taskFile))
    $taskAst = $taskParser.Parse($taskReader, [ref]$taskErrors)
    $taskReader.Dispose()
    if ($taskErrors.Count -ne 0) { throw "Parse failed: $taskFile" }
}
```

`javac -encoding UTF-8 -Xmaxerrs 5 -d target/dong-02-check/classes` đã chạy với danh sách source relative paths, dùng executable `C:\Users\Lenovo\.jdks\openjdk-25.0.2\bin\javac.exe`. `mvn -B -Psqlserver-it '-Dit.test=DongSchemaIT' verify` đã thử, chưa khởi chạy được.

Lệnh DB phải theo runbook/principal/test DB thật do Vương/Khánh cấp. Test runner phải đặt TC_TEST_DATABASE context trên cùng connection trước chạy DONG-02.sql; seed cần TC_EVENT_CATEGORY_REGISTRY. Không chạy script vào database chưa được xác nhận là test. Không báo parser PASS là SQL Server PASS.

## Blocker và bước tiếp

1. KHANH-01/02/04 + DONG-01 + LIEM-01 + VUONG-01: type/dependency chưa ghép develop; model mapping Đông chưa hoàn thiện/JPA chưa validate.
2. KHANH-03: API test/EntityManager harness để tạo DongSchemaIT, verify projection inactive/no-link/correct-org và same connection.
3. VUONG-02: category IDs/fixture registry, manifest/checksum/database README và ghép FK proposal vào 0100. Không tự gán ID danh mục.
4. VUONG-03 + môi trường: principal/credential config và test database thật; kiểm migrations/constraints/multirow/outer rollback/seed repeat, grants riêng.

Checkpoint chỉ lưu source; chưa hoàn thành DONG-02 hoặc M0. Phần còn lại cần các artifact nêu trên rồi chạy SQL/JPA test thật trước nghiệm thu. Không push/merge/deploy hoặc sửa cấu hình GitHub.

## Sửa lỗi compiler sau lần push

Người dùng cung cấp CI log ngày 2026-10-08: Maven compiler 3.15.0 biên dịch 7 main sources, thất bại do jakarta.persistence, Organization/User/role, MembershipAccess và MembershipAuthorizationRepository không tồn tại trên revision đã push. Đây là lỗi đưa Java phụ thuộc vào nhánh schema độc lập, không phải formatting. Tái hiện bằng OpenJDK 25.0.2 trước sửa: javac exit 1, 47 errors.

Đã rút hai file Java phụ thuộc khỏi source; giữ migration/seed/SQL tests. Không thay pom, workflow, compiler excludes, test flags hoặc tạo bản sao type của thành viên khác. Expected sau sửa: toàn bộ main source hiện có compile bằng JDK 25; task DONG-02 vẫn chưa nghiệm thu JPA/SQL. Hai bản source trước đó nằm ở commit 968d52e và có thể review bằng git show.

Fetch mới nhất: develop đã tiến đến 98ba483 và ghép nền Khánh; main ở 90800f7. Chữ ký User vẫn dùng Object, DONG-01/types của Liêm/Vương còn thiếu. Phần audit M0 phía trên phản ánh thời điểm d2282d7, không dùng làm kết luận cho develop mới này. Không tự merge develop hoặc sửa source chung trong lần sửa compiler.

testedRevision lần sửa: 968d52e + diff rút hai file Java và cập nhật tài liệu. Lệnh `javac --release 25 -encoding UTF-8 -d target/dong-02-fix/after` với **toàn bộ** main Java source còn lại: exit 0, compile 5 files. Không có compiler excludes hoặc peer stub. Parse lại ba SQL scripts với ScriptDom: 0 lỗi, exit 0; git diff --check exit 0. Maven không có trên PATH và chưa tìm thấy executable trong Downloads/workspace, nên chưa tái chạy nguyên lệnh Maven CI ở máy này; không báo toàn pipeline PASS từ kiểm chứng javac. SQL/JPA vẫn BLOCKED.
