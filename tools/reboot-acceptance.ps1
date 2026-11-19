[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$report = Join-Path $logDir ('reboot-acceptance-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.txt')
$lines = [System.Collections.Generic.List[string]]::new()

function Add-Result($name, $ok, $detail) {
    $state = if ($ok) { 'PASS' } else { 'FAIL' }
    $lines.Add("[$state] $name : $detail")
}

function Test-Port($port) {
    try { return [bool](Test-NetConnection -ComputerName 127.0.0.1 -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue) } catch { return $false }
}

$redis = Get-Service Redis8 -ErrorAction SilentlyContinue
Add-Result 'Redis8 服务' ($null -ne $redis -and $redis.Status -eq 'Running') "status=$($redis.Status), startType=$($redis.StartType)"
Add-Result 'Redis8 端口 6380' (Test-Port 6380) 'TCP 6380'
Add-Result '旧 Redis 6379 已停用' (-not (Test-Port 6379)) 'TCP 6379'

$task = $null
try { $task = Get-ScheduledTask -TaskName 'LaminarOpsBackend' -ErrorAction Stop } catch {
    try { schtasks /Query /TN LaminarOpsBackend /FO LIST 2>$null | Out-Null; $task = 'schtasks-ok' } catch { $task = $null }
}
Add-Result '后端启动任务' ($null -ne $task) 'LaminarOpsBackend'

$health = $null
try { $health = Invoke-WebRequest 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 10 -SkipHttpErrorCheck } catch {}
Add-Result '应用健康检查' ($null -ne $health -and $health.StatusCode -eq 200) "HTTP $($health.StatusCode)"

$body = $health.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
$redisComponent = $body.components.redisLaminar.status
Add-Result 'Redis 应用级健康' ($redisComponent -eq 'UP') "redisLaminar=$redisComponent"

$ok = ($lines | Where-Object { $_ -match '^\[FAIL\]' }).Count -eq 0
$lines.Insert(0, "服务器重启验收：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$lines.Insert(1, "结果：$(if ($ok) { 'PASS' } else { 'FAIL' })")
$lines | Set-Content $report -Encoding UTF8
$lines | ForEach-Object { Write-Host $_ }
Write-Host "报告：$report"
if (-not $ok) { exit 1 }
