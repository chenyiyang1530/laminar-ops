param(
    [switch]$SkipBackend
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root 'backend'
$web = Join-Path $root 'web-admin'

Write-Host '==============================================' -ForegroundColor Cyan
Write-Host '  Laminar Ops - dev mode (front/back separated)' -ForegroundColor Cyan
Write-Host '==============================================' -ForegroundColor Cyan

if (-not (Test-Path (Join-Path $web 'node_modules'))) {
    Write-Host '[INFO] Installing frontend dependencies...' -ForegroundColor Yellow
    Push-Location $web
    npm install
    Pop-Location
}

if (-not $SkipBackend) {
    Write-Host '[INFO] Starting backend in new window (8080)...' -ForegroundColor Green
    Start-Process powershell -ArgumentList @(
        '-NoExit', '-Command',
        "cd '$backend'; `$env:JAVA_HOME='D:\software\jdk17'; mvn spring-boot:run"
    )
}

Write-Host '[INFO] Starting admin dev server (5173)...' -ForegroundColor Green
Start-Process powershell -ArgumentList @('-NoExit', '-Command', "cd '$web'; npm run dev")

Write-Host '[INFO] Starting hospital portal dev server (5174)...' -ForegroundColor Green
Start-Process powershell -ArgumentList @('-NoExit', '-Command', "cd '$web'; npm run dev:portal")

Start-Sleep -Seconds 3
Write-Host ''
Write-Host '  Admin  : http://localhost:5173/login' -ForegroundColor Cyan
Write-Host '  Portal : http://localhost:5174/login' -ForegroundColor Cyan
Write-Host '  API    : http://127.0.0.1:8080/api  (proxied by Vite)' -ForegroundColor Gray
Write-Host ''
Write-Host 'Files are hot-reloaded by Vite. Press Ctrl+C in each window to stop.' -ForegroundColor Gray
