# Lộ trình và quy trình phối hợp TicketsCenter

Ngày đồng bộ: 08/10/2026. Áp dụng cho Khánh, Đông, Liêm, Thái và Vương với 76 task cấp thành viên.

Đây là kế hoạch triển khai và bàn giao. Tên task, đường dẫn, profile và điều kiện nghiệm thu mô tả đầu ra phải tạo, không xác nhận đã có mã hoặc đã chạy thành công. Một task có thể có nhiều phần bàn giao, commit và PR.

## 1. Nguồn chuẩn và phạm vi sở hữu

Đọc các nguồn sau trước khi nhận task:

1. [spec.md](../../spec.md) và [diagram.md](../classdiagram/diagram.md) quyết định nghiệp vụ, thuộc tính, phương thức và quan hệ của 15 Model. Diagram là XML diagrams.net; đọc cả nội dung phương thức và quan hệ.
2. [TEAM-CONTRACT.md](TEAM-CONTRACT.md) chốt ownership, Service, DTO, Worker SPI, principal, migration và cách ghép các miền.
3. Năm bản nhiệm vụ [Khánh](khanh.md), [Đông](dong.md), [Liêm](liem.md), [Thái](thai.md) và [Vương](vuong.md) quy định Create/Modify/Test, Consumes/Produces, phụ thuộc và tiêu chí của từng Task ID.
4. [GIT-WORKFLOW.md](GIT-WORKFLOW.md) quy định nhánh, commit, PR, reviewer, merge và bàn giao phiên bản.
5. [CONVENTIONS.md](CONVENTIONS.md), [API-MAP.md](API-MAP.md) và [COVERAGE.md](COVERAGE.md) quy định format, HTTP, kiểm chứng và truy vết yêu cầu.
6. [README.md](README.md) tổng hợp phân công; [kế hoạch báo cáo DBMS](../baocao/KE-HOACH-BAO-CAO-DBMS.md) quy định hồ sơ nộp.

Routine tổng hợp cách phối hợp, không thay thế các nguồn trên. Khi phát hiện khác biệt, ghi điểm bất đồng và báo chủ miền; dùng spec/diagram cho nghiệp vụ, TEAM-CONTRACT cùng quyết định triển khai đã ghi cho hợp đồng. Đồng bộ nguồn, caller và test khi đổi hợp đồng. Lịch backend 21 ngày chỉ để truy vết, không phải deadline mới hoặc danh sách việc cộng thêm.

### 1.1. Model, màn hình và adapter

| Thành viên | Model nghiệp vụ | Màn hình sở hữu | Điểm phối hợp |
|---|---|---|---|
| Khánh | `User` | UI-03, UI-24; layout và trạng thái chung | `AuthServlet`, `MeServlet`, health, filters, HTTP, JPA, session, mail |
| Đông | `Organization`, `Event`, `Zone`, `Seat` | UI-01, UI-02, UI-09, UI-10, UI-11, UI-12, UI-13 | Organization/Event adapters; methods kho; Service duyệt tổ chức/sự kiện cho Vương |
| Liêm | `TicketHold`, `TicketHoldItem`, `Order`, `OrderItem`, `Payment`, `Coupon`, `Ticket` | UI-04, UI-05, UI-06, UI-07, UI-14 | Hold/order/payment/ticket/coupon routes; VNPAY/QR; outbox và scheduler chung |
| Thái | `Refund` | UI-08, UI-15, UI-16 | Check-in/refund/cancellation Service; DTO/query cho MeServlet, OrderServlet và AdminServlet |
| Vương | `CommissionRule`, `Settlement` | UI-17, UI-18, UI-19, UI-20, UI-21, UI-22, UI-23 | `AdminServlet`; adapter gọi Service Đông/Thái; bộ chọn tổ chức admin ở UI-14; báo cáo/quyền/CI/hồ sơ |

Chủ Model cung cấp đủ methods của diagram, kể cả methods miền khác gọi. Người nhận yêu cầu sửa qua chủ lớp, không tạo bản Model hoặc DTO thứ hai. Chủ Servlet giữ URL mapping và dispatch; chủ Service cung cấp nghiệp vụ. UI-20 hủy sự kiện và UI-21 duyệt hoàn do Vương làm adapter/JSP, gọi Service của Thái. Không đăng ký hai Servlet cho cùng URL pattern.

### 1.2. Danh mục SQL theo chủ miền

| Nhóm | Khánh | Đông | Liêm | Thái | Vương |
|---|---|---|---|---|---|
| Constraint | C01 | C02, C03, C04, C05, C10 | C06, C07, C08, C09, C11, C12, C13, C14 | C15, C20 | C16, C17, C18, C19 |
| View | Không | V01, V02, V10 | V06, V09 | V05, V07 | V03, V04, V08 |
| Stored Procedure | Không | SP01, SP12 | SP02, SP03, SP06, SP07, SP08, SP09 | SP04, SP05, SP10, SP11, SP13, SP17 | SP14, SP15, SP16 |
| Function | Không | F03 | F01, F06, F08 | F04, F07 | F02, F05, F09, F10 |
| Trigger | Không | TR01, TR02, TR05, TR06 | TR07, TR08 | Không | TR03, TR04, TR09, TR10 |
| Index | Không | IX01, IX02, IX15 | IX03, IX04, IX06, IX09, IX11 | IX05, IX08, IX10 | IX07, IX12, IX13, IX14 |
| Transaction | Không | TX01, TX12 | TX02, TX03, TX06, TX07, TX08, TX09 | TX04, TX05, TX10, TX11, TX13, TX17 | TX14, TX15, TX16 |

Giữ đủ C01–C20, V01–V10, SP01–SP17, F01–F10, TR01–TR10, IX01–IX15, TX01–TX17 và bốn role nghiệp vụ. Unique keys và object kỹ thuật vẫn bắt buộc nhưng không dùng để thay danh mục rubric. Vương ghép catalog/grants; từng chủ miền viết object, caller và test.

Bốn role nghiệp vụ là `tc_buyer`, `tc_manager`, `tc_checkin` và `tc_platform_admin`. Auth/worker dùng principal kỹ thuật với quyền hẹp theo TEAM-CONTRACT, không được thay bằng role nghiệp vụ rộng.

F07 kiểm tra cửa sổ check-in; V05 cung cấp lịch sử quét. F08 kiểm tra coupon hợp lệ. V03/F09 phục vụ dữ liệu tài chính hiện hành và blocker; F05 là doanh thu theo nhóm đơn, F10 là dòng tiền phát sinh. TR04 bảo vệ snapshot chi tiết quyết toán; TR09 bảo vệ header; TR10 giữ AuditLog chỉ thêm. TR05 ghi thay đổi trạng thái Event, không khóa sơ đồ thay TR01/TR02.

