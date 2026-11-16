param(
  [string]$OutputDir = "D:\ideaWork\laminar-ops\backend\certs",
  [string]$Password = "changeit",
  [string]$CommonName = "localhost"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
  throw "keytool 不在 PATH，请安装 JDK 17 并配置 PATH"
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$keystore = Join-Path $OutputDir "laminar-local.p12"
if (Test-Path $keystore) { throw "证书已存在：$keystore，请先备份或删除" }

& keytool -genkeypair -alias laminar -keyalg RSA -keysize 2048 -storetype PKCS12 `
  -keystore $keystore -validity 825 -storepass $Password -keypass $Password `
  -dname "CN=$CommonName, OU=Laminar, O=Laminar, L=Xiamen, ST=Fujian, C=CN" `
  -ext "SAN=dns:localhost,ip:127.0.0.1"

if ($LASTEXITCODE -ne 0) { throw "证书生成失败" }
Write-Host "自签名证书已生成：$keystore" -ForegroundColor Green
Write-Host "仅用于本地 HTTPS 验证，生产请使用正式证书。"
