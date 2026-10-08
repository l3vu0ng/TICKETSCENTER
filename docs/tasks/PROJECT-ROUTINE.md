# Lộ trình & Quy trình phối hợp thực hiện dự án TicketsCenter (ROUTINE CHO CẢ NHÓM)

> **Tài liệu tham chiếu chuẩn:** [spec.md](../../spec.md), [docs/classdiagram/diagram.md](../classdiagram/diagram.md), [docs/tasks/TEAM-CONTRACT.md](TEAM-CONTRACT.md), [docs/tasks/GIT-WORKFLOW.md](GIT-WORKFLOW.md), [CONVENTIONS.md](CONVENTIONS.md).  
> **Áp dụng cho 5 thành viên:** Khánh, Đông, Liêm, Thái, Vương (Tổng cộng 76 task).  
> **Nguyên tắc cốt lõi:** Làm việc song song (Parallel) theo từng mốc (Milestone); làm tuần tự theo cây phụ thuộc trong từng danh sách task cá nhân.

---

## 1. NGUYÊN TẮC LÀM VIỆC SONG SONG (PARALLEL WORKFLOW)

### 1.1. Thứ tự giữa các thành viên vs. Thứ tự cá nhân
* **Hàng ngang (Giữa 5 thành viên):** **LÀM SONG SONG**. Cả 5 người bắt tay làm việc cùng lúc trên các nhánh Git độc lập, không ai phải ngồi chờ ai.
* **Cột dọc (Cá nhân mỗi người):** **LÀM TUẦN TỰ** từ trên xuống dưới (`01` $\rightarrow$ `02` $\rightarrow$ `03`...) vì task sau sử dụng kết quả của task trước.

### 1.2. Giải pháp kỹ thuật để không bị kẹt phụ thuộc (Anti-Deadlock)
1. **Dữ kiện đã được đóng băng 100%:** Toàn bộ tên class, trường, kiểu dữ liệu, quan hệ đã có đầy đủ trong [diagram.md](../classdiagram/diagram.md) và [spec.md](../../spec.md). Khi code, cứ khai báo tham chiếu bình thường (`User buyer`, `Event event`, `Organization org`).
2. **Kỹ thuật Skeleton (Khung rỗng) & Test Fixture:** Nếu nhánh của bạn chưa có class của bạn khác, hãy tạo class khung rỗng tạm thời (chỉ chứa khai báo tên class, fields, getter cơ bản) để code Java biên dịch được (`compile`). Khi viết Unit Test, dùng mock/stub fixture để test độc lập logic của mình.
3. **Database tách riêng khóa ngoại liên miền:** Các file schema riêng từ `0010_...` đến `0050_...` **CHỈ TẠO KHÓA NGOẠI NỘI BỘ MIỀN MÌNH**. Toàn bộ Foreign Keys trỏ chéo sang bảng của người khác nằm ở file `0100_cross_domain_keys.sql` do Vương ghép cuối mốc M0.

---

## 2. VÒNG LẶP 6 BƯỚC THỰC HIỆN 1 TASK CÁ NHÂN (TASK ROUTINE)

Mỗi khi bất kỳ thành viên nào bắt tay vào làm một task cụ thể, luôn thực hiện theo đúng 6 bước sau:

```text
[Bước 1: Rẽ nhánh Git từ develop]
               │
               ▼
[Bước 2: Đối chiếu Hợp đồng (Consumes/Produces)]
               │
               ▼
[Bước 3: Viết Test & Code (Chỉ sửa file sở hữu)]
               │
               ▼
[Bước 4: Chạy kiểm chứng thật (Maven & SQL Server)]
               │
               ▼
[Bước 5: Lập file Bằng chứng & Commit chuẩn]
               │
               ▼
[Bước 6: Tạo PR vào develop & Review chéo]
```

### Chi tiết từng bước:
* **Bước 1: Rẽ nhánh Git đúng quy chuẩn**
  * Cập nhật code mới nhất: `git checkout develop && git pull origin develop`
  * Tạo nhánh: `git checkout -b feature/<tên>/<task-id>-<tên-ngắn>`  
  * *Ví dụ:* `feature/khanh/khanh-01-war-foundation`, `feature/liem/liem-02-sales-schema`
* **Bước 2: Đối chiếu Hợp đồng**
  * Mở [TEAM-CONTRACT.md](TEAM-CONTRACT.md) và file nhiệm vụ cá nhân (`<tên>.md`).
  * Xác định rõ: Task này nhận đầu vào từ ai (Consumes) và bàn giao đầu ra cho ai (Produces).
* **Bước 3: Viết mã nguồn & Unit Test**
  * Tuyệt đối **chỉ sửa các file thuộc phạm vi sở hữu** của mình.
  * Viết Unit Test kiểm tra các ca biên (boundary), ca lỗi (negative), ca tính tiền, kiểm tra null/rỗng.
* **Bước 4: Chạy kiểm chứng trên môi trường thật**
  * Chạy build: `mvn clean test` (hoặc profile `sqlserver-it` nếu có tích hợp DB).
  * Chạy script SQL kiểm thử trên SQL Server local, xác nhận cả ca thành công (`COMMIT`) và ca lỗi vi phạm constraint (`ROLLBACK`).
* **Bước 5: Ghi nhận bằng chứng & Commit**
  * Tạo file `docs/evidence/<tên>/<TASK-ID>.md` ghi lại nhật ký kiểm thử, lệnh đã chạy và kết quả.
  * Commit theo chuẩn Conventional Commits:  
    `feat(<scope>): <nội dung tóm tắt>`  
    Footer: `Task-Id: <TASK-ID>`
