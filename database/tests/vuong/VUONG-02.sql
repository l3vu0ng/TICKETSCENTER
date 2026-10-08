SET NOCOUNT ON;
IF DB_NAME() NOT LIKE N'TicketsCenter[_]Test[_]%'
    THROW 51200, N'VUONG-02 requires a dedicated test database.', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL
    THROW 51201, N'Migration history is missing.', 1;
IF (SELECT COUNT(*) FROM dbo.SchemaMigrationHistory WHERE filename = '0050_settlements_audit.sql') <> 1
    THROW 51202, N'Expected one applied financial migration.', 1;
IF (SELECT COUNT(*) FROM sys.tables WHERE name IN
    ('CommissionRule','Settlement','SettlementOrderSnapshot','SettlementTransferLog','AuditLog','MockPayoutProviderLedger')) <> 6
    THROW 51203, N'Expected all six financial tables.', 1;
IF EXISTS (SELECT 1 FROM dbo.CommissionRule) OR EXISTS (SELECT 1 FROM dbo.Settlement)
    THROW 51204, N'Foundation tests should leave no business fixtures.', 1;
PRINT 'VUONG-02 PASS: one migration history entry, six financial tables, no business fixtures.';
