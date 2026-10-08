# Schema tổ chức và sự kiện — DONG-02

Nguồn: spec §5.3/7/8.2/14.2–3, XML diagram, TEAM-CONTRACT và dong.md tại baseline develop `d2282d7`. Model contracts DONG-01 đang ở checkpoint local `68f3110`, chưa ghép vào develop. Đây là bàn giao source, chưa phải nghiệm thu SQL/JPA.

## Phạm vi và thứ tự

Migration `database/migrations/0020_organizations_events.sql` tạo sáu bảng sở hữu Đông và chỉ các FK nội miền. Script atomic khi chạy độc lập; trong transaction caller dùng savepoint và không commit caller. Nếu transaction caller uncommittable, caller phải rollback toàn bộ. Không cascade DELETE/UPDATE.

Script là migration áp dụng một lần. Vương cần đăng ký thứ tự/checksum trong manifest trước áp dụng chung; manifest kiểm checksum và bỏ qua migration đã áp dụng. Chạy trực tiếp 0020 lần hai trả 51020, không ghi đè và không giả thành công nếu schema đã tồn tại. Nếu một bảng tồn tại mà các bảng khác thiếu, phải điều tra history; không tự drop/reset.

0020 không cần bảng User/CommissionRule để tạo các FK nội miền, nhưng tích hợp M0 cần đủ 0010–0050 rồi 0100, model/JPA/test harness. FK liên miền thiếu không đồng nghĩa dữ liệu đã được bảo vệ đầy đủ. Thứ tự seed: schema → cross keys → category registry → fixtures có Event.

## Data dictionary

Các giới hạn dưới đây là lựa chọn kỹ thuật đã chốt trong dong.md; không phải số liệu kinh doanh tự suy từ spec. UTC lưu bằng datetime2(7). PK/FK là uniqueidentifier, caller cấp UUID. Enum columns dùng Latin1_General_100_BIN2 và kiểm tên chính xác. Không có cột sinh inventory projection khác với diagram.

| Bảng | Columns và nullability |
|---|---|
| Organization | id PK; name nvarchar(200), contactEmail nvarchar(254) NOT NULL; contactPhone nvarchar(32)?, description nvarchar(2000)?, requesterId?; status nvarchar(32) default DRAFT; rejectionReason nvarchar(2000)?; createdAt NOT NULL default SYSUTCDATETIME(); submittedAt?, decidedAt? |
| UserOrganizationRole | id PK; userId, organizationId NOT NULL; role nvarchar(32), active bit NOT NULL; createdAt/updatedAt datetime2(7) NOT NULL default SYSUTCDATETIME() |
| EventCategory | id PK; code nvarchar(64) BIN2 unique, name nvarchar(200), active bit, displayOrder int NOT NULL; displayOrder >= 0 |
| Event | id PK; organizationId/categoryId NOT NULL; title nvarchar(200); description nvarchar(max) với CHECK DATALENGTH <= 40000; venueName nvarchar(200), venueAddress nvarchar(500); coverImageUrl nvarchar(2048)?, coverStorageKey nvarchar(512)?; saleStart/saleEnd/startTime/endTime NOT NULL; status nvarchar(32) default DRAFT; rejectionReason nvarchar(2000)?, commissionRuleId?; createdAt NOT NULL UTC default; submittedAt?, decidedAt? |
| Zone | id PK; eventId NOT NULL; name nvarchar(100), type nvarchar(16), price decimal(19,0) NOT NULL; standingCapacity/standingHeld/standingSold int nullable, theo C05 |
| Seat | id PK; zoneId NOT NULL; rowName nvarchar(8) BIN2, seatNumber int 1…100 NOT NULL; status nvarchar(16) default AVAILABLE |

Tên/title/venue được backend trim/NFC, contactEmail dùng helper Khánh. SQL kiểm text không rỗng cơ bản và độ dài theo storage; không tuyên bố SQL tự normalize NFC hoặc xử lý mọi Unicode whitespace. Giới hạn description 20000 UTF-16 units khớp Java String.length(). Giá whole VND được backend kiểm trước bind; decimal(19,0) có thể làm tròn tham số phân số trước CHECK, nên SQL type không chứng minh HTTP đã từ chối tiền lẻ.

Khu ngồi rows/seatsPerRow 1…100, tổng tối đa 10000, được factory/service sinh ghế; ba standing columns NULL. Khu đứng capacity 1…1000000, held/sold >= 0 và tổng <= capacity. Tổng dùng bigint trong CHECK để tránh overflow từ đầu vào int lớn. F03/V02 tạo projection ở DONG-10. Kiểm Seat thuộc SEATED/cùng Event với hold item và lịch sử tham chiếu nằm ở SP/TR/service các task tương ứng, không giả CHECK đọc bảng khác.

coverStorageKey 512 là giới hạn kỹ thuật cho DONG-08; nullable, không serialize DTO và chỉ dùng namespace/key storage do server tạo. createdAt do clock caller hoặc SQL UTC default; submittedAt/decidedAt được use case ghi trong transaction. UpdatedAt của membership được DONG-05 điều phối; model record không tự lấy clock hoặc kiểm quyền.

