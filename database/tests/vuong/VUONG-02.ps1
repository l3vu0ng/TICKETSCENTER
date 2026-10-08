param([Parameter(Mandatory)][string]$RepositoryRoot)
$ErrorActionPreference = 'Stop'
$runner = Join-Path $RepositoryRoot 'database/migrate.ps1'
if (-not (Test-Path -LiteralPath $runner)) { throw 'Expected the migration runner.' }

# Negative cases use a private copy and must fail before connecting to any server.
$scratch = Join-Path ([IO.Path]::GetTempPath()) ('ticketscenter-manifest-test-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path (Join-Path $scratch 'migrations') | Out-Null
Copy-Item -LiteralPath $runner -Destination (Join-Path $scratch 'migrate.ps1')
foreach ($migration in (Import-Csv -LiteralPath (Join-Path $RepositoryRoot 'database/migrations/manifest.csv'))) {
    Copy-Item -LiteralPath (Join-Path $RepositoryRoot ('database/migrations/' + $migration.file)) -Destination (Join-Path $scratch 'migrations')
}
Copy-Item -LiteralPath (Join-Path $RepositoryRoot 'database/migrations/manifest.csv') -Destination (Join-Path $scratch 'migrations')
$testRunner = Join-Path $scratch 'migrate.ps1'

function ExpectFailure([string]$name, [string]$expected, [string[]]$arguments) {
    $output = & pwsh -NoProfile -File $testRunner @arguments 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0 -or $output -notmatch $expected) { throw "$name did not reject correctly: $output" }
    Write-Output "PASS $name"
}
ExpectFailure 'missing domain manifests' 'Missing required migration' @('-PreflightOnly')
$baseArgs = @('-Scope', 'FinancialFoundation', '-PreflightOnly')
$output = & pwsh -NoProfile -File $testRunner @baseArgs 2>&1 | Out-String
if ($LASTEXITCODE -ne 0 -or $output -notmatch 'PREFLIGHT PASS') { throw "Financial preflight failed: $output" }
Write-Output 'PASS financial preflight'
$identityArgs = @('-Scope', 'IdentityFinancialFoundation', '-PreflightOnly')
$output = & pwsh -NoProfile -File $testRunner @identityArgs 2>&1 | Out-String
if ($LASTEXITCODE -ne 0 -or $output -notmatch 'PREFLIGHT PASS \(IdentityFinancialFoundation\)') { throw "Identity/financial preflight failed: $output" }
Write-Output 'PASS identity/financial preflight'
$manifestPath = Join-Path $scratch 'migrations/manifest.csv'
$originalManifest = Get-Content -Raw -LiteralPath $manifestPath
foreach ($requiredIdentity in @('0010_identity.sql', '0011_identity_validation.sql')) {
    @(Import-Csv -LiteralPath $manifestPath | Where-Object { $_.file -ne $requiredIdentity }) | Export-Csv -NoTypeInformation -LiteralPath $manifestPath
    ExpectFailure ('missing identity ' + $requiredIdentity) 'Missing dependency|Missing required migration' $identityArgs
    $originalManifest | Set-Content -LiteralPath $manifestPath
}
Add-Content -LiteralPath (Join-Path $scratch 'migrations/0050_settlements_audit.sql') -Value '-- changed checksum'
ExpectFailure 'checksum drift' 'Checksum mismatch' $baseArgs
Copy-Item -LiteralPath (Join-Path $RepositoryRoot 'database/migrations/0050_settlements_audit.sql') -Destination (Join-Path $scratch 'migrations/0050_settlements_audit.sql')
ExpectFailure 'demo database guard' 'dedicated test database' @('-Scope', 'FinancialFoundation', '-Server', 'localhost', '-Database', 'TicketsCenter_Demo')
ExpectFailure 'missing environment' 'TC_SQL_HOST and TC_TEST_DATABASE' @('-Scope', 'FinancialFoundation', '-Server', '', '-Database', '')
$originalManifest.Replace('0050_settlements_audit.sql', '../0050_settlements_audit.sql') | Set-Content -LiteralPath $manifestPath
ExpectFailure 'path traversal' 'Invalid migration filename' $baseArgs
$originalManifest | Set-Content -LiteralPath $manifestPath
$row = @(Import-Csv -LiteralPath $manifestPath)
($row | Where-Object { $_.file -eq '0050_settlements_audit.sql' }).dependencies = '0099_missing.sql'
$row | Export-Csv -NoTypeInformation -LiteralPath $manifestPath
ExpectFailure 'missing declared dependency' 'Missing dependency' $baseArgs
Write-Output 'VUONG-02 PASS: 10 preflight/guard cases; no mock SQL integration claim.'
