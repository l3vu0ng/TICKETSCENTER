SET NOCOUNT ON;
IF DB_NAME() NOT LIKE N'TicketsCenter[_]Test[_]%'
    THROW 51210, N'Identity migration verification requires a dedicated test database.', 1;
IF (SELECT COUNT(*) FROM dbo.SchemaMigrationHistory WHERE filename IN
    ('0010_identity.sql','0011_identity_validation.sql','0050_settlements_audit.sql')) <> 3
    THROW 51211, N'Expected identity validation and financial history exactly once.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE filename='0010_identity.sql'
    AND owner=N'KHANH' AND sha256='7d88c32652756f43e355e60d318c264ccd7802dd40956ff9b564ab77ef6ddf7d')
    OR NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE filename='0011_identity_validation.sql'
    AND owner=N'KHANH' AND sha256='81f4a5c82d50509dd4284ff88d408ab612ec8a0789d9a579dac1c6a37d76461d')
    THROW 51212, N'Identity history differs from the received owner artifacts.', 1;
IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')
    AND name IN ('User','OtpRecord','ResetGrant')) <> 3
    THROW 51213, N'Identity tables were not installed.', 1;
IF (SELECT COUNT(*) FROM sys.check_constraints WHERE name IN
    ('CK_User_Normalization','CK_User_AuthVersion','CK_User_Profile','CK_OtpRecord_Bounds','CK_ResetGrant_Bounds')
    AND is_disabled=0 AND is_not_trusted=0) <> 5
    THROW 51214, N'Identity follow-up constraints are missing, disabled or untrusted.', 1;
IF (SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.[User]')
    AND name IN ('normalizedEmail','normalizedUserName') AND collation_name=N'Latin1_General_100_BIN2') <> 2
    THROW 51215, N'Identity normalization collation was not installed.', 1;
IF EXISTS (SELECT 1 FROM dbo.[User]) OR EXISTS (SELECT 1 FROM dbo.OtpRecord)
    OR EXISTS (SELECT 1 FROM dbo.ResetGrant)
    THROW 51216, N'Migration verification must leave identity business fixtures empty.', 1;
PRINT 'VUONG-02 IDENTITY PASS: owner history, schema, trusted constraints and empty fixtures.';
