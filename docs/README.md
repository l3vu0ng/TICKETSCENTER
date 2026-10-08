# Tài liệu TicketsCenter

Đồng bộ ngày 06/10/2026 theo [spec.md](../spec.md) và [diagram.md](classdiagram/diagram.md). Hai tài liệu này quy định mô hình 15 class nghiệp vụ, quyền, luồng, công thức và danh mục SQL bắt buộc. Tài liệu thiết kế và checklist chưa phải minh chứng ứng dụng đã chạy.

## Tài liệu đang sử dụng

| Tài liệu | Vai trò |
|---|---|
| [Đặc tả gốc](../spec.md) | Nguồn chuẩn về phạm vi, nghiệp vụ và nghiệm thu |
| [Sơ đồ lớp](classdiagram/diagram.md) | Nguồn chuẩn về 15 class, thuộc tính, phương thức và quan hệ |
| [Bản sao tham khảo](references/SPEC.md) | Đồng bộ từ đặc tả gốc; chỉ điều chỉnh đường dẫn tương đối |
| [Năm bản nhiệm vụ thành viên](tasks/README.md) | 76 task toàn dự án; mục tiêu, API/SQL/UI/tests, dependencies và bàn giao |
| [Hợp đồng nhóm](tasks/TEAM-CONTRACT.md) / [Quy trình Git](tasks/GIT-WORKFLOW.md) | Ownership và hợp đồng chung; main/develop/feature, nhánh task chung cho tính năng và sửa lỗi, PR/review/tag; không có nhánh release hoặc loại nhánh sửa khẩn cấp riêng |
| [Đánh giá GitFlow](tasks/GITFLOW-GUIDE-REVIEW.md) / [hướng dẫn Word gốc](references/GitFlow_Workflow_Guide.docx) | Đối chiếu tài liệu tham khảo; quy trình dự án trong GIT-WORKFLOW được ưu tiên khi thao tác Git |
| [Lịch backend cũ](tasks/BACKEND-21-DAYS.md) | 84 đầu việc/351 giờ ước lượng, giữ tham khảo và truy vết |
| [Báo cáo nội dung](baocao/BAOCAO.md) | Chương 1 đến 3 của báo cáo TicketsCenter |
| [Báo cáo Word](baocao/BAOCAO.docx) | Báo cáo TicketsCenter, gồm mô tả yêu cầu, dữ liệu và giao diện |
| [Thiết kế dữ liệu](baocao/ThietKeDuLieu.docx) | Ánh xạ 15 class nghiệp vụ và dữ liệu persistence hỗ trợ |
| [Thiết kế dữ liệu dạng văn bản](baocao/THIET-KE-DU-LIEU.md) | Bản ánh xạ có thể đối chiếu và chỉnh sửa cùng báo cáo Word |
| [Bản trình bày thiết kế giao diện](baocao/Nhom10_ThietKeGiaoDien.docx) | Phiên bản báo cáo TicketsCenter đồng bộ nội dung, giữ thông tin nhóm và định dạng riêng |
| [Phân công nhóm](../Báo%20cáo%20phân%20công%20-%20Nhóm%209.docx) | Phạm vi của năm thành viên; hạn ghi trong file là mốc kế hoạch, chưa phải nghiệm thu |
| [Prototype](web-demo/prototype/README.md) | 24 màn hình với dữ liệu mẫu; không thay ứng dụng kết nối SQL Server |
| [Use case XMI](baocao/usecase-xmi/) và [Enterprise Architect](EA/TicketsCenter.qea) | Mô hình tác nhân và chức năng; không dùng số use case để suy ra số class |

## Tài liệu mẫu và lịch sử

Các file `BAOCAO.backup-20260924-225805.docx`, `BAOCAO.before-chapter5-20260924-233441.docx` và kế hoạch có ngày trong `superpowers/plans/` là lịch sử thiết kế. Thuật ngữ của các phiên bản trước có thể khác nguồn chuẩn hiện hành; không dùng để triển khai phiên bản mới.

`DanhSachYeuCau.docx`, `MoHinhHoaYeuCau.docx` và `ThietKeGiaoDien.docx` trong `baocao/` là tài liệu mẫu của đề tài khác, giữ để tham khảo bố cục. Nội dung của các mẫu không thuộc phạm vi TicketsCenter.

## Quy tắc cập nhật

Hai bản báo cáo Word hiện là báo cáo phân tích và thiết kế theo bố cục môn Công nghệ phần mềm. Báo cáo nộp môn Hệ quản trị cơ sở dữ liệu phải theo mục 14.12 của spec: 6 chương, 50–100 trang nội dung và có minh chứng SQL/ứng dụng thực chạy. Chưa được coi các bản thiết kế hiện tại là hồ sơ nộp đã đạt yêu cầu đó. Xem [kế hoạch hoàn thiện báo cáo DBMS](baocao/KE-HOACH-BAO-CAO-DBMS.md).

Các file Word giữ format và ảnh từ bản gốc. Phần chữ/bảng đã đồng bộ theo đặc tả; ảnh cũ có thể còn mô hình hoặc giao diện của phiên bản trước. Theo yêu cầu của tác giả, vị trí ảnh còn thiếu để trống và toàn bộ phần hình sẽ được tác giả cập nhật bằng phần mềm chuyên dụng.

File phân công đang có 5 thành viên; mục 14.12 quy định nhóm 3–4 sinh viên. Giữ nguyên danh sách thực tế, cần xác nhận với giảng viên về quy mô nhóm trước khi chốt hồ sơ nộp.

Khi thay đổi nghiệp vụ, cập nhật đặc tả gốc và sơ đồ trước, rồi đồng bộ bản sao, task, báo cáo, API và prototype. Số class nghiệp vụ luôn được kiểm kê riêng với bảng kỹ thuật, DTO và kiểu giá trị. Không nhân bản đường ghi kho, tiền hoặc quota giữa Java, SP và trigger. Trạng thái nghiệm thu chỉ chuyển sang đạt khi có bằng chứng kiểm thử thực tế.
