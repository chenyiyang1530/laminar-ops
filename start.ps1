param(
    [switch]$SkipFrontend,
    [switch]$BuildBackend
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
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
Write-Host '       Admin:  http://127.0.0.1:8080/app/login    (admin / 123456)' -ForegroundColor Gray
Write-Host '       Portal: http://127.0.0.1:8080/portal/login (hospital1 / 123456)' -ForegroundColor Gray
Write-Host 'Press Ctrl+C to stop.' -ForegroundColor Gray

Set-Location $backend
& java -jar $jar
