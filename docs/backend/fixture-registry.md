# Fixture registry — VUONG-02

Nguồn máy đọc Java: `src/test/java/vn/ticketscenter/support/TestFixtures.java`. Base clock `2026-10-06T03:00:00Z`. Các ID là **đăng ký**, không khẳng định đã seed đầy đủ. SQLtest01 chỉ tạo dữ liệu tài chính trong transaction và rollback; không tạo bảng Organization/Event/Order thay thế. Owner phải cung cấp builder/schema trước seed liên miền; E2E tạo qua API/SP thật.

| ID UUID đầy đủ | Ý nghĩa/owner dữ liệu |
|---|---|
| 00000000-0000-0000-0000-000000000001 / 00000000-0000-0000-0000-000000000002 | buyer_a/buyer_b verified; Khánh |
| 00000000-0000-0000-0000-000000000003 | buyer_unverified; Khánh |
| 00000000-0000-0000-0000-000000000004 | manager A đồng thời CHECK_IN_STAFF B; Khánh/Đông |
| 00000000-0000-0000-0000-000000000005 | manager cuối A; Khánh/Đông |
| 00000000-0000-0000-0000-000000000006 / 00000000-0000-0000-0000-000000000007 | admin / DISABLED; Khánh |
| 90000000-0000-0000-0000-000000000001 / 90000000-0000-0000-0000-000000000002 | tổ chức A/B; Đông |
| 10000000-0000-0000-0000-000000000001 | event A published; saleStart 05/10 03:00, saleEnd 06/10 05:00, start 06:00, end 08:00 UTC; Đông |
| 20000000-0000-0000-0000-000000000001 / 20000000-0000-0000-0000-000000000002 | SEATED A 2×3 giá200000 / STANDING A capacity3 giá300000; Đông |
| 30000000-0000-0000-0000-000000000001 | Seat A1; Đông |
| 40000000-0000-0000-0000-000000000001 | Order A; Liêm |
| 60000000-0000-0000-0000-000000000001 | Commission A; Vương, SQLtest dùng rate250 để chứng minh không cap100 |
| 70000000-0000-0000-0000-000000000001 | Settlement A; Vương |
| 80000000-0000-0000-0000-000000000001 | refund attempt của ví dụ CashFlowRowDto; Thái |
| a0000000-0000-0000-0000-000000000001 | payout thử schema; Vương |

Chưa cấp ID/seed cho membership inactive, event DRAFT/PENDING_APPROVAL/CANCELLED, zero-price zone, coupon30%/fixed vượt30%/quota1/hết hạn hay nhiều payment/refund attempts: chờ builder của owner để đăng ký cùng khóa/cột và fixture từng scenario, không tự đoán thêm domain data. Không có dữ liệu cá nhân, password/token/OTP/QR trong registry.
