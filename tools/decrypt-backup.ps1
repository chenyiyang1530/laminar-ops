param(
  [Parameter(Mandatory = $true)][string]$EncryptedPath,
  [string]$OutputPath = "",
  [string]$Password = $env:BACKUP_ENCRYPTION_PASSWORD
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot
$secretsFile = Join-Path $root ".secrets.local"
if (-not $Password -and (Test-Path $secretsFile)) {
  Get-Content $secretsFile | ForEach-Object {
    if ($_ -match '^BACKUP_ENCRYPTION_PASSWORD=(.+)$') { $Password = $Matches[1] }
  }
}
if (-not $Password) { throw "缺少 BACKUP_ENCRYPTION_PASSWORD" }
. (Join-Path $PSScriptRoot "backup-crypto.ps1")

if (-not $OutputPath) {
  $OutputPath = $EncryptedPath -replace '\.enc$', ''
}
$result = Unprotect-BackupFile -SourcePath $EncryptedPath -OutputPath $OutputPath -Password $Password
Write-Host "解密完成：$result" -ForegroundColor Green
