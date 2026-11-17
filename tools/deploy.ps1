param(
  [Parameter(Mandatory = $true)][string]$JarPath,
  [Parameter(Mandatory = $true)][string]$AdminDist,
  [Parameter(Mandatory = $true)][string]$PortalDist,
  [int]$Port = 8080,
  [string]$BackupRoot = "D:\deploy-backup\laminar-ops"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root = "D:\ideaWork\laminar-ops"
$secretsFile = Join-Path $root '.secrets.local'
if (Test-Path $secretsFile) {
  Get-Content $secretsFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
      $parts = $line.Split('=', 2)
      if (-not [Environment]::GetEnvironmentVariable($parts[0])) {
        [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
      }
    }
  }
}
$backend = Join-Path $root "backend"
$web = Join-Path $root "web-admin"
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backup = Join-Path $BackupRoot $stamp
New-Item -ItemType Directory -Force -Path $backup | Out-Null

function Get-BackendPid {
  $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  return $conn.OwningProcess
}

function Wait-Health {
  param([int]$Seconds = 60)
  for ($i = 0; $i -lt $Seconds; $i++) {
    try {
      $res = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/actuator/health" -TimeoutSec 2 -UseBasicParsing
      if ($res.StatusCode -eq 200) { return $true }
    } catch { }
    Start-Sleep -Seconds 2
  }
  return $false
}

function Copy-Dist($source, $target) {
  if (Test-Path $target) { Remove-Item $target -Recurse -Force }
  Copy-Item $source $target -Recurse -Force
}

function Disable-LegacyRedis {
  $appRedisPort = if ($env:REDIS_PORT) { [int]$env:REDIS_PORT } else { 6379 }
  if ($appRedisPort -eq 6379) {
    Write-Host "[deploy] 应用配置使用 6379，跳过禁用旧 Redis 3" -ForegroundColor Yellow
    return
  }
  $redis8 = Get-Service -Name Redis8 -ErrorAction SilentlyContinue
  $appRedisListening = Test-NetConnection -ComputerName 127.0.0.1 -Port $appRedisPort -InformationLevel Quiet -WarningAction SilentlyContinue
  if (-not $appRedisListening) {
    Write-Host "[deploy] Redis $appRedisPort 未监听，跳过禁用旧 Redis 3" -ForegroundColor Yellow
    return
  }
  $legacy = Get-Service -Name Redis -ErrorAction SilentlyContinue
  if (-not $legacy) {
    Write-Host "[deploy] 未发现旧 Redis 3 服务"
    return
  }
  try {
    Write-Host "[deploy] 检测到旧 Redis 3，应用已使用 Redis $appRedisPort，正在停止并禁用旧服务..."
    Stop-Service -Name Redis -Force
    Set-Service -Name Redis -StartupType Disabled
    sc.exe config Redis start= disabled | Out-Null
    Write-Host "[deploy] 旧 Redis 3 已停止并禁用" -ForegroundColor Green
  } catch {
    Write-Warning "[deploy] 旧 Redis 3 停止失败（需要管理员权限）：$($_.Exception.Message)"
    if ($redis8) { Write-Host "[deploy] 请确认 Redis8 服务状态：$($redis8.Status)" -ForegroundColor Yellow }
  }
}

Disable-LegacyRedis

$oldPid = Get-BackendPid
if ($oldPid) {
  Write-Host "[deploy] 备份运行中的 jar/dist ..."
  Copy-Item (Join-Path $backend "target\laminar-ops-1.0.0.jar") (Join-Path $backup "laminar-ops.jar") -Force
  Copy-Item (Join-Path $web "dist") (Join-Path $backup "dist") -Recurse -Force
  Copy-Item (Join-Path $web "dist-portal") (Join-Path $backup "dist-portal") -Recurse -Force
  Stop-Process -Id $oldPid -Force
  Start-Sleep -Seconds 2
}

try {
  Write-Host "[deploy] 复制新版本 ..."
  Copy-Item $JarPath (Join-Path $backend "target\laminar-ops-1.0.0.jar") -Force
  Copy-Dist $AdminDist (Join-Path $web "dist")
  Copy-Dist $PortalDist (Join-Path $web "dist-portal")

  $log = Join-Path $backend "deploy-app.log"
  $proc = Start-Process -FilePath "java" -ArgumentList @("-jar", "target\laminar-ops-1.0.0.jar", "--server.port=$Port") `
    -WorkingDirectory $backend -RedirectStandardOutput $log -RedirectStandardError "$log.err" -PassThru
  Set-Content -Path (Join-Path $backend ".deploy.pid") -Value $proc.Id

  if (-not (Wait-Health)) { throw "新版本健康检查失败，请查看 $log" }
  Write-Host "[deploy] 部署成功，PID=$($proc.Id)" -ForegroundColor Green
} catch {
  Write-Host "[deploy] 部署失败，开始回滚：$($_.Exception.Message)" -ForegroundColor Red
  $newPid = Get-BackendPid
  if ($newPid) { Stop-Process -Id $newPid -Force -ErrorAction SilentlyContinue }
  if (Test-Path (Join-Path $backup "laminar-ops.jar")) {
    Copy-Item (Join-Path $backup "laminar-ops.jar") (Join-Path $backend "target\laminar-ops-1.0.0.jar") -Force
    if (Test-Path (Join-Path $backup "dist")) { Copy-Dist (Join-Path $backup "dist") (Join-Path $web "dist") }
    if (Test-Path (Join-Path $backup "dist-portal")) { Copy-Dist (Join-Path $backup "dist-portal") (Join-Path $web "dist-portal") }
    $proc = Start-Process -FilePath "java" -ArgumentList @("-jar", "target\laminar-ops-1.0.0.jar", "--server.port=$Port") `
      -WorkingDirectory $backend -RedirectStandardOutput (Join-Path $backend "rollback-app.log") `
      -RedirectStandardError (Join-Path $backend "rollback-app.log.err") -PassThru
    if (Wait-Health) { Write-Host "[deploy] 已回滚到上一版本" -ForegroundColor Yellow }
  }
  throw
}