## 2. Làm song song theo artifact và phụ thuộc

### 2.1. Thứ tự thực hiện

Task ID dùng để truy vết, không phải thứ tự bắt buộc. Một người có thể giao phần nền của task số lớn trước khi hoàn thiện task số nhỏ. Nhóm bắt đầu song song bằng phần việc đã đủ đầu vào; phần tích hợp chờ artifact thật cần dùng, không chờ cả module của người trước.

Phân biệt ba mức đầu ra:

- Hợp đồng nền gồm chữ ký, DTO, enum, schema và SPI do chủ sở hữu cung cấp. Caller có thể compile theo hợp đồng này.
- Nghiệp vụ thật gồm Service/SP/query đã kiểm chứng, đủ quyền, transaction và dữ liệu để adapter gọi.
- Bản ghép gồm adapter/JSP/worker chạy qua các miền, có bằng chứng theo commit.

Một task trải nhiều mốc phải ghi phần nào đã giao, phần nào còn chờ và người nhận. Chỉ đánh dấu toàn task hoàn thành khi đạt toàn bộ checklist trong file thành viên.

### 2.2. Khung tạm và phụ thuộc bị chặn

1. Chủ lớp/type/Service giao hợp đồng tại M0 theo TEAM-CONTRACT. Model phải giữ đủ thuộc tính và methods của diagram; khai báo Service chưa triển khai chỉ được dùng cho contract compile và phải báo lỗi rõ khi gọi nghiệp vụ.
2. Caller không tự thêm class sản phẩm vào phần sở hữu của người khác. Khi thiếu hợp đồng, yêu cầu chủ sở hữu cung cấp và tiếp tục task độc lập. Fixture/mock của test được đặt trong phạm vi test của caller, có nhãn rõ.
3. Khung tạm trong nhánh cá nhân phải ghi chủ cung cấp, chữ ký đang dùng, giới hạn và bước thay thế. Trước merge, dùng artifact chuẩn của chủ sở hữu; không ghép class trùng hoặc trả thành công giả. PR contract M0 phải nêu rõ phần chưa triển khai, không được dùng làm evidence nghiệm thu nghiệp vụ.
4. Khi đổi hợp đồng, phối hợp chủ file và mọi caller/test trong cùng thay đổi. Không sửa yêu cầu để test xanh hoặc tự bỏ ca SQL vì thiếu môi trường.

### 2.3. Bàn giao nền để phá vòng phụ thuộc

| Artifact | Chủ và task cung cấp | Người nhận và thời điểm |
|---|---|---|
| WAR, HTTP/types/clock, User/schema, transaction/principal | Khánh 01–04; phần nền 06/07 | Cả nhóm M0; round-trip JPA chờ manifest/fixture/grants Vương |
| Layout, API client và browser tooling | Khánh phần nền 11; Vương phần nền 15 | Chủ UI nhận từ M0; hoàn thiện từng trang theo M1–M3 |
| Model/DTO/Service hợp đồng sự kiện và kho | Đông 01/02 | Khánh về membership, Liêm/Thái về kho, Vương về commission |
| Model bán vé, outbox schema và Worker SPI | Liêm 01/02 | Email Khánh, image cleanup Đông, refund/cancel Thái đăng ký handler; dispatcher thật từ LIEM-11 ở M2 |
| Refund, `createCompensation`, schema và log kỹ thuật | Thái 01 | LIEM-10/SP09 ở M2 tạo nghĩa vụ bù trừ, không chờ UI/worker hoàn M3 |
| CommissionRule/Settlement, policy validator, audit schema | Vương 01 | DONG-04/SP01 và DONG-09/SP12; chính sách ban đầu giao trước luồng duyệt M1 |
| Manifest, fixture, cross-FK và principal/grants | Vương 02/03 cùng các chủ schema | M0 dựng DB/round-trip; bổ sung quyền khi từng object M1–M3 được tạo |
| Query đơn/vé và adapter tài khoản | Liêm 13, Khánh 13 | M2 ghép UI-06/UI-07 qua MeServlet; M3 bổ sung refund query Thái |
| Refund/cancel Service và Admin adapter | Thái 06–13, Vương 12 | M3 ghép UI-20/UI-21; Thái 14 giữ UI-08 và handoff tài khoản |

Các phụ thuộc M0 có thể hai chiều về kiểu dữ liệu. Giao type/schema trước, rồi chạy contract/round-trip sau khi đủ artifact; không biến chúng thành yêu cầu phải hoàn tất toàn task của nhau trước khi bắt đầu.

### 2.4. Ghép database

Mỗi chủ schema `0010_identity.sql` đến `0050_settlements_audit.sql` chỉ tạo FK nội miền. Vương ghép FK liên miền trong `0100_cross_domain_keys.sql` sau khi bảng đích đã tồn tại. Chủ miền cung cấp tên cột/unique/FK và mapping, Vương giữ manifest/checksum/fixture.

Theo TEAM-CONTRACT, đăng ký ngoại lệ thứ tự trước lần áp dụng đầu tiên:

- `0190_V09.sql` trước `0200_F08.sql`.
- `0300_V02.sql` trước `0350_F03.sql`.
- `0390_SP07.sql` trước `0400_SP02.sql`.

Manifest và thứ tự tên file phải cùng phản ánh phụ thuộc. Không sửa migration đã áp dụng chung; thêm migration kế tiếp. Grant cho object mới được bổ sung cùng lúc giao object. Khánh/Vương chốt principal từ actor và quyền hiện hành, không nhận role DB do browser chọn.

## 3. Vòng lặp sáu bước cho một đơn vị bàn giao

### Bước 1. Kiểm tra workspace và nhận phần việc

Đọc Task ID, phụ thuộc và phạm vi Create/Modify/Test. Kiểm nhánh, diff và thay đổi chưa commit. Giữ thay đổi của người khác; không reset, stash cả workspace hoặc stage tất cả để bắt đầu lại.

Nhánh mặc định là `feature/<ten>/<task-id-lowercase>-<chuc-nang>` từ develop mới nhất đã kiểm; PR về develop. Đồng bộ và tạo nhánh theo GIT-WORKFLOW khi đã có nhánh/môi trường phù hợp. Ngoại lệ sửa bản main đã bàn giao tuân GIT-WORKFLOW §7. Không suy ra quyền push/merge/tag/deploy chỉ từ việc đọc routine.

### Bước 2. Chốt Consumes/Produces và phần bàn giao

Ghi artifact đầu vào, chủ cung cấp, commit/PR đã ghép và chữ ký đang dùng. Phân biệt hợp đồng compile với dependency đã nghiệm thu. Ghi đầu ra, người nhận, tiêu chí đo được và phần mốc của task đang thực hiện.

