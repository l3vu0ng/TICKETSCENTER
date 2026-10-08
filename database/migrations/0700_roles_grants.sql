-- Vương / VUONG-03: financial foundation only; forward migrations grant future modules.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF OBJECT_ID(N'dbo.Settlement',N'U') IS NULL OR OBJECT_ID(N'dbo.AuditLog',N'U') IS NULL
    THROW 51400, N'Financial schema 0050 is required before role foundation.', 1;
IF DATABASE_PRINCIPAL_ID('tc_buyer') IS NULL CREATE ROLE tc_buyer AUTHORIZATION dbo;
IF DATABASE_PRINCIPAL_ID('tc_manager') IS NULL CREATE ROLE tc_manager AUTHORIZATION dbo;
IF DATABASE_PRINCIPAL_ID('tc_checkin') IS NULL CREATE ROLE tc_checkin AUTHORIZATION dbo;
IF DATABASE_PRINCIPAL_ID('tc_platform_admin') IS NULL CREATE ROLE tc_platform_admin AUTHORIZATION dbo;
IF DATABASE_PRINCIPAL_ID('tc_auth_tech') IS NULL CREATE ROLE tc_auth_tech AUTHORIZATION dbo;
IF DATABASE_PRINCIPAL_ID('tc_worker_tech') IS NULL CREATE ROLE tc_worker_tech AUTHORIZATION dbo;

-- Platform admin reads its financial foundation. Mutations await protected Service/SP/TR.
GRANT SELECT ON dbo.CommissionRule TO tc_platform_admin;
GRANT SELECT ON dbo.Settlement TO tc_platform_admin;
GRANT SELECT ON dbo.SettlementOrderSnapshot TO tc_platform_admin;
GRANT SELECT ON dbo.SettlementTransferLog TO tc_platform_admin;
GRANT SELECT ON dbo.AuditLog TO tc_platform_admin;

-- Explicit deny even when an unrelated role/user adds a grant.
DENY SELECT, INSERT, UPDATE, DELETE ON dbo.CommissionRule TO tc_checkin;
DENY SELECT, INSERT, UPDATE, DELETE ON dbo.Settlement TO tc_checkin;
DENY SELECT, INSERT, UPDATE, DELETE ON dbo.SettlementOrderSnapshot TO tc_checkin;
DENY SELECT, INSERT, UPDATE, DELETE ON dbo.SettlementTransferLog TO tc_checkin;
DENY SELECT, INSERT, UPDATE, DELETE ON dbo.AuditLog TO tc_checkin;
DENY SELECT, INSERT, UPDATE, DELETE ON dbo.MockPayoutProviderLedger TO tc_checkin;

-- No broad schema SELECT/DML/EXECUTE, no db_owner, no payout permission for worker.
-- V03/V04/V07/V08/F05/F10 DENY and module grants need the actual object catalog.
