param(
  [string]$DbHost = "127.0.0.1",
  [string]$DbName = "laminar_ops",
  [string]$DbUser = "root",
  [int]$MinTables = 40
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not $env:MYSQL_ROOT_PASSWORD) {
  throw "请先设置环境变量 MYSQL_ROOT_PASSWORD"
}
$RootPassword = $env:MYSQL_ROOT_PASSWORD

$mysqldump = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqldump)) { $mysqldump = "mysqldump.exe" }
if (-not (Test-Path $mysql)) { $mysql = "mysql.exe" }

$stamp = Get-Date -Format "yyyyMMddHHmmss"
$drillDb = "${DbName}_drill_$stamp"
$dumpFile = Join-Path $env:TEMP "laminar-restore-drill-$stamp.sql"
$created = $false
$env:MYSQL_PWD = $RootPassword

try {
  Write-Host "[drill] 导出源库 $DbName ..."
  & $mysqldump --host=$DbHost --user=$DbUser --single-transaction --routines --triggers `
    --default-character-set=utf8mb4 --result-file=$dumpFile $DbName 2>$null
  if ($LASTEXITCODE -ne 0) { throw "导出失败，退出码 $LASTEXITCODE" }

  Write-Host "[drill] 创建临时库 $drillDb ..."
  & $mysql --host=$DbHost --user=$DbUser --default-character-set=utf8mb4 `
    -e "DROP DATABASE IF EXISTS $drillDb; CREATE DATABASE $drillDb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>$null
  if ($LASTEXITCODE -ne 0) { throw "创建临时库失败" }
  $created = $true

  Write-Host "[drill] 恢复到临时库 ..."
  Get-Content $dumpFile -Raw | & $mysql --host=$DbHost --user=$DbUser --default-character-set=utf8mb4 --database=$drillDb 2>$null
  if ($LASTEXITCODE -ne 0) { throw "恢复失败，退出码 $LASTEXITCODE" }

  $tableCount = & $mysql --host=$DbHost --user=$DbUser --default-character-set=utf8mb4 -N -e `
    "select count(*) from information_schema.tables where table_schema='$drillDb';" 2>$null
  $userCount = & $mysql --host=$DbHost --user=$DbUser --default-character-set=utf8mb4 -N -e `
    "select count(*) from $drillDb.sys_user;" 2>$null

  if ([int]$tableCount -lt $MinTables) { throw "恢复后表数量异常：$tableCount" }
  if ([int]$userCount -lt 1) { throw "恢复后 sys_user 为空" }

  Write-Host "[drill] 恢复成功：表 $tableCount 张，用户 $userCount 个" -ForegroundColor Green
  Write-Host "[drill] 演练库：$drillDb"
} finally {
  if ($created) {
    & $mysql --host=$DbHost --user=$DbUser -e "DROP DATABASE IF EXISTS $drillDb;" 2>$null
  }
  Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
  Remove-Item $dumpFile -Force -ErrorAction SilentlyContinue
}
