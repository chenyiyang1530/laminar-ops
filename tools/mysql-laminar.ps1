param(
  [string]$Database = "laminar_ops",
  [string]$Query = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$secretFile = "D:\ideaWork\laminar-ops\.secrets.local"
if (-not (Test-Path $secretFile)) { throw "找不到 $secretFile" }

$cfg = @{}
Get-Content $secretFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $parts = $line.Split('=', 2)
    $cfg[$parts[0]] = $parts[1]
  }
}
if (-not $cfg['DB_PASSWORD']) { throw "缺少 DB_PASSWORD" }

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysql)) { $mysql = "mysql.exe" }

$oldPwd = $env:MYSQL_PWD
try {
  $env:MYSQL_PWD = $cfg['DB_PASSWORD']
  $mysqlArgs = @(
    "--host=127.0.0.1",
    "--user=$($cfg['DB_USERNAME'])",
    "--default-character-set=utf8mb4",
    $Database
  )
  if ($Query) { $mysqlArgs += @("-e", $Query) }
  & $mysql @mysqlArgs
  exit $LASTEXITCODE
} finally {
  if ($null -eq $oldPwd) { Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue }
  else { $env:MYSQL_PWD = $oldPwd }
}
