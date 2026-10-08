-- Azure SQL contained users; invoked with typed @User/@Role/@Password parameters.
-- No server CREATE LOGIN or certificate override. Online evidence is still required.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF CONVERT(int, SERVERPROPERTY('EngineEdition')) <> 5
    THROW 51420, N'Azure SQL Database required for contained-user provisioning.', 1;
IF DB_NAME() NOT LIKE N'TicketsCenter[_]Test[_]%'
    THROW 51421, N'Only a dedicated test database is allowed.', 1;
IF DATABASE_PRINCIPAL_ID(@Role) IS NULL THROW 51422, N'Runtime role not installed.', 1;
IF DATABASE_PRINCIPAL_ID(@User) IS NOT NULL THROW 51423, N'User already exists; no credential replacement.', 1;
DECLARE @statement nvarchar(max);
BEGIN TRANSACTION;
BEGIN TRY
    SET @statement = N'CREATE USER ' + QUOTENAME(@User) + N' WITH PASSWORD=N'''
        + REPLACE(@Password, N'''', N'''''') + N''';';
    EXEC sys.sp_executesql @statement;
    SET @statement = N'ALTER ROLE ' + QUOTENAME(@Role) + N' ADD MEMBER ' + QUOTENAME(@User) + N';';
    EXEC sys.sp_executesql @statement;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
