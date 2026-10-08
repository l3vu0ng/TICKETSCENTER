# Transaction and stored procedure handoff

All work in a runner callback uses one physical JDBC connection. Avoid slow email,
provider requests, polling and sleep while a database transaction is active.

Stored procedures must distinguish their own transaction from an existing caller
transaction. A standalone call can commit its own changes; a participating call must
not commit the caller. Use a savepoint where XACT_STATE permits recovery and propagate
errors with THROW. A doomed caller transaction must be rolled back by its owner.

After native/SP mutations, flush before the SP if relevant, then clear/refresh JPA state
before reading the same objects. Do not duplicate an SP mutation with a dirty entity write.
Current permission/membership checks must run under the use case's transaction/locks.

`database/tests/khanh/KHANH-03.sql` supplies a test-only participating SP. The Java
integration test checks an injected failure after its mutation, transaction count and
restoration of original data, plus two independent actor connections. It needs the real
fixture/principal registry; those checks are not accepted from mock results.
