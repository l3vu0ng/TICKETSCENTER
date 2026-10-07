[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Server,
    [Parameter(Mandatory)][string]$Database,
    [ValidateSet('Local','Azure')][string]$Environment = 'Local',
    [string]$TestLoginPrefix = '',
    [switch]$TrustLocalCertificate
)
$ErrorActionPreference = 'Stop'
if ($Environment -eq 'Azure') { throw 'Azure provisioning BLOCKED: provide the approved Azure authentication caller for azure-users.sql.' }
if ($Database -notmatch '^TicketsCenter_Test_[A-Za-z0-9_]+$') { throw 'Dedicated test database required.' }
if ($TestLoginPrefix -and $TestLoginPrefix -notmatch '^tc_vuongtest_[0-9a-f]{12}_$') { throw 'Invalid isolated login prefix.' }
if ($TrustLocalCertificate -and ($Environment -ne 'Local' -or $Server -notmatch '^(localhost|127\.0\.0\.1|\.)$')) {
    throw 'Certificate override allowed only for localhost.'
}
$identities = @(
    @{ login='tc_buyer_login'; user='tc_buyer_user'; role='tc_buyer'; secret='TC_BUYER_PASSWORD' },
    @{ login='tc_manager_login'; user='tc_manager_user'; role='tc_manager'; secret='TC_MANAGER_PASSWORD' },
    @{ login='tc_checkin_login'; user='tc_checkin_user'; role='tc_checkin'; secret='TC_CHECKIN_PASSWORD' },
    @{ login='tc_admin_login'; user='tc_admin_user'; role='tc_platform_admin'; secret='TC_ADMIN_PASSWORD' }
)
# Fail before any mutation when even one required secret is absent.
foreach ($identity in $identities) {
    $password = [Environment]::GetEnvironmentVariable($identity.secret)
    if ([string]::IsNullOrWhiteSpace($password) -or $password.Length -gt 128) { throw "Required secret unavailable/invalid: $($identity.secret)" }
}
$builder = [System.Data.SqlClient.SqlConnectionStringBuilder]::new()
$builder.set_DataSource($Server)
$builder.set_InitialCatalog($Database)
$builder.set_IntegratedSecurity($true)
$builder.set_Encrypt($true)
$builder.set_TrustServerCertificate([bool]$TrustLocalCertificate)
$builder.set_ConnectTimeout(5)
$connection = [System.Data.SqlClient.SqlConnection]::new($builder.get_ConnectionString())
$script = [IO.File]::ReadAllText((Join-Path $PSScriptRoot $(if ($Environment -eq 'Local') { 'local-logins.sql' } else { 'azure-users.sql' })))
try {
    $connection.Open()
    $transaction = $connection.BeginTransaction()
    $completed = @()
    foreach ($identity in $identities) {
        $command = $connection.CreateCommand()
        try {
            $command.CommandTimeout = 30
            $command.Transaction = $transaction
            $command.CommandText = $script
            foreach ($parameter in @('Login','User','Role','Password')) {
                $size = 128
                $null = $command.Parameters.Add('@'+$parameter, [Data.SqlDbType]::NVarChar, $size)
            }
            $command.Parameters['@Login'].Value = $TestLoginPrefix + $identity.login
            $command.Parameters['@User'].Value = $identity.user
            $command.Parameters['@Role'].Value = $identity.role
            $command.Parameters['@Password'].Value = [Environment]::GetEnvironmentVariable($identity.secret)
            $null = $command.ExecuteNonQuery()
            $completed += "PROVISIONED $($identity.user) -> $($identity.role) ($Environment)"
        } catch { throw "Provisioning failed for $($identity.user); credentials and SQL details suppressed." }
        finally { $command.Dispose() }
    }
    $transaction.Commit()
    $completed | Write-Output
} catch {
    if ($transaction) { try { $transaction.Rollback() } catch { } }
    throw
} finally {
    $password = $null
    if ($transaction) { $transaction.Dispose() }
    $connection.Dispose()
}
