$ErrorActionPreference = 'Stop'

if (Get-NetTCPConnection -State Listen -LocalPort 8848 -ErrorAction SilentlyContinue) {
    Write-Host '[READY] Nacos is already listening on localhost:8848.' -ForegroundColor Green
    exit 0
}

$nacosHome = $env:NACOS_HOME
if (-not $nacosHome) {
    $nacosHome = [Environment]::GetEnvironmentVariable('NACOS_HOME', 'User')
}
if (-not $nacosHome) {
    $nacosHome = Join-Path $env:USERPROFILE 'Applications\nacos-2.1.0\nacos'
}

$startup = Join-Path $nacosHome 'bin\startup.cmd'
if (-not (Test-Path -LiteralPath $startup)) {
    throw 'Nacos 2.1.0 was not found. Set NACOS_HOME to the extracted Nacos directory.'
}

$logs = Join-Path $nacosHome 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', 'call', ('"' + $startup + '"'), '-m', 'standalone') `
    -WorkingDirectory (Join-Path $nacosHome 'bin') -WindowStyle Hidden | Out-Null

for ($attempt = 0; $attempt -lt 45; $attempt++) {
    if (Get-NetTCPConnection -State Listen -LocalPort 8848 -ErrorAction SilentlyContinue) {
        Write-Host '[READY] Nacos started in standalone mode on localhost:8848.' -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Seconds 1
}

throw "Nacos did not start in time. Check $logs."
