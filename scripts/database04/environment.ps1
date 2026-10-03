param([switch]$RequireDatabaseCredentials, [switch]$RequireJwtSecret)
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
$taskRequiredVariables = @()
if ($RequireDatabaseCredentials) { $taskRequiredVariables += 'DB_PASSWORD' }
if ($RequireJwtSecret) { $taskRequiredVariables += 'JWT_SECRET' }
foreach ($taskVariable in $taskRequiredVariables) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($taskVariable))) {
        throw "Defina $taskVariable no ambiente ou em backend/.env.database04.local.ps1 (ignorado pelo Git)."
    }
}
if ([string]::IsNullOrWhiteSpace($env:DB_URL)) { $env:DB_URL = 'jdbc:postgresql://localhost:5432/palco_dev_manu04' }
if ([string]::IsNullOrWhiteSpace($env:DB_USER)) { $env:DB_USER = 'postgres' }
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $taskJdk = Get-ChildItem -LiteralPath (Join-Path $env:ProgramFiles 'Java') -Directory |
        Where-Object { $_.Name -like 'jdk-21*' } | Sort-Object Name -Descending | Select-Object -First 1
    if ($null -eq $taskJdk) { throw 'Defina JAVA_HOME para um JDK 21 instalado.' }
    $env:JAVA_HOME = $taskJdk.FullName
}
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
