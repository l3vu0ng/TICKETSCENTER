-- Test database only. Requires VUONG-02 buyer_unverified and VUONG-03 AUTH_TECH grants.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT LIKE N'%[_]test' THROW 51000, 'Use a dedicated test database.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.[User] WHERE id='00000000-0000-0000-0000-000000000003')
    THROW 51000, 'Missing VUONG-02 buyer_unverified fixture.', 1;
GO
CREATE OR ALTER PROCEDURE dbo.tc_test_profile_write @id uniqueidentifier, @name nvarchar(120)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @own bit = CASE WHEN @@TRANCOUNT=0 THEN 1 ELSE 0 END;
    IF @own=1 BEGIN TRANSACTION;
    ELSE SAVE TRANSACTION khanh_profile;
    BEGIN TRY
        UPDATE dbo.[User] SET fullName=@name WHERE id=@id;
        IF @@ROWCOUNT <> 1 THROW 51000, 'Fixture user not found.', 1;
        IF @own=1 COMMIT;
    END TRY
    BEGIN CATCH
        IF @own=1 AND XACT_STATE()<>0 ROLLBACK;
        ELSE IF @own=0 AND XACT_STATE()=1 ROLLBACK TRANSACTION khanh_profile;
        THROW;
    END CATCH;
END;
GO
DECLARE @id uniqueidentifier='00000000-0000-0000-0000-000000000003';
DECLARE @original nvarchar(120)=(SELECT fullName FROM dbo.[User] WHERE id=@id);
BEGIN TRY
    BEGIN TRANSACTION;
    EXEC dbo.tc_test_profile_write @id=@id, @name=N'M0 rollback test';
    IF @@TRANCOUNT<>1 THROW 51000, 'SP committed the outer transaction.', 1;
    THROW 51001, 'Injected failure after mutation.', 1;
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK;
    IF ERROR_NUMBER()<>51001 THROW;
END CATCH;
IF (SELECT fullName FROM dbo.[User] WHERE id=@id)<>@original
    THROW 51000, 'Mutation survived rollback.', 1;
IF @@TRANCOUNT<>0 THROW 51000, 'Transaction leaked.', 1;
GO
