SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
IF DB_NAME() NOT LIKE N'TicketsCenter[_]Test[_]%'
    THROW 51300, N'VUONG-03 requires a dedicated test database.', 1;
IF DATABASE_PRINCIPAL_ID('tc_buyer') IS NULL OR DATABASE_PRINCIPAL_ID('tc_manager') IS NULL
   OR DATABASE_PRINCIPAL_ID('tc_checkin') IS NULL OR DATABASE_PRINCIPAL_ID('tc_platform_admin') IS NULL
    THROW 51301, N'Expected four runtime roles before authorization tests.', 1;
IF DATABASE_PRINCIPAL_ID('tc_buyer_user') IS NULL OR DATABASE_PRINCIPAL_ID('tc_manager_user') IS NULL
   OR DATABASE_PRINCIPAL_ID('tc_checkin_user') IS NULL OR DATABASE_PRINCIPAL_ID('tc_admin_user') IS NULL
    THROW 51302, N'Expected four provisioned users; tests do not impersonate an administrator as a buyer.', 1;
GO
CREATE PROCEDURE #ExpectDenied @statement nvarchar(max)
AS
BEGIN
    DECLARE @error int;
    BEGIN TRY EXEC sys.sp_executesql @statement; END TRY
    BEGIN CATCH SET @error = ERROR_NUMBER(); END CATCH;
    IF @error IS NULL OR @error <> 229
        THROW 51303, N'Expected permission denied 229.', 1;
END;
GO
BEGIN TRY
    EXECUTE AS USER = 'tc_admin_user';
    IF IS_ROLEMEMBER('db_owner') = 1 THROW 51304, N'Runtime admin must not be db_owner.', 1;
    SELECT TOP (0) id FROM dbo.CommissionRule;
    SELECT TOP (0) id FROM dbo.Settlement;
    REVERT;
    EXECUTE AS USER = 'tc_buyer_user';
    EXEC #ExpectDenied N'SELECT TOP(0) * FROM dbo.Settlement';
    EXEC #ExpectDenied N'UPDATE dbo.Settlement SET paidAmount=paidAmount';
    REVERT;
    EXECUTE AS USER = 'tc_manager_user';
    EXEC #ExpectDenied N'SELECT TOP(0) * FROM dbo.Settlement';
    EXEC #ExpectDenied N'UPDATE dbo.CommissionRule SET fixedFee=fixedFee';
    REVERT;
    EXECUTE AS USER = 'tc_checkin_user';
    EXEC #ExpectDenied N'SELECT TOP(0) * FROM dbo.CommissionRule';
    EXEC #ExpectDenied N'SELECT TOP(0) * FROM dbo.Settlement';
    EXEC #ExpectDenied N'SELECT TOP(0) * FROM dbo.SettlementOrderSnapshot';
    EXEC #ExpectDenied N'SELECT TOP(0) * FROM dbo.SettlementTransferLog';
    EXEC #ExpectDenied N'SELECT TOP(0) * FROM dbo.MockPayoutProviderLedger';
    REVERT;

    -- Test competing permission sources on a disposable WITHOUT LOGIN user.
    CREATE USER tc_vuong_permission_probe WITHOUT LOGIN;
    GRANT SELECT ON dbo.CommissionRule TO tc_vuong_permission_probe;
    EXECUTE AS USER = 'tc_vuong_permission_probe';
    SELECT TOP(0) id FROM dbo.CommissionRule;
    REVERT;
    REVOKE SELECT ON dbo.CommissionRule FROM tc_vuong_permission_probe;
    EXECUTE AS USER = 'tc_vuong_permission_probe';
    EXEC #ExpectDenied N'SELECT TOP(0) id FROM dbo.CommissionRule';
    REVERT;
    GRANT SELECT ON dbo.CommissionRule TO tc_vuong_permission_probe;
    ALTER ROLE tc_checkin ADD MEMBER tc_vuong_permission_probe;
    EXECUTE AS USER = 'tc_vuong_permission_probe';
    EXEC #ExpectDenied N'SELECT TOP(0) id FROM dbo.CommissionRule';
    REVERT;
    DROP USER tc_vuong_permission_probe;
    PRINT 'VUONG-03 PASS: admin read, 11 denied calls, GRANT/REVOKE/DENY competing source; no business SP/HTTP integration claimed.';
END TRY
BEGIN CATCH
    IF USER_NAME() IN ('tc_admin_user','tc_buyer_user','tc_manager_user','tc_checkin_user','tc_vuong_permission_probe') REVERT;
    IF DATABASE_PRINCIPAL_ID('tc_vuong_permission_probe') IS NOT NULL DROP USER tc_vuong_permission_probe;
    THROW;
END CATCH;