## Constraints và khóa

| Mã | SQL object | Bất biến |
|---|---|---|
| C02 | UQ_UserOrganizationRole_User_Organization | Unique pair userId–organizationId kể cả active=false |
| C03 | UQ_Seat_Zone_Row_Number | Nhãn hàng/số ghế unique trong cùng Zone |
| C04 | CK_Event_TimeRange | saleStart < saleEnd <= startTime < endTime, các mốc NOT NULL |
| C05 | CK_Zone_Price_Quota | Giá >=0; STANDING bắt buộc cả ba quota, tổng không vượt; SEATED cả ba NULL |
| C10 | DF_Organization_Status + CK_Organization_Requester | Default DRAFT; requester nullable chỉ DRAFT |

Khóa kỹ thuật bổ sung: PK sáu bảng, UQ_EventCategory_Code; CHECK enum/text/label/displayOrder/rejectionReason. Các FK nội miền: Event→Organization/category, Zone→Event, Seat→Zone và UserOrganizationRole→Organization. Bảo vệ layout sau publish và Manager cuối thuộc TR01/02/06 ở DONG-07/09/05, chưa nằm trong 0020.

## Đề xuất FK liên miền gửi Vương VUONG-02

Vương ghép file 0100 sau khi các bảng nền thật tồn tại. Các đề xuất chưa được áp dụng:

| Constraint name đề xuất | Nguồn → đích | Delete/update |
|---|---|---|
| FK_Organization_Requester | dbo.Organization(requesterId) → dbo.[User](id) | NO ACTION |
| FK_UserOrganizationRole_User | dbo.UserOrganizationRole(userId) → dbo.[User](id) | NO ACTION |
| FK_Event_CommissionRule | dbo.Event(commissionRuleId) → dbo.CommissionRule(id) | NO ACTION |

Một FK commissionRuleId không kiểm rule cùng organization hoặc đang hiệu lực; SP12/model/service kiểm dưới khóa. Các owner khác cung cấp FK vào Event/Zone/Seat/Organization của mình cho Vương; Đông không viết FK sales/refund/settlement vào 0020. Không thêm cascade để xóa lịch sử tài chính.

## Seed danh mục

`database/seeds/event-categories.sql` nhận **registry đã duyệt của Vương** dưới JSON array từ SESSION_CONTEXT key TC_EVENT_CATEGORY_REGISTRY trên cùng connection. Mỗi entry có id/code/name/active/displayOrder. Registry cần mã/ID duy nhất, UUID 36 ký tự, bool true/false, displayOrder >= 0 và text trong giới hạn. SESSION_CONTEXT value bị SQL Server giới hạn 8000 bytes; registry lớn phải được phối hợp đổi transport trước, không cắt payload.

Script từ chối thiếu registry, scalar/array entry sai, duplicate ID/code hoặc mapping ID/code đã tồn tại khác. Dùng UPDLOCK/HOLDLOCK và update/insert theo code, giữ UUID ổn định; reapply không tạo category mới, không delete category bị bỏ khỏi payload. UUID không được sinh bằng NEWID trong seed. Test seed reapply/rollback/conflict phải chạy qua harness với registry thật. Hiện registry chưa có nên chưa thực thi seed.

## Kiểm chứng và phần còn thiếu

`database/tests/dong/DONG-02.sql` yêu cầu test database context TC_TEST_DATABASE khớp DB_NAME() và buyer A từ fixture chung (UUID 00000000-0000-0000-0000-000000000001). ID test còn lại sinh transaction-local, không gán mã fixture production. 18 ca constraint âm, C04 equality/C05 quota3/giá0/C10 default hợp lệ, multirow statement rollback và outer/caller savepoint rollback. Test không để lại domain rows hoặc đổi User. XACT_ABORT OFF chỉ trong script test để kiểm statement atomicity, sau đó khôi phục trạng thái ban đầu.

JPA UserOrganizationRole mapping đã tạo với lazy associations, enum string và unique pair. JpaMembershipAuthorizationRepository implements interface DONG-01: query bằng EntityManager caller, bind userId/orgId, trả Optional rỗng chỉ khi không có link; link inactive vẫn trả active=false. Không mở transaction/connection hoặc kiểm User.status thay Khánh. Quyền ghi phải được recheck dưới khóa trong transaction của use case, không coi query không khóa này tự chống revoke race.

Chưa compiler/JPA runtime: dependency Jakarta/User/Organization/OrganizationRole, MembershipAccess/interface của DONG-01 và build/harness chưa ghép. Entity mapping của Organization/EventCategory/Event/Zone/Seat từ DONG-01 còn cần hoàn thiện khi các type nền có mặt. DongSchemaIT chưa tạo vì cần API test harness và mapping thật; không có test skip hoặc DTO rỗng giả thành công.

Vương cần ghép catalog/checksum/fixture/0100; Khánh cần build/User/EntityManager harness. Không sửa database/README.md/manifest hay persistence.xml chung. Kết quả parse tĩnh và blocker runtime ghi ở `docs/evidence/dong/DONG-02.md`; chưa đóng task.
