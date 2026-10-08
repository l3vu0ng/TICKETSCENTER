# VUONG-01 — kiểm tra đầu vào, chưa triển khai

- Ngày kiểm tra: 07/10/2026, Asia/Bangkok (UTC+07:00).
- Trạng thái task: **BLOCKED**. Đây là evidence kiểm tra điều kiện bắt đầu, không phải evidence nghiệm thu Model/SQL hay tích hợp.
- `testedRevision`: `04336c267c38c959d3bceaa59cfc08c7ee828407`.
- Nhánh khi kiểm: `main`; working tree sạch trước kiểm và trước tạo tài liệu này.
- Evidence là artifact local chưa commit. Không có commit triển khai mới vì chưa có develop để rẽ nhánh theo hợp đồng.
- Phạm vi thay đổi phiên này: chỉ tài liệu này trong vùng evidence của Vương; API/Service/Servlet/SQL/UI chưa thay đổi.

## Nguồn và quyết định chọn task

Đã đọc spec.md (1144 dòng), XML docs/classdiagram/diagram.md (633 dòng, gồm thuộc tính/phương thức/quan hệ), toàn bộ TEAM-CONTRACT.md (231 dòng), vuong.md (488 dòng), CONVENTIONS.md, API-MAP.md, COVERAGE.md và GIT-WORKFLOW.md ở testedRevision trên. Đối chiếu thêm KHANH-01/02/03/04 để xác định artifact nền và người cung cấp.

Spec cập nhật 05/10/2026; TEAM-CONTRACT và API-MAP đồng bộ 07/10/2026. Ngày tài liệu là phiên bản hợp đồng, không phải tiến độ nghiệm thu.

VUONG-01 là task nền đầu tiên chưa có implementation/evidence ở revision đã kiểm. Chưa có task VUONG nào đủ đầu vào để triển khai trọn vẹn. Chỉ chuẩn bị bảng truy vết và ghi kết quả kiểm tra độc lập; không chọn một task phụ thuộc còn thiếu rồi báo đã ghép.

GIT-WORKFLOW §3 quy định: “Nếu chưa có baseline/develop thì ghi blocker bootstrap, không tự rẽ từ main để vượt hợp đồng.” Remote thực tế cũng chưa có develop; không dùng ngoại lệ §7 vì main hiện là baseline tài liệu, chưa phải bản phần mềm đã bàn giao. Không tạo develop/feature hay commit trực tiếp main trong phiên này.

## Kiểm kê thực tế

| Kiểm tra | Expected để nhận task | Actual tại testedRevision | Kết luận |
|---|---|---|---|
| `git status --short` | Bảo toàn thay đổi đang có | Không có output trước khi tạo evidence | Checkout ban đầu sạch |
| `git branch -a` | Có develop đã kiểm để rẽ feature task | Chỉ main và remotes/origin/main | BLOCKED bootstrap |
| `git ls-remote --heads origin` | Remote có develop nếu dùng luồng chung | Chỉ `04336c267c38c959d3bceaa59cfc08c7ee828407 refs/heads/main`; exit 0 | BLOCKED bootstrap |
| `rg --files` và kiểm src đệ quy | Có nguồn và test nền | Không có file ứng dụng hay test trong src; chỉ tài liệu/prototype/POM | Chưa có artifact nền |
| POM | WAR, dependency/plugin được Khánh khóa, unit và SQL IT | Không packaging/dependencies/profiles/build config; source/target 25; Maven dùng default jar | KHANH-01 chưa bàn giao |
| `mvn -version` | Maven dùng JDK 25 | Maven 3.9.16, Java 17.0.16, Windows 11 amd64 | Runtime đang dùng chưa đúng spec |
| ClockProvider/kiểu chung | Đúng package/chữ ký hợp đồng | ClockProvider.java, ActorContext.java chưa tồn tại | KHANH-02/06 chưa bàn giao |
| Model quan hệ | Organization và Event do Đông cung cấp | Cả hai file chưa tồn tại | DONG-01 chưa bàn giao cho mapping đầy đủ |
| Migration nền | Schema miền có thật, FK ghép sau bảng | 0010/0020/0030/0040/0050 chưa tồn tại | Chưa có bộ migration |
| SQL test/tooling | Test DB riêng và SQL test thật | SqlServerTestSupport.java, VUONG-01.sql chưa có; TC_SQL_HOST và TC_TEST_DATABASE chưa cấu hình | BLOCKED SQL, chưa thử kết nối |
| Browser | WAR/Tomcat/test DB và browser profile có thật | Không WAR, JSP/Servlet, profile; APP_BASE_URL chưa cấu hình | BLOCKED browser |

