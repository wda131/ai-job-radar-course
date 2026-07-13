$ErrorActionPreference = 'Stop'

$web = (Resolve-Path (Join-Path $PSScriptRoot '..\ai-job-radar-web')).Path
if (-not (Test-Path (Join-Path $web 'node_modules'))) {
    Push-Location $web
    try { npm install } finally { Pop-Location }
}

Start-Process -FilePath 'npm.cmd' -ArgumentList 'run', 'dev' -WorkingDirectory $web -WindowStyle Hidden
Write-Host '前端已启动，请访问 http://localhost:5173。'
