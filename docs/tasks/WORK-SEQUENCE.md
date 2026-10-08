# Trình tự làm việc cho thành viên và AI

Cập nhật: 08/10/2026. Áp dụng cho Khánh, Đông, Liêm, Thái, Vương và các phiên AI được giao triển khai TicketsCenter.

Tài liệu này chuyển quy định trong [GIT-WORKFLOW](GIT-WORKFLOW.md) và [TEAM-CONTRACT](TEAM-CONTRACT.md) thành các bước thao tác. Khi có khác biệt, dùng hai tài liệu đó để đối chiếu; nghiệp vụ vẫn theo [spec.md](../../spec.md) và [diagram.md](../classdiagram/diagram.md). Đây là hướng dẫn, không xác nhận task, CI, bảo vệ nhánh hoặc ứng dụng đã đạt.

## 1. Luồng chung và trách nhiệm

```text
Nhận task → đọc nguồn → kiểm dependency → tạo feature từ develop
→ triển khai từng phần → kiểm thử và evidence → commit → push feature
→ PR vào develop → review và sửa → merge → kiểm bản tích hợp
→ mọi feature đang làm cập nhật develop
→ đạt mốc nghiệm thu → PR develop vào main → kiểm main → tag khi được giao
```

| Người thực hiện | Trách nhiệm |
|---|---|
| Tác giả / AI được giao task | Đúng phạm vi, hợp đồng, kiểm thử, evidence; báo rõ phần thiếu và bàn giao đầu ra |
| Chủ dependency hoặc file chung | Cung cấp chữ ký/schema/đầu ra nền; phối hợp thay đổi ảnh hưởng người khác |
| Reviewer | Kiểm đúng revision, đầu ra và ảnh hưởng tới miền nhận; tác giả không tự approve |
| Vương | Điều phối thứ tự ghép, migrations, kiểm bản tích hợp và hồ sơ; quyền merge thực tế do nhóm giao |
| Người được giao merge/phát hành | Thực hiện PR/merge/tag theo quyền và điều kiện đã kiểm; không suy ra quyền từ tên vai trò |

Làm theo **task hoặc phần task kiểm chứng được**, không giữ một nhánh cá nhân cho cả dự án. Năm người có thể làm song song; người sử dụng đầu ra phải chờ dependency cần thiết được ghép và kiểm. Không đợi một người hoàn thành toàn bộ module mới chuyển việc cho người tiếp theo.

## 2. Thứ tự triển khai theo mốc

| Mốc | Thứ tự đầu ra cần phối hợp | Điều kiện chuyển mốc |
|---|---|---|
| M0 — nền chung | Khánh cung cấp build/helpers/types/transaction/auth interfaces/layout; mỗi chủ miền cung cấp Model/DTO/schema/contract; Liêm cung cấp worker SPI; Vương ghép manifest/fixture/quyền và FK sau bảng nền | Contract compile, WAR health, kết nối DB và chữ ký/FK được kiểm trên bản ghép; stub ghi rõ chưa triển khai |
| M1 — tài khoản và danh mục | Nền auth/quyền → đăng ký/đăng nhập/OTP; tổ chức/duyệt/phí ban đầu/manager → Event/khu/ghế/publish | Buyer verified và sự kiện bán được tạo qua API thật, JSP và quyền/membership hiện tại được kiểm |
| M2 — mua và vào cửa | Kho đã publish + buyer hợp lệ → hold → order/coupon → payment hoặc luồng 0đ → Ticket/QR → check-in | Luồng mua → QR → check-in đạt; race/rollback/replay trên SQL Server thật, bằng chứng sandbox tách khỏi mock |
| M3 — hoàn và tài chính | Contract payment/Ticket/kho → refund/cancel/worker/recovery → reconcile/settlement/payout → reports/CSV/audit và đủ giao diện | Luồng 0đ/có tiền/UNKNOWN/restart, blocker/freeze, payout cạnh tranh và quyền chéo tổ chức được kiểm |
| M4 — bàn giao | Bản ghép ổn định → fresh-clone/install/migrations/seeds/WAR → restore/benchmark/acceptance → hồ sơ/demo | Ma trận PASS/FAIL/BLOCKED gắn revision; hồ sơ và bằng chứng theo spec, online khi có cấu hình/quyền |

