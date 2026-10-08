-- Owner: Đông / DONG-02. Cross-domain keys are proposed in docs/backend/dong-schema.md.
-- Apply once via Vương's checksum manifest. Existing objects must never be silently overwritten.
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.Organization', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.UserOrganizationRole', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.EventCategory', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.Event', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.Zone', N'U') IS NOT NULL
   OR OBJECT_ID(N'dbo.Seat', N'U') IS NOT NULL
    THROW 51020, '0020 schema objects already exist; use the migration manifest and verify its checksum.', 1;

DECLARE @ownsTransaction bit = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
IF @ownsTransaction = 1 BEGIN TRANSACTION;
ELSE SAVE TRANSACTION DongSchema0020;

BEGIN TRY
    CREATE TABLE dbo.Organization (
        id uniqueidentifier NOT NULL CONSTRAINT PK_Organization PRIMARY KEY,
        name nvarchar(200) NOT NULL,
        contactEmail nvarchar(254) NOT NULL,
        contactPhone nvarchar(32) NULL,
        description nvarchar(2000) NULL,
        requesterId uniqueidentifier NULL,
        status nvarchar(32) COLLATE Latin1_General_100_BIN2 NOT NULL CONSTRAINT DF_Organization_Status DEFAULT N'DRAFT',
        rejectionReason nvarchar(2000) NULL,
        createdAt datetime2(7) NOT NULL CONSTRAINT DF_Organization_CreatedAt DEFAULT SYSUTCDATETIME(),
        submittedAt datetime2(7) NULL,
        decidedAt datetime2(7) NULL,
        CONSTRAINT CK_Organization_Status CHECK (status IN (N'DRAFT', N'PENDING_APPROVAL', N'APPROVED', N'REJECTED')),
        CONSTRAINT CK_Organization_Requester CHECK (status = N'DRAFT' OR requesterId IS NOT NULL),
        CONSTRAINT CK_Organization_Text CHECK (
            LEN(LTRIM(RTRIM(name))) > 0 AND LEN(LTRIM(RTRIM(contactEmail))) > 0
            AND (contactPhone IS NULL OR LEN(LTRIM(RTRIM(contactPhone))) > 0)),
        CONSTRAINT CK_Organization_RejectionReason CHECK (
            status <> N'REJECTED' OR (rejectionReason IS NOT NULL AND LEN(LTRIM(RTRIM(rejectionReason))) > 0))
    );

    CREATE TABLE dbo.EventCategory (
        id uniqueidentifier NOT NULL CONSTRAINT PK_EventCategory PRIMARY KEY,
        code nvarchar(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
        name nvarchar(200) NOT NULL,
        active bit NOT NULL,
        displayOrder int NOT NULL,
        CONSTRAINT UQ_EventCategory_Code UNIQUE (code),
        CONSTRAINT CK_EventCategory_Text CHECK (LEN(LTRIM(RTRIM(code))) > 0 AND LEN(LTRIM(RTRIM(name))) > 0),
        CONSTRAINT CK_EventCategory_DisplayOrder CHECK (displayOrder >= 0)
    );

    CREATE TABLE dbo.UserOrganizationRole (
        id uniqueidentifier NOT NULL CONSTRAINT PK_UserOrganizationRole PRIMARY KEY,
        userId uniqueidentifier NOT NULL,
        organizationId uniqueidentifier NOT NULL,
        role nvarchar(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
        active bit NOT NULL,
        createdAt datetime2(7) NOT NULL CONSTRAINT DF_UserOrganizationRole_CreatedAt DEFAULT SYSUTCDATETIME(),
        updatedAt datetime2(7) NOT NULL CONSTRAINT DF_UserOrganizationRole_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_UserOrganizationRole_User_Organization UNIQUE (userId, organizationId),
        CONSTRAINT CK_UserOrganizationRole_Role CHECK (role IN (N'MANAGER', N'CHECK_IN_STAFF')),
        CONSTRAINT FK_UserOrganizationRole_Organization FOREIGN KEY (organizationId) REFERENCES dbo.Organization(id)
            ON DELETE NO ACTION ON UPDATE NO ACTION
    );

    CREATE TABLE dbo.Event (
        id uniqueidentifier NOT NULL CONSTRAINT PK_Event PRIMARY KEY,
        organizationId uniqueidentifier NOT NULL,
        title nvarchar(200) NOT NULL,
        description nvarchar(max) NOT NULL,
        categoryId uniqueidentifier NOT NULL,
        venueName nvarchar(200) NOT NULL,
        venueAddress nvarchar(500) NOT NULL,
        coverImageUrl nvarchar(2048) NULL,
        coverStorageKey nvarchar(512) NULL,
        saleStart datetime2(7) NOT NULL,
        saleEnd datetime2(7) NOT NULL,
        startTime datetime2(7) NOT NULL,
        endTime datetime2(7) NOT NULL,
        status nvarchar(32) COLLATE Latin1_General_100_BIN2 NOT NULL CONSTRAINT DF_Event_Status DEFAULT N'DRAFT',
        rejectionReason nvarchar(2000) NULL,
        commissionRuleId uniqueidentifier NULL,
        createdAt datetime2(7) NOT NULL CONSTRAINT DF_Event_CreatedAt DEFAULT SYSUTCDATETIME(),
        submittedAt datetime2(7) NULL,
        decidedAt datetime2(7) NULL,
        CONSTRAINT CK_Event_Status CHECK (status IN (N'DRAFT', N'PENDING_APPROVAL', N'REJECTED', N'PUBLISHED', N'CANCELLED')),
        CONSTRAINT CK_Event_TimeRange CHECK (saleStart < saleEnd AND saleEnd <= startTime AND startTime < endTime),
        CONSTRAINT CK_Event_Text CHECK (
            LEN(LTRIM(RTRIM(title))) > 0 AND LEN(LTRIM(RTRIM(description))) > 0 AND DATALENGTH(description) <= 40000
            AND LEN(LTRIM(RTRIM(venueName))) > 0 AND LEN(LTRIM(RTRIM(venueAddress))) > 0),
        CONSTRAINT CK_Event_RejectionReason CHECK (
            status <> N'REJECTED' OR (rejectionReason IS NOT NULL AND LEN(LTRIM(RTRIM(rejectionReason))) > 0)),
        CONSTRAINT FK_Event_Organization FOREIGN KEY (organizationId) REFERENCES dbo.Organization(id)
            ON DELETE NO ACTION ON UPDATE NO ACTION,
        CONSTRAINT FK_Event_Category FOREIGN KEY (categoryId) REFERENCES dbo.EventCategory(id)
            ON DELETE NO ACTION ON UPDATE NO ACTION
    );

    CREATE TABLE dbo.Zone (
        id uniqueidentifier NOT NULL CONSTRAINT PK_Zone PRIMARY KEY,
        eventId uniqueidentifier NOT NULL,
        name nvarchar(100) NOT NULL,
        type nvarchar(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
        price decimal(19,0) NOT NULL,
        standingCapacity int NULL,
        standingHeld int NULL,
        standingSold int NULL,
        CONSTRAINT CK_Zone_Type CHECK (type IN (N'SEATED', N'STANDING')),
        CONSTRAINT CK_Zone_Name CHECK (LEN(LTRIM(RTRIM(name))) > 0),
        CONSTRAINT CK_Zone_Price_Quota CHECK (
            price >= 0 AND (
                (type = N'SEATED' AND standingCapacity IS NULL AND standingHeld IS NULL AND standingSold IS NULL)
                OR (type = N'STANDING'
                    AND standingCapacity IS NOT NULL AND standingCapacity BETWEEN 1 AND 1000000
                    AND standingHeld IS NOT NULL AND standingHeld >= 0
                    AND standingSold IS NOT NULL AND standingSold >= 0
                    AND CONVERT(bigint, standingHeld) + CONVERT(bigint, standingSold) <= standingCapacity))),
        CONSTRAINT FK_Zone_Event FOREIGN KEY (eventId) REFERENCES dbo.Event(id)
            ON DELETE NO ACTION ON UPDATE NO ACTION
    );

    CREATE TABLE dbo.Seat (
        id uniqueidentifier NOT NULL CONSTRAINT PK_Seat PRIMARY KEY,
        zoneId uniqueidentifier NOT NULL,
        rowName nvarchar(8) COLLATE Latin1_General_100_BIN2 NOT NULL,
        seatNumber int NOT NULL,
        status nvarchar(16) COLLATE Latin1_General_100_BIN2 NOT NULL CONSTRAINT DF_Seat_Status DEFAULT N'AVAILABLE',
        CONSTRAINT UQ_Seat_Zone_Row_Number UNIQUE (zoneId, rowName, seatNumber),
        CONSTRAINT CK_Seat_Label CHECK (LEN(LTRIM(RTRIM(rowName))) > 0 AND seatNumber BETWEEN 1 AND 100),
        CONSTRAINT CK_Seat_Status CHECK (status IN (N'AVAILABLE', N'HELD', N'SOLD')),
        CONSTRAINT FK_Seat_Zone FOREIGN KEY (zoneId) REFERENCES dbo.Zone(id)
            ON DELETE NO ACTION ON UPDATE NO ACTION
    );

    IF @ownsTransaction = 1 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @ownsTransaction = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    ELSE IF @ownsTransaction = 0 AND XACT_STATE() = 1 ROLLBACK TRANSACTION DongSchema0020;
    -- An uncommittable caller transaction must be rolled back by that caller.
    THROW;
END CATCH;
