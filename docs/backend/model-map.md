# Financial model/schema map — VUONG-01/02

Cập nhật routine main `90800f7`: nền Khánh `7b403f2` đã nhận và schema
0010/0011 đã vào manifest. 106 unit và migration identity/tài chính đạt khi
kiểm riêng; Organization/Event thực, 0020/0030/0040 và JPA TCP vẫn thiếu.
Số 62 unit và nhận xét chưa nhận identity ở dưới là kết quả lịch sử.

Hợp đồng `04336c2`, XML diagram hiện hành; cập nhật M0 ngày08/10/2026 sau VUONG-01 `a4a37a3`. Chỉ CommissionRule và Settlement là lớp nghiệp vụ Vương. Hai entity đã có tham chiếu có kiểu Organization/Event; không dùng UUID/Object thay thế quan hệ. Organization/Event thực phải do Đông cung cấp. Checkout kiểm độc lập chỉ dùng shell tạm để biên dịch, không commit shell hoặc tạo bảng owner. PERSISTED computed SQL vẫn là giá trị dẫn xuất, không field mutable của Model/JPA.

| Diagram/technical data | Schema 0050 đã có | Java/hành vi và gate còn thiếu |
|---|---|---|
| CommissionRule ratePercent/fixedFee/effectiveFrom/effectiveTo, Organization association | id PK; organizationId NOT NULL; decimal(19,6)/(19,0); datetime2(7); C16; version | Entity/ManyToOne Organization; isEffectiveAt `[from,to)` và calculateFee HALF_UP/cap đã có; 12 unit pass độc lập; mapping với Organization thực chờ Đông |
| Settlement Event association/status/grossRevenue/totalRefund/totalCommission/paidAmount/pendingAmount | Event unique; status enum DRAFT/CONFIRMED/PAID; các tổng decimal(19,0); C18 | Entity/OneToOne Event; đủ recalculate/confirm/beginPayout/recordPayoutResult, 14 unit pass độc lập; SQL aggregate/blocker/locking/replay của M3 chờ nguồn |
| Settlement /netPayable, /availableToPay | computed PERSISTED từ các tổng, không input DML | Getter dẫn xuất @Transient; không setter hoặc writable SQL mapping |
| SettlementOrderSnapshot (không Model thứ16) | PK(settlementId,orderId), FK Settlement; C17 | SettlementOrderSnapshotRecord có @EmbeddedId với equals/hashCode, @Immutable; TR04 M3/Order FK chờ Liêm |
| SettlementTransferLog (không Payout class) | payoutId PK, FK Settlement, C19, status PENDING/SUCCEEDED/FAILED | SettlementTransferLogRecord @Immutable đã có; SP16/replay/paid+pending xuyên hàng ở M3 |
| AuditLog (persistence) | id PK; actorId nullable chỉ SYSTEM; source USER/SYSTEM | AuditLogRecord @Immutable/Source enum đã có; actor User FK chờ Khánh, TR10 append-only ở VUONG-11 |
| MockPayoutProviderLedger (simulation) | payoutId PK, amount>0, status SUCCEEDED/FAILED | MockPayoutProviderRecord @Immutable đã có; adapter ở M3; unique không tự chứng minh idempotency payload |

0050 không cascade delete lịch sử. FK liên miền tại 0100 phải dùng bảng/cột được owner bàn giao: CommissionRule.organizationId→Organization.id; Settlement.eventId→Event.id; Snapshot.orderId→Order.id; AuditLog.actorId→User.id. Không tạo FK trước bảng thật hoặc FK giả. PK/NOT NULL/enum đã kiểm SQL; mapping toàn15class và bốn schema khác cần owner evidence, chưa nghiệm thu từ bảng này.

Java đã compile/package trên archive Khánh và fixture quan hệ, 62 unit pass;
không coi đó là build develop. `VuongFinancialMappingIT` đã được tạo với
Hibernate schema validation và assertions tiền/UUID/enum/UTC/Unicode/rollback,
nhưng SQL Server local TCP Enabled=0 nên JDBC chưa kết nối; ghi BLOCKED,
không nhận mapping runtime đã đạt từ SQLcmd constraint tests.
