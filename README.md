# TicketsCenter

TicketsCenter là hệ thống quản lý sự kiện và bán vé trực tuyến, được thiết kế cho đồ án Hệ quản trị cơ sở dữ liệu. Dự án bao gồm quản lý tổ chức, sự kiện, khu vực và ghế ngồi; giữ vé, đặt hàng, thanh toán thử nghiệm, phát hành vé QR, check-in, hoàn vé và đối soát.

Thiết kế chi tiết nằm trong [SPEC.md](spec.md). Phân công cho năm thành viên tại [docs/tasks](docs/tasks/README.md), dùng chung [hợp đồng triển khai](docs/tasks/TEAM-CONTRACT.md) và [quy trình Git](docs/tasks/GIT-WORKFLOW.md). Nhóm dùng feature theo task cho cả tính năng và sửa lỗi, mặc định từ develop; ghép/kiểm trên develop rồi PR bản nghiệm thu vào main và tạo tag. Không dùng nhánh release hoặc loại nhánh sửa khẩn cấp riêng. Ngoại lệ sửa bản main đã bàn giao khi develop còn việc chưa nghiệm thu theo mục 7 của quy trình Git.

## Công nghệ dự kiến

| Thành phần | Công nghệ |
|---|---|
| Ứng dụng web | Java 25, Servlet/JSP/JSTL, Tomcat 11 |
| Giao diện | HTML, Bootstrap, CSS, JavaScript |
| Dữ liệu | SQL Server, JPA/Hibernate |
| Build | Maven, đóng gói WAR |
| Thanh toán | VNPAY Sandbox |
| Triển khai online | Docker trên Render, Azure SQL Database |

Thanh toán chỉ chạy trong môi trường thử nghiệm; hoàn tiền và chi trả được mô phỏng.

## Trạng thái dự án

Repository hiện có đặc tả, sơ đồ 15 class nghiệp vụ, bộ khung Maven, năm bản phân công toàn dự án và prototype 24 màn hình HTML dùng dữ liệu mẫu. Chưa có mã Java/JSP hoặc script SQL triển khai ứng dụng; `pom.xml` chưa có dependencies và cấu hình đóng gói WAR, chưa có Dockerfile. File `application.properties` hiện có chưa chứng minh kết nối được sử dụng bởi ứng dụng. Vì vậy các bước dưới đây là **quy trình cài đặt và chạy theo thiết kế**, chưa thể thực hiện đầy đủ trên phiên bản hiện tại.

## Yêu cầu môi trường local

- JDK 25 và Maven.
- Apache Tomcat 11.
- Microsoft SQL Server; có thể dùng SSMS để quản lý database.
- Tài khoản VNPAY Sandbox nếu cần thử luồng thanh toán.

## Cấu hình

Khi phần ứng dụng được triển khai, cấu hình sẽ lấy từ biến môi trường. Các tên dưới đây được đề xuất trong [SPEC.md](spec.md#113-nhóm-biến-môi-trường-dự-kiến); mã đọc cấu hình và file `.env.example` chưa được tạo.

| Nhóm | Biến dự kiến | Mục đích |
|---|---|---|
| Ứng dụng | `APP_ENV`, `APP_BASE_URL`, `PORT` | Môi trường, địa chỉ truy cập và cổng chạy |
| Database | `DB_URL`, `DB_USER`, `DB_PASSWORD` | Kết nối SQL Server |
| Thanh toán | `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` | Tích hợp VNPAY Sandbox |
| OTP | `OTP_HMAC_SECRET` | Bảo vệ mã xác minh |
| Quản trị | `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Khởi tạo tài khoản quản trị demo |

Thông tin SMTP/email, lưu ảnh, URL VNPAY Sandbox và giới hạn kết nối database sẽ được chốt khi triển khai. Không đưa mật khẩu hoặc khóa bí mật vào repository.

## Cài đặt và chạy theo thiết kế

1. Cài các thành phần trong mục **Yêu cầu môi trường local**.
2. Tạo database SQL Server và chạy các script trong `database/migrations`, sau đó nạp dữ liệu mẫu từ `database/seeds` theo thứ tự được công bố. Các thư mục và script này chưa có trong repository.
3. Thiết lập biến môi trường cho database và các tích hợp cần dùng. Tài khoản chạy ứng dụng chỉ được cấp quyền cần thiết; tài khoản chạy migration được cấu hình riêng.
4. Khi `pom.xml` đã có dependencies và cấu hình WAR, build ứng dụng từ thư mục gốc:

   ```bash
   mvn clean package
   ```

5. Chép file WAR trong `target/` vào thư mục `webapps/` của Tomcat 11 rồi khởi động Tomcat. Địa chỉ truy cập sẽ phụ thuộc vào tên file WAR hoặc context path được cấu hình.

Để chạy bản online, thiết kế dự kiến dùng WAR trong Docker trên Render và Azure SQL Database. Cần hoàn thiện Dockerfile, cấu hình môi trường và kiểm thử database trước khi triển khai.

## Tài liệu

- [Đặc tả thiết kế](spec.md)
- [Sơ đồ 15 class nghiệp vụ](docs/classdiagram/diagram.md)
- [Danh mục tài liệu và nguồn chuẩn](docs/README.md)
- [Kế hoạch backend](docs/tasks/README.md)
- [Prototype giao diện](docs/web-demo/prototype/README.md)

`spec.md` và `docs/classdiagram/diagram.md` là nguồn chuẩn hiện hành. `docs/references/SPEC.md` là bản sao tham khảo được đồng bộ từ đặc tả gốc, không phải một đặc tả độc lập. Refund trực tiếp quản lý nghĩa vụ và lần xử lý hiện hành; Settlement trực tiếp quản lý số liệu và số dư chi trả. Các bản ghi quyền, nhật ký, snapshot, OTP, redemption và outbox thuộc persistence, không làm tăng số class nghiệp vụ.