* **Bước 6: Tạo Pull Request & Merge**
  * Push nhánh lên remote, mở PR vào nhánh `develop`.
  * Thành viên nhận đầu ra của task sẽ vào review chéo, xác nhận đạt yêu cầu rồi merge.

---

## 3. LỘ TRÌNH PHỐI HỢP 5 THÀNH VIÊN THEO 5 MỐC (MILESTONE ROADMAP)

```text
┌────────────────────────────────────────────────────────────────────────┐
│ M0: Xây dựng Nền tảng, Hợp đồng Interfaces, 15 Model & Database Schema │
├────────────────────────────────────────────────────────────────────────┤
│ M1: Xác thực tài khoản, Duyệt Ban tổ chức & Quản lý Sự kiện / Ghế ngồi │
├────────────────────────────────────────────────────────────────────────┤
│ M2: Giữ vé 10 phút, Thanh toán VNPAY, Phát hành vé QR & Quét Check-in  │
├────────────────────────────────────────────────────────────────────────┤
│ M3: Yêu cầu Hoàn vé, Hủy sự kiện, Đối soát tài chính & Đủ 24 màn hình  │
├────────────────────────────────────────────────────────────────────────┤
│ M4: Kiểm thử tranh chấp (Concurrency), Benchmark Index & Đóng gói nộp  │
└────────────────────────────────────────────────────────────────────────┘
```

---

### 📌 MỐC M0: HẠ TẦNG NỀN TẢNG, HỢP ĐỒNG & SCHEMA
> **Mục tiêu:** Cả 5 người cùng cung cấp "khung xương" (Interfaces, DTOs, 15 Model và DDL Database). Không ai làm logic sâu, chỉ làm hợp đồng để phá vỡ thế kẹt phụ thuộc.

| Thành viên | Tasks thực hiện song song trong M0 | Đầu ra bàn giao cho cả nhóm |
|---|---|---|
| **Khánh** | • `KHANH-01`: Cấu hình `pom.xml`, khóa dependencies Tomcat 11, Hibernate 6.x, test profiles.<br>• `KHANH-02`: Kiểu dữ liệu chung `ActorContext`, `PageRequest`, `HttpResponses`, `BusinessException`.<br>• `KHANH-03`: `TransactionRunner` quản lý transaction JPA `resource-local`.<br>• `KHANH-04`: Entity `User` & script `0010_identity.sql`.<br>• `KHANH-11`: Layout JSP và CSS/Bootstrap dùng chung. | Khung build WAR chạy được `mvn test`, Entity `User`, context đăng nhập và bộ layout dùng chung. |
| **Đông** | • `DONG-01`: 4 Model `Organization`, `Event`, `Zone`, `Seat`, Enum `ZoneType` & DTOs.<br>• `DONG-02`: Script `0020_organizations_events.sql` & Constraints C02-05, C10. | Model sự kiện/khu/ghế cho Liêm làm Hold/Order, Thái làm check-in/hủy. |
| **Liêm** | • `LIEM-01`: 7 Model (`TicketHold`, `TicketHoldItem`, `Order`, `OrderItem`, `Payment`, `Coupon`, `Ticket`), DTOs và Worker SPI `JobHandlerRegistry`.<br>• `LIEM-02`: Script `0030_sales.sql` & Constraints C06-09, C11-14. | Model vé/đơn hàng cho Thái làm Refund và Vương làm Settlement; Worker SPI chung. |
| **Thái** | • `THAI-01`: Model `Refund` (gồm hàm `createCompensation`), DTOs và Script `0040_refunds_checkin.sql`. | Cấu trúc Refund & schema bồi hoàn để Liêm viết thủ tục thanh toán `SP09`. |
| **Vương** | • `VUONG-01`: 2 Model `CommissionRule`, `Settlement` & Script `0050_settlements_audit.sql`.<br>• `VUONG-02`: Ghép Manifest, Script khóa ngoại liên miền `0100_cross_domain_keys.sql` & Fixture DB.<br>• `VUONG-03`: Script tạo 4 Role/Login DB (`tc_buyer`, `tc_manager`, `tc_checkin`, `tc_platform_admin`). | Chốt phân quyền SQL Server và tổng hợp chạy thử tạo database hoàn chỉnh từ 5 schema. |

🏁 **Điều kiện đóng Mốc M0:** Dự án `mvn clean compile` thành công 100%; chạy toàn bộ file SQL (`0010` $\rightarrow$ `0050` + `0100`) tạo được database hoàn chỉnh không lỗi khóa ngoại.

---

### 📌 MỐC M1: TÀI KHOẢN, DUYỆT TỔ CHỨC & TẠO SỰ KIỆN
> **Mục tiêu:** Hệ thống có thể đăng ký, đăng nhập, duyệt ban tổ chức, tạo sự kiện và thiết lập sơ đồ ghế.

