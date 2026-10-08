-- Owner: Đông / DONG-02. Run only on a configured test database, after 0010/0020/0100 and fixture setup.
-- The test runner must set SESSION_CONTEXT('TC_TEST_DATABASE') to the configured database name.
-- Shared buyer A must come from Vương's fixture registry. Other IDs below are transaction-local test rows.
SET NOCOUNT ON;
DECLARE @expectedDatabase sysname = TRY_CONVERT(nvarchar(128), SESSION_CONTEXT(N'TC_TEST_DATABASE'));
IF @expectedDatabase IS NULL OR @expectedDatabase <> DB_NAME()
    THROW 51022, 'DONG-02 requires an explicit test database context matching this connection.', 1;
IF OBJECT_ID(N'dbo.[User]', N'U') IS NULL OR OBJECT_ID(N'dbo.Seat', N'U') IS NULL
    THROW 51022, 'DONG-02 requires the identity and event migrations.', 1;
DECLARE @buyerId uniqueidentifier = '00000000-0000-0000-0000-000000000001';
IF NOT EXISTS (SELECT 1 FROM dbo.[User] WHERE id = @buyerId)
    THROW 51022, 'Shared buyer A is missing from the fixture registry seed.', 1;

DECLARE @organizationId uniqueidentifier = NEWID(), @categoryId uniqueidentifier = NEWID(),
        @eventId uniqueidentifier = NEWID(), @standingId uniqueidentifier = NEWID(),
        @otherStandingId uniqueidentifier = NEWID(), @seatedId uniqueidentifier = NEWID();
DECLARE @initialTranCount int = @@TRANCOUNT, @originalXactAbort bit = CASE WHEN @@OPTIONS & 16384 = 16384 THEN 1 ELSE 0 END;
-- Constraint errors must roll back their statement while the test transaction remains readable.
SET XACT_ABORT OFF;
IF @initialTranCount = 0 BEGIN TRANSACTION;
ELSE SAVE TRANSACTION Dong02Tests;

