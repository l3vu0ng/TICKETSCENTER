-- ============================================================
-- Migration: 0010_identity.sql
-- Owner: Khánh (KHANH-04)
-- Creates: [User] table, OtpRecord, ResetGrant with C01
-- DB: SQL Server (Azure SQL compatible)
-- Run: sqlcmd -S "$env:TC_SQL_HOST" -d "$env:TC_DATABASE" -E -b -i 0010_identity.sql
-- ============================================================

SET NOCOUNT ON;
SET XACT_ABORT ON;

-- ============================================================
-- [User] table
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'User')
BEGIN
    CREATE TABLE [User] (
        id                  UNIQUEIDENTIFIER    NOT NULL DEFAULT NEWID()
                                                CONSTRAINT PK_User PRIMARY KEY,
        email               NVARCHAR(254)       NOT NULL,
        normalizedEmail     NVARCHAR(254)       NOT NULL,
        userName            NVARCHAR(32)        NOT NULL,
        normalizedUserName  NVARCHAR(32)        NOT NULL,
        fullName            NVARCHAR(120)       NOT NULL,
        phone               NVARCHAR(20)        NULL,
        status              NVARCHAR(20)        NOT NULL
                                                CONSTRAINT CHK_User_status
                                                CHECK (status IN ('ACTIVE', 'DISABLED')),
        emailVerified       BIT                 NOT NULL DEFAULT 0,
        platformRole        NVARCHAR(20)        NOT NULL DEFAULT 'CUSTOMER'
                                                CONSTRAINT CHK_User_platformRole
                                                CHECK (platformRole IN ('CUSTOMER', 'ADMIN')),
        passwordHash        NVARCHAR(255)       NOT NULL,
        authVersion         INT                 NOT NULL DEFAULT 1,
        createdAt           DATETIME2(7)        NOT NULL DEFAULT SYSUTCDATETIME(),
        version             INT                 NOT NULL DEFAULT 0
    );

    -- C01: Unique normalized email and username
    CREATE UNIQUE INDEX UQ_User_NormalizedEmail
        ON [User] (normalizedEmail);

    CREATE UNIQUE INDEX UQ_User_NormalizedUserName
        ON [User] (normalizedUserName);

    -- Performance indexes
    CREATE INDEX IX_User_status
        ON [User] (status)
        INCLUDE (id, emailVerified, platformRole, authVersion);

    PRINT 'Created table [User] with C01 constraints';
END
ELSE
BEGIN
    PRINT '[User] table already exists — skipping';
END;
GO

-- ============================================================
-- [OtpRecord] table
-- One active OTP record per user per purpose (filtered unique index)
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'OtpRecord')
BEGIN
    CREATE TABLE [OtpRecord] (
        id              UNIQUEIDENTIFIER    NOT NULL DEFAULT NEWID()
                                            CONSTRAINT PK_OtpRecord PRIMARY KEY,
        userId          UNIQUEIDENTIFIER    NOT NULL
                                            CONSTRAINT FK_OtpRecord_User
                                            REFERENCES [User] (id),
        purpose         NVARCHAR(30)        NOT NULL
                                            CONSTRAINT CHK_OtpRecord_purpose
                                            CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
        codeHmac        NVARCHAR(255)       NOT NULL,   -- HMAC of the 6-digit code; plain code never stored
        createdAt       DATETIME2(7)        NOT NULL DEFAULT SYSUTCDATETIME(),
        expiresAt       DATETIME2(7)        NOT NULL,
        lastSentAt      DATETIME2(7)        NOT NULL DEFAULT SYSUTCDATETIME(),
        failedAttempts  INT                 NOT NULL DEFAULT 0,
        consumedAt      DATETIME2(7)        NULL,       -- set when OTP is successfully verified
        invalidatedAt   DATETIME2(7)        NULL        -- set when explicitly invalidated (e.g., new code issued)
    );

    -- One current (not consumed, not invalidated) OTP per user per purpose
    -- NOTE: cannot use GETUTCDATE() in filtered index — use NULL check instead
    CREATE UNIQUE INDEX UQ_OtpRecord_ActivePerUserPurpose
        ON [OtpRecord] (userId, purpose)
        WHERE consumedAt IS NULL AND invalidatedAt IS NULL;

    CREATE INDEX IX_OtpRecord_userId
        ON [OtpRecord] (userId, purpose, expiresAt)
        INCLUDE (failedAttempts, consumedAt, invalidatedAt);

    PRINT 'Created table [OtpRecord]';
END
ELSE
BEGIN
    PRINT '[OtpRecord] table already exists — skipping';
END;
GO

-- ============================================================
-- [ResetGrant] table
-- One-use password reset grant, bound to session hash
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ResetGrant')
BEGIN
    CREATE TABLE [ResetGrant] (
        id                  UNIQUEIDENTIFIER    NOT NULL DEFAULT NEWID()
                                                CONSTRAINT PK_ResetGrant PRIMARY KEY,
        userId              UNIQUEIDENTIFIER    NOT NULL
                                                CONSTRAINT FK_ResetGrant_User
                                                REFERENCES [User] (id),
        sessionBindingHash  NVARCHAR(255)       NOT NULL,  -- hash of session token; plain token never stored
        createdAt           DATETIME2(7)        NOT NULL DEFAULT SYSUTCDATETIME(),
        expiresAt           DATETIME2(7)        NOT NULL,
        consumedAt          DATETIME2(7)        NULL       -- set when reset is successfully performed
    );

    CREATE INDEX IX_ResetGrant_userId
        ON [ResetGrant] (userId, expiresAt)
        INCLUDE (consumedAt, sessionBindingHash);

    PRINT 'Created table [ResetGrant]';
END
ELSE
BEGIN
    PRINT '[ResetGrant] table already exists — skipping';
END;
GO

PRINT '0010_identity.sql completed successfully';
GO
