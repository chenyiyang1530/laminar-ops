param(
  [string]$RedisDist = "D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service",
  [string]$InstallRoot = "D:\laminar-redis",
  [int]$Port = 6380,
  [string]$RedisUser = "laminar_ops",
  [bool]$DisableLegacyRedis = $true
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
  throw "请以管理员身份运行服务器初始化脚本"
}
if (-not $env:REDIS_PASSWORD) {
  throw "请先设置 REDIS_PASSWORD"
}
if (-not (Test-Path (Join-Path $RedisDist "RedisService.exe"))) {
  throw "找不到 RedisService.exe：$RedisDist"
}

$distTarget = Join-Path $InstallRoot "redis"
$dataDir = Join-Path $InstallRoot "data"
$logDir = Join-Path $InstallRoot "logs"
New-Item -ItemType Directory -Force -Path $InstallRoot, $dataDir, $logDir | Out-Null

if (-not (Test-Path (Join-Path $distTarget "RedisService.exe"))) {
  if (Test-Path $distTarget) { Remove-Item $distTarget -Recurse -Force }
  Copy-Item $RedisDist $distTarget -Recurse -Force
}

$aclFile = Join-Path $distTarget "users.acl"
"user default off`nuser $RedisUser on >$env:REDIS_PASSWORD ~* &* +@all" |
  Set-Content $aclFile -Encoding ascii

$confFile = Join-Path $distTarget "redis-laminar.conf"
@"
port $Port
bind 127.0.0.1
protected-mode no
dir $($dataDir.Replace('\','/'))
aclfile $($aclFile.Replace('\','/'))
appendonly yes
save 900 1
logfile $((Join-Path $logDir 'redis.log').Replace('\','/'))
loglevel notice
maxmemory 1gb
maxmemory-policy volatile-lru
maxmemory-samples 5
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-use-rdb-preamble yes
"@ | Set-Content $confFile -Encoding ascii

# 停止可能占用 6380 的前台 Redis，避免服务安装后端口冲突
$listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
foreach ($listener in $listeners) {
  Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue
}
Get-Process RedisService -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

$existing = Get-Service -Name Redis8 -ErrorAction SilentlyContinue
if ($existing) {
  Stop-Service Redis8 -Force -ErrorAction SilentlyContinue
}

$installer = Join-Path $distTarget "RedisService.exe"
if (-not $existing) {
  & $installer install -c $confFile --dir $dataDir --port $Port --service-name Redis8 --start-mode auto
  if ($LASTEXITCODE -ne 0) { throw "Redis8 服务安装失败，退出码 $LASTEXITCODE" }
} else {
  Write-Host "Redis8 服务已存在，更新配置后重启"
}

Start-Service Redis8
sc.exe failure Redis8 reset= 86400 actions= restart/5000/restart/10000/restart/30000 | Out-Null
sc.exe config Redis8 start= auto | Out-Null

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
  if (Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue) {
    $ready = $true; break
  }
  Start-Sleep -Seconds 2
}
if (-not $ready) { throw "Redis8 已安装但端口 $Port 未监听" }

$cli = Join-Path $distTarget "redis-cli.exe"
$pong = & $cli -p $Port --user $RedisUser --pass $env:REDIS_PASSWORD PING 2>$null
if ($pong -ne "PONG") { throw "Redis8 ACL 登录失败" }

if ($DisableLegacyRedis) {
  $legacy = Get-Service -Name Redis -ErrorAction SilentlyContinue
  if ($legacy) {
    Stop-Service -Name Redis -Force
    Set-Service -Name Redis -StartupType Disabled
    sc.exe config Redis start= disabled | Out-Null
    Write-Host "旧 Redis 3 已停止并禁用"
  }
}

Write-Host "Redis8 服务初始化完成：127.0.0.1:$Port" -ForegroundColor Green
Write-Host "配置文件：$confFile"
Write-Host "数据目录：$dataDir"
Write-Host "服务：$(Get-Service Redis8 | Select-Object -ExpandProperty Status)"