Nếu thiếu artifact, ghi blocker cụ thể cùng chủ xử lý; chuyển sang phần độc lập. Draft PR hoặc mock không chứng minh dependency đã đạt.

### Bước 3. Viết mã và kiểm chứng phù hợp

Chỉ sửa file được giao; shared file phải phối hợp owner. Với logic, API, quyền và transaction, viết ca hành vi thất bại có ý nghĩa rồi triển khai. Configuration/glue dùng smoke check phù hợp. Chọn boundary/negative/race/replay/recovery theo rủi ro của task, không tạo test chỉ lặp lại implementation.

SP sở hữu đường ghi nghiệp vụ. Java không dirty-write rồi gọi SP để lặp cùng mutation. Không giữ transaction khi gọi VNPAY, email hoặc storage. Dùng một dispatcher/scheduler của Liêm; handler các miền chỉ đăng ký vào nền chung.

### Bước 4. Chạy trên đúng môi trường và kiểm dữ liệu

Sau khi tooling tương ứng được tạo, dùng lệnh của task. Các lệnh chung chạy từ gốc repo:

```powershell
mvn -B verify
mvn -B -Psqlserver-it verify
mvn -B -Pbrowser-it verify
```

Unit test không chứng minh SQL Server hoặc browser. Profile `sqlserver-it`/`browser-it` phải được tạo và kiểm cấu hình trước khi dùng; thiếu DB/runtime phải fail hoặc ghi BLOCKED, không skip rồi báo PASS. Chạy SQL test qua `sqlcmd -b -i <file-test>` với xác thực theo runbook, không đặt mật khẩu trên command line.

Kiểm cả HTTP/UI và dữ liệu trước/sau. Ca rollback gây lỗi sau mutation; ca tranh chấp dùng hai connection thật với điểm đồng bộ và timeout hữu hạn; trigger có ca nhiều dòng. Quyền kiểm cả HTTP và direct-SP. Ca provider thật tách khỏi adapter test và mô phỏng.

### Bước 5. Ghi evidence và commit đơn vị đã kiểm

Evidence đặt tại `docs/evidence/<ten>/<TASK-ID>.md`, dùng tên ASCII chữ thường của thành viên. Ghi phần bàn giao, commit được kiểm, môi trường/fixture, case, expected/actual, lệnh, exit code, DB trước/sau, log đã lọc và blocker. Không lưu password, OTP, cookie, raw QR hoặc provider secret.

Commit dùng tiếng Anh theo `type(scope): description`, có footer `Task-Id`, `Validation`, `Evidence`. Task lớn có nhiều commit/PR con; checkpoint chưa đạt không được tính hoàn thành hoặc merge như bản đã nghiệm thu. Kiểm diff và stage đường dẫn cụ thể theo GIT-WORKFLOW.

### Bước 6. Review, ghép và xác nhận người nhận

PR nêu task/phần, phạm vi, dependency/caller, contract/migration, validation/evidence và giới hạn. Develop cần ít nhất một reviewer không phải tác giả; tiền, quyền, transaction hoặc hợp đồng liên miền cần hai reviewer chuyên môn. Shared files cần owner phối hợp. Chỉ người được nhóm giao quyền mới merge.

Sau merge, chạy smoke bản ghép và kiểm lại khi revision thay đổi; ghi evidence và người nhận. Main nhận bản nghiệm thu qua PR develop → main với hai reviewer, kiểm SHA main trước khi tag/gói bàn giao. Quy trình chi tiết và ngoại lệ theo GIT-WORKFLOW.

## 4. Lộ trình M0–M4 và điều kiện đóng mốc

Mốc mô tả thứ tự nghiệm thu bản ghép. Có thể chuẩn bị phần độc lập của mốc sau, nhưng chưa công nhận mốc đó nếu điều kiện đầu vào còn thiếu. Các ca concurrency/recovery chạy từ khi nghiệp vụ được giao, không dồn tất cả đến M4.

### 4.1. M0. Nền chung, hợp đồng và schema

| Thành viên | Phần phải giao tại M0 |
|---|---|
| Khánh | 01–04; interface/type của 06; nền filters/CSRF của 07; khung layout/API client của 11 |
| Đông | 01/02: bốn Model, DTO/Service hợp đồng, schema membership/category/event/kho |
| Liêm | 01/02: bảy Model, DTO, schema bán vé/outbox và Worker SPI |
| Thái | 01: Refund đủ diagram, DTO/schema và log kỹ thuật cho bù trừ |
| Vương | 01–03: policy/models/schema, manifest/fixture/cross-FK và principal/grants; phần browser tooling/CI nền của 15 |

Đóng M0 khi WAR triển khai được trên Tomcat và health hoạt động; kết nối SQL Server thật; DB mới dựng được năm schema/cross-FK/fixture nền; JPA mapping và signatures của 15 Model được kiểm. Contract tests dùng được shared types/SPI/layout/principal. Service chưa triển khai được ghi rõ; compile thành công riêng lẻ chưa đủ để đóng mốc.

### 4.2. M1. Tài khoản, tổ chức và sự kiện

| Thành viên | Phần phải giao tại M1 |
|---|---|
| Khánh | 05–10; hoàn thiện 06/07/11; 12 UI-03; phần profile/membership/UI-24 của 13 |
| Đông | 03–10: hồ sơ BTC, SP01/fee/manager, membership, draft/editor/ảnh, submit/publish/SP12, UI-01/UI-02; menu/list nền của 11 |
| Liêm | 03 chuẩn bị SP07 và hợp đồng hủy/giải phóng cho M2 |
| Thái | Phần 02 về F07, quyền và cửa sổ check-in cho query của Đông |
| Vương | 04 policy/F02/TR03; audit consumer của 11; Admin adapter/UI-19/UI-20 của 12; bổ sung grants theo object |

OTP ở KHANH-08 gửi ngay qua MailSender sau commit, không queue mã rõ vào outbox. KHANH-09 xác minh OTP/reset grant; KHANH-10 seed admin, không phải EmailJob. Ảnh M1 có cleanup handler/intent; chạy qua dispatcher chung khi LIEM-11 được giao ở M2.

Đóng M1 khi buyer đăng ký/xác minh email/đăng nhập qua luồng thật; reset mật khẩu vô hiệu phiên cũ; admin seed dùng được theo cấu hình. Admin duyệt tổ chức cùng phí ban đầu và MANAGER trong một transaction. Manager tạo khu ngồi/đứng, submit sự kiện và admin publish được; trang chủ/chi tiết đọc DB thật. Có evidence CSRF, owner/org, membership hiện hành và khóa layout sau publish. Email provider thiếu cấu hình được ghi BLOCKED, không coi mock là gửi mail thật.

### 4.3. M2. Mua vé và check-in

