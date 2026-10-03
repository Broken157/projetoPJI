param([switch]$Offline)
. (Join-Path $PSScriptRoot 'environment.ps1') -RequireDatabaseCredentials -RequireJwtSecret
$taskArguments = @('-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false',
    '-Dspring-boot.run.arguments=--spring.profiles.active=banco-oficial-local', 'spring-boot:run')
if ($Offline) { $taskArguments = @('-o') + $taskArguments }
Push-Location (Join-Path $taskProjectRoot 'backend')
try { & .\mvnw.cmd @taskArguments; $taskExit = $LASTEXITCODE } finally { Pop-Location }
exit $taskExit
