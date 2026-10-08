# DONG-01 — triển khai lại hợp đồng miền

Ngày kiểm: 2026-10-08 (Asia/Bangkok). Trạng thái toàn task: **BLOCKED / chưa nghiệm thu**.

## Revision và nguồn

- Nhánh: `feature/dong/dong-01-domain-contracts-rebuild`, rẽ từ `origin/develop`.
- `testedRevision`: `04336c267c38c959d3bceaa59cfc08c7ee828407` + working-tree diff DONG-01 trước checkpoint.
- Source chuẩn đã đọc đầy đủ trong phiên: `spec.md`, XML `docs/classdiagram/diagram.md`, toàn bộ `TEAM-CONTRACT.md`, `CONVENTIONS.md`, `API-MAP.md`, `COVERAGE.md`, `GIT-WORKFLOW.md`, `dong.md` và task dependency KHANH-01/02/03/04/06/11, LIEM-01/02, THAI-01, VUONG-01/02/03. Đối chiếu lại trước viết mã: các file này không thay đổi so với SHA nguồn.
- PROJECT-ROUTINE được đọc từ remote main, nơi tài liệu đã được thêm; chưa nằm trong develop. Ownership và chữ ký dùng TEAM-CONTRACT/file cá nhân. Không tạo type production của người khác để che dependency thiếu.

## Đầu ra

36 file main Java: 11 model/value/enum, 17 DTO/command, 7 Service interfaces và MembershipAuthorizationRepository. Sáu file kiểm chứng: OrganizationTest, EventTest, ZoneTest, SeatTest, DongContractIT và DomainValuesCheck.

- Organization: cùng hồ sơ DRAFT → PENDING_APPROVAL → APPROVED/REJECTED; quyết định lặp giữ kết quả, requester bắt buộc khi gửi.
- Event: lịch đúng, gửi/duyệt/từ chối, rule cùng tổ chức và hiệu lực, khóa sửa sau gửi, cancel trước start; cửa bán/check-in nửa mở.
- Zone: khu ngồi/đứng, nhãn A…Z/AA, quota, VND nguyên 0…9999999999999999999; layout khóa sau publish nhưng mutation kho hợp lệ còn dùng được khi hủy.
- Seat: kiểm đúng Seat/Zone/Event/quantity cho hold item và Ticket REFUNDED trước trả kho. Persistence của Liêm/Thái phải chứng minh ownership/replay/locking; model riêng lẻ không chứng minh exactly-once.
- Service/Repository chỉ khai báo chữ ký M0; không đăng ký endpoint và không trả DTO thành công giả.

Đây là model thuần Java cho DONG-01. JPA mapping/schema và round-trip thuộc DONG-02, chưa thực hiện. createdAt được caller truyền; submittedAt/decidedAt chưa được điền bằng persistence use case. Contact email yêu cầu helper chuẩn hóa của Khánh trước factory. Không tự thêm clock/transaction/query/storage vào Model.

## Môi trường thực tế

- Windows, PowerShell; JDK tìm thấy tại `C:\Users\Lenovo\.jdks\openjdk-25.0.2`.
- `java -version` qua đường dẫn tuyệt đối: OpenJDK 25.0.2, build 25.0.2+10-69. JDK chưa có trên PATH nhưng chạy được bằng đường dẫn này.
- Maven không có trên PATH; baseline pom chưa có dependencies/plugins JPA/JUnit/Mockito và WAR build của KHANH-01.
- `sqlcmd` có sẵn. TC_SQL_HOST và TC_TEST_DATABASE chưa cấu hình; không kết nối/chạy DB, không có dữ liệu before/after để báo cáo.

## Expected / actual

| Kiểm chứng | Expected | Actual | Exit | Trạng thái |
|---|---|---|---|---|
| Ca đỏ độc lập trước tạo source | Compiler phát hiện thiếu EventSchedule/EventDetails/EventCategory | 15 lỗi thiếu package/class trên harness tạm | 1 | RED như dự kiến |
| Biên dịch phần độc lập | Value types, enums, DTO không dùng peer type và executable check compile | 20 source files compile | 0 | PASS chỉ phần độc lập |
| Chạy DomainValuesCheck | Lịch sai/null bị chặn; saleEnd=start nhận; trim/NFC/giới hạn đúng | `PASS: 12 domain value assertions (no peer types or DB)` | 0 | PASS chỉ 12 assertion |
| Biên dịch toàn bộ main source với JDK | Toàn bộ source compile sau khi dependencies được ghép | 99 lỗi thiếu peer types và jakarta.persistence; đã rà soát categories diagnostics | 1 | BLOCKED |
| Maven bốn unit suites | Bốn suites thực thi và assertions đạt | `mvn` không được nhận diện; chưa chạy JUnit/Mockito suites | 1 | BLOCKED |
| DongContractIT | API/DTO/model và chữ ký User typed-Organization được kiểm | Source đã tạo, chưa compile/run do dependencies/tooling thiếu | — | BLOCKED |
| JPA/SQL/HTTP/browser | Kiểm trên DB/container/browser thật theo task liên quan | Chưa chạy; DONG-01 không thêm SQL/UI/endpoint | — | BLOCKED phần tích hợp |