| Thành viên | Phần phải giao tại M2 |
|---|---|
| Liêm | Hoàn thiện 03; 04–14: hold/order/coupon/VNPAY/SP09/QR/query/UI/outbox/expiry/reconcile; 15 kiểm race/rollback/replay/recovery/sandbox |
| Thái | Hoàn thiện 02; 03–05: SP04, V05/IX05, camera/manual và UI-15/UI-16 |
| Khánh | 13 ghép orders/tickets/hold qua MeServlet; 14 EmailJob và email vé qua dispatcher Liêm |
| Đông | 11 ghép coupon/check-in adapters; phần mua vé của 12 kiểm biến động kho |
| Vương | 12 phối hợp admin coupon adapter; 14 bắt đầu catalog/caller; 15 kiểm bản ghép; bổ sung grants |

LIEM-10/SP09 đã xử lý đơn 0đ và tạo nghĩa vụ PAYMENT_COMPENSATION cho thu muộn/trùng. Nó dùng Refund/schema từ THAI-01 M0; việc chuyển tiền bù trừ được ghép với worker Thái ở M3. LIEM-13 cung cấp UI-06/UI-07; LIEM-14 cung cấp UI-04/UI-05/UI-14.

Đóng M2 khi giữ 1–8 vé/TTL 10 phút, tạo Order/coupon, thanh toán VNPAY Sandbox, phát hành QR và check-in chạy xuyên API/SQL/JSP. Kiểm riêng đơn 0đ không tạo Payment; hai buyer tranh ghế/quota; một Hold ACTIVE/user; callback lặp không phát hành trùng; thu muộn/trùng chỉ tạo một nghĩa vụ bù trừ. Kiểm hai lần quét chỉ một lần USED, đúng giờ/quyền; rollback và worker restart không nhân kho/vé. Evidence sandbox phải có riêng; thiếu credentials không được thay bằng mock rồi đóng toàn mốc.

### 4.4. M3. Hoàn tiền, hủy, tài chính và đủ 24 UI

| Thành viên | Phần phải giao tại M3 |
|---|---|
| Thái | 06–14: F04/SP05/V07, quyết định/retry/zero refund, adapter mô phỏng bền vững, SP11, refund worker, SP13/SP17/cancel worker, UI-08 và handoff |
| Vương | 05–10: V03/F09/SP14/SP15/TR04/TR09/SP16/V08/F05/F10/V04/CSV/UI-17; 11 UI-18/audit UI-23/TR10; 12 refund/cancel Admin/UI-20/UI-21; 13 UI-22; 15 kiểm bản ghép |
| Liêm | 15 bổ sung race refund/cancel/late capture/recovery; giữ SP09 và compensation publication đúng hợp đồng |
| Khánh | 13 ghép refund query; 14 email REFUND_COMPLETED/REFUND_REJECTED |
| Đông | 11 ghép báo cáo; 12 kiểm trả kho/hold/bán/hoàn/hủy qua các miền |

Đóng M3 khi đủ 24 UI theo bảng ownership và COVERAGE, có loading/empty/error, quyền và dữ liệu thật. Hoàn 0đ không tạo transfer giả; hoàn có tiền cập nhật nghĩa vụ/attempt/vé/kho nguyên tử; UNKNOWN đối soát cùng attempt, không tự coi thất bại để thử mới. Hủy/restart tiếp tục theo lô mà không nhân nghĩa vụ/kho. Compensation không làm tăng doanh thu vé hoặc trả kho của vé hợp lệ.

SP15 kiểm endTime và F09, tính lại/confirm cùng transaction; TR04/TR09 giữ snapshot đã đóng. Payout lặp/đồng thời không vượt số dư và phục hồi theo payoutId. Báo cáo F05 và F10 phân biệt nhóm đơn với dòng tiền phát sinh; CSV cùng filter/quyền và chống injection; audit không lộ secret. Các khoản hoàn/chi dùng mô phỏng có ledger bền vững, ghi nhãn đúng nguồn chứng minh.

### 4.5. M4. Tái lập, benchmark và hồ sơ bàn giao

| Thành viên | Phần phải giao tại M4 |
|---|---|
| Khánh | 15 nghiệm thu auth/layout/mail/Me routes, security/recovery và phần hồ sơ |
| Đông | 13 nghiệm thu miền, benchmark IX01/IX02/IX15 và hồ sơ |
| Liêm | 16 ghép migration/runbook, benchmark IX03/IX04/IX06/IX09/IX11 và hồ sơ |
| Thái | 15 recovery/quyền/miền, benchmark IX05/IX08/IX10 và hồ sơ |
| Vương | Hoàn thiện runbook/grants của 02/03; 14 catalog/benchmark toàn bộ 15 index; 15 whole-project checks; 16 Docker/restore; 17 báo cáo/slide/demo |

Đóng M4 khi checkout sạch dựng được DB/migration/grants/seed và WAR trên Tomcat; chạy local/Docker theo runbook, kiểm backup/restore cùng ledger kỹ thuật. Build/unit, SQL Server IT, browser và security checks của bản ghép có ma trận PASS/FAIL/BLOCKED gắn commit/evidence. FAIL phải sửa; BLOCKED không được tính PASS hoặc tuyên bố nghiệm thu toàn bộ.

Mỗi index IX01–IX15 có query/caller, execution plan, reads/time trước/sau trên cùng fixture và đánh giá write cost; Vương tổng hợp, chủ miền cung cấp bằng chứng. Có báo cáo sáu chương với 50–100 trang nội dung, slide tối đa 15 trang, demo và gói source/database/runbook theo kế hoạch báo cáo. Các phần cần tài khoản/quyền online được nghiệm thu riêng khi đủ cấu hình; chạy local không chứng minh đã deploy Render/Azure SQL.

Nhóm giữ phân công năm người. Chênh lệch yêu cầu nhóm 3–4 người trong spec cần xác nhận với giảng viên khi nộp; không tự xóa thành viên. Ngày bắt đầu, deadline và năng lực chưa chốt thì ghi còn chờ xác nhận, không lấy lịch cũ làm tiến độ thật.

## 5. Danh mục 76 task và điểm bàn giao

Tên công việc dưới đây lấy từ heading của file thành viên. Cột phụ thuộc nêu artifact chính để điều phối; đọc mục Phụ thuộc, Consumes/Produces và checklist của Task ID trong file nguồn trước khi triển khai. M0/M1 hoặc M2/M3 nghĩa là task có phần bàn giao ở từng mốc, không phải chờ toàn task rồi mới chuyển giao.

### 5.1. Khánh, 15 task

Nguồn chi tiết: [khanh.md](khanh.md).

