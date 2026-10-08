[CmdletBinding()]
param(
    [ValidateSet('Full', 'FinancialFoundation', 'IdentityFinancialFoundation')][string]$Scope = 'Full',
    [string]$Server = $env:TC_SQL_HOST,
    [string]$Database = $env:TC_TEST_DATABASE,
    [ValidateSet('Integrated', 'SqlLogin')][string]$Authentication = 'Integrated',
    [switch]$TrustLocalCertificate,
    [switch]$PreflightOnly
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$migrationDirectory = Join-Path $PSScriptRoot 'migrations'
$manifestPath = Join-Path $migrationDirectory 'manifest.csv'
if (-not (Test-Path -LiteralPath $manifestPath)) { throw 'Missing migration manifest.' }
$entries = @(Import-Csv -LiteralPath $manifestPath)
if ($entries.Count -eq 0) { throw 'Empty migration manifest.' }
$seen = @{}
$previous = ''
foreach ($entry in $entries) {
    if ($entry.file -notmatch '^[0-9]{4}_[A-Za-z0-9_]+\.sql$') { throw "Invalid migration filename: $($entry.file)" }
    if ($entry.sha256 -notmatch '^[0-9a-fA-F]{64}$') { throw "Missing SHA256: $($entry.file)" }
    if ([string]::IsNullOrWhiteSpace($entry.owner)) { throw "Missing owner: $($entry.file)" }
    if ($seen.ContainsKey($entry.file) -or [string]::CompareOrdinal($previous, $entry.file) -ge 0) {
        throw "Manifest must be unique and sorted: $($entry.file)"
    }
    foreach ($dependency in @($entry.dependencies -split ';' | Where-Object { $_ })) {
        if (-not $seen.ContainsKey($dependency)) { throw "Missing dependency before $($entry.file): $dependency" }
    }
    $path = Join-Path $migrationDirectory $entry.file
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing migration file: $($entry.file)" }
    # Canonical UTF-8 without BOM, LF line endings: stable with Windows Git autocrlf.
    $text = [IO.File]::ReadAllText($path).Replace("`r`n", "`n")
    if ($text -match '(?m)^\s*:' -or $text.Contains('$(')) { throw "Migration contains sqlcmd directives/variables: $($entry.file)" }
    $bytes = [Text.Encoding]::UTF8.GetBytes($text)
    $actual = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant()
    if ($actual -cne $entry.sha256.ToLowerInvariant()) { throw "Checksum mismatch: $($entry.file)" }
    $seen[$entry.file] = $entry
    $previous = $entry.file
}
$required = if ($Scope -eq 'Full') {
    @('0010_identity.sql','0011_identity_validation.sql','0020_organizations_events.sql','0030_sales.sql','0040_refunds_checkin.sql',
      '0050_settlements_audit.sql','0100_cross_domain_keys.sql','0700_roles_grants.sql')
} elseif ($Scope -eq 'IdentityFinancialFoundation') {
    @('0010_identity.sql','0011_identity_validation.sql','0050_settlements_audit.sql')
} else { @('0050_settlements_audit.sql') }
foreach ($file in $required) {
    if (-not $seen.ContainsKey($file)) { throw "Missing required migration: $file (scope $Scope)" }
}
if ($Scope -ne 'Full') {
    $entries = @($entries | Where-Object { $_.file -in $required })
}
if ($PreflightOnly) { Write-Output "PREFLIGHT PASS ($Scope); no SQL connection or integration claimed."; exit 0 }
if ([string]::IsNullOrWhiteSpace($Server) -or [string]::IsNullOrWhiteSpace($Database)) {
    throw 'TC_SQL_HOST and TC_TEST_DATABASE (or -Server/-Database) are required.'
}
if ($Database -notmatch '^TicketsCenter_Test_[A-Za-z0-9_]+$') { throw 'Only a dedicated test database is allowed.' }
if ($TrustLocalCertificate -and $Server -notmatch '^(localhost|127\.0\.0\.1|\.|'+[regex]::Escape($env:COMPUTERNAME)+')(\\[^,]+)?(,[0-9]+)?$') {
    throw 'TrustLocalCertificate is restricted to the local SQL Server.'
}
$sqlcmd = (Get-Command sqlcmd -ErrorAction Stop).Source
$sqlArgs = @('-S', $Server, '-d', $Database, '-b', '-I', '-f', '65001', '-r', '1', '-l', '5', '-t', '30')
if ($TrustLocalCertificate) { $sqlArgs += '-C' }
if ($Authentication -eq 'Integrated') { $sqlArgs += '-E' }
else {
    if ([string]::IsNullOrWhiteSpace($env:TC_TEST_LOGIN) -or [string]::IsNullOrWhiteSpace($env:SQLCMDPASSWORD)) {
        throw 'TC_TEST_LOGIN and SQLCMDPASSWORD are required for SqlLogin; no password command argument.'
    }
    $sqlArgs += @('-U', $env:TC_TEST_LOGIN)
}
function InvokeSql([string]$query) {
    $result = & $sqlcmd @sqlArgs '-h' '-1' '-W' '-Q' $query 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) { throw "SQL migration command failed: $result" }
    return $result.Trim()
}
$null = InvokeSql @'
SET NOCOUNT ON;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL
CREATE TABLE dbo.SchemaMigrationHistory (
    filename varchar(128) NOT NULL PRIMARY KEY,
    sha256 char(64) NOT NULL,
    owner nvarchar(32) NOT NULL,
    appliedAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
'@
foreach ($entry in $entries) {
    $filename = $entry.file
    $hash = $entry.sha256.ToLowerInvariant()
    $owner = $entry.owner.Replace("'", "''")
    $recorded = InvokeSql "SET NOCOUNT ON; SELECT sha256 FROM dbo.SchemaMigrationHistory WHERE filename = '$filename';"
    if ($recorded) {
        if ($recorded -cne $hash) { throw "Applied checksum mismatch: $filename; never edit an applied migration." }
        Write-Output "UNCHANGED $filename"
        continue
    }
    $path = Join-Path $migrationDirectory $filename
    if ($path.Contains('"')) { throw 'Unsupported quote in migration path.' }
    $wrapper = @"
:on error exit
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 10000;
BEGIN TRANSACTION;
DECLARE @lockResult int;
EXEC @lockResult = sys.sp_getapplock @Resource=N'TicketsCenter.Migrations', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=10000;
IF @lockResult < 0 THROW 51100, 'Could not lock migrations', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE filename='$filename')
    THROW 51101, 'Concurrent migration already applied; rerun after checking checksum', 1;
GO
:r "$path"
GO
IF @@TRANCOUNT <> 1 THROW 51102, 'Migration changed transaction ownership', 1;
INSERT dbo.SchemaMigrationHistory(filename,sha256,owner) VALUES('$filename','$hash',N'$owner');
COMMIT TRANSACTION;
"@
    $wrapperPath = [IO.Path]::GetTempFileName()
    try {
        [IO.File]::WriteAllText($wrapperPath, $wrapper, [Text.UTF8Encoding]::new($false))
        $result = & $sqlcmd @sqlArgs '-i' $wrapperPath 2>&1 | Out-String
        if ($LASTEXITCODE -ne 0) { throw "Migration failed (history not committed): $filename`n$result" }
        Write-Output "APPLIED $filename"
    } finally { Remove-Item -LiteralPath $wrapperPath }
}
Write-Output "MIGRATIONS PASS ($Scope); Full acceptance also requires seeds, inventory and real module tests."
