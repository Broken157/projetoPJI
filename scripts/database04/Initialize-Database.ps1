param(
    [ValidatePattern('^palco_dev_manu04(?:_[a-z0-9_]+)?$')][string]$DatabaseName = 'palco_dev_manu04',
    [ValidateSet('localhost', '127.0.0.1')][string]$DatabaseHost = 'localhost',
    [ValidateRange(1,65535)][int]$Port = 5432,
    [string]$PsqlPath = (Join-Path $env:ProgramFiles 'PostgreSQL\18\bin\psql.exe')
)
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskLocalEnvironment = Join-Path $taskProjectRoot 'backend\.env.database04.local.ps1'
if (Test-Path -LiteralPath $taskLocalEnvironment) {
    $taskExistingEnvironment = @{}
    foreach ($taskVariable in @('DB_URL','DB_USER','DB_PASSWORD','JWT_SECRET')) {
        $taskValue = [Environment]::GetEnvironmentVariable($taskVariable)
        if (-not [string]::IsNullOrWhiteSpace($taskValue)) { $taskExistingEnvironment[$taskVariable] = $taskValue }
    }
    . $taskLocalEnvironment
    foreach ($taskVariable in $taskExistingEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($taskVariable, $taskExistingEnvironment[$taskVariable], 'Process')
    }
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) { throw 'Defina DB_PASSWORD no ambiente.' }
if ([string]::IsNullOrWhiteSpace($env:DB_USER)) { $env:DB_USER = 'postgres' }
$taskSnapshot = Join-Path $taskProjectRoot 'database04\palco-database'
$taskManifest = ConvertFrom-StringData ([IO.File]::ReadAllText((Join-Path $taskProjectRoot 'database04\snapshot.properties')))
if ($taskManifest['source.sha256'] -ne '52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b') {
    throw 'Identidade do snapshot invalida.'
}
$taskFiles = @(Get-ChildItem -LiteralPath $taskSnapshot -Recurse -File)
if ($taskFiles.Count -ne 46) { throw 'O pacote database04 deve conter os 46 arquivos completos.' }
foreach ($taskFile in $taskFiles) {
    $taskRelative = [IO.Path]::GetRelativePath($taskSnapshot, $taskFile.FullName).Replace('\','/')
    if ((Get-FileHash -LiteralPath $taskFile.FullName).Hash.ToLowerInvariant() -ne $taskManifest['file.sha256.' + $taskRelative]) {
        throw "Integridade do snapshot diverge: $taskRelative"
    }
}
[string[]]$taskRelativePaths = @($taskFiles | ForEach-Object { [IO.Path]::GetRelativePath($taskSnapshot, $_.FullName).Replace('\','/') })
[Array]::Sort($taskRelativePaths, [StringComparer]::Ordinal)
$taskCanonical = [Text.StringBuilder]::new()
foreach ($taskRelative in $taskRelativePaths) {
    $taskHash = (Get-FileHash -LiteralPath (Join-Path $taskSnapshot $taskRelative)).Hash.ToLowerInvariant()
    [void]$taskCanonical.Append($taskRelative + "`t" + $taskHash + "`n")
}
$taskFingerprint = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($taskCanonical.ToString()))).ToLowerInvariant()
if ($taskFingerprint -ne 'db8f05cabadfd3935b7c703b7b21252f170fa529c0d30e7e61755b46d7fd5f39') {
    throw 'Fingerprint do pacote completo diverge do database04 oficial sem alteracao SQL.'
}
$taskPreviousPassword = $env:PGPASSWORD
$env:PGPASSWORD = $env:DB_PASSWORD
$taskConnection = @('-X', '--no-password', '-h', $DatabaseHost, '-p', "$Port", '-U', $env:DB_USER)
try {
    $taskVersion = & $PsqlPath @taskConnection -d postgres -At -c 'show server_version_num'
    if ($LASTEXITCODE -ne 0 -or [int]$taskVersion -lt 180000) { throw 'PostgreSQL 18 ou superior indisponivel.' }
    $taskExists = & $PsqlPath @taskConnection -d postgres -At -c "select count(*) from pg_database where datname='$DatabaseName'"
    if ($LASTEXITCODE -ne 0 -or $taskExists.Trim() -ne '0') { throw "Banco $DatabaseName ja existe; nenhuma inicializacao executada." }
    & $PsqlPath @taskConnection -d postgres -v ON_ERROR_STOP=1 -c "create database $DatabaseName template template0 encoding 'UTF8'"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar banco novo.' }
    $taskLogDirectory = Join-Path $taskProjectRoot 'backend\target-maven\database04-init'
    New-Item -ItemType Directory -Force -Path $taskLogDirectory | Out-Null
    $taskLog = Join-Path $taskLogDirectory ($DatabaseName + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
    Push-Location $taskSnapshot
    try {
        & $PsqlPath @taskConnection -d $DatabaseName -a -v ON_ERROR_STOP=1 -v VERBOSITY=verbose -f init.sql *> $taskLog
        $taskInitExit = $LASTEXITCODE
    } finally { Pop-Location }
    Write-Output "init.sql codigo $taskInitExit; log completo: $taskLog"
    if ($taskInitExit -ne 0) { throw 'Inicializacao incompleta; nenhum SQL adicional sera aplicado.' }
} finally { $env:PGPASSWORD = $taskPreviousPassword }
