# VUONG-01 — chính sách phí ban đầu, phần M0

## Cập nhật M0 ngày 08/10/2026 — Model và hợp đồng tài chính

Nhánh `feature/vuong/vuong-01-finance-contracts-part-2`, tiếp nối `be8fabc`;
nền develop vẫn `04336c267c38c959d3bceaa59cfc08c7ee828407`.
Đã đối chiếu routine ở main `baa15e76ef40c9e7bf030b05d4f857419fb75d50`
với spec/diagram, TEAM-CONTRACT, CONVENTIONS, API-MAP, COVERAGE, GIT-WORKFLOW
và task Vương. Routine gán nhầm task: 0050 thuộc VUONG-01, 0100 thuộc
VUONG-02; VUONG-04 là chính sách phí/F02/TR03 ở M1 theo hợp đồng chuẩn.
Giữ task ID chuẩn, chỉ sửa file Vương sở hữu.

**Đã cung cấp:** hai entity với quan hệ có kiểu `Organization`/`Event`,
enum trạng thái/kết quả, bốn mapping persistence, DTO tài chính, interface
CommissionService (gồm getEffective cho Đông) và SettlementService.
Các interface chưa có implementation M1/M3; không trả thành công giả.
Không tạo business class Payout/SettlementItem, không chép shared types của Khánh.

| Diagram/schema | File/hành vi | Kiểm chứng |
|---|---|---|
| ratePercent, fixedFee, effectiveFrom/effectiveTo, Organization | CommissionRule: BigDecimal/Instant, ManyToOne, PK/version; không setter sửa applied terms | Compile trên nền Khánh + fixture quan hệ |
| isEffectiveAt | Khoảng [from,to), thiếu now trả400 | CommissionRuleTest: bốn biên thời gian/null |
| calculateFee | HALF_UP đồng nguyên, cap remaining; 0 trả0; từ chối null/âm/ngoài decimal | CommissionRuleTest: 12 ca, gồm 200001→5000, 20→20, 25→3 |
| Event, status, gross/refund/commission, paid/pending | Settlement: OneToOne unique eventId, PK/version, confirmedAt; BigDecimal trực tiếp | SettlementTest + SQL01 |
| /netPayable, /availableToPay | Getter dẫn xuất, @Transient; không ghi vào computed SQL columns | Tổng 500000−100000−50000=350000; pending trừ available |
| recalculate/confirm | Chỉ DRAFT; kiểm toàn bộ trước mutation; chốt tổng, net0 thành PAID | SettlementTest: frozen totals, invalid-input atomicity, zero-net |
| beginPayout/recordPayoutResult | Reserve theo số dư; FAILED giải phóng; SUCCEEDED tăng paid; PAID chỉ paid=net và pending=0 | SettlementTest: hai reservation, failed/new attempt, partial/full paid, vượt số dư |
| Snapshot/log/provider/audit | Composite snapshot PK; UUID IDs; enum strings; nvarchar; Instant UTC datetime2; @Immutable read projections | Biên dịch đạt; JPA→SQL roundtrip BLOCKED (TCP local tắt) |

**Ranh giới payout:** Service/SP16 phải khóa Settlement, kiểm log cùng payoutId/
amount và replay trước gọi model. Model kiểm invariant tổng; không giữ map lịch
sử hoặc tự chứng minh idempotency. Confirm cũng cần Service/SP15 kiểm endTime,
nguồn tiền và nghĩa vụ chưa giải quyết trong cùng transaction. M0 không phải
nghiệm thu payout/confirmation nghiệp vụ M3.

### Kiểm chứng mới, không sử dụng kết quả lịch sử để báo PASS

JDK25, Maven3.9.16; archive nền Khánh đúng `1edbceda45063491c662d09600c7b4f2e2d779b9`
và overlay source/test Vương trong thư mục tạm. Chỉ thư mục tạm có hai class
quan hệ rỗng Organization/Event phục vụ compilation; không tạo bảng owner,
không commit skeleton vào repository. Unit dùng fixture JPA no-arg chỉ làm
tham chiếu, không gọi/test hành vi chủ miền.

