[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root 'backend'
$jar = Join-Path $backend 'target\laminar-ops-1.0.0.jar'
$controlJar = Join-Path $backend 'target\laminar-ops-1.0.0-control.jar'
$jarArg = 'target\laminar-ops-1.0.0.jar'
if (Test-Path $controlJar) {
    $jar = $controlJar
    $jarArg = 'target\laminar-ops-1.0.0-control.jar'
}
$secretFile = Join-Path $root '.secrets.local'
$logDir = Join-Path $root 'logs'
$log = Join-Path $logDir 'autostart-app.log'
$err = Join-Path $logDir 'autostart-app.err'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (!(Test-Path $jar)) { throw "后端 jar 不存在：$jar" }
if (!(Test-Path $secretFile)) { throw "缺少 $secretFile" }

$cfg = @{}
Get-Content $secretFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
        $parts = $line.Split('=', 2)
        $cfg[$parts[0].Trim()] = $parts[1].Trim()
    }
}
foreach ($key in $cfg.Keys) {
    if (-not [Environment]::GetEnvironmentVariable($key, 'Process')) {
        [Environment]::SetEnvironmentVariable($key, $cfg[$key], 'Process')
    }
}

$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'D:\software\jdk17' }
$java = Join-Path $javaHome 'bin\java.exe'
if (!(Test-Path $java)) { $java = 'java' }
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

$redisPort = if ($cfg['REDIS_PORT']) { [int]$cfg['REDIS_PORT'] } else { 6380 }
& (Join-Path $root 'tools\ensure-redis.ps1') -Port $redisPort
if ($LASTEXITCODE -ne 0) { throw 'Redis8 启动失败' }

while ($true) {
    Add-Content $log ("[{0}] starting backend" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    $proc = Start-Process -FilePath $java `
        -ArgumentList @('-jar', $jarArg, '--server.port=8080') `
        -WorkingDirectory $backend `
        -RedirectStandardOutput $log `
        -RedirectStandardError $err `
        -PassThru
    Wait-Process -Id $proc.Id -ErrorAction SilentlyContinue
    Add-Content $log ("[{0}] backend exited, restarting in 10s" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Start-Sleep -Seconds 10
}