Chi tiết artifact M0 và dependency từng task nằm ở [TEAM-CONTRACT](TEAM-CONTRACT.md), mục 3 và 7, cùng file của [Khánh](khanh.md), [Đông](dong.md), [Liêm](liem.md), [Thái](thai.md), [Vương](vuong.md).

**Dependency chéo cần cung cấp sớm:** Refund Model/schema/DTO của Thái và CommissionRule/policy của Vương có phần nền ở M0 để Liêm triển khai SP09. Worker/hoàn tiền và đối soát hoàn thiện sau contract thanh toán/hoàn. Không biến thứ tự này thành yêu cầu làm xong toàn bộ task của Thái hoặc Vương trước Liêm.

Trong cùng mốc, phần không phụ thuộc nhau được làm song song. Mỗi PR ghi dependency bằng task/artifact, commit hoặc PR đã merge; “người kia nói đã xong” hoặc Draft PR chưa đủ để báo tích hợp đạt.

## 3. Các bước cho mỗi task

### Bước 1 — nhận việc và xác định phạm vi

Người giao việc nêu thành viên, task ID thật, phần task, đầu ra cần bàn giao, dependency, thư mục làm việc và quyền thực hiện commit/push/PR/merge. Khi yêu cầu trực tiếp của người giao việc đã cho phép một hành động, AI dùng quyền đó trong phạm vi đã giao; không hỏi lại chỉ vì mẫu hướng dẫn có bước phân quyền. Không tự suy ra quyền push/merge/deploy từ việc đọc tài liệu.

Trước task triển khai đầu tiên, đọc đầy đủ các nguồn được yêu cầu trong file thành viên: spec/diagram, TEAM-CONTRACT, CONVENTIONS, API-MAP, COVERAGE, GIT-WORKFLOW và bản nhiệm vụ của mình. Với task tiếp theo, kiểm revision và đọc lại các phần nguồn/dependency đã thay đổi; đọc tài liệu này để giữ đúng trình tự.

**Đầu ra:** xác định Create/Modify/Test, chủ file, contracts nhận/cung cấp và điều kiện đạt của phần được giao. Không bịa task ID, Issue hoặc trạng thái hoàn thành.

### Bước 2 — kiểm tra repo và tách thư mục làm việc

```powershell
git status --short --branch
git remote -v
git worktree list
git fetch origin
git branch -a
```

Xác nhận đúng repo, nhánh và revision; ghi nhận file chưa commit. Mỗi người dùng clone riêng; nhiều phiên AI trên cùng máy dùng worktree/thư mục riêng. **Một thư mục chỉ có một nhánh được checkout tại một thời điểm**: tạo nhiều nhánh trong cùng thư mục không tách file cho nhiều phiên.

Không chuyển nhánh/reset/stash toàn bộ khi phiên khác đang sửa thư mục đó. AI chỉ thao tác trong checkout được giao và chỉ stage file thuộc nhiệm vụ. Nếu đang detached HEAD, bảo toàn công việc và tạo/nhận nhánh theo cơ chế môi trường trước khi push; không đoán nhánh đích.

**Điểm kiểm tra:** nếu fetch thất bại, tiếp tục phần độc lập từ revision đã biết và ghi giới hạn; phải kiểm lại remote trước push/merge, không báo đã đồng bộ.

### Bước 3 — xác nhận dependency đã có

Đọc task và kiểm file/chữ ký/schema thực tế trong `develop`. Ghi cho từng dependency: artifact cần dùng, chủ cung cấp, task/PR/commit, đã ghép hay đang thiếu.

- Đã merge vào develop và bản ghép đạt: có thể triển khai phần sử dụng.
- Đang ở feature của người khác: có thể xem/review sau khi họ push, nhưng chưa coi là đầu ra tích hợp đã đạt.
- Thiếu: báo BLOCKED cho phần phụ thuộc, yêu cầu đầu ra nền đúng owner và tiếp tục phần độc lập.

Không tự viết lại module của người khác hoặc trả success giả để vượt dependency. Nếu cần thử phối hợp trên nhánh tạm, ghi rõ base/commit lấy từ đâu và kết quả chỉ là kiểm thử tạm; nhánh đó không thay bản đã ghép trên develop.