`sqlcmd` và Docker CLI có trên PATH, nhưng chưa xác minh SQL Server/container/Tomcat đang chạy. Không suy ra thiếu SQL Server từ thiếu cấu hình, và không tự chọn database demo hoặc target khác để thử.

## Lệnh thực chạy và kết quả

Các lệnh dưới đây chạy trên testedRevision, trước khi tạo evidence; không có sửa code/POM giữa các lệnh. Không tắt fail-on-missing-tests. Các profile được gọi chỉ để kiểm hiện trạng tooling; không có SQL/browser test nào thực sự chạy.

| Lệnh PowerShell | Expected theo task/hợp đồng | Actual | Exit | Trạng thái nghiệm thu |
|---|---|---|---:|---|
| `mvn -B verify` | WAR chạy được, unit assertions có thật | BUILD SUCCESS, không có mã để compile/test; tạo target/TicketsCenter-1.0-SNAPSHOT.jar (1492 bytes), không WAR | 0 | FAIL gate WAR; không chứng minh unit hoặc JDK25 |
| `mvn -B '-Dtest=CommissionRuleTest,SettlementTest' test` | Hai bộ test tài chính thực chạy và đạt | BUILD FAILURE: No tests matching pattern "CommissionRuleTest, SettlementTest" were executed | 1 | BLOCKED: test/implementation chưa có |
| `mvn -B -Psqlserver-it '-Dit.test=VuongMigrationIT' verify` | Profile/Failsafe thực chạy IT; thiếu môi trường phải fail | Profile sqlserver-it does not exist; BUILD SUCCESS của default jar, không chạy IT | 0 | BLOCKED SQL; tooling chưa đạt yêu cầu fail khi thiếu profile/môi trường |
| `mvn -B -Pbrowser-it verify` | Browser E2E thật trên WAR/test DB riêng | Profile browser-it does not exist; BUILD SUCCESS của default jar, không chạy E2E | 0 | BLOCKED browser; không phải PASS tích hợp |
| SQLtest01 qua sqlcmd | Constraint/unique thực trên SQL Server | Chưa chạy: script, migrations, test database/runbook chưa có | Không áp dụng | BLOCKED |

Trích log đã lọc (không có connection string, token hoặc bí mật):

```text
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] Nothing to compile - all classes are up to date.
[INFO] --- surefire:3.5.4:test (default-test) @ TicketsCenter ---
[INFO] Building jar: E:\TICKETSCENTER\target\TicketsCenter-1.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
[INFO] Finished at: 2026-10-07T14:35:38+07:00

[INFO] BUILD FAILURE
[ERROR] No tests matching pattern "CommissionRuleTest, SettlementTest" were executed!
[INFO] Finished at: 2026-10-07T14:36:05+07:00

[WARNING] The requested profile "sqlserver-it" could not be activated because it does not exist.
[WARNING] The requested profile "browser-it" could not be activated because it does not exist.
```

Surefire/compiler/jar versions trong log là phiên bản effective do Maven hiện tại chọn, chưa phải quyết định dependency/plugin đã được nhóm khóa. DB trước/sau, fixture, principal, HTTP và ảnh browser: không có quan sát vì chưa chạy các tầng đó; không dùng mock để thay bằng chứng.

## Bảng truy vết chuẩn bị cho VUONG-01

