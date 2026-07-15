$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker was not found. Install and start Docker Desktop, or use native MySQL, Nacos, Redis, and RabbitMQ.'
}

$composeFile = Join-Path $root 'docker-compose.yml'
docker compose -f $composeFile up -d
if ($LASTEXITCODE -ne 0) {
    throw 'Docker infrastructure failed to start. Run docker compose ps for details.'
}

Write-Host 'Infrastructure is starting. Use docker compose ps to check health.' -ForegroundColor Green