| Thành viên | Tasks thực hiện trong M1 | Phối hợp liên miền |
|---|---|---|
| **Khánh** | • `KHANH-05`: Đăng ký tài khoản nguyên tử, hash BCrypt & chống duplicate username/email.<br>• `KHANH-06`: Quản lý Session, hoàn thiện `AuthService` & `ActorContext`.<br>• `KHANH-07`: Filter bảo mật CSRF & Error Handling.<br>• `KHANH-08`: Gửi OTP kích hoạt tài khoản qua Email & Outbox.<br>• `KHANH-09`: Đăng nhập, quản lý `authVersion` & thu hồi phiên (Logout).<br>• `KHANH-12`: Màn hình `UI-03` (Đăng ký, Đăng nhập, Quên mật khẩu).<br>• `KHANH-13`: Endpoint thông tin cá nhân `/me` nền tảng. | Cung cấp tài khoản người mua hợp lệ và tài khoản BTC hợp lệ cho cả nhóm test. |
| **Đông** | • `DONG-03`: Người dùng gửi yêu cầu mở BTC (`UI-09`).<br>• `DONG-04`: Viết `SP01` duyệt tổ chức, gán quyền `MANAGER` (`TX01`).<br>• `DONG-05`: Quản lý thành viên tổ chức, View `V10`, Trigger `TR06` (`UI-11`).<br>• `DONG-06 $\rightarrow$ 08`: BTC tạo sự kiện, thiết lập khu/ghế (`UI-12, UI-13`), upload ảnh bìa & job dọn ảnh.<br>• `DONG-09`: Viết `SP12` công khai sự kiện, Trigger `TR01`, `TR02`, `TR05`.<br>• `DONG-10`: Màn hình xem danh sách `UI-01`, chi tiết và chọn ghế `UI-02` (View `V01`, `V02`, Hàm `F03`).<br>• `DONG-11`: Dashboard tổ chức `UI-10` nền tảng. | Chuyển giao sự kiện đã công khai (`PUBLISHED`) và sơ đồ ghế sẵn sàng để Liêm làm chức năng giữ chỗ. |
| **Vương** | • `VUONG-04`: Quản lý chính sách hoa hồng, Trigger `TR03` (`UI-22`).<br>• `VUONG-05`: Projections tài chính, số dư và blockers nền tảng.<br>• `VUONG-12`: Adapter cho Admin duyệt tổ chức (`UI-19`) và duyệt sự kiện (`UI-20`). | Gọi `SP01` và `SP12` của Đông trên giao diện Admin; bảo vệ chính sách hoa hồng. |
| **Liêm** | Chuẩn bị sẵn logic nội bộ Coupon, cấu trúc Hold và tích hợp nút chọn ghế từ `UI-02`. | Nhận lượt chọn ghế từ màn hình `UI-02` của Đông. |
| **Thái** | `THAI-02`: Xác thực khung giờ mở cửa check-in `[startTime - 60p, endTime)` và phân quyền Staff. | Sẵn sàng nhận sự kiện từ Đông để mở cổng check-in. |

🏁 **Điều kiện đóng Mốc M1:** Khách có thể đăng ký/đăng nhập; Admin duyệt được Tổ chức; Tổ chức tạo được sự kiện và hiển thị lên trang chủ `UI-01` / `UI-02`.

---

### 📌 MỐC M2: GIỮ VÉ, THANH TOÁN VNPAY, XUẤT VÉ QR & CHECK-IN
> **Mục tiêu:** Luồng thương mại điện tử cốt lõi chạy thông suốt từ lúc chọn ghế đến lúc quét vé vào cửa.

| Thành viên | Tasks thực hiện trong M2 | Phối hợp liên miền |
|---|---|---|
| **Liêm** *(Chính)* | • `LIEM-03`: `SP07` nhả ghế khi hết hạn giữ vé (`TX07`).<br>• `LIEM-04`: `SP02` tạo `TicketHold` (tối đa 8 vé, TTL 10 phút, khóa bi quan) (`TX02`).<br>• `LIEM-05`: `SP06` tạo đơn hàng `Order` từ lượt giữ (`TX06`).<br>• `LIEM-06, 07`: Quản lý Coupon (`UI-14`), View `V09`, Hàm `F01`, Trigger `TR07` và `SP03` áp mã (`TX03`).<br>• `LIEM-08`: `SP08` bắt đầu thanh toán VNPAY (`TX08`).<br>• `LIEM-09`: Xử lý VNPAY IPN Webhook & Return URL.<br>• `LIEM-10`: `SP09` ghi nhận thanh toán, `F06` phân bổ tiền vé, đổi ghế sang `SOLD`, phát hành vé `Ticket` (`TX09`).<br>• `LIEM-11, 12`: Kích hoạt Outbox Dispatcher & Background worker quét hold hết hạn.<br>• `LIEM-13, 14`: View `V06`, Trigger `TR08`, xuất vé QR, Màn hình thanh toán (`UI-04`), kết quả (`UI-05`), đơn của tôi (`UI-06`), vé của tôi (`UI-07`). | • Nhận lượt chọn ghế từ màn hình `UI-02` của Đông.<br>• Phát hành `Ticket` có mã QR an toàn cho Thái làm Check-in.<br>• Gửi message `EMAIL` vào outbox để Khánh gửi thư báo vé thành công. |
| **Thái** | • `THAI-03`: Viết `SP04` check-in vé (chống quét trùng) (`TX04`).<br>• `THAI-04`: View `V05`, Hàm `F07` xem lịch sử check-in & Index `IX05`.<br>• `THAI-05`: Màn hình nhân viên quét camera QR `UI-15` và nhập mã vé `UI-16`. | Quét mã QR do Liêm phát hành để cho khách vào cửa. |
| **Khánh** | • `KHANH-10`: Viết `EmailJob` đọc Outbox gửi mail vé cho khách.<br>• `KHANH-13`: `MeServlet` hiển thị danh sách đơn hàng và vé của khách (`/me/orders`, `/me/tickets`).<br>• `KHANH-14`: Template email thông báo vé xuất xưởng. | Hiển thị dữ liệu đơn hàng và vé do Liêm tạo ra. |
| **Đông** | • `DONG-11`: Hoàn thiện Organization Dashboard `UI-10` điều hướng sang Coupon.<br>• `DONG-12`: Kiểm thử biến động tồn kho khi Hold/Order/Sell ghế. | Đảm bảo kho ghế không bị nhảy âm và bất biến sau khi bán. |
| **Vương** | `VUONG-05`: View `V03`, `V04`, Hàm `F02`, `F08` tính toán số dư thanh toán & Doanh thu. | Cập nhật số liệu tài chính khi có đơn hàng thành công. |

