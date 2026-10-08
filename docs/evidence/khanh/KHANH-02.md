# KHANH-02 — HTTP và kiểu chung

Nguồn/nhánh/môi trường/lệnh: [M0-AUDIT](M0-AUDIT.md).
**PASS helpers độc lập; caller/DTO liên miền cần review khi ghép.**

Sửa money canonical0..19digits/không double, UUID đủ36ký tự, page1..size100/overflow,
BigDecimal chuỗi chính xác, Instant UTC, Page copy bất biến và data:null.
Accept quyết định HTML/JSON; lỗi generic, correlation bounded và không ghi SQL secret.

Expected/actual: HttpContractTest17 và ErrorHandlingTest1 PASS, skip0 trong verify.
WAR/JSP thực chứng minh escaped text, context path, UTF-8 và correlation404.
SQL profile HTTP tests đã chuẩn bị; chưa chạy trên bản ghép SQL.
ProfileDto còn Page<?> chờ MembershipDto Đông, chưa đạt exact type.
