$ErrorActionPreference = 'Stop'

$dependencies = [ordered]@{
    MySQL = 3306
    RabbitMQ = 5672
    Redis = 6379
    Nacos = 8848
}
$enhancements = [ordered]@{
    Ollama = 11434
    Elasticsearch = 9200
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

foreach ($item in $enhancements.GetEnumerator()) {
    $ready = Get-NetTCPConnection -State Listen -LocalPort $item.Value -ErrorAction SilentlyContinue
    if ($ready) {
        Write-Host ("[ENABLED] {0,-13} localhost:{1}" -f $item.Key, $item.Value) -ForegroundColor Cyan
    } else {
        Write-Host ("[OPTIONAL] {0,-13} localhost:{1}" -f $item.Key, $item.Value) -ForegroundColor DarkYellow
    }
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host '[ENABLED] Docker Compose command is available.' -ForegroundColor Cyan
} else {
    Write-Host '[OPTIONAL] Docker is not installed; IDEA and native infrastructure remain supported.' -ForegroundColor DarkYellow
}

if ($missing.Count -gt 0) {
    Write-Host ("Missing dependencies: " + ($missing -join ', ') + '. Core services can start, but the course demo is incomplete.')
    exit 1
}

Write-Host 'All required course-project infrastructure dependencies are ready.'