🏁 **Điều kiện đóng Mốc M2:** Người mua giữ ghế $\rightarrow$ thanh toán VNPAY Sandbox $\rightarrow$ nhận vé QR $\rightarrow$ nhân viên dùng camera quét check-in đổi vé thành `USED`.

---

### 📌 MỐC M3: HOÀN TIỀN, HỦY SỰ KIỆN, ĐỐI SOÁT TÀI CHÍNH & ĐỦ 24 UI
> **Mục tiêu:** Xử lý các quy trình hậu mãi phức tạp, hoàn tiền, hủy sự kiện, chốt sổ tài chính và hoàn thiện toàn bộ 24 màn hình.

| Thành viên | Tasks thực hiện trong M3 | Phối hợp liên miền |
|---|---|---|
| **Thái** *(Chính)* | • `THAI-06, 07`: Khách gửi yêu cầu hoàn vé (`UI-08`, `SP05`, View `V07, V08`).<br>• `THAI-08 $\rightarrow$ 11`: Admin duyệt hoàn (`SP10`, `TX10`), cổng chuyển tiền hoàn mô phỏng, `SP11` ghi nhận hoàn (`TX11`), worker đối soát hoàn tiền.<br>• `THAI-12, 13`: Admin hủy sự kiện (`SP13`, `TX13`), worker quét từng đơn tự động duyệt hoàn (`SP17`, `TX17`).<br>• `THAI-14`: Màn hình Admin duyệt hoàn `UI-21` và quản trị hủy sự kiện `UI-20`. | • Đổi trạng thái vé của Liêm thành `REFUND_PENDING` $\rightarrow$ `REFUNDED`.<br>• Gọi hàm trả kho của Đông để phục hồi ghế trống. |
| **Vương** *(Chính)* | • `VUONG-06 $\rightarrow$ 08`: `SP14` tính lại doanh thu (`TX14`), `SP15` chốt đối soát `CONFIRMED` (`TX15`), `SP16` chi trả tiền cho BTC `PAID` (`TX16`).<br>• `VUONG-09 $\rightarrow$ 11`: Báo cáo doanh thu BTC (`UI-17`), xuất CSV an toàn, Báo cáo toàn sàn và Audit Log (`UI-23`, Trigger `TR04`).<br>• `VUONG-13`: Màn hình Quyết toán & Chi trả tiền cho BTC (`UI-22`). | Đọc dữ liệu từ đơn hàng của Liêm và dữ liệu hoàn tiền của Thái để quyết toán. |
| **Liêm** | • `LIEM-15`: Xử lý thanh toán trễ sau khi hết hạn giữ vé $\rightarrow$ tự động tạo `Refund` bù trừ (`PAYMENT_COMPENSATION`).<br>• Hoàn thiện các góc cạnh của `UI-04 $\rightarrow$ UI-07` và `UI-14`. | Phối hợp với Thái để xử lý các khoản hoàn bù trừ kỹ thuật. |
| **Khánh** | • `KHANH-12`: Hoàn thiện trang thông tin cá nhân `UI-24`.<br>• `KHANH-13`: Tích hợp truy vấn yêu cầu hoàn vé vào `MeServlet`.<br>• `KHANH-14`: Gửi email thông báo kết quả hoàn vé (`REFUND_COMPLETED`, `REFUND_REJECTED`). | Hiển thị trạng thái hoàn tiền cho người mua và gửi thư thông báo. |
| **Đông** | Hoàn thiện toàn bộ các trang Dashboard tổ chức (`UI-10`), phân quyền thành viên (`UI-11`) và kiểm thử hoàn kho (`DONG-12`). | Hoàn tất đủ 24 màn hình. |

🏁 **Điều kiện đóng Mốc M3:** Đủ 24 màn hình JSP hoạt động thật; luồng hoàn vé và hủy sự kiện chạy tự động; chốt được tiền thanh toán cho ban tổ chức.

---

### 📌 MỐC M4: TỐI ƯU HIỆU NĂNG, ĐÓNG GÓI & BẢO VỆ ĐỒ ÁN
> **Mục tiêu:** Kiểm thử tải, tranh chấp đồng thời, đo benchmark Index và chuẩn bị hồ sơ nộp điểm rubric.