| Task ID | Công việc chuẩn | Mốc bàn giao | Phụ thuộc chính | Artifact và điểm nghiệm thu |
|---|---|---|---|---|
| `KHANH-01` | Build WAR và health/test tooling | M0 | Không; phối hợp runtime/tooling Vương | WAR, health, Maven test tooling và cấu hình nền |
| `KHANH-02` | Kiểu dữ liệu, HTTP và lỗi chung | M0 | KHANH-01; các chủ miền review kiểu/DTO | HTTP/error/parse, Page/PageRequest, ClockProvider và kiểu chung |
| `KHANH-03` | JPA và transaction giữ principal/actor | M0 | KHANH-01/02/04; fixture VUONG-02, principal/grants VUONG-03 | JPA factory, TransactionRunner, cùng connection và outer transaction |
| `KHANH-04` | User đủ diagram và schema identity/C01 | M0 | KHANH-01/02; Organization/membership Đông; manifest Vương | User đủ methods, C01, 0010_identity.sql; round-trip khi đủ nền03 |
| `KHANH-05` | Đăng ký chuẩn hóa nguyên tử và hash mật khẩu | M1 | KHANH-02/03/04/07; OTP08 và auth TECH grants | Register CUSTOMER, normalized uniqueness, hash và duplicate/race |
| `KHANH-06` | Session, ActorContext và quyền hiện hành | M0 nền; M1 | Types/JPA/User Khánh; registration05; membership Đông, grants Vương | ActorContext/auth interfaces M0; login/session/authVersion/quyền M1 |
| `KHANH-07` | CSRF, validation và bảo vệ request | M0 nền; M1 | KHANH-01/02 và nền06; IPN contract Liêm | CSRF, validation, filters/error/security headers; áp dụng request thật M1 |
| `KHANH-08` | OTP gửi ngay và MailSender | M1 | KHANH-02/03/04/06/07; provider/secret và grants | OtpService/MailSender gửi sau commit; HMAC, expiry/resend, không OTP rõ trong outbox |
| `KHANH-09` | Verify OTP và reset grant một lần | M1 | KHANH-03/04/06/07/08 | Verify OTP đúng purpose; reset grant một lần; tăng authVersion nguyên tử |
| `KHANH-10` | Seed admin idempotent và cấu hình auth public | M1 | KHANH-03/04/05/06/07; VUONG-02/03 | AdminSeeder idempotent, cấu hình demo/public; restart không ghi đè password |
| `KHANH-11` | Layout, CSS và Fetch client dùng chung | M0 nền; M1 | KHANH-01/02/07; asset/layout contract các chủ UI | Layout/CSS/API client/UI states; khung M0, ghép trang M1 |
| `KHANH-12` | UI03 auth/OTP/forgot/reset | M1 | KHANH-05 đến KHANH-11; membership Đông | UI-03 auth/OTP/forgot/reset và điều hướng đúng quyền |
| `KHANH-13` | MeServlet, UI24 và query xuyên miền | M1/M2/M3 | KHANH-02/03/06/07/11/12; query Đông/Liêm/Thái theo mốc | MeServlet/UI-24; M1 profile/membership, M2 orders/tickets/hold, M3 refunds |
| `KHANH-14` | EmailJob vé/kết quả hoàn | M2/M3 | KHANH-02/03/08; outbox Liêm, refund producer Thái, grants | EmailJob M2 vé; M3 kết quả hoàn; đăng ký dispatcher chung |
| `KHANH-15` | Nghiệm thu ghép và bàn giao | M4 | KHANH-01 đến KHANH-14; bản ghép và tooling Vương | Security/recovery/auth/layout/mail/Me evidence, runbook và hồ sơ miền |

### 5.2. Đông, 13 task

Nguồn chi tiết: [dong.md](dong.md).

| Task ID | Công việc chuẩn | Mốc bàn giao | Phụ thuộc chính | Artifact và điểm nghiệm thu |
|---|---|---|---|---|
| `DONG-01` | Model và hợp đồng nền đủ cho các module khác | M0 | Build/types/User Khánh; Model/DTO Liêm và CommissionRule Vương | Bốn Model đủ methods; catalog DTO/Service và mutations kho |
| `DONG-02` | Schema nền, constraint và danh mục | M0 | DONG-01; User schema, audit/fee schema và manifest chung | 0020_organizations_events.sql, membership/category và C02/C03/C04/C05/C10 |
| `DONG-03` | Hồ sơ tạo tổ chức và UI-09 | M1 | DONG-01/02; HTTP/JPA/auth/CSRF/layout Khánh | OrganizationRequestServlet/UI-09; submit/get/list/reject Service cho Admin |
| `DONG-04` | Duyệt tổ chức nguyên tử SP01/TX01 | M1 | DONG-02/03; policy validator/schema Vương; User/transaction Khánh | SP01/TX01 duyệt cùng initial fee/MANAGER/audit, replay không nhân quyền |
| `DONG-05` | Membership/V10/TR06 và UI-11 | M1 | DONG-02/04; User và AuthorizationService Khánh | MembershipService/V10/TR06/UI-11; OrganizationServlet và quyền hiện hành |
| `DONG-06` | CRUD Event nháp và UI-12 | M1 | DONG-01/02/04/05 | Event nháp/UI-12; query nội bộ đủ nghiệm thu, scope đúng tổ chức |
| `DONG-07` | Khu/ghế, giá và UI-13 | M1 | DONG-01/02/06; price snapshot contract LIEM-01 | Zone/Seat editor UI-13, ngồi/đứng, giá không hồi tố |
| `DONG-08` | Ảnh bìa, storage và handler dọn ảnh | M1; worker ghép M2 | DONG-06/07; config Khánh, Job SPI Liêm, storage/runbook Vương | Upload ảnh bền qua redeploy; cleanup handler/intent, dispatcher chung M2 |
| `DONG-09` | Submit/reject/publish, SP12/TX12 và bảo vệ layout/audit | M1 | DONG-06/07/08; rule/TR03/grants Vương; actor Khánh | Submit/reject/publish SP12/TX12; TR01/TR02 khóa layout, TR05 audit |
| `DONG-10` | Query/View/F03, danh mục và UI-01/02 | M1; hold adapter M2 | DONG-02/07/09; Hold contract Liêm; CheckInService/F07 Thái | V01/V02/F03/UI-01/UI-02; query public/internal và tồn kho thật |
| `DONG-11` | UI-10 và adapter wildcard ghép coupon/report/check-in | M1/M2/M3 | DONG-03/05/06/10; HTTP/auth/layout/Me membership Khánh; Service các miền | UI-10 và Organization/Event adapters; ghép coupon/check-in M2, reports M3 |
| `DONG-12` | Hợp đồng kho, hoàn/hủy và các race xuyên module | M2/M3 | Kho/layout Đông; SP02/SP07/SP09 Liêm; SP10/SP11/SP13/SP17 Thái | Inventory contract IT; mua vé M2, hoàn/hủy M3, kiểm race/replay |
| `DONG-13` | Ba index, quyền miền, hồ sơ và nghiệm thu M4 | M4 | DONG-01 đến DONG-12; benchmark/tooling/grants/runbook Vương | IX01/IX02/IX15, quyền miền, fresh-clone evidence và hồ sơ |

