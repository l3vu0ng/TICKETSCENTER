# Migration registry — Vương / VUONG-02

Manifest thực thi ở `migrations/manifest.csv`. Chỉ đăng ký file đã giao với checksum thật; không tạo dòng SHA giả cho schema thiếu. Hiện chỉ 0050 được đăng ký và kiểm độc lập. Nền 0010 của Khánh có trên feature `1edbced`, chưa vào develop; 0020/0030/0040 chưa có. 0100 chỉ tạo sau khi các chủ miền chốt PK/cột. Vì vậy scope Full bắt buộc fail trước kết nối; không coi FinancialFoundation là fresh install toàn hệ thống.

| Migration cần nhận | Owner | Trạng thái |
|---|---|---|
| 0010_identity.sql | Khánh | Feature đã có; chờ tích hợp/review/checksum vào manifest |
| 0020_organizations_events.sql | Đông | Chưa giao |
| 0030_sales.sql | Liêm | Chưa giao |
| 0040_refunds_checkin.sql | Thái | Chưa giao |
| 0050_settlements_audit.sql | Vương | Có; SQLtest01 đã kiểm SQL Server local |
| 0100_cross_domain_keys.sql | Vương | Chờ cột/PK/FK bốn miền; không tạo FK vào bảng giả |
| 0700_roles_grants.sql | Vương | VUONG-03 nền; chưa có catalog object đầy đủ |

Thứ tự dependency đã khóa: `0190_V09.sql` trước `0200_F08.sql`; `0300_V02.sql` trước `0350_F03.sql`; `0390_SP07.sql` trước `0400_SP02.sql`. Chỉ đăng ký khi artifact thật có; không đổi tên migration đã áp dụng chung.

Checksum = SHA256 của nội dung UTF-8 không BOM, đổi CRLF thành LF (không bỏ whitespace cuối dòng). Git Windows autocrlf không làm sai checksum. `dependencies` gồm filename trước đó, phân cách `;`; tên file phải cùng thứ tự dependency. Runner từ chối duplicate, file thiếu, traversal, hash lệch, sqlcmd directives/variables trong migration và dependency chưa có trước caller.

`SchemaMigrationHistory` là hạ tầng kỹ thuật, không thêm lớp nghiệp vụ. Mỗi migration và history entry dùng cùng transaction/connection, có application lock; lỗi SQL ngắt sqlcmd và connection rollback transaction chưa commit. Applied hash lệch phải fail; không sửa history để vượt qua lỗi trong DB dùng chung. Concurrent runner có thể phải chạy lại sau lỗi cạnh tranh; không báo thành công cho migration chưa commit.

Lệnh và cách kiểm thật: [database-runbook](../docs/backend/database-runbook.md). Registry ID/clock: [fixture-registry](../docs/backend/fixture-registry.md). Mapping/schema còn thiếu: [model-map](../docs/backend/model-map.md), [normalization](../docs/backend/normalization.md). Chưa có seed toàn miền hoặc FK hoàn chỉnh; chưa nghiệm thu VUONG-02.