### Bước 4 — tạo nhánh feature từ develop mới nhất

Ví dụ cho một task cụ thể; chỉ chạy khi checkout sạch, độc lập và tên nhánh chưa tồn tại:

```powershell
git switch -c feature/khanh/khanh-01-war-foundation origin/develop
```

Các task khác lấy tên thật từ bảng trong file thành viên. Task lớn có phần review độc lập dùng suffix `-part-1`, `-part-2`; không tự đổi task ID. Nhánh đã tồn tại thì kiểm owner/base rồi tiếp tục nhánh được giao, không tạo lại hoặc ghi đè.

Không tạo task mới từ feature trước đó chỉ để giữ code chưa tích hợp. Nếu dependency chưa ghép, ưu tiên ghép phần nền trước; ngoại lệ nhánh phụ thuộc phải ghi rõ và được phối hợp với người điều phối.

**Nhánh nối tiếp đã tồn tại:** kiểm lịch sử và diff với develop; ghép phần nền trước rồi cập nhật nhánh sau. PR sau chỉ được review khi thấy rõ thay đổi còn lại và dependency đã đạt. Không reset/rewrite lịch sử đã chia sẻ để che commit kế thừa.

### Bước 5 — triển khai một phần có thể kiểm chứng

Đọc caller và hợp đồng trước khi sửa. Với logic nghiệp vụ/lỗi, nhận diện ca thất bại có ý nghĩa → triển khai → chạy lại ca và regression liên quan. Cấu hình hoặc tài liệu dùng kiểm tra phù hợp với phạm vi.

Phối hợp owner khi sửa `pom.xml`, `web.xml`, DTO, contract khóa, manifest hoặc file chung. Đăng ký migration theo quy định và thứ tự trong `database/README.md`; migration đã áp dụng dùng migration sửa tiến. Không tự đổi spec, quyền, đường ghi tiền/kho hoặc thêm class nghiệp vụ để giải quyết lỗi compile.

### Bước 6 — chạy kiểm chứng và lưu evidence

Chỉ chạy những lệnh phù hợp với task và profile đã có. Các lệnh mục tiêu của dự án:

```powershell
mvn -B verify
mvn -B -Psqlserver-it verify
mvn -B -Pbrowser-it verify
git diff --check
```

Kiểm profile có tồn tại, test nào thực sự chạy và exit code; Maven kết thúc thành công nhưng không chạy test cần thiết không chứng minh task đạt. SQL Server/browser/provider thiếu thì ghi BLOCKED đúng phần; mock chỉ chứng minh phạm vi mock. Không chạy ba lệnh như một bằng chứng mặc định cho mọi task, và không dùng kiểm tra tài liệu làm bằng chứng backend.

Evidence theo `docs/evidence/<ten>/<TASK-ID>.md`: phạm vi phần task, testedRevision (SHA đã kiểm hoặc SHA nền + diff trước commit), môi trường/fixture, lệnh, expected/actual, exit code, log đã lọc và dependency còn thiếu. Không in hoặc commit secrets, dữ liệu cá nhân, OTP/raw QR hay runtime state.

**Điểm kiểm tra:** chưa đạt điều kiện của phần task thì không chuyển sang yêu cầu merge. Có thể lưu checkpoint và Draft PR ghi rõ blocker; checkpoint không phải task hoàn thành.

### Bước 7 — commit đúng phạm vi

```powershell
git status --short
git diff
git diff --check
```

Stage từng đường dẫn thật của task, rồi kiểm:

```powershell
git diff --cached --check
git diff --cached
git status --short
```

Sau đó commit theo Conventional Commits; subject/body tiếng Anh, footer Task-Id thật, Validation và Evidence theo GIT-WORKFLOW §4. Không `git add .` ở thư mục có thay đổi người khác. Commit một phần có kiểm chứng khi phần đó đạt; không đợi xong toàn module mới commit.

### Bước 8 — cập nhật develop và push feature

Đang ở feature có base develop, checkout sạch:

```powershell
git fetch origin
git merge origin/develop
```

Nếu có conflict, đọc hai phía, phối hợp owner, stage đúng file đã giải quyết và hoàn tất merge; không chọn toàn bộ ours/theirs. Chạy lại kiểm chứng bị ảnh hưởng và cập nhật evidence sau khi đổi bản nền.

