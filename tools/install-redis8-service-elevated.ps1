$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$dir = "D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service"
$conf = Join-Path $dir "redis-laminar.conf"
$data = "D:\ideaWork\redis8-service\data"
$log = "D:\ideaWork\redis8-service\install-service.log"

try {
  $conn = Get-NetTCPConnection -LocalPort 6380 -State Listen -ErrorAction SilentlyContinue
  foreach ($c in $conn) { Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue }
  Get-Process RedisService -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
  Start-Sleep -Seconds 2

  & (Join-Path $dir "RedisService.exe") install -c $conf --dir $data --port 6380 --service-name Redis8 --start-mode auto *> $log
  if ($LASTEXITCODE -ne 0) { throw "RedisService install 失败，退出码 $LASTEXITCODE，详见 $log" }
  Start-Service Redis8
  Start-Sleep -Seconds 3
  $svc = Get-Service Redis8
  Add-Content $log "SERVICE=$($svc.Name) STATUS=$($svc.Status) STARTTYPE=$($svc.StartType)"
  if ($svc.Status -ne "Running") { throw "Redis8 未运行" }
  $tcp = Get-NetTCPConnection -LocalPort 6380 -State Listen -ErrorAction SilentlyContinue
  if (-not $tcp) { throw "6380 未监听" }
  Add-Content $log "PORT_6380_LISTENING"
} catch {
  Add-Content $log "ERROR=$($_.Exception.Message)"
  throw
}
