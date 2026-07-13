$ErrorActionPreference = 'Stop'

$parent = (Resolve-Path (Join-Path $PSScriptRoot '..\ai-job-radar-parent')).Path
$services = @('user-service', 'job-service', 'match-service', 'application-service', 'interview-service', 'gateway')

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw '未找到 java，请先配置 JDK 17 和 JAVA_HOME。'
}

foreach ($service in $services) {
    $jar = Get-ChildItem (Join-Path $parent "$service\target") -Filter "$service-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $jar) {
        throw "未找到 $service 的 JAR，请先在 ai-job-radar-parent 执行 mvn clean package。"
    }
    $quotedJar = '"' + $jar.FullName + '"'
    Start-Process -FilePath 'java' -ArgumentList '-jar', $quotedJar -WorkingDirectory $parent -WindowStyle Hidden
}

Write-Host '后端服务已启动，请在 Nacos 控制台确认 gateway 和五个业务服务均为健康状态。'
