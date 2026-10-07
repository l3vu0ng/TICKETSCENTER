-- Execute through provision-principals.ps1 with typed parameters, never sqlcmd password variables.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT LIKE N'TicketsCenter[_]Test[_]%'
    THROW 51410, N'Principal provisioning is limited to a dedicated test database.', 1;
IF DATABASE_PRINCIPAL_ID(@Role) IS NULL THROW 51411, N'Runtime role not installed.', 1;
IF SUSER_ID(@Login) IS NOT NULL OR DATABASE_PRINCIPAL_ID(@User) IS NOT NULL
    THROW 51412, N'Login/user exists; refusing to rotate or replace credentials.', 1;
DECLARE @statement nvarchar(max);
BEGIN TRANSACTION;
BEGIN TRY
    SET @statement = N'CREATE LOGIN ' + QUOTENAME(@Login) + N' WITH PASSWORD=N'''
        + REPLACE(@Password, N'''', N'''''') + N''', CHECK_POLICY=ON, CHECK_EXPIRATION=ON;';
    EXEC sys.sp_executesql @statement;
    SET @statement = N'CREATE USER ' + QUOTENAME(@User) + N' FOR LOGIN ' + QUOTENAME(@Login) + N';';
    EXEC sys.sp_executesql @statement;
    SET @statement = N'ALTER ROLE ' + QUOTENAME(@Role) + N' ADD MEMBER ' + QUOTENAME(@User) + N';';
    EXEC sys.sp_executesql @statement;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
