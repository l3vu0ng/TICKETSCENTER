# KHANH-04 — User và schema identity

Nguồn/nhánh/môi trường/lệnh: [M0-AUDIT](M0-AUDIT.md).
**PARTIAL: User độc lập PASS; Organization mapping/C01 SQL DEFERRED.**

Sửa createCustomer normalization/invariants, updateProfile validate cả2trường trước khi ghi,
verifyEmail idempotent và eligibility ACTIVE+verified; sửa test disabled vốn không đổi status.
Bổ sung nationalized/UUID/UTC JPA mapping OtpRecord/ResetGrantRecord; repository bind query.
Giữ0010, thêm0011 collation keys/check bounds để review manifest.

Expected/actual: UserTest14 PASS, skip0; có invalid phone không đổi fullName.
KhanhIdentityMappingIT/KHANH-04.sql đã chuẩn bị; chưa chạy round-trip/C01 race trên SQL.
assignRole/revokeRole/getOrganizationRoles còn Object/transient chờ Organization và
UserOrganizationRole Đông. Không tuyên bố đã đủ diagram/persistence.
