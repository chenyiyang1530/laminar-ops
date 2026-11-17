$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$log = "D:\ideaWork\laminar-ops\redis3-disable.log"

try {
  Stop-Service Redis -Force
  Set-Service Redis -StartupType Disabled
  $svc = Get-Service Redis
  Add-Content $log "NAME=$($svc.Name) STATUS=$($svc.Status) STARTTYPE=$($svc.StartType)"
  if ($svc.Status -ne "Stopped") { throw "Redis 3 未停止" }
  if ($svc.StartType -ne "Disabled") { throw "Redis 3 未禁用" }
  Add-Content $log "SUCCESS"
  exit 0
} catch {
  Add-Content $log "ERROR=$($_.Exception.Message)"
  exit 1
}
