# Persistence and transaction foundation

Use the ServletContext `transactionRunner` resource. Repositories receive the callback
EntityManager; they do not begin/commit transactions. Do not return an EntityManager
or a lazy entity graph from the callback.

```java
runner.required(principal, actor, em -> {
    // Validate current permissions and mutate using this EntityManager.
    return resultDto;
});
```

Nested calls with the same actor/principal join. Any nested failure makes the outer
transaction rollback-only, even if the caller catches the failure. Changes of actor
or principal are rejected. TransactionContext is thread-confined and always removed.

PersistenceFactory supplies a Hibernate session with the borrowed JDBC connection.
The single pool has max five connections; login, borrow and query timeouts are bounded.
The broker rejects sysadmin/db_owner before business access. EXECUTE AS uses only the
configured DB user; SESSION_CONTEXT holds actorId and actorType, including SYSTEM
with null actorId. Cleanup reverts with the cookie and clears context; failures evict.

EntityManagerFactory is lazy, RESOURCE_LOCAL, explicit entity registry, validate-only.
SQL datetime2 stores UTC Instants; identity strings use nationalized mappings.
No runtime schema creation/update occurs.

Review required with Vương: singleton user mappings, broker schema/IMPERSONATE grants,
ownership chains, pool reuse under concurrent actors, connection cleanup failure and
Azure behavior. DB validation is deferred at the user's request; unit mocks prove
control flow only. See KHANH-03 and M0-AUDIT evidence.
