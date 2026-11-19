param(
    [int]$Port = 6380
)

$ErrorActionPreference = 'Stop'
$redisDist = 'D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service'
$conf = Join-Path $redisDist 'redis-laminar.conf'
$data = 'D:\ideaWork\redis8-service\data'

function Test-RedisPort {
    try {
        if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { return $true }
    } catch {
        # 非管理员会话可能无法读取连接表，继续用 TCP 探测
    }
    return [bool](Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue)
}

if (Test-RedisPort) {
    Write-Host "[OK] Redis 已在运行：127.0.0.1:$Port" -ForegroundColor Green
    exit 0
}

# 优先使用已注册的 Redis8 服务（生产环境应使用服务，开机自动启动）
$service = Get-Service -Name 'Redis8' -ErrorAction SilentlyContinue
if ($service) {
    if ($service.Status -ne 'Running') {
        Write-Host "[INFO] 启动 Redis8 服务..." -ForegroundColor Yellow
        Start-Service -Name 'Redis8'
    }
} else {
    # 未安装服务时，用项目自带的 RedisService 前台模式兜底，方便本机/试点启动
    $exe = Join-Path $redisDist 'RedisService.exe'
    if (!(Test-Path $exe) -or !(Test-Path $conf)) {
        throw "Redis8 未安装且找不到 $exe 或配置文件 $conf"
    }
    Write-Host "[INFO] Redis8 服务未安装，启动 RedisService 前台进程（端口 $Port）..." -ForegroundColor Yellow
    Start-Process -FilePath $exe `
        -ArgumentList @('run', '--foreground', '-c', $conf, '--dir', $data, '--port', "$Port") `
        -WorkingDirectory $redisDist -WindowStyle Hidden
}

for ($i = 0; $i -lt 30; $i++) {
    if (Test-RedisPort) {
        Write-Host "[OK] Redis 已启动：127.0.0.1:$Port" -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Seconds 1
}

throw "Redis 启动失败：端口 $Port 未监听。请检查 $redisDist 和配置文件。"