BEGIN TRY
    INSERT dbo.Organization (id, name, contactEmail) VALUES (@organizationId, N'DONG-02 test', N'dong02@example.test');
    IF NOT EXISTS (SELECT 1 FROM dbo.Organization WHERE id = @organizationId AND status = N'DRAFT' AND requesterId IS NULL)
        THROW 51022, 'C10 draft/default/requester assertion failed.', 1;
    INSERT dbo.EventCategory (id, code, name, active, displayOrder)
    VALUES (@categoryId, CONVERT(nvarchar(36), @categoryId), N'DONG-02 test category', 1, 0);
    INSERT dbo.Event (id, organizationId, title, description, categoryId, venueName, venueAddress,
                     saleStart, saleEnd, startTime, endTime)
    VALUES (@eventId, @organizationId, N'DONG-02 test', N'Test description', @categoryId, N'Venue', N'Address',
            '2026-10-06T03:00:00', '2026-10-06T06:00:00', '2026-10-06T06:00:00', '2026-10-06T08:00:00');
    INSERT dbo.Zone (id, eventId, name, type, price, standingCapacity, standingHeld, standingSold)
    VALUES (@standingId, @eventId, N'Standing', N'STANDING', 0, 3, 1, 2),
           (@otherStandingId, @eventId, N'Standing two', N'STANDING', 300000, 3, 0, 0),
           (@seatedId, @eventId, N'Seated', N'SEATED', 200000, NULL, NULL, NULL);
    INSERT dbo.Seat (id, zoneId, rowName, seatNumber) VALUES (NEWID(), @seatedId, N'A', 1);
    INSERT dbo.UserOrganizationRole (id, userId, organizationId, role, active)
    VALUES (NEWID(), @buyerId, @organizationId, N'MANAGER', 0);

    DECLARE @cases TABLE (testName nvarchar(100), command nvarchar(max), constraintName sysname);
    INSERT @cases VALUES
        (N'C04 equal sale times', N'UPDATE dbo.Event SET saleEnd=saleStart WHERE id=@eventId', N'CK_Event_TimeRange'),
        (N'C04 equal event times', N'UPDATE dbo.Event SET endTime=startTime WHERE id=@eventId', N'CK_Event_TimeRange'),
        (N'C04 sale ends after start', N'UPDATE dbo.Event SET saleEnd=DATEADD(second,1,startTime) WHERE id=@eventId', N'CK_Event_TimeRange'),
        (N'C05 overflow quota', N'UPDATE dbo.Zone SET standingHeld=2 WHERE id=@standingId', N'CK_Zone_Price_Quota'),
        (N'C05 null capacity', N'UPDATE dbo.Zone SET standingCapacity=NULL WHERE id=@standingId', N'CK_Zone_Price_Quota'),
        (N'C05 null held', N'UPDATE dbo.Zone SET standingHeld=NULL WHERE id=@standingId', N'CK_Zone_Price_Quota'),
        (N'C05 null sold', N'UPDATE dbo.Zone SET standingSold=NULL WHERE id=@standingId', N'CK_Zone_Price_Quota'),
        (N'C05 negative held', N'UPDATE dbo.Zone SET standingHeld=-1 WHERE id=@standingId', N'CK_Zone_Price_Quota'),
        (N'C05 negative price', N'UPDATE dbo.Zone SET price=-1 WHERE id=@standingId', N'CK_Zone_Price_Quota'),
        (N'C05 seated counters', N'UPDATE dbo.Zone SET standingHeld=0 WHERE id=@seatedId', N'CK_Zone_Price_Quota'),
        (N'C05 capacity limit', N'UPDATE dbo.Zone SET standingCapacity=1000001 WHERE id=@standingId', N'CK_Zone_Price_Quota'),
        (N'C10 pending requester', N'UPDATE dbo.Organization SET status=N''PENDING_APPROVAL'' WHERE id=@organizationId', N'CK_Organization_Requester'),
        (N'C10 approved requester', N'UPDATE dbo.Organization SET status=N''APPROVED'' WHERE id=@organizationId', N'CK_Organization_Requester'),
        (N'C10 rejected requester', N'UPDATE dbo.Organization SET status=N''REJECTED'', rejectionReason=N''reason'' WHERE id=@organizationId', N'CK_Organization_Requester'),
        (N'C03 duplicate seat', N'INSERT dbo.Seat(id,zoneId,rowName,seatNumber) VALUES(NEWID(),@seatedId,N''A'',1)', N'UQ_Seat_Zone_Row_Number'),
        (N'C02 duplicate inactive pair', N'INSERT dbo.UserOrganizationRole(id,userId,organizationId,role,active) VALUES(NEWID(),@buyerId,@organizationId,N''CHECK_IN_STAFF'',0)', N'UQ_UserOrganizationRole_User_Organization'),
        (N'C02 duplicate active pair', N'INSERT dbo.UserOrganizationRole(id,userId,organizationId,role,active) VALUES(NEWID(),@buyerId,@organizationId,N''MANAGER'',1)', N'UQ_UserOrganizationRole_User_Organization'),
        (N'Canonical enum spelling', N'UPDATE dbo.Organization SET status=N''draft'' WHERE id=@organizationId', N'CK_Organization_Status');

    DECLARE @testName nvarchar(100), @command nvarchar(max), @constraintName sysname, @passed int = 0;
    DECLARE cases CURSOR LOCAL FAST_FORWARD FOR SELECT testName, command, constraintName FROM @cases;
    OPEN cases;
    FETCH NEXT FROM cases INTO @testName, @command, @constraintName;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        BEGIN TRY
            EXEC sys.sp_executesql @command,
                N'@organizationId uniqueidentifier,@eventId uniqueidentifier,@standingId uniqueidentifier,@seatedId uniqueidentifier,@buyerId uniqueidentifier',
                @organizationId, @eventId, @standingId, @seatedId, @buyerId;
            THROW 51022, 'An invalid statement was accepted.', 1;
        END TRY
        BEGIN CATCH
            IF ERROR_NUMBER() NOT IN (547, 2601, 2627) OR CHARINDEX(@constraintName, ERROR_MESSAGE()) = 0
                THROW;
            SET @passed += 1;
        END CATCH;
        FETCH NEXT FROM cases INTO @testName, @command, @constraintName;
    END;
    CLOSE cases;
    DEALLOCATE cases;
    IF @passed <> 18 THROW 51022, 'Not all negative constraint cases were exercised.', 1;

    UPDATE dbo.Zone SET standingHeld=0, standingSold=0 WHERE id IN (@standingId, @otherStandingId);
    BEGIN TRY
        UPDATE dbo.Zone SET standingHeld=CASE WHEN id=@standingId THEN 1 ELSE 4 END
        WHERE id IN (@standingId, @otherStandingId);
        THROW 51022, 'Multirow quota mutation was accepted.', 1;
    END TRY
    BEGIN CATCH
        IF ERROR_NUMBER() <> 547 OR CHARINDEX(N'CK_Zone_Price_Quota', ERROR_MESSAGE()) = 0 THROW;
    END CATCH;
    IF EXISTS (SELECT 1 FROM dbo.Zone WHERE id IN (@standingId, @otherStandingId) AND standingHeld <> 0)
        THROW 51022, 'Failed multirow statement retained a partial mutation.', 1;

    IF @initialTranCount = 0 ROLLBACK TRANSACTION;
    ELSE ROLLBACK TRANSACTION Dong02Tests;
    IF EXISTS (SELECT 1 FROM dbo.Organization WHERE id=@organizationId)
       OR EXISTS (SELECT 1 FROM dbo.EventCategory WHERE id=@categoryId)
       OR @@TRANCOUNT <> @initialTranCount
        THROW 51022, 'Outer rollback or caller transaction count assertion failed.', 1;
    IF @originalXactAbort = 1 SET XACT_ABORT ON;
    PRINT CONCAT('PASS: DONG-02 constraint cases=', @passed, '; multirow statement and outer rollback assertions passed.');
END TRY
BEGIN CATCH
    IF @initialTranCount = 0 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    ELSE IF @initialTranCount > 0 AND XACT_STATE() = 1 ROLLBACK TRANSACTION Dong02Tests;
    IF @originalXactAbort = 1 SET XACT_ABORT ON;
    THROW;
END CATCH;