| Thành viên | Tasks thực hiện trong M4 | Sản phẩm bàn giao nghiệm thu |
|---|---|---|
| **Khánh** | `KHANH-15`: Đóng gói, kiểm thử bảo mật (XSS, CSRF, Session revocation) & Tái deploy WAR sạch. | Báo cáo kiểm thử bảo mật, xác nhận WAR chạy độc lập trên Tomcat 11. |
| **Đông** | `DONG-13`: Đo Benchmark 3 Index (`IX01`, `IX02`, `IX15`), kiểm tra quyền 4 Role trên miền Sự kiện & Nghiệm thu. | Execution Plans IX01, IX02, IX15, báo cáo miền Đông. |
| **Liêm** | `LIEM-16`: Kiểm thử tranh chấp Concurrency (`TX02`, `TX09`), đo Benchmark 5 Index (`IX03`, `IX04`, `IX06`, `IX09`, `IX11`) & Nghiệm thu. | Kịch bản 2 connection tranh ghế, Execution Plans 5 Index, báo cáo miền Liêm. |
| **Thái** | `THAI-15`: Kiểm thử phục hồi (Recovery khi crash mạng lúc thanh toán/hoàn), kiểm thử quyền Check-in Staff & Nghiệm thu. | Kịch bản crash recovery, Execution Plan IX05, báo cáo miền Thái. |
| **Vương** *(Chủ trì)* | • `VUONG-14`: Tổng hợp đo Benchmark toàn bộ 15 Non-Clustered Index trong hệ thống.<br>• `VUONG-15`: Chạy toàn bộ test tích hợp hệ thống (WAR, Database, Browser IT).<br>• `VUONG-16`: Đóng gói Docker chạy trên Render / Azure SQL Database.<br>• `VUONG-17`: Tổng hợp Báo cáo đồ án 6 chương (Word 50–100 trang), Slide $\le$ 15 trang & Video/Kịch bản demo. | Bộ hồ sơ đồ án hoàn chỉnh, file Word báo cáo, Slide thuyết trình và link Docker demo. |

---

## 4. CHI TIẾT LỘ TRÌNH 76 TASK CHO 5 THÀNH VIÊN

---

### 4.1. Lộ trình 15 task của KHÁNH (`KHANH-01` $\rightarrow$ `KHANH-15`)

| Task ID | Tên công việc của Khánh | Mốc | Phụ thuộc | Sản phẩm bàn giao chính |
|---|---|---|---|---|
| **KHANH-01** | Build Maven WAR, Health Check & Cấu hình môi trường | **M0** | Không | `pom.xml`, `web.xml`, `HealthServlet.java`, `.env.example` |
| **KHANH-02** | Shared HTTP Types, Request Parsers & Error Envelope | **M0** | KHANH-01 | `HttpResponses`, `PageRequest`, `BusinessException`, `ApiError` |
| **KHANH-03** | `TransactionRunner` Resource-local JPA & `PrincipalKind` | **M0** | KHANH-01, 02 | `TransactionRunner.java`, `persistence.xml`, `PersistenceFactory` |
| **KHANH-04** | Model `User`, Repository & DDL `0010_identity.sql` (C01) | **M0** | KHANH-01, 02 | `User.java`, `UserDto.java`, `0010_identity.sql` |
| **KHANH-05** | Đăng ký tài khoản nguyên tử & Hash mật khẩu BCrypt | **M1** | KHANH-04 | `AuthService.register()`, chống duplicate username/email |
| **KHANH-06** | `ActorContext`, Principal đăng nhập & Session Context | **M1** | KHANH-02, 03 | `ActorContext.java`, `AuthService.java` |
| **KHANH-07** | Filter bảo mật, CSRF Protection & Security Headers | **M1** | KHANH-02, 06 | `CorrelationIdFilter`, `ErrorHandlingFilter`, `CsrfFilter` |
| **KHANH-08** | Gửi OTP kích hoạt qua Email & Outbox | **M1** | KHANH-04, 05 | Quản lý vòng đời `OtpRecord`, HMAC secret |
| **KHANH-09** | Đăng nhập, quản lý `authVersion` & Thu hồi phiên (Logout) | **M1** | KHANH-05, 06 | `AuthServlet.java`, kiểm tra tài khoản `ACTIVE` |
| **KHANH-10** | Background Worker gửi Email (`EmailJob`) | **M2** | LIEM-01 | `EmailJob.java` đăng ký vào `JobHandlerRegistry` của Liêm |
| **KHANH-11** | Layout nền JSP/JSTL, CSS/Bootstrap & Components | **M0** | KHANH-01 | Khung layout trang web dùng chung cho 24 màn hình |
| **KHANH-12** | Màn hình `UI-03` (Đăng ký, Đăng nhập, Quên MK) & `UI-24` | **M1** | KHANH-09, 11 | `auth.jsp`, `login.jsp`, `profile.jsp` |
| **KHANH-13** | `MeServlet` (Xem thông tin cá nhân, đơn hàng, vé) | **M1 $\rightarrow$ M3** | LIEM-14 | `/me`, `/me/orders`, `/me/tickets` |
| **KHANH-14** | Email thông báo vé phát hành và kết quả hoàn tiền | **M2 $\rightarrow$ M3** | KHANH-10 | Templates `TICKETS_ISSUED`, `REFUND_COMPLETED` |
| **KHANH-15** | Kiểm thử bảo mật (Security Tests), Tái deploy & Bàn giao | **M4** | Toàn bộ | Báo cáo kiểm thử CSRF/XSS, hồ sơ rubric M4 |

---

### 4.2. Lộ trình 13 task của ĐÔNG (`DONG-01` $\rightarrow$ `DONG-13`)

