# KHANH-01 — WAR và tooling

Nguồn/nhánh/môi trường/lệnh: [M0-AUDIT](M0-AUDIT.md).
**PASS phần độc lập; SQL ready UP và review runtime DEFERRED.**

Sửa Hibernate6.6.40/Persistence3.1 cho JDK25, Mockito agent tương thích, SLF4J binding,
WAR/provided APIs/JSTL/Bootstrap local, formatter trong verify và failIfNoTests.
Cấu hình env validate không in secret; SameSite=Lax đặt qua Servlet6.1 API.
Pool khởi động lazy để live không phụ thuộc SQL; cleanup driver/pool khi undeploy.

Expected/actual: clean verify exit0, 59 unit/1 container IT, fail/error/skip0.
WAR triển khai rồi gỡ 2 lần trên Tomcat11.0.25: live200 UP, ready503 DOWN,
live sau probe vẫn200, không Hikari thread sống sau undeploy; WAR không env/test/container APIs.
Negative SQL profile exit1 rõ thiếu TC_SQL_HOST, không skipped.

Chưa chứng minh ready200, SQL JDBC integrated authentication và schema validate/grants thật.
