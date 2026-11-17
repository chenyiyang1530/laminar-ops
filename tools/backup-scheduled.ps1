param(
  [string]$BackupDir = "D:\backups\laminar-ops",
  [string]$OffsitePath = $env:BACKUP_OFFSITE_PATH,
  [string]$RcloneRemote = $env:BACKUP_RCLONE_REMOTE
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$root = Split-Path -Parent $PSScriptRoot

$secretsFile = Join-Path $root ".secrets.local"
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

& (Join-Path $PSScriptRoot "backup.ps1") -BackupDir $BackupDir
if ($LASTEXITCODE -ne 0) { throw "本地备份失败" }

$latest = Get-ChildItem $BackupDir -Filter "laminar-ops-*.zip.enc" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $latest) { throw "没有找到加密备份文件" }
Write-Host "本地备份：$($latest.FullName)"

if ($RcloneRemote) {
  $rclone = Get-Command rclone.exe -ErrorAction SilentlyContinue
  if (-not $rclone) { throw "配置了 RCLONE_REMOTE 但未安装 rclone" }
  & $rclone.Source copy $latest.FullName $RcloneRemote
  if ($LASTEXITCODE -ne 0) { throw "rclone 异地复制失败" }
  Write-Host "异地备份完成：$RcloneRemote"
} elseif ($OffsitePath) {
  New-Item -ItemType Directory -Force -Path $OffsitePath | Out-Null
  Copy-Item $latest.FullName (Join-Path $OffsitePath $latest.Name) -Force
  Write-Host "异地备份完成：$OffsitePath"
} else {
  Write-Warning "未配置异地备份；请设置 BACKUP_OFFSITE_PATH 或 BACKUP_RCLONE_REMOTE"
}
