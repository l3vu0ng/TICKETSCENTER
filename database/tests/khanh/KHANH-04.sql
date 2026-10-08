-- Negative constraints are exercised inside rollback-only test transactions.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT LIKE N'%[_]test' THROW 51000, 'Use a dedicated test database.', 1;
DECLARE @fixture uniqueidentifier='00000000-0000-0000-0000-000000000001';
IF NOT EXISTS (SELECT 1 FROM dbo.[User] WHERE id=@fixture)
    THROW 51000, 'Missing VUONG-02 buyer_a fixture.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    INSERT dbo.[User](id,email,normalizedEmail,userName,normalizedUserName,fullName,status,passwordHash)
    SELECT NEWID(),email,normalizedEmail,userName,normalizedUserName,fullName,status,passwordHash
    FROM dbo.[User] WHERE id=@fixture;
    ROLLBACK;
    THROW 51000, 'C01 allowed duplicate normalized identity.', 1;
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK;
    IF ERROR_NUMBER() NOT IN (2601,2627) THROW;
END CATCH;

BEGIN TRY
    BEGIN TRANSACTION;
    INSERT dbo.OtpRecord(id,userId,purpose,codeHmac,createdAt,expiresAt,lastSentAt,failedAttempts)
    VALUES(NEWID(),@fixture,N'VERIFY_EMAIL',N'test-digest',SYSUTCDATETIME(),DATEADD(minute,5,SYSUTCDATETIME()),SYSUTCDATETIME(),6);
    ROLLBACK;
    THROW 51000, 'OTP attempt constraint did not reject 6.', 1;
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK;
    IF ERROR_NUMBER()<>547 THROW;
END CATCH;
IF @@TRANCOUNT<>0 THROW 51000, 'Transaction leaked.', 1;