Ví dụ push nhánh task đã được giao quyền:

```powershell
git push -u origin feature/khanh/khanh-01-war-foundation
```

Đổi tên ví dụ thành nhánh thật. Nếu bị non-fast-forward, fetch và kiểm lịch sử rồi phối hợp/merge; không force-push main/develop hoặc tự rewrite nhánh đã chia sẻ.

### Bước 9 — mở PR vào develop và nhận review

Trên GitHub chọn **base: develop**, **compare: feature của task**. Kiểm danh sách commit/file trước khi mở PR; không đưa các task chưa đạt đi kèm vào develop.

PR ghi task/phần task, vấn đề/kết quả, scope, dependency đã ghép, contracts/migrations/caller, lệnh/kết quả/evidence, giới hạn và người review. Dùng Draft khi phần bàn giao chưa sẵn sàng. Người nhận đầu ra là reviewer ưu tiên; develop cần ít nhất một reviewer không phải tác giả, phần tiền/quyền/transaction/hợp đồng liên miền cần hai reviewer chuyên môn.

Sửa phản hồi trên cùng feature, kiểm lại và push commit tiếp theo. Reviewer xác nhận revision mới khi thay đổi ảnh hưởng kết luận. Không tự approve hoặc coi AI tự kiểm là đã có review của thành viên.

### Bước 10 — merge vào develop và kiểm bản ghép

Người được giao quyền merge kiểm đủ review, checks phù hợp, dependency và revision hiện tại. Dùng merge commit theo quy ước nhóm; chỉ squash khi nhóm đã thống nhất đúng trường hợp trong GIT-WORKFLOW §5.

Sau merge, lấy SHA develop mới và chạy smoke/checks liên quan trên bản ghép; ghi evidence kết quả thật. Nếu lỗi tích hợp, báo owner và sửa qua feature/PR, phối hợp revert khi cần; không commit trực tiếp để làm develop xanh.

Chỉ cập nhật phần task thành Done khi hành vi, dependency, kiểm chứng, review và evidence đều đủ. Merge một PR nền không tự đóng toàn bộ task nhiều phần. Nhánh feature chỉ dọn sau merge, hết dependency và có quyền của chủ nhánh.

### Bước 11 — mọi người nhận thay đổi mới và bắt đầu task kế tiếp

Người đang làm feature từ develop chạy bước 8 trong checkout riêng rồi kiểm lại phần bị ảnh hưởng. Người bắt đầu task mới quay lại bước 1–4 từ develop đã kiểm; không tiếp tục tạo chuỗi feature dựa trên code chưa ghép.

**Xem được không đồng nghĩa đã nhận code:** nhánh đã push có thể được mọi người có quyền repo xem trên GitHub. `git fetch` chỉ tải commit/ref; code trong thư mục hiện tại đổi khi checkout hoặc tích hợp. Nhánh chưa push và file chưa commit trên máy cá nhân chưa được chia sẻ qua GitHub. Nhánh không phải ranh giới quyền riêng tư; file chưa commit trong thư mục chung có thể ảnh hưởng nhiều phiên.

### Bước 12 — đưa bản nghiệm thu từ develop lên main

Theo từng mốc, người điều phối chốt scope và SHA develop, tạm dừng nhận tính năng mới vào ứng viên. Kiểm đúng môi trường và điều kiện mốc; M4 gồm fresh-install, migration/seed/quyền, WAR/luồng UI, restore, benchmark và hồ sơ. Lỗi ứng viên sửa qua feature/PR vào develop rồi cập nhật SHA và kiểm lại.

Mở PR **base: main**, **compare: develop**; cần hai reviewer không phải tác giả của phần cần review và evidence đúng revision. Sau merge commit, kiểm SHA main thực tế. Chỉ tạo tag/Release/triển khai/nộp hồ sơ khi được giao và mốc thực đạt; baseline tài liệu không phải bản ứng dụng đã nghiệm thu.

Nếu main có commit chưa có trong develop, mở PR **base: develop**, **compare: main**, kiểm và ghép trước đợt tiếp theo. Mọi feature đang làm tiếp tục cập nhật develop. Không để hướng dẫn/hợp đồng sửa trên main bị thiếu ở develop.

