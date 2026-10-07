# Normalization notes — VUONG-02, phần tài chính

Chỉ phân tích schema 0050 đã có; không khẳng định ERD toàn hệ thống hoàn tất. UUID surrogate keys không tự chứng minh 3NF: xét functional dependencies thực tế.

| Bảng | Candidate key / functional dependency | Thiết kế |
|---|---|---|
| CommissionRule | id → organizationId, terms, effective interval, version | Nhiều policy/tổ chức; không đặt unique organizationId hoặc tự cấm interval chồng lấn khi spec chưa quy định |
| Settlement | id và eventId (unique) → status, totals, paid/pending, confirmedAt, version | Một settlement/Event; net/available dẫn xuất từ snapshot tổng, SQL computed; tổng lưu có chủ đích cho bản chốt |
| SettlementOrderSnapshot | (settlementId,orderId) → gross/refund/commission/net | Không partial dependency được yêu cầu; mỗi dòng phản ánh cutoff/chính sách lúc chốt, không tra policy hiện tại để thay lịch sử |
| SettlementTransferLog | payoutId → settlementId, amount, status, reference, times | Lịch sử chi bất biến theo ID, nhiều lần/settlement; trạng thái tổng Settlement được SP16 cập nhật nguyên tử |
| AuditLog | id → actor/source/action/aggregate/detail/time | Log kỹ thuật; không copy profile user hoặc metadata miền vào cột độc lập |
| MockPayoutProviderLedger | payoutId → settlementId, amount, result, reference, acceptedAt | Ledger mô phỏng bền vững; không suy ra checksum payload/replay đúng chỉ từ unique |

Tổng Settlement và netSnapshot là lưu dư có chủ đích để snapshot tài chính truy vết được. C17/C18 bảo vệ nội hàng; SP14/15/16 và trigger snapshot phải đối chiếu tổng xuyên hàng và đóng băng sau confirm. Không dùng CHECK giả xuyên bảng. Các phụ thuộc Order.acceptedPayment→Payment.orderId và Refund.currentAttempt→RefundTransferLog.refundId là điều kiện cùng aggregate; FK đơn không thay SP recheck. Phân tích đầy đủ Order/Refund/Organization/category và candidate keys toàn hệ thống chờ schema đúng từ các owner.
