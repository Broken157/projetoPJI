[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    throw 'Defina JWT_SECRET no ambiente com pelo menos 32 caracteres antes de iniciar.'
}
Push-Location (Join-Path $PSScriptRoot 'backend')
try {
    & '.\mvnw.cmd' '-Dspring-boot.run.profiles=banco-oficial-local' spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar o backend.' }
} finally { Pop-Location }
