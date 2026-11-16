param(
    [switch]$SkipFrontend,
    [switch]$BuildBackend
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$secretsFile = Join-Path $root '.secrets.local'
if (Test-Path $secretsFile) {
    Get-Content $secretsFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $parts = $line.Split('=', 2)
            if (-not [Environment]::GetEnvironmentVariable($parts[0])) {
                [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
            }
        }
    }
}
$backend = Join-Path $root 'backend'
$web = Join-Path $root 'web-admin'

function Test-TcpPort([int]$port) {
    try {
        return (Test-NetConnection -ComputerName 127.0.0.1 -Port $port -WarningAction SilentlyContinue).TcpTestSucceeded
    } catch {
        return $false
    }
}

Write-Host '==============================================' -ForegroundColor Cyan
Write-Host '  Laminar Ops - one click start' -ForegroundColor Cyan
Write-Host '==============================================' -ForegroundColor Cyan

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host '[ERROR] java not found in PATH. Please install JDK 17 or set JAVA_HOME.' -ForegroundColor Red
    exit 1
}

$missing = @()
if (-not $env:DB_PASSWORD) { $missing += 'DB_PASSWORD' }
if (-not $env:JWT_SECRET) { $missing += 'JWT_SECRET' }
if (-not $env:CONFIG_SECRET) { $missing += 'CONFIG_SECRET' }
if ($missing.Count -gt 0) {
    Write-Host ('[ERROR] 缺少环境变量: ' + ($missing -join ', ')) -ForegroundColor Red
    Write-Host '请参考 .env.example 设置数据库密码、JWT 密钥和敏感参数加密密钥。' -ForegroundColor Yellow
    exit 1
}

if (-not (Test-TcpPort 3306)) {
    Write-Host '[ERROR] MySQL 127.0.0.1:3306 is not reachable. Please start MySQL first.' -ForegroundColor Red
    exit 1
}
if (-not (Test-TcpPort 6379)) {
    Write-Host '[ERROR] Redis 127.0.0.1:6379 is not reachable. Please start Redis first.' -ForegroundColor Red
    exit 1
}
Write-Host '[OK] MySQL 3306 / Redis 6379 are reachable.' -ForegroundColor Green

if (-not $SkipFrontend -and -not (Test-Path (Join-Path $web 'dist\index.html'))) {
    Write-Host '[INFO] Frontend dist not found, building...' -ForegroundColor Yellow
    Push-Location $web
    npm install
    npm run build
    npm run build:portal
    Pop-Location
}

$jar = Join-Path $backend 'target\laminar-ops-1.0.0.jar'
if ($BuildBackend -or -not (Test-Path $jar)) {
    Write-Host '[INFO] Building backend jar...' -ForegroundColor Yellow
    Push-Location $backend
    mvn -DskipTests package
    Pop-Location
}

Write-Host '[INFO] Starting backend: http://127.0.0.1:8080' -ForegroundColor Green
Write-Host '       Admin:  http://127.0.0.1:8080/app/login    (账号见初始化说明，首次登录必须改密)' -ForegroundColor Gray
Write-Host '       Portal: http://127.0.0.1:8080/portal/login (院方账号由管理员创建)' -ForegroundColor Gray
Write-Host 'Press Ctrl+C to stop.' -ForegroundColor Gray

Set-Location $backend
& java -jar $jar
