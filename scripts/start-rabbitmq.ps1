$ErrorActionPreference = 'Stop'

if (Get-NetTCPConnection -State Listen -LocalPort 5672 -ErrorAction SilentlyContinue) {
    Write-Host '[READY] RabbitMQ is already listening on localhost:5672.' -ForegroundColor Green
    exit 0
}

$erlangHome = $env:ERLANG_HOME
if (-not $erlangHome) {
    $erlangHome = [Environment]::GetEnvironmentVariable('ERLANG_HOME', 'User')
}
if (-not $erlangHome -and (Test-Path -LiteralPath 'C:\Program Files\Erlang OTP\bin\erl.exe')) {
    $erlangHome = 'C:\Program Files\Erlang OTP'
}

$rabbitHome = $env:RABBITMQ_SERVER
if (-not $rabbitHome) {
    $rabbitHome = [Environment]::GetEnvironmentVariable('RABBITMQ_SERVER', 'User')
}
if (-not $rabbitHome -and (Test-Path -LiteralPath 'C:\Users\Administrator\Applications\rabbitmq_server-4.3.2\sbin\rabbitmq-server.bat')) {
    $rabbitHome = 'C:\Users\Administrator\Applications\rabbitmq_server-4.3.2'
}

if (-not $erlangHome -or -not (Test-Path -LiteralPath (Join-Path $erlangHome 'bin\erl.exe'))) {
    throw 'Erlang was not found. Set ERLANG_HOME to the Erlang installation directory.'
}
if (-not $rabbitHome -or -not (Test-Path -LiteralPath (Join-Path $rabbitHome 'sbin\rabbitmq-server.bat'))) {
    throw 'RabbitMQ was not found. Set RABBITMQ_SERVER to the extracted RabbitMQ directory.'
}

$env:ERLANG_HOME = $erlangHome
$env:RABBITMQ_SERVER = $rabbitHome
$env:RABBITMQ_BASE = Join-Path $env:LOCALAPPDATA 'RabbitMQ'
New-Item -ItemType Directory -Force -Path $env:RABBITMQ_BASE | Out-Null

$plugins = Join-Path $rabbitHome 'sbin\rabbitmq-plugins.bat'
& $plugins enable rabbitmq_management | Out-Null

$server = Join-Path $rabbitHome 'sbin\rabbitmq-server.bat'
$stdout = Join-Path $env:RABBITMQ_BASE 'server.stdout.log'
$stderr = Join-Path $env:RABBITMQ_BASE 'server.stderr.log'
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/d', '/c', 'call', ('"' + $server + '"')) `
    -WorkingDirectory (Join-Path $rabbitHome 'sbin') -WindowStyle Hidden `
    -RedirectStandardOutput $stdout -RedirectStandardError $stderr | Out-Null

for ($attempt = 0; $attempt -lt 25; $attempt++) {
    if (Get-NetTCPConnection -State Listen -LocalPort 5672 -ErrorAction SilentlyContinue) {
        Write-Host '[READY] RabbitMQ started on localhost:5672; management UI is on localhost:15672.' -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Seconds 1
}

throw "RabbitMQ did not start in time. Check $stdout and $stderr."
