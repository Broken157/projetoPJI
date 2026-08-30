[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$frontendDirectory = Join-Path $projectRoot 'frontend'
$backendDirectory = Join-Path $projectRoot 'backend'
$frontendIndex = Join-Path $frontendDirectory 'build\index.html'

$nodeCommand = Get-Command node -ErrorAction SilentlyContinue
$npmCommand = Get-Command npm.cmd -ErrorAction SilentlyContinue
$javaCommand = Get-Command java -ErrorAction SilentlyContinue

if ($null -eq $nodeCommand -or $null -eq $npmCommand) {
    throw 'Node.js 22.x e npm nao foram encontrados no PATH.'
}

$nodeVersion = (& $nodeCommand.Source --version).Trim()
if ($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch '^v22\.') {
    throw "Node.js 22.x e obrigatorio. Versao encontrada: $nodeVersion"
}

if ($null -eq $javaCommand) {
    throw 'JDK 21 nao foi encontrado no PATH.'
}

$javaVersionLine = (& $javaCommand.Source -version 2>&1 | Select-Object -First 1)
if ($LASTEXITCODE -ne 0 -or $javaVersionLine -notmatch 'version "21[\.]') {
    throw "JDK 21 e obrigatorio. Versao encontrada: $javaVersionLine"
}

Push-Location $frontendDirectory
try {
    & $npmCommand.Source ci
    if ($LASTEXITCODE -ne 0) {
        throw 'npm ci falhou.'
    }

    & $npmCommand.Source run build
    if ($LASTEXITCODE -ne 0) {
        throw 'npm run build falhou.'
    }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $frontendIndex -PathType Leaf)) {
    throw 'O build React terminou sem gerar frontend/build/index.html.'
}

Push-Location $backendDirectory
try {
    & '.\mvnw.cmd' clean package
    if ($LASTEXITCODE -ne 0) {
        throw 'mvn package falhou.'
    }
} finally {
    Pop-Location
}
