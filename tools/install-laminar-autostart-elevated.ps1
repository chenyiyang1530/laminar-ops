param(
    [string]$TaskName = 'LaminarOpsBackend'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$root = 'D:\ideaWork\laminar-ops'
$pwsh = 'C:\Program Files\PowerShell\7\pwsh.exe'
$wrapper = Join-Path $root 'tools\run-backend-autostart.ps1'

$principalCheck = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principalCheck.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw '请以管理员身份运行此脚本'
}
if (!(Test-Path $wrapper)) { throw "找不到 $wrapper" }

$action = New-ScheduledTaskAction -Execute $pwsh `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$wrapper`"" `
    -WorkingDirectory $root
$trigger = New-ScheduledTaskTrigger -AtStartup
$settings = New-ScheduledTaskSettingsSet `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 1) `
    -ExecutionTimeLimit ([TimeSpan]::Zero) `
    -MultipleInstances IgnoreNew `
    -StartWhenAvailable `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger `
    -Settings $settings -Principal $principal -Description 'Laminar Ops backend autostart and restart' -Force | Out-Null

$task = Get-ScheduledTask -TaskName $TaskName
Write-Host "任务已安装：$($task.TaskName) 状态=$($task.State)"
Write-Host "触发器：系统启动；运行身份：SYSTEM；失败重启：3 次，间隔 1 分钟"