| Task ID | Tên công việc của Đông | Mốc | Phụ thuộc | Sản phẩm bàn giao chính |
|---|---|---|---|---|
| **DONG-01** | Model `Organization`, `Event`, `Zone`, `Seat`, Enum `ZoneType` & DTOs | **M0** | KHANH-04 | 4 Model Java đầy đủ quan hệ, DTO catalog |
| **DONG-02** | DDL Schema `0020_organizations_events.sql` & Constraints C02-05, C10 | **M0** | DONG-01 | Bảng tổ chức, sự kiện, khu, ghế |
| **DONG-03** | Gửi hồ sơ đăng ký Ban tổ chức & Màn hình `UI-09` | **M1** | DONG-02 | `OrganizationRequestServlet`, `request.jsp` |
| **DONG-04** | Stored Procedure `SP01` duyệt BTC, gán phí ban đầu & quyền MANAGER (`TX01`) | **M1** | DONG-03, VUONG-01 | `0400_SP01.sql`, duyệt nguyên tử |
| **DONG-05** | Quản lý thành viên BTC, View `V10`, Trigger `TR06` bảo vệ Manager cuối & `UI-11` | **M1** | DONG-04 | `0300_V10.sql`, `0500_TR06.sql`, `members.jsp` |
| **DONG-06** | CRUD Sự kiện nháp (Draft) & Màn hình `UI-12` | **M1** | DONG-05 | `EventServlet`, `events.jsp` |
| **DONG-07** | Thiết lập cấu hình Ghế/Khu đứng, đổi giá không hồi tố & `UI-13` (Editor) | **M1** | DONG-06 | `ZoneServlet`, `editor.jsp` |
| **DONG-08** | Tải lên ảnh bìa sự kiện & Background Job dọn ảnh mồ côi (`ImageCleanupJob`) | **M1** | DONG-07, LIEM-01 | `ImageStorage.java`, `ImageCleanupJob.java` |
| **DONG-09** | Duyệt/Công khai sự kiện `SP12` (`TX12`), Trigger `TR01`, `TR02`, `TR05` khóa sơ đồ | **M1** | DONG-08, VUONG-01 | `0400_SP12.sql`, các trigger bảo vệ layout |
| **DONG-10** | View `V01`, `V02`, Hàm tồn kho `F03` & Màn hình trang chủ `UI-01`, chi tiết `UI-02` | **M1** | DONG-09 | `0300_V01.sql`, `0300_V02.sql`, `0350_F03.sql`, `list.jsp`, `detail.jsp` |
| **DONG-11** | Dashboard Ban tổ chức `UI-10` & Servlet adapter chuyển tiếp sang Coupon/Báo cáo | **M1 $\rightarrow$ M3** | DONG-05..10 | `dashboard.jsp`, bộ điều hướng chung |
| **DONG-12** | Kiểm thử tích hợp biến động tồn kho (Giữ - Bán - Hoàn - Hủy sự kiện) | **M2 $\rightarrow$ M3** | LIEM-10, THAI-10 | `DongInventoryContractIT.java`, kịch bản tranh chấp |
| **DONG-13** | Đo Benchmark 3 Index (`IX01`, `IX02`, `IX15`), phân quyền DB miền Đông & Hồ sơ | **M4** | Toàn bộ | Benchmark scripts, báo cáo nghiệm thu M4 |

---

### 4.3. Lộ trình 16 task của LIÊM (`LIEM-01` $\rightarrow$ `LIEM-16`)

| Task ID | Tên công việc của Liêm | Mốc | Phụ thuộc | Sản phẩm bàn giao chính |
|---|---|---|---|---|
| **LIEM-01** | Cung cấp 7 Model, DTOs, Service Interfaces & Worker SPI | **M0** | KHANH-04, DONG-01 | 7 file Model Java, DTOs, `JobHandlerRegistry` |
| **LIEM-02** | DDL Schema `0030_sales.sql` & Constraints C06-C09, C11-C14 | **M0** | LIEM-01 | Script tạo bảng bán vé, đơn hàng, thanh toán |
| **LIEM-03** | Stored Procedure `SP07` (Hủy/Nhả giữ vé) & Transaction TX07 | **M2** | LIEM-02 | `0390_SP07.sql` |
| **LIEM-04** | Stored Procedure `SP02` (Tạo giữ vé 10p, tối đa 8 vé) & TX02 | **M2** | LIEM-03, DONG-10 | `0400_SP02.sql`, Service `HoldService` |
| **LIEM-05** | Stored Procedure `SP06` (Tạo Order từ Hold) & TX06 | **M2** | LIEM-04 | `0400_SP06.sql`, Service `OrderService` |
| **LIEM-06** | View `V09`, Function `F01`, Trigger `TR07` (Mã giảm giá Coupon) & `UI-14` | **M2** | LIEM-02 | `0190_V09.sql`, `0200_F01.sql`, `0500_TR07.sql` |
| **LIEM-07** | Stored Procedure `SP03` (Áp mã Coupon vào đơn) & TX03 | **M2** | LIEM-05, LIEM-06 | `0400_SP03.sql` |
| **LIEM-08** | Stored Procedure `SP08` (Bắt đầu thanh toán Payment) & TX08 | **M2** | LIEM-05, LIEM-07 | `0400_SP08.sql`, Service `PaymentService` |
| **LIEM-09** | Tích hợp VNPAY Sandbox (URL generator & IPN Webhook verify) | **M2** | LIEM-08 | `VnpayGateway.java`, `VnpayServlet.java` |
| **LIEM-10** | Function `F06` (Phân bổ tiền vé) & `SP09` (Ghi nhận thanh toán, phát hành vé QR) | **M2** | LIEM-09, THAI-01 | `0200_F06.sql`, `0400_SP09.sql`, `QrEncoder.java` |
| **LIEM-11** | Transactional Outbox Publisher & Dispatcher | **M2** | LIEM-01, LIEM-10 | `OutboxPublisher.java`, `OutboxWorker.java` |
| **LIEM-12** | Background Jobs: Quét Hold hết hạn & Đối soát giao dịch | **M2** | LIEM-03, LIEM-11 | `HoldExpiryJob.java`, `PaymentReconciliationJob.java` |
| **LIEM-13** | View `V06`, Trigger `TR08` & Endpoint xem vé/tải ảnh QR | **M2** | LIEM-10 | `0300_V06.sql`, `0500_TR08.sql`, `TicketServlet.java` |
| **LIEM-14** | Màn hình JSP: `UI-04` (Checkout), `UI-05` (Kết quả), `UI-06` (Đơn), `UI-07` (Vé) | **M2** | LIEM-05..13 | 5 trang JSP kết nối dữ liệu thật |
| **LIEM-15** | Xử lý thanh toán trễ / bù trừ `PAYMENT_COMPENSATION` | **M3** | LIEM-10, THAI-10 | Kiểm thử ca tiền về sau khi hủy/hết hạn |
| **LIEM-16** | Đóng gói, kiểm thử Concurrency TX02/TX09 & Benchmark Index | **M4** | Toàn bộ | File Evidence, Execution Plans IX03, IX04, IX06, IX09, IX11 |

