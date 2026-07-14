$ErrorActionPreference = 'Stop'

$parent = (Resolve-Path (Join-Path $PSScriptRoot '..\ai-job-radar-parent')).Path
$services = @('user-service', 'job-service', 'match-service', 'application-service',
    'interview-service', 'notification-service', 'gateway')
$logs = Join-Path $parent 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java was not found. Configure JDK 17 and JAVA_HOME first.'
}

foreach ($service in $services) {
    $jar = Get-ChildItem (Join-Path $parent "$service\target") -Filter "$service-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $jar) {
        throw "JAR for $service was not found. Run mvn clean package first."
    }
    Start-Process -FilePath 'java' -ArgumentList '-jar', ('"' + $jar.FullName + '"') `
        -WorkingDirectory $parent -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logs "$service.out.log") `
        -RedirectStandardError (Join-Path $logs "$service.err.log")
}

Write-Host 'Backend started: Gateway plus six business services. Logs are under ai-job-radar-parent/logs.'