| Lệnh/ca | Actual | Trạng thái |
|---|---|---|
| Test viết trước model, `mvn -B -Dtest=CommissionRuleTest,SettlementTest test` | Thiếu model/type, exit1 | RED |
| Thử Mockito trên JDK25 | Byte Buddy nền không nhận Java25; bỏ nhu cầu Mockito trong test mới | Lỗi hạ tầng được tránh bằng fixture, không đổi pom Khánh |
| `mvn -B clean package` trên checkout kiểm riêng | 62 unit: 15 Khánh + 21 validator + 12 CommissionRule + 14 Settlement; 0 failure/error/skipped; WAR | PASS riêng, không phải develop tích hợp |
| `mvn -B -Psqlserver-it -Dit.test=VuongMigrationIT,DatabasePrincipalsIT,AuthorizationMatrixIT verify` | 62 unit + 3 IT; 0 failure/error/skipped; SQL01 20 constraint rejections và rollback thật | PASS nền tài chính/quyền local |
| Thêm `VuongFinancialMappingIT` vào danh sách IT | Driver TCP localhost:1433 connection refused; IT lỗi, không skip | BLOCKED JPA mapping roundtrip |
| Đọc registry SQL Server / listener / connection hiện hành | TCP Enabled=0; kết nối SQLcmd Shared memory; chỉ listener local1434 | Chưa đổi/restart cấu hình SQL Server |
| `mvn -B -DskipTests compile` trên nhánh thực | Thiếu dependency jakarta/hibernate/shared types/Organization/Event | BLOCKED build tích hợp |

Mapping IT có guard DB test, validate schema trước insert, đọc BigDecimal/
UUID/enum/UTC100ns/Unicode qua Hibernate và rollback + kiểm qua session mới.
Tái lập với SQL Server bật TCP, `TC_JDBC_URL` (databaseName=TicketsCenter_Test_*),
`TC_JDBC_USER`, `TC_JDBC_PASSWORD` từ process environment và principal test chỉ
có quyền SELECT/INSERT trên năm bảng tài chính được dùng. Không chứa password
trong URL/lệnh/evidence. Test chưa chạy tới assertion đọc dữ liệu, **không nhận
mapping đã được nghiệm thu SQL**.

Hai lượt SQL dùng database test và prefix login ngẫu nhiên riêng. Bốn runtime
login đăng nhập thật; admin đọc CommissionRule, ba principal còn lại bị lỗi229.
Ca GRANT/REVOKE/DENY có thực chứng SQL03. Test rows rollback về0; database và
năm login của mỗi lượt (bốn runtime + một mapping probe) được dọn, kiểm master
xác nhận CLEANUP PASS. Không động database demo hoặc file cấu hình người dùng.
Log lọc: [VUONG-01-m0-validation.log](VUONG-01-m0-validation.log).

**Còn thiếu để đóng M0:** Khánh đưa foundation/shared types vào develop;
Đông giao Organization/Event và 0020; Khánh/Liêm/Thái giao 0010/0030/0040
để ghép 0100/seed thật. Chưa có peer review chấp thuận. Chưa push/merge.
Các phần dưới đây là evidence lịch sử, được giữ để truy vết; danh sách
“chưa có Model” ngày07 đã được cập nhật bằng phần ngày08 này.

---

## Tiếp tục: schema độc lập đã kiểm SQL Server thật

Ngày 07/10/2026, sau commit policy `85192d4d04e3e7f2c3e40938b58d4b617b26d16c`; hợp đồng và remote không đổi. Thêm `database/migrations/0050_settlements_audit.sql` và `database/tests/vuong/VUONG-01.sql`, chỉ vùng Vương sở hữu. Commit chứa thay đổi này và evidence là revision schema được kiểm (lấy `git log -1 --format=%H -- database/migrations/0050_settlements_audit.sql`).

SQL Server local `17.0.1135.8`, Enterprise Developer Edition; Windows integrated authentication. Database riêng `TicketsCenter_Test_Vuong_b196ce73f3d8`, ban đầu 0 bảng; sau migration 6 bảng, sau test 0 dòng nghiệp vụ. Không kết nối database demo. `-C` chỉ dùng localhost với certificate local chưa được trust; không áp dụng chính sách này cho online. Log: [VUONG-01-schema-validation.log](VUONG-01-schema-validation.log).

