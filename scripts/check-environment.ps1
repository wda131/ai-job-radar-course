$ErrorActionPreference = 'Stop'

$dependencies = [ordered]@{
    MySQL = 3306
    RabbitMQ = 5672
    Redis = 6379
    Nacos = 8848
}
$missing = @()

foreach ($item in $dependencies.GetEnumerator()) {
    $ready = Get-NetTCPConnection -State Listen -LocalPort $item.Value -ErrorAction SilentlyContinue
    if ($ready) {
        Write-Host ("[READY] {0,-9} localhost:{1}" -f $item.Key, $item.Value) -ForegroundColor Green
    } else {
        Write-Host ("[MISSING] {0,-9} localhost:{1}" -f $item.Key, $item.Value) -ForegroundColor Yellow
        $missing += $item.Key
    }
}

if ($missing.Count -gt 0) {
    Write-Host ("Missing dependencies: " + ($missing -join ', ') + '. Core services can start, but the course demo is incomplete.')
    exit 1
}

Write-Host 'All course-project infrastructure dependencies are ready.'
