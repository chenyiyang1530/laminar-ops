param(
  [int]$Port = 6380,
  [int]$IntervalSeconds = 30
)

$ErrorActionPreference = "Continue"
$dir = "D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service"
$conf = Join-Path $dir "redis-laminar.conf"
$data = "D:\ideaWork\redis8-service\data"
$log = "D:\ideaWork\redis8-service\watchdog.log"

function Write-WatchdogLog([string]$Message) {
  Add-Content $log ("[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message)
}

Write-WatchdogLog "watchdog started port=$Port interval=$IntervalSeconds"
while ($true) {
  $listening = Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
  if (-not $listening) {
    Write-WatchdogLog "port $Port is down, restarting RedisService"
    try {
      Start-Process -FilePath (Join-Path $dir "RedisService.exe") `
        -ArgumentList @("run", "--foreground", "-c", $conf, "--dir", $data, "--port", "$Port") `
        -WorkingDirectory $dir -WindowStyle Hidden
      Start-Sleep -Seconds 5
    } catch {
      Write-WatchdogLog "restart failed: $($_.Exception.Message)"
    }
  }
  Start-Sleep -Seconds $IntervalSeconds
}