| Lệnh/ca | Expected | Actual | Exit / trạng thái |
|---|---|---|---|
| `sqlcmd -S localhost -d <testDb> -E -C -I -b -i database/tests/vuong/VUONG-01.sql` trước schema | Thiếu schema phải fail | THROW 51001 | 1 / RED |
| Migration lần đầu khi sqlcmd chưa bật QUOTED_IDENTIFIER | Computed PERSISTED cần SET đúng | SQL 1934; điều tra DBCC USEROPTIONS thấy thiếu option, chỉ CommissionRule đã tạo | 1 / lỗi được sửa trước commit |
| Dọn duy nhất bảng rỗng trong DB test riêng; chạy `0050` đã sửa SET và transaction | Có sáu bảng, C16–19/PK/unique/enum/NOT NULL | Tạo đủ CommissionRule, Settlement, SettlementOrderSnapshot, SettlementTransferLog, AuditLog, MockPayoutProviderLedger | 0 / PASS schema |
| Chạy lại SQLtest01 với `-I -b` | 20 rejection đúng 547/515/2627; net350000; rollback fixtures | Tất cả đạt; 0 dòng còn lại | 0 / PASS constraint thật |
| DB test mới: caller BEGIN TRAN → 0050 → kiểm @@TRANCOUNT=1 → THROW sau mutation → ROLLBACK | Không commit caller; không giữ schema dở dang | Sáu bảng rollback, 0 bảng còn lại; DB test phụ được xóa sau kiểm | 0 / PASS transaction schema |

`netPayable` và `availableToPay` là computed PERSISTED, không có setter; các tổng snapshot và paid/pending là decimal(19,0). FK nội miền snapshot/log→Settlement có thật, no cascade delete. FK organization/event/order/actor sẽ ở 0100 khi nhận schema thật; chưa tạo bảng thay thế. MockPayoutProviderLedger chỉ là cấu trúc dữ liệu **mô phỏng bền vững**, chưa có adapter/payout tích hợp và không được coi nhà cung cấp thật.

**Handoff cho Đông:** bảng CommissionRule có `id, organizationId, ratePercent decimal(19,6), fixedFee decimal(19,0), effectiveFrom/effectiveTo datetime2(7), version int`. SP01 tạo rule cùng APPROVED/MANAGER trong transaction của Đông. `applied` là trạng thái suy ra từ Event dùng rule, không thêm cờ SQL tùy ý. Đông phải ghép schema/caller và review trước tích hợp.

**Còn BLOCKED:** CommissionRule/Settlement Java đủ quan hệ/phương thức, bốn JPA persistence records, FK liên miền, SP14–16/TR bảo vệ snapshot/audit, runtime SQL qua JPA và browser. Khánh có PrincipalKind trên feature nhưng chưa có TransactionRunner/nền trong develop; Đông chưa có Organization/Event. Tiêu đề, bảng và checklist bên dưới là evidence của commit policy trước, không phải cập nhật nghiệm thu SQL hiện tại. Toàn VUONG-01 vẫn chưa tick hoàn thành.

- Ngày kiểm: 07/10/2026, Asia/Bangkok. **PARTIAL**: DTO/validator đạt unit trên nền Khánh; toàn task và bản tích hợp develop còn **BLOCKED**.
- Nhánh: `feature/vuong/vuong-01-finance-contracts-part-1`, rẽ từ develop `04336c267c38c959d3bceaa59cfc08c7ee828407`.
- Dependency đã kiểm riêng: Khánh `1edbceda45063491c662d09600c7b4f2e2d779b9`, nhánh `feature/khanh/khanh-01-war-foundation`. Dependency này chưa vào develop/main khi kiểm; không có PR trên GitHub.
- Evidence, log và ba file Java được commit cùng nhau với footer `Task-Id: VUONG-01`. SHA của commit chứa evidence là revision đầu ra; lấy bằng `git log -1 --format=%H -- src/main/java/vn/ticketscenter/service/settlement/CommissionPolicyValidator.java` và ghi trong Issue #5. Không tự ghi SHA tương lai.
- Đã đọc spec/diagram, toàn bộ TEAM-CONTRACT, CONVENTIONS, API-MAP, COVERAGE, GIT-WORKFLOW và task Vương; nguồn hợp đồng vẫn là `04336c2`. Bản kiểm trước khi develop xuất hiện giữ ở [preflight](VUONG-01-preflight-04336c2.md); không dùng kết quả cũ làm nghiệm thu phiên này.

## Khánh đã giao gì

Issue #1 tick KHANH-01/02/04. Remote feature có POM WAR, dependency/plugin, shared types/error/clock, User/schema. Kiểm độc lập trên archive đúng SHA: `mvn -B verify` exit 0, **15 unit / 0 failure / 0 error / 0 skipped**, tạo WAR. Đây chứng minh phần build/unit, chưa đủ chứng minh auth/DB/transaction hoặc tất cả tiêu chí của ba task.

Chưa có task evidence của Khánh trong `docs/evidence/khanh/` (chỉ `.gitkeep`). Chưa có TransactionRunner; AuthService là skeleton. User còn dùng Object cho organization role, chưa ghép Organization đúng hợp đồng. SQL health IT được phép skip khi thiếu TC_APP_BASE_URL; profile browser không có browser IT. Những điểm này giao Khánh xử lý/review; Vương không sửa file Khánh hay coi checkbox là evidence tích hợp.