### 5.3. Liêm, 16 task

Nguồn chi tiết: [liem.md](liem.md).

| Task ID | Công việc chuẩn | Mốc bàn giao | Phụ thuộc chính | Artifact và điểm nghiệm thu |
|---|---|---|---|---|
| `LIEM-01` | Cấp Model, DTO và SPI cho cả nhóm từ đầu M0 | M0 | Types/User Khánh; Organization/Event/Zone/Seat Đông; Refund/fee contracts | Bảy Model, DTO/Service, JobHandlerRegistry và Worker SPI |
| `LIEM-02` | Schema bán vé, khóa và hợp đồng SQL | M0 | LIEM-01; schema/contracts liên miền và manifest Vương | 0030_sales.sql, outbox/redemption và C06/C07/C08/C09/C11/C12/C13/C14 |
| `LIEM-03` | SP07 giải phóng giữ vé nguyên tử | M1 chuẩn bị; M2 | LIEM-01/02; Seat/Zone methods Đông, TransactionRunner/actor Khánh | SP07/TX07 trả ghế/quota/coupon một lần; không đặt Payment FAILED do TTL |
| `LIEM-04` | SP02 giữ 1–8 vé và dọn Hold cũ cross-Event | M2 | LIEM-01/02/03; kho/event public Đông, actor/principal Khánh | SP02/TX02 giữ 1–8 vé/10 phút; dọn hold cũ, chống oversell |
| `LIEM-05` | SP06 một Order/Hold với snapshot bất biến | M2 | LIEM-01/02/03/04; snapshot/clock/transaction chung | SP06/TX06 một Order/Hold, snapshot Item và tổng bất biến |
| `LIEM-06` | Coupon Service, F01/F08/V09/TR07 | M2 | LIEM-01/02; AuthorizationService Khánh, Organization Đông | CouponService/V09/F01/F08/TR07; management/eligibility theo scope |
| `LIEM-07` | SP03 giữ/đổi/bỏ coupon nguyên tử | M2 | LIEM-03/05/06; Payment schema | SP03/TX03 giữ/đổi/bỏ coupon nguyên tử, đúng quota |
| `LIEM-08` | SP08 khởi tạo Payment, dùng lại lần đang theo dõi | M2 | LIEM-05/07; TransactionRunner/principal Khánh | SP08/TX08 và PaymentIntent; giữ lần đang PENDING/UNKNOWN |
| `LIEM-09` | VNPAY Sandbox verifier và HTTP callback | M2 | LIEM-01/08; config/credentials; apply sink LIEM-10 khi ghép | VNPAY verifier/Return/IPN; browser không xác nhận tiền, evidence sandbox riêng |
| `LIEM-10` | F06 và SP09 phát hành/đơn0đ/bù trừ | M2 | LIEM-03/05/07/08/09; Refund/schema/log THAI-01; fee/grants Vương | F06/SP09/TX09 phát hành, đơn0đ và PAYMENT_COMPENSATION; không chờ refund worker M3 |
| `LIEM-11` | Một dispatcher outbox có lease và lifecycle | M2 | LIEM-01/02/10; config/system actor Khánh; đăng ký handler các miền | Outbox dispatcher/claim/ack lease, token/lifecycle; một scheduler |
| `LIEM-12` | Dọn Hold và reconcile Payment PENDING/UNKNOWN | M2 | LIEM-03/09/10/11; compensation contract Thái | HoldExpiry/PaymentReconciliation; query trusted result vào SP09 |
| `LIEM-13` | Query lịch sử, vé, QR và snapshot guard | M2; refund query ghép M3 | LIEM-01/05/10; HTTP/layout Khánh, Ticket/refund contracts Thái | V06/TR08, query/QR/UI-06/UI-07; query bàn giao MeServlet Khánh |
| `LIEM-14` | UI04 checkout, UI05 trạng thái và UI14 coupon | M2 | LIEM-04 đến LIEM-10, LIEM-13; layout/API client, Organization/Admin adapters | UI-04 checkout, UI-05 kết quả, UI-14 coupon qua dữ liệu thật |
| `LIEM-15` | Race, rollback, replay và sandbox mua thật | M2/M3 | LIEM-03 đến LIEM-14; check-in/refund/cancel Thái; auth/grants/fixture | Race/rollback/replay/recovery/sandbox mua thật; bổ sung ca hoàn/hủy M3 |
| `LIEM-16` | Ghép migration, benchmark và hồ sơ bàn giao | M4 | LIEM-01 đến LIEM-15; manifest/tooling/browser/runbook chung | IX03/IX04/IX06/IX09/IX11, migration/runbook, hồ sơ và bàn giao miền |

### 5.4. Thái, 15 task

Nguồn chi tiết: [thai.md](thai.md).

