# VUONG-15 — browser tooling và CI nền M0

Ngày08/10/2026. Nhánh `feature/vuong/vuong-15-browser-foundation` từ develop
`d2282d7`, consume foundation Khánh `7b403f2` bằng merge local; routine mới main
`90800f7`. Đã đối chiếu spec/diagram, TEAM-CONTRACT, task15, GIT-WORKFLOW,
CONVENTIONS/API-MAP/COVERAGE không đổi. Chỉ thêm file Vương/test tooling,
không sửa pom.xml/shared layout hoặc các workflow của thành viên khác.

Đầu ra: BrowserTestSupport, WholeProjectE2EIT **phần M0 smoke**, Playwright Node
pin1.62.1/lockfile/runner, workflow verify.yml và acceptance runbook. Foundation
Khánh là dependency thật, chưa coi merge local là owner review chấp thuận.
API/use case sản phẩm không đổi. Service login501 vẫn chưa triển khai M1.

| Kiểm thực chạy | Expected/actual | Trạng thái |
|---|---|---|
| Viết test trước helper; Maven test-compile | Thiếu BrowserTestSupport, exit1 | RED |
| Browser lần đầu | Connector lỗi JDK AF_UNIX; HTTP timeout, IT fail | FAIL được điều tra |
| Selector.open tối giản / http-it owner | Cùng IOException/Invalid argument: connect; không phải browser/API logic | Root cause ở môi trường JDK Windows |
| Probe với absent Unix-socket directory, process-only option | PipeImpl fallback TCP; SELECTOR PASS | PASS workaround local, không đổi sản phẩm/hệ thống |
| Kiểm thêm Enter skip-link | Main không nhận focus vì thiếu tabindex; chỉ giao contract Tab/outline M0, ghi owner issue | DEFERRED accessibility đầy đủ |
| npm lockfile/ci, node --check | Hai package pin; 0 audit vulnerabilities; script parse | PASS tooling |
| TC_APP_ENV=production rồi browser-it | Test guard fail trước Tomcat; không fake PASS | PASS negative guard |
| M0 browser-it trên JDK25/Maven3.9.16/Tomcat11.0.25/Edge154 | 59 unit +1 browser IT, fail/error/skipped0; WAR, formatter; bốn viewport, JSP escape/CSP/assets/keyboard/UI state/CSRF403/login501 | PASS local foundation |
| Parse verify.yml bằng YAML parser với duplicate-key check | Hai job/trigger hợp lệ | PASS syntax; không thay chứng minh GitHub run |

Lượt cuối dùng npm ci trong thư mục test và default module resolution, không
dựa vào Playwright module override. HMAC cấu hình test sinh ngẫu nhiên trong
process, không lưu/in; target chỉ có synthetic screenshot/log, không có
token/cookie. Test fixture servlet chỉ được đăng ký trong Tomcat test, không
được đóng trong WAR; process/browser/container được teardown.
Log lọc: [VUONG-15-validation.log](VUONG-15-validation.log).

**Giới hạn:** chưa đóng toàn VUONG-15 hoặc M0. Thiếu schema/cross-FK/seed và
JDBC runtime/full15Model; SQL local TCP tắt. Workflow cũ thiếu test target/env
đúng và owner phải phối hợp; browser M1–M4 nghiệp vụ chưa có. Khánh review
profile/framework/shared layout; Liêm review CI/fixture; các owner xác nhận
consumer artifact. Không nhận peer approvals từ việc tạo draft PR.