---

### 4.4. Lộ trình 15 task của THÁI (`THAI-01` $\rightarrow$ `THAI-15`)

| Task ID | Tên công việc của Thái | Mốc | Phụ thuộc | Sản phẩm bàn giao chính |
|---|---|---|---|---|
| **THAI-01** | Model `Refund`, DTOs, Schema `0040_refunds_checkin.sql` & Hàm bồi hoàn | **M0** | KHANH-04, LIEM-01 | `Refund.java`, `RefundDto`, script SQL nền M0 |
| **THAI-02** | Xác thực khung giờ check-in `[startTime - 60p, endTime)` & Quyền Staff | **M1 $\rightarrow$ M2** | DONG-09 | `CheckInService.isCheckInOpen()`, `CheckInWindowDto` |
| **THAI-03** | Stored Procedure `SP04` Check-in vé, chống quét trùng (`TX04`) & Trigger `TR08` | **M2** | LIEM-10, THAI-02 | `0400_SP04.sql`, ghi log `TicketCheckInLog` |
| **THAI-04** | View `V05`, Hàm `F07` xem lịch sử check-in & Benchmark Index `IX05` | **M2** | THAI-03 | `0300_V05.sql`, `0350_F07.sql`, `IX05` |
| **THAI-05** | Màn hình Quét Camera QR `UI-15` & Nhập mã vé thủ công `UI-16` | **M2** | THAI-03, 04 | Giao diện HTML5 Scanner/Camera cho nhân viên |
| **THAI-06** | Khách gửi yêu cầu hoàn vé, Stored Procedure `SP05` & Màn hình `UI-08` | **M3** | LIEM-10 | `0400_SP05.sql`, `refund-request.jsp` |
| **THAI-07** | View `V07`, `V08` & API truy vấn lịch sử yêu cầu hoàn tiền | **M3** | THAI-06 | `0300_V07.sql`, `0300_V08.sql` |
| **THAI-08** | Admin duyệt/từ chối hoàn vé `SP10` (`TX10`) & Hoàn tiền 0đ | **M3** | THAI-07, VUONG-01 | `0400_SP10.sql`, xử lý trạng thái `APPROVED`/`REJECTED` |
| **THAI-09** | Giả lập cổng chi tiền hoàn (Refund Gateway) & Lưu `RefundTransferLog` | **M3** | THAI-08 | Tích hợp chuyển tiền hoàn mô phỏng |
| **THAI-10** | Stored Procedure `SP11` ghi nhận kết quả chuyển tiền hoàn (`TX11`) | **M3** | THAI-09, DONG-01 | `0400_SP11.sql`, nhả lại ghế trống về kho |
| **THAI-11** | Background Worker đối soát và thử lại các khoản hoàn tiền treo | **M3** | THAI-10, LIEM-11 | `RefundReconciliationJob` đăng ký Outbox |
| **THAI-12** | Admin hủy sự kiện `SP13` (`TX13`), chuyển vé sang `INVALIDATED` | **M3** | DONG-09 | `0400_SP13.sql`, tự động tạo nghĩa vụ hoàn |
| **THAI-13** | Background Worker `SP17` tự động hoàn tiền hàng loạt cho sự kiện bị hủy | **M3** | THAI-12, LIEM-11 | `0400_SP17.sql`, `EventCancellationJob` |
| **THAI-14** | Màn hình Admin duyệt hoàn vé `UI-21` & Điều hướng quản trị | **M3** | THAI-08..12 | `refund-admin.jsp` |
| **THAI-15** | Kiểm thử khôi phục (Recovery), Bảo mật và Bàn giao nghiệm thu | **M4** | Toàn bộ | Kịch bản kiểm thử crash/rollback, bằng chứng M4 |

---

### 4.5. Lộ trình 17 task của VƯƠNG (`VUONG-01` $\rightarrow$ `VUONG-17`)