## File và hợp đồng đã triển khai

| File của Vương | Đầu ra | Người nhận |
|---|---|---|
| `src/main/java/vn/ticketscenter/dto/settlement/CommissionPolicyCommand.java` | Record đúng bốn trường BigDecimal ratePercent/fixedFee, Instant effectiveFrom/effectiveTo | Đông: OrganizationService.approve/SP01; Vương: AdminServlet ở VUONG-12 |
| `src/main/java/vn/ticketscenter/service/settlement/CommissionPolicyValidator.java` | `void validate(CommissionPolicyCommand)`; dùng BusinessException thật của Khánh | Đông gọi trước SQL; VUONG-04 dùng lại |
| `src/test/java/vn/ticketscenter/service/settlement/CommissionPolicyValidatorTest.java` | 21 ca tham số kiểm dữ liệu, lỗi HTTP và precision | Đông/Khánh review; Liêm/Thái review tiền |

Validator từ chối null, tỷ lệ/phí âm, khoản VND lẻ, số vượt decimal(19,6)/decimal(19,0), phần thập phân cần làm tròn, thiếu thời gian hoặc from >= to. Chấp nhận zero, trailing zero không đổi giá trị, giá trị biên SQL và rate > 100: không thêm cap nghiệp vụ, không mặc định 10% và không làm tròn đầu vào. Dùng `BusinessException.badRequest("INVALID_COMMISSION_POLICY", safeMessage)` => HTTP 400 theo module Khánh; không tạo bản sao exception.

Validator không truy cập DB, không mở transaction và không tạo rule. Đông tạo APPROVED/CommissionRule/MANAGER cùng transaction SP01. HTTP body vẫn là `initialPolicy` theo TEAM-CONTRACT §3.5; decimal JSON được adapter Khánh parse theo hợp đồng, không suy ra exponential HTTP syntax từ ca BigDecimal Java. Chưa có caller triển khai trong repo này, chưa kiểm end-to-end approve.

**API:** chỉ thêm kiểu/chữ ký Java đã thống nhất, chưa tạo endpoint. **SQL/UI:** chưa tạo/chạy migration, SP, Servlet/JSP hay ảnh; không có DB trước/sau vì chưa kết nối hoặc mutation. CommissionRule/Settlement/persistence/0050 chưa triển khai do thiếu model quan hệ và bản nền tích hợp.

## Môi trường, fixture và cách kiểm

Windows 11; Maven 3.9.16; OpenJDK `25+36` tại `C:\Users\Le Vuong\.jdks\openjdk-25`. JAVA_HOME/PATH chỉ đổi trong process lệnh; không sửa máy hoặc POM. Fixture thuần Java: FROM `2026-10-06T03:00:00Z`, TO `2027-01-01T00:00:00Z`; không phụ thuộc now và không DB/mock/provider.

Checkout tạm `%TEMP%/ticketscenter-vuong-01-1edbced/checkout` lấy bằng git archive từ SHA Khánh, không merge/cherry-pick lên feature/develop. Overlay **chỉ ba file Java Vương trong bảng**. POM và BusinessException dùng nguyên bản Khánh. Kết quả này là kiểm kết hợp dependency được chỉ rõ, không phải develop tích hợp đã đạt.

| Lệnh/ca thực chạy | Expected | Actual | Exit / gate |
|---|---|---|---|
| Archive Khánh, `mvn -B verify` | Nền WAR có unit thật | 15/15 unit, không skip, WAR tạo | 0 / PASS build-unit nền |
| Overlay test trước production, `mvn -B -Dtest=CommissionPolicyValidatorTest test` | Thiếu type thì không compile | Thiếu DTO/validator | 1 / chưa có type |
| DTO thật + validator no-op chỉ trong checkout tạm, cùng lệnh test | Negative assertions phải phát hiện thiếu hành vi | 21 chạy, 14 fail, 0 error/skip | 1 / RED đúng kỳ vọng |
| Thay no-op bằng validator thật, `mvn -B verify` | 21 test mới + 15 nền đạt, đóng WAR | 36 chạy, 0 fail/error/skip; WAR có DTO/validator/BusinessException, không đóng Servlet API vào lib | 0 / PASS build-unit kết hợp |
| Cùng checkout, `mvn -B -Psqlserver-it verify` | IT thật cần app/DB riêng; thiếu môi trường không được coi PASS | 36 unit đạt; KhanhHealthIT 3/3 **skipped**; không test SQL tài chính | 0 / **BLOCKED SQL/runtime** |
| Cùng checkout, `mvn -B -Pbrowser-it verify` | Browser thật trên app | 36 unit đạt, **0 browser test** | 0 / **BLOCKED browser** |
| Checkout feature từ develop, `mvn -B verify` | Dependency trong develop để compile | Thiếu package vn.ticketscenter.exception/BusinessException; POM nền develop chưa có dependency WAR/JUnit | 1 / **BLOCKED tích hợp** |
| CommissionRuleTest/SettlementTest/SQLtest01 | Nghiệm thu toàn VUONG-01 | Chưa tồn tại ở phạm vi này; không chạy và không báo đạt | **BLOCKED** |

