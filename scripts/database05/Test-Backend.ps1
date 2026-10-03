param([switch]$LocalPostgreSQL, [string]$Tests, [switch]$Offline)
. (Join-Path $PSScriptRoot 'environment.ps1')
$env:PALCO_TEST_DATABASE05_PATH = Join-Path $taskProjectRoot 'database05\palco-database'
if ($LocalPostgreSQL) {
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { throw 'Defina DB_PASSWORD para os testes locais.' }
    $taskDatabaseUri = [Uri]($env:DB_URL.Substring('jdbc:'.Length))
    $taskPort = if ($taskDatabaseUri.Port -lt 0) { 5432 } else { $taskDatabaseUri.Port }
    $env:PALCO_TEST_LOCAL_POSTGRES_URL = 'jdbc:postgresql://' + $taskDatabaseUri.Host + ':' + $taskPort + '/postgres'
    $env:PALCO_TEST_LOCAL_POSTGRES_USER = $env:DB_USER
    $env:PALCO_TEST_LOCAL_POSTGRES_PASSWORD = $env:DB_PASSWORD
}
$taskArguments = @('test')
if ($Tests) { $taskArguments += '-Dtest=' + $Tests }
if ($Offline) { $taskArguments = @('-o') + $taskArguments }
Push-Location (Join-Path $taskProjectRoot 'backend')
try { & .\mvnw.cmd @taskArguments; $taskExit = $LASTEXITCODE } finally { Pop-Location }
exit $taskExit
