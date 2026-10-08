-- Follow-up to 0010: deterministic identity keys and technical record bounds.
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.[User]', N'U') IS NULL
        THROW 51000, '0010_identity.sql must run first.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_User_Normalization')
    BEGIN
        DROP INDEX UQ_User_NormalizedEmail ON dbo.[User];
        DROP INDEX UQ_User_NormalizedUserName ON dbo.[User];
        ALTER TABLE dbo.[User] ALTER COLUMN normalizedEmail NVARCHAR(254) COLLATE Latin1_General_100_BIN2 NOT NULL;
        ALTER TABLE dbo.[User] ALTER COLUMN normalizedUserName NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL;
        CREATE UNIQUE INDEX UQ_User_NormalizedEmail ON dbo.[User](normalizedEmail);
        CREATE UNIQUE INDEX UQ_User_NormalizedUserName ON dbo.[User](normalizedUserName);

        ALTER TABLE dbo.[User] WITH CHECK ADD CONSTRAINT CK_User_Normalization CHECK (
            normalizedEmail = LOWER(LTRIM(RTRIM(email))) COLLATE Latin1_General_100_BIN2
            AND normalizedUserName = LOWER(LTRIM(RTRIM(userName))) COLLATE Latin1_General_100_BIN2
            AND DATALENGTH(normalizedEmail) = DATALENGTH(LTRIM(RTRIM(normalizedEmail)))
            AND DATALENGTH(normalizedUserName) = DATALENGTH(LTRIM(RTRIM(normalizedUserName)))
        );
        ALTER TABLE dbo.[User] WITH CHECK ADD CONSTRAINT CK_User_AuthVersion CHECK (authVersion >= 1 AND version >= 0);
        ALTER TABLE dbo.[User] WITH CHECK ADD CONSTRAINT CK_User_Profile CHECK (
            LEN(LTRIM(RTRIM(fullName))) BETWEEN 1 AND 120
            AND LEN(normalizedUserName) BETWEEN 3 AND 32
            AND normalizedUserName NOT LIKE N'%[^a-z0-9_]%' COLLATE Latin1_General_100_BIN2
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_OtpRecord_Bounds')
        ALTER TABLE dbo.OtpRecord WITH CHECK ADD CONSTRAINT CK_OtpRecord_Bounds CHECK (
            failedAttempts BETWEEN 0 AND 5
            AND expiresAt > createdAt
            AND lastSentAt >= createdAt
            AND DATALENGTH(codeHmac) > 0
            AND (consumedAt IS NULL OR consumedAt >= createdAt)
            AND (invalidatedAt IS NULL OR invalidatedAt >= createdAt)
        );
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_ResetGrant_Bounds')
        ALTER TABLE dbo.ResetGrant WITH CHECK ADD CONSTRAINT CK_ResetGrant_Bounds CHECK (
            expiresAt > createdAt AND DATALENGTH(sessionBindingHash) > 0
            AND (consumedAt IS NULL OR consumedAt >= createdAt)
        );
    COMMIT;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK;
    THROW;
END CATCH;
