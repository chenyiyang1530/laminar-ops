$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$log = "D:\ideaWork\redis8-service\recovery-config.log"
try {
  $svc = Get-Service Redis8 -ErrorAction Stop
  sc.exe failure Redis8 reset= 86400 actions= restart/5000/restart/10000/restart/30000 *>> $log
  sc.exe config Redis8 start= auto *>> $log
  $q = sc.exe qfailure Redis8
  Add-Content $log "--- qfailure ---"
  Add-Content $log ($q -join "`n")
  if ($svc.StartType -ne "Automatic") { Set-Service Redis8 -StartupType Automatic }
  Write-Host "Redis8 故障恢复已配置：" -ForegroundColor Green
  Write-Host "5秒/10秒/30秒三级自动重启，24小时重置计数"
} catch {
  Add-Content $log "ERROR=$($_.Exception.Message)"
  Write-Host "配置失败：$($_.Exception.Message)" -ForegroundColor Red
  Write-Host "请以管理员身份运行本脚本。" -ForegroundColor Yellow
  exit 1
}