| Task ID | Tên công việc của Vương | Mốc | Phụ thuộc | Sản phẩm bàn giao chính |
|---|---|---|---|---|
| **VUONG-01** | Model `CommissionRule`, `Settlement`, DTOs & Schema `0050_settlements_audit.sql` | **M0** | DONG-01 | 2 Model tài chính, schema tài chính M0 |
| **VUONG-02** | Ghép Manifest, Script khóa ngoại liên miền `0100_cross_domain_keys.sql` & Fixture DB | **M0** | 5 Schema M0 | Dựng Database hoàn chỉnh từ 5 schema |
| **VUONG-03** | Cấu hình 4 Role Database (`tc_buyer`, `tc_manager`, `tc_checkin`, `tc_platform_admin`) | **M0** | VUONG-02 | Script phân quyền bảo mật SQL Server |
| **VUONG-04** | Quản lý chính sách hoa hồng BTC, Trigger `TR03` & Màn hình `UI-22` | **M1** | VUONG-01 | `0500_TR03.sql`, chống sửa phí đã áp dụng |
| **VUONG-05** | View `V03`, `V04`, Hàm `F02`, `F08` tính toán số dư thanh toán & Doanh thu | **M1 $\rightarrow$ M2** | VUONG-01, LIEM-10 | `0300_V03.sql`, `0300_V04.sql`, `F02`, `F08` |
| **VUONG-06** | Stored Procedure `SP14` tính toán lại quyết toán tài chính từ các đơn (`TX14`) | **M3** | LIEM-10, THAI-10 | `0400_SP14.sql`, cập nhật số tiền khấu trừ |
| **VUONG-07** | Stored Procedure `SP15` chốt sổ quyết toán tài chính (`CONFIRMED`) (`TX15`) | **M3** | VUONG-06 | `0400_SP15.sql`, đóng băng số liệu |
| **VUONG-08** | Stored Procedure `SP16` chi trả tiền cho Ban tổ chức (`PAID`) (`TX16`) | **M3** | VUONG-07 | `0400_SP16.sql`, mô phỏng payout |
| **VUONG-09** | View `V03`, `V04` tính toán doanh thu sự kiện tách biệt dòng tiền và cohort | **M3** | VUONG-06 | Tính toán KPI tài chính chính xác |
| **VUONG-10** | Dịch vụ Báo cáo & Xuất file CSV an toàn (chống CSV Injection) | **M3** | VUONG-09 | `ReportService.java`, xuất file thống kê |
| **VUONG-11** | Báo cáo toàn sàn `UI-23`, Trigger `TR04` ghi Audit Log & Màn hình Audit | **M3** | VUONG-10 | `0500_TR04.sql`, `audit.jsp` |
| **VUONG-12** | Admin Servlet adapter duyệt Tổ chức (`UI-19`), duyệt Sự kiện (`UI-20`) | **M1 $\rightarrow$ M3** | DONG-04, 09 | Tích hợp điều phối quản trị chung |
| **VUONG-13** | Màn hình Quyết toán & Chi trả tiền cho Ban tổ chức `UI-22` | **M3** | VUONG-08 | `settlement.jsp` |
| **VUONG-14** | Đo Benchmark toàn bộ 15 Non-Clustered Index trong hệ thống | **M4** | Toàn bộ Index | Đo Execution Plans, STATISTICS IO/TIME |
| **VUONG-15** | Thiết lập CI/CD, chạy toàn bộ kiểm thử tích hợp (WAR, DB, Browser) | **M4** | Toàn bộ IT | Kịch bản tự động hóa kiểm thử |
| **VUONG-16** | Đóng gói WAR, cấu hình Docker trên Render / Azure SQL | **M4** | KHANH-15 | `Dockerfile`, cấu hình triển khai thực tế |
| **VUONG-17** | Tổng hợp Hồ sơ Báo cáo đồ án 6 chương (Word), Slide & Kịch bản Demo | **M4** | Cả nhóm | Bộ hồ sơ nộp môn học DBMS |

---

## 5. MA TRẬN REVIEW CHÉO (PEER REVIEW MATRIX)

Khi một thành viên tạo Pull Request vào nhánh `develop`, người được chỉ định review sẽ vào kiểm tra theo bảng sau:

| Tác giả PR | Thành viên Reviewer chính | Trọng tâm kiểm tra |
|---|---|---|
| **Khánh** | Vương & Liêm | Session, Authentication filter, bảo mật CSRF, TransactionRunner JPA. |
| **Đông** | Liêm & Vương | Cấu trúc rạp/ghế, khóa layout sau publish, logic kho đứng/ngồi. |
| **Liêm** | Đông (kho ghế) & Thái (vé/refund) | Khóa giữ chỗ không bị trùng ghế, phân bổ tiền vé F06, phát hành vé QR. |
| **Thái** | Liêm (vé/order) & Đông (trả kho) | Hoàn tiền trả ghế đúng vị trí, check-in không bị quét trùng, hủy sự kiện. |
| **Vương** | Khánh & Liêm | Độ chính xác tính toán hoa hồng, quyền hạn 4 Role DB, hiệu năng Index. |

---

## 6. MẪU PROMPT CHUẨN GIAO CHO AI THEO TỪNG THÀNH VIÊN

Khi cần giao việc cho ChatGPT / Codex, hãy copy mẫu prompt sau và thay thế `<TÊN>` và `<TASK-ID>`:

```text
Tôi phụ trách phần của <TÊN> trong dự án TicketsCenter.
Nhiệm vụ lần này: <TASK-ID> theo đúng docs/tasks/<TÊN>.md và PROJECT-ROUTINE.md.

Trước khi viết mã:
1. Đọc kỹ spec.md, docs/classdiagram/diagram.md, docs/tasks/TEAM-CONTRACT.md và CONVENTIONS.md.
2. Kiểm tra trạng thái mã nguồn và nhánh Git hiện tại; chỉ sửa các file thuộc phạm vi sở hữu của <TÊN>.
3. Nếu phụ thuộc vào Model/Class của thành viên khác chưa có, áp dụng kỹ thuật Skeleton (khung rỗng) để mã nguồn compile được 100%, không dùng stub thành công giả.
4. Đối với Database: Chỉ tạo khóa ngoại nội bộ trong schema riêng. Tuyệt đối không tạo Foreign Key liên miền sang bảng chưa tồn tại.
5. Viết Unit Test và Script kiểm thử SQL chứng minh cả ca thành công và ca lỗi bị chặn (Boundary/Negative/Rollback).
6. Commit theo chuẩn Conventional Commits kèm Task-Id và tạo file bằng chứng nghiệm thu docs/evidence/<TÊN>/<TASK-ID>.md.
```
