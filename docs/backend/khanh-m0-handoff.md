# Bàn giao nền M0 của Khánh

Phạm vi: KHANH-01–04, phần nền KHANH-06/07 và KHANH-11. Nhánh
`feature/khanh/khanh-01-war-foundation`, PR vào develop.
Đây là bàn giao artifact để nhóm triển khai tiếp; chưa đóng toàn bộ M0 hoặc các task M1.

## Thành phần dùng chung

| Đầu ra | Cách dùng |
|---|---|
| WAR/JDK25/Tomcat11 | [Build và biến môi trường](build.md); `mvn -B verify`; formatter Java 4-space AOSP |
| Actor/clock/principal | ActorContext, ClockProvider và PrincipalKind; danh tính/principal do server quyết định |
| TransactionRunner | Lấy attribute `transactionRunner` của ServletContext rồi inject vào Service |
| HTTP/DTO | HttpResponses, RequestParsers, Page/PageRequest, ApiError và BusinessException; [hợp đồng](http.md) |
| User/identity | User/UserDto/UserRepository, OTP/reset technical records; [schema](identity-data.md) |
| Layout/Fetch | ViewSupport, header/footer/notifications, apiClient/uiState; [thành phần frontend](../frontend/shared-components.md) |
| Mail SPI | MailSender/MailMessage/MailReceipt là contract; gửi email/provider thuộc KHANH-08/14 |

Repository nhận EntityManager của callback, không tự commit và không giữ nó ngoài use case.
Service trả DTO đã materialize trước khi callback kết thúc:

```java
runner.required(principal, actor, em -> {
    authorization.requireOrganizationRole(actor, organizationId, allowedRoles);
    return repository.queryDto(em, organizationId);
});
```

Ví dụ minh họa cách nối Service, không phải Service/query production đã có.
Nested required cùng actor/principal tham gia transaction hiện tại. Khác context hoặc lỗi
nested sẽ rollback. Không chờ email/provider khi transaction đang mở.

## Phần mỗi người cần nối

- **Đông:** dùng User/identity enums và layout. Cung cấp Organization,
  UserOrganizationRole, MembershipDto và MembershipAuthorizationRepository hiện hành.
  Cùng Khánh thay Object/transient role map bằng exact Organization mapping và nối guard.
- **Liêm:** dùng runner/actor/HTTP/page/clock và Fetch; SP tham gia cùng transaction/connection,
  không tự commit caller. IPN GET dùng chữ ký và response protocol riêng.
- **Thái:** dùng actor/current-role guard/runner, error envelope, layout và UI states.
  Enum fulfillment hiện có của Thái được giữ nguyên khi ghép develop.
- **Vương:** nhận 0010_identity.sql và đề xuất 0011_identity_validation.sql để review/đăng ký
  manifest; cấp fixture, DB USERS/principal hẹp, pool budget và grants. CI/browser-it do Vương nối.

## Giới hạn cần giữ khi phát triển

User role map hiện vẫn Map<Object,OrganizationRole>/transient; ProfileDto còn Page<?>.
Đây chưa là hợp đồng persistence hoàn chỉnh với Đông. Guard tổ chức hiện deny khi thiếu adapter.
Broker EXECUTE AS là đề xuất, chưa nghiệm thu SQL grants/actor isolation/SP rollback.
Auth/Me nghiệp vụ M1 chưa triển khai và trả501; CSRF/health chạy thật.
Không dùng skeleton, test DTO hoặc mock để đánh dấu task nghiệp vụ hoàn thành.

Người dùng đã yêu cầu tạm bỏ qua Đông/SQL Server test và cho phép push/merge bàn giao
phần ban đầu này vào develop. Không suy ra đã có peer approval hoặc toàn bộ M0 đạt.
PR/evidence nêu rõ phần được kiểm và phần chờ tích hợp.

## Kiểm chứng trước push

Bản ghép với develop d2282d702445911e65d174a1e853273c4164bf96:
clean verify/http-it exit0, 62 unit + 1 WAR IT, fail/error/skip0;
70 Java files đúng formatter, 5 Node tests PASS và browser320/375/768/1440 PASS.
Hoàn tất 08/10/2026 12:09:26 +07:00; output ở Local Temp/ticketscenter-m0-handoff-20261008.
WAR SHA-256: FC217BBFBDF2D35053F9D3AB3830E9D3A5E4DB20A390B8EA7F00D83F25D1B3CE.
[M0 audit/từng task](../evidence/khanh/M0-AUDIT.md) giữ chi tiết và các phần DEFERRED.

CI từ develop yêu cầu FeaturePackageStructureTest; đã bổ sung 3 kiểm tra thực về
package/Jakarta, boundary Model và uniqueness của Servlet mappings, tất cả PASS.
CI SQL còn gọi Day06IT/07IT/08IT/09IT/11IT chưa tồn tại trong bản ghép này.
SQL env/name cũng khác hợp đồng TC_TEST_DATABASE.
Không sửa CI của thành viên khác hoặc làm integration skip để báo xanh. Vương cần nối
manifest/fixture/principal/test targets thật; trạng thái GitHub CI phải đọc từ run thực tế.