| Task ID | Công việc chuẩn | Mốc bàn giao | Phụ thuộc chính | Artifact và điểm nghiệm thu |
|---|---|---|---|---|
| `THAI-01` | Refund đủ diagram, DTO và schema nền | M0 | User/Event/Order/Payment/Ticket contracts; manifest/cross-FK Vương | Refund đủ methods/createCompensation, 0040 schema/log/DTO, C15/C20 |
| `THAI-02` | F07, quyền và cửa sổ check-in | M1/M2 | THAI-01; JPA/auth Khánh; DONG-01/02 schema/query membership | F07/CheckInWindow/quyền M1; list Event đúng scope M2 |
| `THAI-03` | SP04/TX04 check-in nguyên tử | M2 | THAI-01/02; Ticket.checkIn/vé phát hành Liêm; khóa chung | SP04/TX04 quét một lần và log kết quả; TR08 do Liêm sở hữu |
| `THAI-04` | V05/IX05 và lịch sử quét | M2 | THAI-03 | V05/IX05 và query lịch sử quét phân trang, không lộ QR/tài chính |
| `THAI-05` | JSP UI15/UI16 và camera/manual | M2 | THAI-02/03/04; layout/API client Khánh, browser Vương | UI-15/UI-16 camera/manual, chọn Event và cùng endpoint check-in |
| `THAI-06` | F04 và SP05/TX05 gửi yêu cầu hoàn | M3 | THAI-01; Order/Ticket/paidAmount Liêm; Event Đông | F04/SP05/TX05 vé đủ điều kiện, một nghĩa vụ mở/vé |
| `THAI-07` | V07/IX10 và query lịch sử hoàn | M3 | THAI-06; refund/log schema THAI-01 | V07/IX10, query nghĩa vụ/current attempt/lịch sử; V08 thuộc Vương |
| `THAI-08` | SP10/TX10 quyết định, retry và zero refund | M3 | THAI-06/07; Ticket methods Liêm, trả kho Đông, outbox SPI | SP10/TX10 quyết định/retry; hoàn0đ COMPLETED không transfer giả |
| `THAI-09` | Adapter mô phỏng hoàn bền vững | M3 | THAI-08; config/ClockProvider Khánh; storage/runbook Vương | MockRefundProviderLedger/adapter submit/query theo attemptId, sống qua restart |
| `THAI-10` | SP11/TX11 ghi kết quả, trả kho và IX08 | M3 | THAI-08/09; Ticket/kho; khóa chung với settlement Vương | SP11/TX11/IX08, ghi kết quả và trả kho một lần; UNKNOWN giữ attempt |
| `THAI-11` | RefundJob và phối hợp bù trừ SP09 | M3 | THAI-08/09/10; worker SPI/SP09 Liêm | RefundJob, reconciliation/compensation sau commit; retry đúng trạng thái |
| `THAI-12` | SP13/TX13 khởi động hủy Event | M3 | Event.cancel/TR05 Đông; outbox/kho Liêm; THAI-01 | SP13/TX13 hủy có hiệu lực ngay, intent xử lý theo lô |
| `THAI-13` | SP17/TX17 từng Order và cancellation worker | M3 | THAI-08/10/11/12; SP07/SP09 Liêm; kho Đông và Job SPI | SP17/TX17 từng Order/cancellation worker, tiếp tục sau restart |
| `THAI-14` | UI08 và API tài khoản/Admin handoff | M3 | THAI-06 đến THAI-11; layout Khánh, browser/Admin adapter Vương | UI-08/refund pages và API handoff; chủ Me/Order/Admin sửa adapter của mình |
| `THAI-15` | Nghiệm thu race/recovery, quyền và bàn giao | M4 | THAI-01 đến THAI-14; bản ghép các miền | Recovery/race/quyền, IX05/IX08/IX10, fresh-clone evidence/hồ sơ miền |

### 5.5. Vương, 17 task

Nguồn chi tiết: [vuong.md](vuong.md).

| Task ID | Công việc chuẩn | Mốc bàn giao | Phụ thuộc chính | Artifact và điểm nghiệm thu |
|---|---|---|---|---|
| `VUONG-01` | Hai Model tài chính và hợp đồng nền | M0 | Types/ClockProvider Khánh; Organization contracts Đông | CommissionRule/Settlement, policy validator/DTO, 0050 schema, C16/C17/C18/C19 |
| `VUONG-02` | Manifest, FK liên miền, fixture và runbook DB | M0; hoàn tất M4 | Năm schema/chủ miền; registry/fixture contracts | Manifest/cross-FK/checksum/fixture/runbook; M4 tái lập DB sạch |
| `VUONG-03` | Bốn role/login và principal ứng dụng | M0; bổ sung M1/M2/M3/M4 | VUONG-02; principal/transaction Khánh; từng object của chủ miền | Bốn role/login và TECH quyền hẹp; bổ sung grant/negative tests khi giao object |
| `VUONG-04` | CommissionService, F02 và TR03 | M1 | VUONG-01/02/03; schema/publish Đông | CommissionService/F02/TR03/IX14; policy đã áp dụng không sửa phí hồi tố |
| `VUONG-05` | V03/F09 và dữ liệu tài chính hiện hành | M3 | VUONG-02; nguồn thu Liêm, refund/outbox Thái | V03/F09 và DTO blocker; dữ liệu hiện hành, một Order một dòng |
| `VUONG-06` | SP14/TX14 tính lại DRAFT | M3 | VUONG-04/05; thứ tự khóa đã review liên miền | SP14/TX14 tính lại DRAFT, snapshot từng Order và tổng nguyên tử |
| `VUONG-07` | SP15/TX15 xác nhận và đóng băng | M3 | VUONG-05/06; SP09 Liêm/SP11 Thái và khóa chung | SP15/TX15, TR04/TR09; endTime/blocker/recalc/confirm/freeze |
| `VUONG-08` | SP16/TX16 payout mô phỏng, V08/IX12 | M3 | VUONG-07; ADMIN principal VUONG-03 | SP16/TX16/V08/IX12, ledger mô phỏng payout bền vững và replay |
| `VUONG-09` | F05/F10/V04 và ReportService | M3 | VUONG-04/05/07; V01/V02 Đông, V05/V07 Thái, V06/V09 Liêm | F05/F10/V04/IX07/ReportService; cohort và cashflow tách rõ |
| `VUONG-10` | CSV và JSP UI17/UI23 báo cáo | M3 | VUONG-09; layout/API client Khánh, browser tooling VUONG-15 | CSV/UI-17/UI-23 cùng scope/filter/tổng, chống CSV injection |
| `VUONG-11` | Audit append-only và overview | M1 nền; M3 | VUONG-01; actor context Khánh; TR05/audit các miền | Audit consumer M1; M3 UI-18/overview và audit UI-23, TR10/IX13 |
| `VUONG-12` | AdminServlet, UI19/UI20/UI21 | M1/M2/M3 | Duyệt org/event Service Đông; coupon Liêm; refund/cancel Thái | AdminServlet/UI-19/UI-20 M1; coupon adapter M2; UI-21/hủy M3 |
| `VUONG-13` | UI22 đối soát/chi trả | M3 | VUONG-04 đến VUONG-08; layout/auth/API client, browser tooling | UI-22 policy/settlement/payout, blocker/snapshot/pending rõ |
| `VUONG-14` | Catalog SQL và benchmark 15 index | M2 catalog; M4 | Tất cả SQL objects/callers và benchmark do chủ miền cung cấp | Catalog đủ rubric; tổng hợp plan/reads/time/write cost của 15 index |
| `VUONG-15` | CI, browser profile và nghiệm thu bản ghép | M0 tooling; M2/M3/M4 | Build/unit/SQL profile Khánh; fixture và bản ghép; môi trường test | CI/browser profile nền; whole-project API/SQL/UI/security/recovery checks |
| `VUONG-16` | Docker/runbook/restore | M4 | WAR/config Khánh; manifest/grants/seed; storage/ledger/runbook các miền | Docker/local/fresh clone/backup-restore; online kiểm riêng khi đủ cấu hình/quyền |
| `VUONG-17` | Báo cáo môn học, demo và bàn giao | M4 | Evidence/hồ sơ từng miền; bản ghép và kế hoạch báo cáo DBMS | Báo cáo sáu chương/50–100 trang nội dung, slide tối đa15, demo/gói nộp |

