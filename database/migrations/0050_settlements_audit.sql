-- Owner: Vương / VUONG-01. Apply once through the manifest runner.
-- Cross-domain organization/event/order/actor FKs belong to 0100 (VUONG-02).
-- This script creates no substitute domain tables and no cross-row CHECK.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;

DECLARE @ownsTransaction bit = CASE WHEN @@TRANCOUNT = 0 THEN 1 ELSE 0 END;
IF @ownsTransaction = 1 BEGIN TRANSACTION;
ELSE SAVE TRANSACTION Vuong0050;
BEGIN TRY

CREATE TABLE dbo.CommissionRule (
    id uniqueidentifier NOT NULL CONSTRAINT PK_CommissionRule PRIMARY KEY,
    organizationId uniqueidentifier NOT NULL,
    ratePercent decimal(19,6) NOT NULL,
    fixedFee decimal(19,0) NOT NULL,
    effectiveFrom datetime2(7) NOT NULL,
    effectiveTo datetime2(7) NOT NULL,
    version int NOT NULL CONSTRAINT DF_CommissionRule_Version DEFAULT 0,
    CONSTRAINT CK_CommissionRule_Terms CHECK
        (ratePercent >= 0 AND fixedFee >= 0 AND effectiveFrom < effectiveTo)
);

CREATE TABLE dbo.Settlement (
    id uniqueidentifier NOT NULL CONSTRAINT PK_Settlement PRIMARY KEY,
    eventId uniqueidentifier NOT NULL CONSTRAINT UQ_Settlement_Event UNIQUE,
    status varchar(16) NOT NULL CONSTRAINT CK_Settlement_Status CHECK (status IN ('DRAFT','CONFIRMED','PAID')),
    grossRevenue decimal(19,0) NOT NULL,
    totalRefund decimal(19,0) NOT NULL,
    totalCommission decimal(19,0) NOT NULL,
    netPayable AS CONVERT(decimal(19,0), grossRevenue - totalRefund - totalCommission) PERSISTED,
    paidAmount decimal(19,0) NOT NULL,
    pendingAmount decimal(19,0) NOT NULL,
    availableToPay AS CONVERT(decimal(19,0), grossRevenue - totalRefund - totalCommission - paidAmount - pendingAmount) PERSISTED,
    confirmedAt datetime2(7) NULL,
    version int NOT NULL CONSTRAINT DF_Settlement_Version DEFAULT 0,
    CONSTRAINT CK_Settlement_Amounts CHECK
        (grossRevenue >= 0 AND totalRefund >= 0 AND totalCommission >= 0
         AND paidAmount >= 0 AND pendingAmount >= 0
         AND totalRefund + totalCommission <= grossRevenue
         AND paidAmount + pendingAmount <= grossRevenue - totalRefund - totalCommission)
);

CREATE TABLE dbo.SettlementOrderSnapshot (
    settlementId uniqueidentifier NOT NULL,
    orderId uniqueidentifier NOT NULL,
    grossAmount decimal(19,0) NOT NULL,
    refundAmount decimal(19,0) NOT NULL,
    commissionAmount decimal(19,0) NOT NULL,
    netAmount decimal(19,0) NOT NULL,
    CONSTRAINT PK_SettlementOrderSnapshot PRIMARY KEY (settlementId, orderId),
    CONSTRAINT FK_SettlementOrderSnapshot_Settlement FOREIGN KEY (settlementId) REFERENCES dbo.Settlement(id),
    CONSTRAINT CK_SettlementOrderSnapshot_Amounts CHECK
        (grossAmount >= 0 AND refundAmount >= 0 AND commissionAmount >= 0 AND netAmount >= 0
         AND refundAmount <= grossAmount AND commissionAmount <= grossAmount - refundAmount
         AND netAmount = grossAmount - refundAmount - commissionAmount)
);

CREATE TABLE dbo.SettlementTransferLog (
    payoutId uniqueidentifier NOT NULL CONSTRAINT PK_SettlementTransferLog PRIMARY KEY,
    settlementId uniqueidentifier NOT NULL,
    amount decimal(19,0) NOT NULL,
    status varchar(16) NOT NULL CONSTRAINT CK_SettlementTransferLog_Status CHECK (status IN ('PENDING','SUCCEEDED','FAILED')),
    reference nvarchar(255) NULL,
    createdAt datetime2(7) NOT NULL CONSTRAINT DF_SettlementTransferLog_Created DEFAULT SYSUTCDATETIME(),
    completedAt datetime2(7) NULL,
    CONSTRAINT FK_SettlementTransferLog_Settlement FOREIGN KEY (settlementId) REFERENCES dbo.Settlement(id),
    CONSTRAINT CK_SettlementTransferLog_Amount CHECK (amount > 0)
);

CREATE TABLE dbo.AuditLog (
    id uniqueidentifier NOT NULL CONSTRAINT PK_AuditLog PRIMARY KEY,
    actorId uniqueidentifier NULL,
    source varchar(8) NOT NULL,
    action varchar(80) NOT NULL,
    aggregateType varchar(80) NOT NULL,
    aggregateId uniqueidentifier NOT NULL,
    detail nvarchar(max) NULL,
    createdAt datetime2(7) NOT NULL CONSTRAINT DF_AuditLog_Created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_AuditLog_Source CHECK
        ((source = 'USER' AND actorId IS NOT NULL) OR (source = 'SYSTEM' AND actorId IS NULL))
);

-- Durable simulation data only; not a real bank/provider integration.
-- Repository must verify settlement/amount when replaying the same payoutId.
CREATE TABLE dbo.MockPayoutProviderLedger (
    payoutId uniqueidentifier NOT NULL CONSTRAINT PK_MockPayoutProviderLedger PRIMARY KEY,
    settlementId uniqueidentifier NOT NULL,
    amount decimal(19,0) NOT NULL,
    status varchar(16) NOT NULL CONSTRAINT CK_MockPayoutProviderLedger_Status CHECK (status IN ('SUCCEEDED','FAILED')),
    reference nvarchar(255) NULL,
    acceptedAt datetime2(7) NOT NULL CONSTRAINT DF_MockPayoutProviderLedger_Accepted DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_MockPayoutProviderLedger_Amount CHECK (amount > 0)
);

IF @ownsTransaction = 1 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @ownsTransaction = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    ELSE IF @ownsTransaction = 0 AND XACT_STATE() = 1 ROLLBACK TRANSACTION Vuong0050;
    THROW;
END CATCH;