TC_SQL_HOST, TC_TEST_DATABASE, TC_APP_BASE_URL chưa cấu hình khi kiểm. Không chọn DB demo hoặc deploy để vượt blocker. No-op phục vụ bước RED chỉ tồn tại trong checkout tạm rồi được thay thế; không có trong file nguồn được commit. Trích log đã lọc: [VUONG-01-validation.log](VUONG-01-validation.log).

Tái lập GREEN sau khi checkout commit Vương (giữ nguyên SHA nền Khánh):

```powershell
$taskRoot = Join-Path $env:TEMP ('ticketscenter-vuong-replay-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $taskRoot | Out-Null
git archive --format=zip --output="$taskRoot/foundation.zip" 1edbceda45063491c662d09600c7b4f2e2d779b9
Expand-Archive -LiteralPath "$taskRoot/foundation.zip" -DestinationPath "$taskRoot/checkout"
$taskFiles = @(
  'src/main/java/vn/ticketscenter/dto/settlement/CommissionPolicyCommand.java',
  'src/main/java/vn/ticketscenter/service/settlement/CommissionPolicyValidator.java',
  'src/test/java/vn/ticketscenter/service/settlement/CommissionPolicyValidatorTest.java'
)
foreach ($taskFile in $taskFiles) {
  $taskDestination = Join-Path "$taskRoot/checkout" $taskFile
  New-Item -ItemType Directory -Force (Split-Path $taskDestination) | Out-Null
  Copy-Item -LiteralPath $taskFile -Destination $taskDestination
}
$env:JAVA_HOME = 'C:\Users\Le Vuong\.jdks\openjdk-25'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn -B -f "$taskRoot/checkout/pom.xml" verify
# Expected: 36 tests, 0 failures/errors/skipped; WAR.
# SQL/browser vẫn BLOCKED đến khi có môi trường/test thật và guard thiếu môi trường.
```

## Checklist và bàn giao còn thiếu

- [x] Kiểm remote/Issue Khánh và verify độc lập build/unit trên SHA cụ thể.
- [x] DTO và validator M0 đúng hợp đồng, 21 unit thật đạt trên dependency Khánh.
- [x] Lưu code/test/evidence/log cùng commit; Issue #5 chỉ ghi phần đã kiểm, giữ VUONG-01 tổng hợp unchecked.
- [ ] Khánh/người có quyền tích hợp: review/đưa KHANH-01/02 vào develop qua quy trình PR; bổ sung task evidence, TransactionRunner/PrincipalKind, sửa IT guard và mapping role thật cùng Đông. Phiên này không tạo PR/push/merge.
- [ ] Đông: Organization/Event đúng diagram, schema nền và SP01 caller. Review DTO/validator này; tạo rule trong transaction approve của Đông, không gọi service mở transaction riêng.
- [ ] Vương sau khi nhận model: CommissionRule/Settlement và mapping persistence, 0050, model tests/SQLtest01 thật; tiếp đó VUONG-02 FK/manifest/seeds, VUONG-03 principal với TransactionRunner.
- [ ] Liêm/Thái: hợp đồng Order/Refund và schema/evidence miền để các task tổng tiền, snapshot/đối soát nhận đầu vào đúng.
- [ ] Môi trường SQL Server test/Tomcat11 và TC_APP_BASE_URL; browser tests/SQL support thật. Khi dependency vào develop, chạy lại build/unit/SQL/browser trên SHA tích hợp; không dùng GREEN checkout tạm để tick toàn task.

Reviewer đầu ra: Đông (approve/chính sách), Khánh (exception/build), Liêm/Thái (tiền). Chưa có reviewer chấp thuận trong phiên này. Không push/merge/deploy/nộp hồ sơ.
