# KHANH-03 — JPA/transaction/principal

Nguồn/nhánh/môi trường/lệnh: [M0-AUDIT](M0-AUDIT.md).
**PASS unit/lifecycle; SQL integration DEFERRED.**

Bổ sung PersistenceFactory/Listener, TransactionRunner/Context/ActorSessionContext,
RESOURCE_LOCAL validate-only, pool chung tối đa5 và scope cleanup/discard.
Nested cùng actor/principal dùng cùng EM; lỗi kể cả caller bắt lại vẫn rollback-only.
Broker không sysadmin/db_owner, switch USER rồi đặt SESSION_CONTEXT trên cùng connection;
revert/clear khi trả. Mapping USERS/grants cần Vương review.

Expected/actual: TransactionRunnerTest6 PASS (nested/rollback/flush/context/principal),
redeploy2lần không thread leak và DB outage probe hữu hạn trên Tomcat.
KhanhTransactionIT và KHANH-03.sql có barrier2connections/test-only SP/rollback sau UPDATE,
nhưng chưa thực thi. Không suy ra SQL rollback, quyền hoặc actor isolation từ mock.
