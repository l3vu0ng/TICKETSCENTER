# Khánh M0 — kiểm tra và sửa nền tảng

Kiểm tra ngày 08/10/2026. **Phần độc lập đã được sửa và kiểm chứng; chưa đóng toàn bộ M0.**
Người dùng yêu cầu tạm bỏ qua Organization/membership của Đông và SQL Server test.
Các phần đó được ghi DEFERRED, không chuyển thành PASS.

## Nguồn và phạm vi

- Nhánh sửa: `feature/khanh/khanh-01-war-foundation`; SHA nền `1edbceda45063491c662d09600c7b4f2e2d779b9`.
- Bản đã pull: `origin/develop=04336c267c38c959d3bceaa59cfc08c7ee828407`.
  Develop chưa chứa nền Java/SQL của Khánh; sửa ở nhánh feature hiện có.
- PROJECT-ROUTINE đọc từ `origin/main=baa15e76ef40c9e7bf030b05d4f857419fb75d50`,
  đường dẫn `docs/tasks/PROJECT-ROUTINE.md` (chưa có trong nhánh feature).
- Đối chiếu spec, XML diagram, TEAM-CONTRACT, CONVENTIONS, API-MAP, COVERAGE,
  GIT-WORKFLOW và khanh.md trong SHA nền. Routine đặt 01–04/11 ở M0;
  khanh.md bổ sung phần nền 06–07. Luồng đăng nhập/OTP/mail và các trang M1 chưa thuộc nghiệm thu này.
- Chỉ sửa nền/shared files và miền identity do Khánh sở hữu. Giữ nguyên 0010;
  ràng buộc bổ sung ở 0011 để Vương review/đăng ký manifest. Không sửa DDL miền khác.

## Kết quả theo đầu việc

| Task | Đã sửa/kiểm chứng độc lập | Còn thiếu để nghiệm thu toàn task |
|---|---|---|
| [KHANH-01](KHANH-01.md) | WAR JDK25/Tomcat11, dependency tương thích, formatter, fail khi thiếu môi trường IT, live/ready khi DB mất, redeploy | Ready UP trên SQL Server/grants thật; review runtime của Vương |
| [KHANH-02](KHANH-02.md) | UUID/money/page/UTC/envelope/Accept/error/correlation, nullable data | Ghép DTO/caller các miền; ProfileDto cần MembershipDto của Đông |
| [KHANH-03](KHANH-03.md) | RESOURCE_LOCAL runner, nested rollback-only, pool tổng tối đa5, cleanup/lifecycle, script/test SQL đã chuẩn bị | Connection/principal/SESSION_CONTEXT/SP rollback và schema validate trên SQL thật |
| [KHANH-04](KHANH-04.md) | User profile/verify/eligibility, normalization, OTP/reset JPA records, migration bổ sung | Map<Organization,OrganizationRole> và assign/revoke đúng kiểu + mapping của Đông; C01/round-trip SQL thật |
| [KHANH-06](KHANH-06.md) | Actor USER/SYSTEM, reread ACTIVE/authVersion, guard dùng EM hiện tại, cookie/session config | Adapter MembershipAuthorizationRepository của Đông; login/logout/rotation và race ghi ở M1 |
| [KHANH-07](KHANH-07.md) | CSRF session, JSON64KiB/depth, reject forged authority, CSP/no-store, returnTo, limiter bounded | Wire ngưỡng login/OTP/reset và provider/IPN nghiệp vụ ở M1/M2 |
| [KHANH-11](KHANH-11.md) | Layout/assets/Fetch/UI states, escape/UTF-8, browser320/375/768/1440/tab/focus | Nav với membership thật/revoke; các page nghiệp vụ và browser-it chung do Vương cấu hình |

`User.organizationRoles` còn là transient Map<Object,...>, `ProfileDto.memberships` còn Page<?>.
Đây là thiếu hợp đồng đã nhận diện, **không phải bản mapping hoàn chỉnh**.
AuthorizationService chưa có adapter Đông nên từ chối quyền tổ chức; không cấp quyền giả.
Principal broker/EXECUTE AS là đề xuất chờ Vương xác minh bằng USERS/grants thật.

## Kiểm chứng thực tế

Môi trường Windows/OneDrive; JDK25.0.4.1, Maven3.10.0, Tomcat11.0.25,
Node24.19.0, Playwright1.62.1, Edge154.0.4258.62. Config test dùng SQL127.0.0.1:1
cố ý không có DB; HMAC tạo ngẫu nhiên khi chạy, không lưu giá trị.

| Lệnh/check | Expected | Actual |
|---|---|---|
| `mvn -B -Phttp-it "-Dtc.build.directory=$taskBuildPath" clean verify` | Build/WAR/format/unit/container thành công | Exit0; 59 unit + 1 container IT, fail0/error0/skip0; 64 Java files đúng formatter |
| `node --test src/test/js/shared-client.test.cjs` | CSRF/session/path/nullable/retry đúng | Exit0; 5 pass, fail0/skip0 |
| http-it + TC_BROWSER_NODE/TC_PLAYWRIGHT_MODULE/TC_BROWSER_EXECUTABLE | Trình duyệt thật không tràn ngang, escape/UTF-8/focus/state/CSP đúng | Edge headless, 4 viewport PASS; [ảnh](KHANH-11.md) |
| `mvn -B -Psqlserver-it -Dit.test=KhanhHealthIT -Dtest=ApplicationConfigTest "-Dtc.build.directory=$negativeBuildPath" clean verify`, thiếu TC_SQL_HOST | Exit khác0, không skip | Exit1; 1 IT, errors1, skipped0: Integration environment is missing TC_SQL_HOST. Đây là negative check, không là SQL integration PASS |
| SQL Server/migrations/grants/C01/SP/actor reuse | Chạy trên database _test/principal hẹp | DEFERRED theo người dùng; chưa chạy |
| `git diff --check` | Không lỗi whitespace | Exit0 |

`taskBuildPath` = Local Temp/ticketscenter-m0-20261008;
`negativeBuildPath` = Local Temp/ticketscenter-m0-negative-20261008.
Dùng output riêng vì OneDrive khóa target/classes/META-INF, không bỏ qua lỗi clean.
Lần clean verify cuối thành công lúc 11:51:23 +07:00; kiểm trực quan đủ 4 ảnh sau khi sửa UTF-8.
WAR SHA-256: `76F28D2BD048F3BC5AE89C6138C4F7A68C4D9CEF869703F837C5078D55B84140`.
Lệnh/config tái hiện ở [build.md](../../backend/build.md).
Không lưu XML reports thô vì chúng chứa environment runtime.

## Điểm cần ghép sau

Đông cung cấp Organization/UserOrganizationRole/MembershipDto/repository hiện hành.
Vương xác nhận pool/principal USERS/broker hẹp, manifest0011, fixture registry và SQL Server _test.
Sau đó sửa exact types/adapter, chạy fresh-schema validate, C01 race, SP rollback,
actor reuse và nav revocation trên bản ghép develop. Chỉ đóng M0 khi các bằng chứng đó đạt.
