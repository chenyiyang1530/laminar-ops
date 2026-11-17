$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root = "D:\ideaWork\laminar-ops"
$backend = Join-Path $root "backend"
$jar = Join-Path $backend "target\laminar-ops-1.0.0.jar"
$secretFile = Join-Path $root ".secrets.local"
$log = Join-Path $backend "app-current.log"

if (-not (Test-Path $jar)) { throw "找不到后端 jar：$jar，请先执行 mvn package" }
if (-not (Test-Path $secretFile)) { throw "找不到 $secretFile" }

$cfg = @{}
Get-Content $secretFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $parts = $line.Split('=', 2)
    $cfg[$parts[0]] = $parts[1]
  }
}
if (-not $cfg['DB_PASSWORD']) { throw "缺少 DB_PASSWORD" }

$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/laminar_ops?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = $cfg['DB_USERNAME']
$env:DB_PASSWORD = $cfg['DB_PASSWORD']
foreach ($key in @('REDIS_HOST','REDIS_PORT','REDIS_USERNAME','REDIS_PASSWORD','JWT_SECRET','CONFIG_SECRET','FILE_SIGN_SECRET','MONITOR_TOKEN')) {
  if ($cfg[$key]) { Set-Item -Path "Env:$key" -Value $cfg[$key] }
}

$health = "http://127.0.0.1:8080/actuator/health"
try {
  $response = Invoke-WebRequest -Uri $health -TimeoutSec 3 -UseBasicParsing
  if ($response.StatusCode -eq 200) {
    Write-Host "后端已经在运行：$health" -ForegroundColor Green
    exit 0
  }
} catch { }

if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
  throw "8080 已被占用但健康检查失败，请先查看占用进程或停止旧进程"
}

if (-not (Get-NetTCPConnection -LocalPort 6380 -State Listen -ErrorAction SilentlyContinue)) {
  Write-Host "Redis8 未运行，请先启动 Redis8（6380）" -ForegroundColor Yellow
  exit 1
}

$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
$proc = Start-Process -FilePath $java `
  -ArgumentList @("-jar", "target\laminar-ops-1.0.0.jar", "--server.port=8080") `
  -WorkingDirectory $backend `
  -RedirectStandardOutput $log `
  -RedirectStandardError (Join-Path $backend "app-current.err") `
  -PassThru

Write-Host "后端启动中，PID=$($proc.Id)，日志：$log"
for ($i = 0; $i -lt 45; $i++) {
  Start-Sleep -Seconds 2
  try {
    $response = Invoke-WebRequest -Uri $health -TimeoutSec 2 -UseBasicParsing
    if ($response.StatusCode -eq 200) {
      Write-Host "后端启动成功：$health" -ForegroundColor Green
      exit 0
    }
  } catch { }
}

throw "后端启动失败，请查看 $log 和 app-current.err"
