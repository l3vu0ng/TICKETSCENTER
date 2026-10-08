-- Real SQL Server constraint tests; dedicated test DB only; no domain substitutes.
SET NOCOUNT ON;
SET XACT_ABORT OFF;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
IF DB_NAME() NOT LIKE N'TicketsCenter[_]Test[_]%'
    THROW 51000, N'VUONG-01 requires a dedicated TicketsCenter_Test_ database.', 1;
IF OBJECT_ID(N'dbo.CommissionRule', N'U') IS NULL
    THROW 51001, N'Expected VUONG-01 financial schema before running tests.', 1;
GO
CREATE PROCEDURE #ExpectRejected @statement nvarchar(max), @expected int
AS
BEGIN
    DECLARE @actual int;
    BEGIN TRY
        EXEC sys.sp_executesql @statement;
    END TRY
    BEGIN CATCH
        SET @actual = ERROR_NUMBER();
    END CATCH;
    IF @actual IS NULL OR @actual <> @expected
        THROW 51002, N'Expected SQL constraint rejection did not occur.', 1;
END;
GO
BEGIN TRANSACTION;
BEGIN TRY
    INSERT dbo.CommissionRule(id, organizationId, ratePercent, fixedFee, effectiveFrom, effectiveTo)
    VALUES ('60000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001',
            250, 0, '2026-10-06T03:00:00', '2027-01-01T00:00:00');
    EXEC #ExpectRejected N'UPDATE dbo.CommissionRule SET ratePercent = -1', 547;
    EXEC #ExpectRejected N'UPDATE dbo.CommissionRule SET fixedFee = -1', 547;
    EXEC #ExpectRejected N'UPDATE dbo.CommissionRule SET effectiveTo = effectiveFrom', 547;
    EXEC #ExpectRejected N'UPDATE dbo.CommissionRule SET ratePercent = NULL', 515;

    INSERT dbo.Settlement(id, eventId, status, grossRevenue, totalRefund, totalCommission, paidAmount, pendingAmount)
    VALUES ('70000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
            'DRAFT', 500000, 100000, 50000, 0, 0);
    IF (SELECT netPayable FROM dbo.Settlement) <> 350000
        THROW 51003, N'Expected derived netPayable 350000.', 1;
    EXEC #ExpectRejected N'UPDATE dbo.Settlement SET totalRefund = -1', 547;
    EXEC #ExpectRejected N'UPDATE dbo.Settlement SET totalCommission = 500001', 547;
    EXEC #ExpectRejected N'UPDATE dbo.Settlement SET paidAmount = 300000, pendingAmount = 100000', 547;
    EXEC #ExpectRejected N'UPDATE dbo.Settlement SET status = ''UNKNOWN''', 547;
    EXEC #ExpectRejected N'INSERT dbo.Settlement(id,eventId,status,grossRevenue,totalRefund,totalCommission,paidAmount,pendingAmount) VALUES(NEWID(),''10000000-0000-0000-0000-000000000001'',''DRAFT'',0,0,0,0,0)', 2627;

    INSERT dbo.SettlementOrderSnapshot(settlementId, orderId, grossAmount, refundAmount, commissionAmount, netAmount)
    VALUES ('70000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 500000, 100000, 50000, 350000);
    EXEC #ExpectRejected N'UPDATE dbo.SettlementOrderSnapshot SET netAmount = 1', 547;
    EXEC #ExpectRejected N'UPDATE dbo.SettlementOrderSnapshot SET refundAmount = 500001', 547;
    EXEC #ExpectRejected N'INSERT dbo.SettlementOrderSnapshot SELECT * FROM dbo.SettlementOrderSnapshot', 2627;
    EXEC #ExpectRejected N'INSERT dbo.SettlementOrderSnapshot(settlementId,orderId,grossAmount,refundAmount,commissionAmount,netAmount) VALUES(NEWID(),NEWID(),1,0,0,1)', 547;

    INSERT dbo.SettlementTransferLog(payoutId, settlementId, amount, status)
    VALUES ('a0000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 100000, 'PENDING');
    EXEC #ExpectRejected N'UPDATE dbo.SettlementTransferLog SET amount = 0', 547;
    EXEC #ExpectRejected N'UPDATE dbo.SettlementTransferLog SET status = ''UNKNOWN''', 547;
    EXEC #ExpectRejected N'INSERT dbo.SettlementTransferLog(payoutId,settlementId,amount,status) SELECT payoutId,settlementId,amount,status FROM dbo.SettlementTransferLog', 2627;

    INSERT dbo.AuditLog(id, actorId, source, action, aggregateType, aggregateId, detail)
    VALUES (NEWID(), NULL, 'SYSTEM', 'TEST', 'Settlement', '70000000-0000-0000-0000-000000000001', N'Synthetic SQL test; no secrets');
    EXEC #ExpectRejected N'UPDATE dbo.AuditLog SET source = ''USER''', 547;
    EXEC #ExpectRejected N'UPDATE dbo.AuditLog SET source = ''INVALID''', 547;

    INSERT dbo.MockPayoutProviderLedger(payoutId, settlementId, amount, status, reference)
    VALUES ('a0000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 100000, 'SUCCEEDED', 'simulated-test');
    EXEC #ExpectRejected N'UPDATE dbo.MockPayoutProviderLedger SET amount = 0', 547;
    EXEC #ExpectRejected N'INSERT dbo.MockPayoutProviderLedger SELECT * FROM dbo.MockPayoutProviderLedger', 2627;
    ROLLBACK TRANSACTION;
    PRINT 'VUONG-01 PASS: derived net, 20 constraint rejections, rollback; no FK integration claimed.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
