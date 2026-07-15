$ErrorActionPreference = 'Stop'

$web = (Resolve-Path (Join-Path $PSScriptRoot '..\ai-job-radar-web')).Path
$port = if ($env:FRONTEND_PORT) { $env:FRONTEND_PORT } else { '5174' }
if (-not (Test-Path (Join-Path $web 'node_modules'))) {
    Push-Location $web
    try { npm install } finally { Pop-Location }
}

Start-Process -FilePath 'npm.cmd' -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0', '--port', $port) `
    -WorkingDirectory $web -WindowStyle Hidden
Write-Host "Frontend is starting at http://127.0.0.1:$port"
