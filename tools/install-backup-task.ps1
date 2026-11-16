param(
  [string]$TaskName = "LaminarOpsBackup",
  [string]$At = "02:00",
  [string]$BackupDir = "D:\backups\laminar-ops"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$script = Join-Path $PSScriptRoot "backup-scheduled.ps1"
if (-not (Test-Path $script)) { throw "找不到备份脚本：$script" }

$pwsh = "C:\Program Files\PowerShell\7\pwsh.exe"
if (-not (Test-Path $pwsh)) { $pwsh = "powershell.exe" }

try {
  $action = New-ScheduledTaskAction -Execute $pwsh `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$script`" -BackupDir `"$BackupDir`""
  $trigger = New-ScheduledTaskTrigger -Daily -At $At
  $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable `
    -DontStopIfGoingOnBatteries -AllowStartIfOnBatteries
  Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger `
    -Settings $settings -User $env:USERNAME -Description "laminar-ops MySQL 与上传文件每日备份" -Force | Out-Null
  Write-Host "定时备份已创建：$TaskName，每天 $At" -ForegroundColor Green
  Write-Host "备份目录：$BackupDir"
  Write-Host "请确认当前用户环境变量 DB_PASSWORD 已设置，任务才能读取数据库密码。"
} catch {
  Write-Host "创建计划任务失败：$($_.Exception.Message)" -ForegroundColor Red
  Write-Host "请在“以管理员身份运行”的 PowerShell 7 中执行本脚本。" -ForegroundColor Yellow
  exit 1
}