Lần truyền absolute source paths chứa tiếng Việt vào javac bị lỗi tên file (exit 2). Đã sửa lệnh kiểm chứng dùng relative ASCII paths; compile phần độc lập sau đó exit 0. Đây là vấn đề gọi compiler trên máy này, không phải thay đổi package/path dự án.

## Lệnh tái chạy phần độc lập

Chạy từ repository root bằng PowerShell. Source được chọn tường minh theo phần độc lập, không chèn peer stub.

```powershell
$taskJdk = 'C:\Users\Lenovo\.jdks\openjdk-25.0.2\bin'
$taskWorkspace = (Get-Location).Path
$taskModelRoot = 'src/main/java/vn/ticketscenter/model'
$taskSources = @('EventCategory','EventDetails','EventSchedule','EventStatus','ZoneType','SeatStatus') |
    ForEach-Object { "$taskModelRoot/event/$_.java" }
$taskSources += "$taskModelRoot/identity/OrganizationStatus.java"
$taskSources += @(Get-ChildItem 'src/main/java/vn/ticketscenter/dto/event' -File -Filter '*.java' |
    ForEach-Object { [IO.Path]::GetRelativePath($taskWorkspace, $_.FullName) })
$taskSources += @('OrganizationCommand','OrganizationDto','RequestFilter') |
    ForEach-Object { "src/main/java/vn/ticketscenter/dto/identity/$_.java" }
$taskSources += 'src/test/java/vn/ticketscenter/model/event/DomainValuesCheck.java'
& "$taskJdk\javac.exe" -encoding UTF-8 -d target/dong-01-check/independent @taskSources
if ($LASTEXITCODE -ne 0) { throw 'Independent compilation failed' }
& "$taskJdk\java.exe" -cp target/dong-01-check/independent vn.ticketscenter.model.event.DomainValuesCheck
if ($LASTEXITCODE -ne 0) { throw 'Independent domain checks failed' }
```

Lệnh toàn main source đã thử: cùng JDK, `javac -encoding UTF-8 -Xmaxerrs 300 -d target/dong-01-check/full` với danh sách mọi main Java file. Diagnostic categories: cannot find symbol; package jakarta.persistence/dto.common/dto.settlement/model.fulfillment/model.order/model.settlement/model.ticketing does not exist.

Lệnh Maven đã thử: `mvn -B '-Dtest=OrganizationTest,EventTest,ZoneTest,SeatTest' test`. Lệnh IT phải chạy khi hạ tầng khả dụng: `mvn -B -Psqlserver-it '-Dit.test=DongContractIT' verify`. Không dùng javac độc lập thay kết luận Maven/WAR/SQL PASS.

## Blocker / người cung cấp / bước tiếp

| Owner/task | Artifact cần có trên develop | Tác động |
|---|---|---|
| Khánh KHANH-01/02/04 | Build dependency; ActorContext/Page/PageRequest; User/OrganizationRole/UserStatus | Compile toàn module và chạy unit/contract |
| Liêm LIEM-01 | TicketHoldItem, Ticket/TicketStatus, Coupon | Seat contract tests và Organization association |
| Vương VUONG-01 | CommissionRule, Settlement, CommissionPolicyCommand | Publish/model graph/approval contract |
| Khánh KHANH-03, Vương VUONG-02/03 | Test harness/manifest/fixture/principals và DB cấu hình thật | DONG-02 persistence/SQL verification tiếp theo |

Nhánh Khánh `1edbced` chưa được ghép vào develop. User ở đó dùng Object trong assignRole/revokeRole/map thay Organization; DongContractIT sẽ yêu cầu chữ ký đúng khi ghép. Cần chủ lớp sửa, Đông không sửa file User.

Checkpoint theo GIT-WORKFLOW §4 chỉ lưu nguồn chưa đủ dependency; task không được đóng từ checkpoint. Sau khi artifact M0 được ghép, cập nhật feature từ develop, biên dịch đầy đủ, chạy bốn unit suites/DongContractIT, rồi tiếp tục DONG-02. Không thay đổi spec/hợp đồng chung/pom/SQL/Servlet/UI, không push/merge/deploy.
