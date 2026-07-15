$ErrorActionPreference = "Stop"

$project = Split-Path -Parent $PSScriptRoot
$importer = Join-Path $project "job-importer"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw "Node.js was not found. Install the course Node.js environment first."
}

if (-not (Test-Path (Join-Path $importer "node_modules"))) {
    & npm.cmd --prefix $importer install
    if ($LASTEXITCODE -ne 0) { throw "Failed to install job-importer dependencies." }
    & npx.cmd --prefix $importer playwright install chromium
    if ($LASTEXITCODE -ne 0) { throw "Failed to install Playwright Chromium." }
}

$gateway = Get-NetTCPConnection -State Listen -LocalPort 9000 -ErrorAction SilentlyContinue
if (-not $gateway) {
    throw "Gateway port 9000 is not listening. Run scripts\start-backend.ps1 first."
}

& npm.cmd --prefix $importer run import:boss -- @args
exit $LASTEXITCODE
