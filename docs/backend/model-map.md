# Financial model/schema map — VUONG-01/02

Hợp đồng `04336c2`, XML diagram hiện hành. Chỉ CommissionRule và Settlement là lớp nghiệp vụ Vương; chưa tạo Java model thiếu quan hệ bằng UUID/Object thay thế. Organization/Event phải do Đông cung cấp. PERSISTED computed SQL vẫn là giá trị dẫn xuất, không field mutable của Model/JPA.

| Diagram/technical data | Schema 0050 đã có | Java/hành vi và gate còn thiếu |
|---|---|---|
| CommissionRule ratePercent/fixedFee/effectiveFrom/effectiveTo, Organization association | id PK; organizationId NOT NULL; decimal(19,6)/(19,0); datetime2(7); C16; version | isEffectiveAt `[from,to)`; calculateFee HALF_UP/cap remaining; Organization thật, JPA và tests chờ Đông |
| Settlement Event association/status/grossRevenue/totalRefund/totalCommission/paidAmount/pendingAmount | Event unique; status enum DRAFT/CONFIRMED/PAID; các tổng decimal(19,0); C18 | Event thật; recalculate chỉ DRAFT, confirm(now), beginPayout, recordPayoutResult; SQL aggregate/locking/replay chờ nguồn |
| Settlement /netPayable, /availableToPay | computed PERSISTED từ các tổng, không input DML | Getter dẫn xuất, JPA read-only; không setter |
| SettlementOrderSnapshot (không Model thứ16) | PK(settlementId,orderId), FK Settlement; C17 | SettlementOrderSnapshotRecord và snapshot đóng băng/TR04; Order FK chờ Liêm |
| SettlementTransferLog (không Payout class) | payoutId PK, FK Settlement, C19, status PENDING/SUCCEEDED/FAILED | SettlementTransferLogRecord; SP16/replay/paid+pending tổng xuyên hàng |
| AuditLog (persistence) | id PK; actorId nullable chỉ SYSTEM; source USER/SYSTEM | AuditLogRecord; actor User FK chờ Khánh, TR10 append-only ở VUONG-11 |
| MockPayoutProviderLedger (simulation) | payoutId PK, amount>0, status SUCCEEDED/FAILED | MockPayoutProviderRecord/adapter ở task tài chính; unique không tự chứng minh idempotency payload |

0050 không cascade delete lịch sử. FK liên miền tại 0100 phải dùng bảng/cột được owner bàn giao: CommissionRule.organizationId→Organization.id; Settlement.eventId→Event.id; Snapshot.orderId→Order.id; AuditLog.actorId→User.id. Không tạo FK trước bảng thật hoặc FK giả. PK/NOT NULL/enum đã kiểm SQL; mapping toàn15class và bốn schema khác cần owner evidence, chưa nghiệm thu từ bảng này.
