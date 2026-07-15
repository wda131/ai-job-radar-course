$ErrorActionPreference = "Stop"

$project = Split-Path -Parent $PSScriptRoot
$importer = Join-Path $project "job-importer"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw "Node.js was not found."
}

if (-not (Test-Path (Join-Path $importer "node_modules"))) {
    & npm.cmd --prefix $importer install
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to install job-importer dependencies."
    }
}

if (-not (Get-NetTCPConnection -State Listen -LocalPort 9000 -ErrorAction SilentlyContinue)) {
    throw "Gateway port 9000 is not listening."
}

if (-not $env:RADAR_USERNAME -or -not $env:RADAR_PASSWORD) {
    throw "Set RADAR_USERNAME and RADAR_PASSWORD first."
}

$env:API_BASE_URL = if ($env:API_BASE_URL) { $env:API_BASE_URL } else { "http://127.0.0.1:9000" }
& npm.cmd --prefix $importer run bridge:boss
exit $LASTEXITCODE