## 4. Ngoại lệ cần xử lý rõ

| Tình huống | Cách xử lý |
|---|---|
| Sửa lỗi trước khi bàn giao | Feature từ develop, commit `fix`, PR vào develop; theo luồng bình thường |
| Main đã bàn giao có lỗi, develop còn việc chưa nghiệm thu | Theo GIT-WORKFLOW §7: feature từ main → PR main → kiểm main → PR main về develop; không kéo develop chưa đạt vào bản sửa |
| Được người giao việc yêu cầu đưa riêng tài liệu lên main | Giữ thay đổi tài liệu độc lập với feature đang làm, kiểm diff/links; thực hiện đúng quyền đã giao và ghi rõ phạm vi. Nếu là ngoại lệ push trực tiếp, không biến thành quy tắc cho task khác; vẫn phải phối hợp đồng bộ main → develop |
| Thiếu quyền, branch protection hoặc checks chặn push/merge | Báo kết quả thật và chuẩn bị nhánh/PR trong quyền có sẵn; không vô hiệu hóa bảo vệ nhánh hoặc tự force-push |
| Đã commit nhưng chưa push / đã push nhưng chưa merge | Báo chính xác trạng thái, SHA và nhánh; không báo mọi người đã nhận code |
| Thiếu DB/provider/browser hoặc dependency | BLOCKED phần cần môi trường/đầu ra đó, ghi owner và bước cần tiếp; tiếp tục phần độc lập |

## 5. Mẫu giao việc cho AI

Điền thông tin thật trong các trường dưới đây trước khi giao, bỏ quyền không được cấp. Mẫu không cấp quyền sẵn và không yêu cầu AI làm toàn bộ nhiệm vụ của một thành viên.

```text
Thành viên / Task / phần task: [thông tin thật]
Checkout/worktree và nhánh được giao: [đường dẫn và tên thật]
Đầu ra cần đạt trong phiên này: [artifact/hành vi/tiêu chí]
Dependency: [task/artifact, owner, commit/PR đã ghép hoặc blocker]
Quyền được giao: [sửa file / chạy checks / commit / push feature / mở PR /
                 merge PR cụ thể / xuất bản cụ thể]

Đọc nguồn bắt buộc trong file thành viên và docs/tasks/WORK-SEQUENCE.md.
Kiểm repo/nhánh/revision/ownership, giữ mọi thay đổi có sẵn của người khác.
Triển khai theo bước 1–11; mặc định feature từ develop, PR vào develop.
Task lớn bàn giao từng phần có kiểm chứng; thiếu dependency hoặc môi trường
thì ghi BLOCKED đúng phần và tiếp tục phần độc lập. Không báo mock/stub là
tích hợp thật. Không đổi spec hoặc viết lại phần của owner khác để vượt lỗi.
Lưu evidence, chỉ stage file thuộc nhiệm vụ, dùng Task-Id thật.
Thực hiện quyền đã được giao; không hỏi lại quyền rõ ràng trong phiên,
không suy ra quyền merge/main/tag/deploy từ việc đọc mẫu này.
Kết thúc bằng báo cáo theo mẫu mục 6, nêu bước tiếp theo và người nhận.
```

## 6. Mẫu báo cáo bàn giao

```text
Thành viên / Task / phần task:
Mốc và trạng thái: In progress / Draft / Ready for review / Done / BLOCKED
Nhánh / base / commit SHA:
Đầu ra đã làm; file/API/SQL/UI và caller bị ảnh hưởng:
Dependency đã ghép; dependency còn thiếu và owner:
Kiểm chứng: testedRevision, môi trường, lệnh, expected/actual, exit code
Evidence:
Push: chưa push hoặc nhánh/SHA trên remote đã xác nhận
PR: chưa mở hoặc URL, base/compare, review/checks hiện tại
Merge: chưa merge hoặc SHA develop/main đã xác nhận; kết quả kiểm bản ghép
Giới hạn và phần chưa hoàn thành:
Bước tiếp theo / người nhận đầu ra:
```

Người nhận bàn giao dựa vào artifact, revision và bằng chứng thực tế. Số commit, nhánh có tên đúng, push thành công hoặc checklist trong tài liệu không tự chứng minh phần mềm đã đạt.