Mọi file và ca dưới đây là **đầu ra dự kiến, chưa tạo/chưa chạy**. Đây là phần chuẩn bị độc lập, không tick checklist hoàn thành.

| Thuộc tính/phương thức/quan hệ nguồn | File Vương sẽ sở hữu | Ca expected cần kiểm sau khi đủ đầu vào |
|---|---|---|
| CommissionRule.ratePercent, fixedFee, effectiveFrom, effectiveTo | model/settlement/CommissionRule.java | BigDecimal, tiền nguyên, nonnegative/non-null; effectiveFrom < effectiveTo; mapping decimal(19,6)/decimal(19,0)/UTC |
| isEffectiveAt(Instant): Boolean | CommissionRule.java; CommissionRuleTest.java | From-inclusive/to-exclusive, null không hợp lệ |
| calculateFee(BigDecimal): BigDecimal | CommissionRule.java; CommissionRuleTest.java | Remaining 200001/rate 2.5/fixed 0 → 5000; 20/10/100 → 20; remaining 0 → 0; null/âm bị từ chối |
| Organization 1 → CommissionRule 0..* | CommissionRule.java; 0050_settlements_audit.sql | Tham chiếu đúng Organization của Đông; FK liên miền ghép tại VUONG-02 |
| Settlement.status, grossRevenue, totalRefund, totalCommission, paidAmount, pendingAmount | model/settlement/Settlement.java; SettlementTest.java | Lưu trực tiếp, BigDecimal; không âm; refund+commission ≤ gross; paid+pending ≤ net |
| /netPayable, /availableToPay | Settlement.java; SettlementTest.java | Dẫn xuất: gross−refund−commission; net−paid−pending; không setter độc lập |
| recalculate(grossRevenue,totalRefund,totalCommission): void | Settlement.java; SettlementTest.java | Chỉ DRAFT; gross 600000/refund 200000/commission 50000 → net 350000 |
| confirm(Instant): void | Settlement.java; SettlementTest.java | Đóng băng tổng/confirmedAt; net 0 → PAID không tạo chuyển tiền; kiểm end/blocker thuộc Service/SP15 về sau |
| beginPayout(UUID,BigDecimal): void | Settlement.java; SettlementTest.java | Net 350000/reserve 100000 → paid 0/pending 100000/available 250000; amount dương, đủ số dư |
| recordPayoutResult(UUID,BigDecimal,PayoutResult,String?): void | Settlement.java; SettlementTest.java | SUCCEEDED chuyển pending sang paid; FAILED giải phóng pending; enum chỉ SUCCEEDED/FAILED; replay/amount identity phải phối hợp log/Service ở VUONG-08 |
| Event 1 → Settlement 0..1 | Settlement.java; 0050_settlements_audit.sql | Một Settlement/Event, dùng Event của Đông; FK liên miền tại VUONG-02 |
| CommissionPolicyCommand và validate(command): void | dto/settlement/CommissionPolicyCommand.java; service/settlement/CommissionPolicyValidator.java | Validate miền và precision theo contract, không business default hay tự đặt cap phần trăm; Đông SP01 dùng scalar trong cùng TX01 |
| Snapshot/log/audit/simulator persistence | persistence/settlement/*Record.java; persistence/audit/AuditLogRecord.java; 0050_settlements_audit.sql | Không thêm class nghiệp vụ; unique Event/snapshot pair/payoutId; actor nullable chỉ SYSTEM; simulator ledger bền vững |
| C16/C17/C18/C19 | 0050_settlements_audit.sql; database/tests/vuong/VUONG-01.sql | Constraint/unique/NULL thực trên SQL Server; không CHECK xuyên bảng giả |

Path Java/test trong bảng theo gốc `src/main/java/vn/ticketscenter/` và `src/test/java/vn/ticketscenter/`; migration dưới `database/migrations/`. Không thêm API hoặc UI trong task nền này. AdminServlet vẫn phải gọi OrganizationService/EventService của Đông và RefundService/CancellationService của Thái theo TEAM; không tạo caller thay thế.

## Sàng lọc các task còn lại

Tại testedRevision chưa có evidence/implementation của task Vương. Không suy tiến độ từ checkbox hay tên commit. Blocker bootstrap áp dụng toàn bộ; các đầu vào thiếu thêm được liệt kê dưới đây.

| Task | Đầu vào còn thiếu khiến chưa thể nghiệm thu/triển khai trọn vẹn |
|---|---|
| VUONG-02 | Năm schema nền, builder/contract từng miền, tooling SQL; có thể chuẩn bị registry/normalization độc lập nhưng chưa dựng/ghép DB |
| VUONG-03 | TransactionRunner/PrincipalKind và caller/quyền từng SQL object |
| VUONG-04 | VUONG-01–03, publish/schema Đông |
| VUONG-05 | Schema/payment của Liêm, Refund/outbox của Thái, VUONG-02 |
| VUONG-06 | VUONG-04/05 và thứ tự khóa chung đã review |
| VUONG-07 | VUONG-05/06, SP09 Liêm/SP11 Thái và lock contract |
| VUONG-08 | VUONG-07, ADMIN principal, ledger/transaction |
| VUONG-09 | VUONG-04/05/07 và View các miền |
| VUONG-10 | ReportService/query của VUONG-09, layout/API client Khánh |
| VUONG-11 | VUONG-01, actor context, TR05 Đông và audit các miền |
| VUONG-12 | Auth/CSRF Khánh; Organization/Event Service Đông; Refund/Cancellation Service Thái; VUONG-04 |
| VUONG-13 | VUONG-04–08, layout/auth/API client, browser tooling |
| VUONG-14 | Scripts/caller/tests/datasets và evidence SQL từng owner |
| VUONG-15 | KHANH-01 WAR/unit/SQL profile, runtime/test DB, browser compatibility/runner được nhóm chọn; chưa có bản ghép |
| VUONG-16 | WAR/modules/migrations/principals/adapters; chưa có gói để smoke/restore |
| VUONG-17 | Evidence VUONG-01–16 và evidence/thuyết minh từng miền; chưa có hồ sơ nghiệm thu để tổng hợp |

## Người nhận bàn giao và điều kiện tiếp tục

1. Người được nhóm giao bootstrap/quản trị Git: thiết lập develop từ baseline đã review theo GIT-WORKFLOW; không suy quyền này từ vai trò Vương. Nhánh dự kiến sau gate: feature/vuong/vuong-01-finance-contracts, base develop.
2. Khánh — KHANH-01/02: WAR/JDK25, dependencies/plugins/test tooling đã khóa, ClockProvider/kiểu chung ở đúng package; KHANH-03 cung cấp persistence/SQL test support và cấu hình DB test an toàn. ClockProvider được tạo ở KHANH-02 dù mô tả Nhận của VUONG-01 nhắc KHANH-01; dùng TEAM §3.4 và artifact cụ thể, không tự sửa hợp đồng.
3. Đông — DONG-01/02: Organization/Event model và schema để mapping quan hệ; nhận CommissionPolicyCommand/validator khi VUONG-01 triển khai. Initial policy vẫn được Đông SP01 ghi cùng Organization/map role/TX01.
4. Liêm/Thái: cung cấp schema/builder và review nguồn tiền khi ghép VUONG-02 và các task sau. Không cần chờ họ hoàn tất mọi nghiệp vụ để bắt đầu model tài chính sau khi gate nền được mở.
5. Vương/Khánh: phối hợp profile browser-it trong VUONG-15 sau khi có WAR/runtime/tool được chọn; không sửa POM đơn phương.

Chưa gửi tin nhắn cho thành viên, tạo PR, push, merge, deploy, reset DB, nộp hồ sơ hoặc tạo ảnh. Task VUONG-01 vẫn mở/BLOCKED; tài liệu này không xác nhận reviewer đã chấp nhận hay tích hợp đã đạt.