## 6. Review chéo và trạng thái bàn giao

| Tác giả | Reviewer ưu tiên | Trọng tâm |
|---|---|---|
| Khánh | Vương, Đông; Liêm khi có payment/IPN/outbox | Transaction/principal, session/OTP/CSRF, membership, layout/mail/Me adapters |
| Đông | Liêm, Vương; Thái khi có kho/check-in/cancel | Kho ngồi/đứng, layout locks, policy/publish/audit, ownership adapters |
| Liêm | Đông, Thái; Khánh khi có auth/IPN, Vương khi có tiền/principal | Hold/quota, snapshot/phân bổ, callback/compensation, outbox và recovery |
| Thái | Liêm, Đông; Vương khi có settlement/Admin/payout locks | Check-in, nghĩa vụ/attempt, zero/UNKNOWN, trả kho, cancel và handoff |
| Vương | Khánh, Liêm; Thái khi có blockers/hoàn | Grants, tài chính/snapshot, báo cáo/audit/Admin, catalog/tooling và bản bàn giao |

Bảng này chỉ chọn người theo phạm vi, không thay reviewer cụ thể ghi trong task. PR tiền/quyền/transaction/hợp đồng liên miền cần hai người chuyên môn. Người nhận đầu ra xác nhận chữ ký/DTO dùng được; tác giả không tự approve.

Mỗi phần bàn giao ghi trạng thái chưa làm, đang làm, bị chặn hoặc đã kiểm chứng. Kết quả check ghi PASS/FAIL/BLOCKED và evidence. Blocker ghi artifact/môi trường thiếu, owner, ảnh hưởng đến mốc và phần độc lập tiếp tục được. Vương điều phối, không mặc nhiên là người duy nhất có quyền merge hoặc reviewer duy nhất.

## 7. Mẫu giao một task hoặc phần task cho AI

Thay `<ten>` bằng `khanh`, `dong`, `liem`, `thai` hoặc `vuong`. Thay Task ID và phần bàn giao bằng công việc thực tế; không dùng tên có dấu làm đường dẫn file.

```text
Tôi phụ trách <tên thành viên> trong dự án TicketsCenter.
Triển khai <TASK-ID>, phần <artifact/mốc cụ thể>, theo docs/tasks/<ten>.md.
Đọc docs/tasks/PROJECT-ROUTINE.md để phối hợp, không triển khai cả kế hoạch.

Trước khi sửa:
1. Đọc spec.md, docs/classdiagram/diagram.md, docs/tasks/TEAM-CONTRACT.md,
   docs/tasks/GIT-WORKFLOW.md, docs/tasks/CONVENTIONS.md, API-MAP.md và COVERAGE.md
   trong docs/tasks; đọc task nguồn và các mục phụ thuộc liên quan.
2. Kiểm trạng thái repo/nhánh/diff. Phân biệt file đã có với đầu ra phải tạo.
   Xác định owner, Consumes/Produces, reviewer và điều kiện nghiệm thu phần được giao.
3. Chỉ sửa phạm vi được giao. Shared file hoặc hợp đồng khác miền phải phối hợp owner.
   Thiếu type/Service thì yêu cầu artifact chuẩn, tiếp tục phần độc lập;
   không tự tạo Model/DTO trùng hoặc trả thành công giả.
4. Tuân manifest và FK nội miền/cross-FK. Không sửa migration đã áp dụng chung.
5. Chạy check đúng task trên môi trường cần thiết; kiểm dữ liệu và case lỗi/rollback,
   race/replay/recovery phù hợp. Tách mock, mô phỏng và sandbox/provider thật.
6. Ghi docs/evidence/<ten>/<TASK-ID>.md với commit, môi trường, case, expected/actual,
   lệnh/exit code và blocker. Chỉ tick checklist có bằng chứng.
7. Nếu được giao commit, dùng Conventional Commits tiếng Anh và footer Task-Id,
   Validation, Evidence; stage đúng đường dẫn. Push/merge/tag/deploy cần phạm vi
   được giao rõ và tuân GIT-WORKFLOW, không suy ra quyền từ prompt mẫu này.

Báo cáo file đã đổi, artifact đã bàn giao, checks đã chạy, kết quả và phần bị chặn.
```

## 8. Kiểm tra đồng bộ routine sau khi nguồn thay đổi

Đối chiếu Task ID/tên/mốc với năm file thành viên; ownership và danh mục SQL với TEAM-CONTRACT; meaning/state/invariants với spec/diagram; reviewer/commit với GIT-WORKFLOW; UI/caller/evidence với API-MAP/COVERAGE. Mọi thay đổi hợp đồng phải cập nhật cả nguồn và caller liên quan.

Lệnh PowerShell sau chạy tại gốc repo, chỉ đọc tài liệu. Nó kiểm đủ 76 task không trùng, tên task khớp file thành viên và đích Markdown tồn tại. Điều kiện đóng mốc và semantics SQL vẫn cần người review theo nguồn chuẩn.

```powershell
$taskRoot = Join-Path (Get-Location) 'docs/tasks'
$routine = Get-Content -LiteralPath (Join-Path $taskRoot 'PROJECT-ROUTINE.md') -Raw -Encoding UTF8
$rows = [regex]::Matches($routine, '(?m)^\| `((?:KHANH|DONG|LIEM|THAI|VUONG)-\d{2})` \| ([^|]+) \|')
$actual = @{}
foreach ($row in $rows) {
    $id = $row.Groups[1].Value
    if ($actual.ContainsKey($id)) { throw "Duplicate task: $id" }
    $actual[$id] = $row.Groups[2].Value.Trim()
}
$expected = @{}
foreach ($name in @('khanh', 'dong', 'liem', 'thai', 'vuong')) {
    $source = Get-Content -LiteralPath (Join-Path $taskRoot "$name.md") -Raw -Encoding UTF8
    foreach ($heading in [regex]::Matches($source, '(?m)^#{2,3} ([A-Z]+-\d{2}) [^\w\s] (.+?)\r?$')) {
        $expected[$heading.Groups[1].Value] = $heading.Groups[2].Value.Trim()
    }
}
if ($rows.Count -ne 76 -or $expected.Count -ne 76) { throw 'Expected 76 tasks in routine and member plans' }
foreach ($id in $expected.Keys) {
    if ($actual[$id] -cne $expected[$id]) { throw "Task title mismatch: $id" }
}
foreach ($link in [regex]::Matches($routine, '\[[^\]]+\]\(([^)]+\.md)\)')) {
    if (!(Test-Path -LiteralPath (Join-Path $taskRoot $link.Groups[1].Value))) {
        throw "Missing Markdown target: $($link.Groups[1].Value)"
    }
}
'PASS: 76 task IDs/titles and Markdown targets'
```
