-- Repair indexes when 0010 was run with sqlcmd's default QUOTED_IDENTIFIER OFF.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.OtpRecord', N'U') IS NULL
        THROW 51000, '0010_identity.sql must run first.', 1;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.OtpRecord')
          AND name = N'UQ_OtpRecord_ActivePerUserPurpose'
    )
        CREATE UNIQUE INDEX UQ_OtpRecord_ActivePerUserPurpose
            ON dbo.OtpRecord(userId, purpose)
            WHERE consumedAt IS NULL AND invalidatedAt IS NULL;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.OtpRecord')
          AND name = N'IX_OtpRecord_userId'
    )
        CREATE INDEX IX_OtpRecord_userId
            ON dbo.OtpRecord(userId, purpose, expiresAt)
            INCLUDE(failedAttempts, consumedAt, invalidatedAt);

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.OtpRecord')
          AND name = N'UQ_OtpRecord_ActivePerUserPurpose'
          AND is_unique = 1 AND has_filter = 1 AND is_disabled = 0
    )
        THROW 51001, 'Current OTP uniqueness index is missing or disabled.', 1;

    COMMIT;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK;
    THROW;
END CATCH;
