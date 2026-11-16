param(
  [string]$BackupDir = "D:\backups\laminar-ops",
  [string]$UploadDir = "D:\ideaWork\laminar-ops\backend\upload",
  [string]$DbHost = "127.0.0.1",
  [string]$DbName = "laminar_ops",
  [string]$DbUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }),
  [int]$RetentionDays = 30
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not $env:DB_PASSWORD) {
  throw "请先设置环境变量 DB_PASSWORD 再执行备份脚本"
}

$mysqldump = if ($env:MYSQLDUMP) { $env:MYSQLDUMP } else {
  $candidates = @(
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe"
  )
  $found = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
  if ($found) { $found } else { "mysqldump.exe" }
}
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$work = Join-Path $BackupDir $stamp
New-Item -ItemType Directory -Force -Path $work | Out-Null

$sqlFile = Join-Path $work "$DbName.sql"
$env:MYSQL_PWD = $env:DB_PASSWORD
try {
  & $mysqldump --host=$DbHost --user=$DbUser --single-transaction --no-tablespaces --routines --triggers `
    --default-character-set=utf8mb4 --databases $DbName --result-file=$sqlFile
  if ($LASTEXITCODE -ne 0) {
    throw "mysqldump 执行失败，退出码 $LASTEXITCODE"
  }
} finally {
  Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

if ((Get-Item $sqlFile).Length -le 0) {
  throw "数据库备份文件为空，请检查 mysqldump 和数据库连接"
}

if (Test-Path $UploadDir) {
  Copy-Item -Path $UploadDir -Destination (Join-Path $work "upload") -Recurse -Force
}

$archive = Join-Path $BackupDir "laminar-ops-$stamp.zip"
Compress-Archive -Path (Join-Path $work "*") -DestinationPath $archive -CompressionLevel Optimal
Remove-Item -Path $work -Recurse -Force

Get-ChildItem -Path $BackupDir -Filter "laminar-ops-*.zip" |
  Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
  Remove-Item -Force

Write-Host "备份完成: $archive"
