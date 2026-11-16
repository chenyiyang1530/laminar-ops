param(
  [switch]$Execute
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysql)) { $mysql = "mysql.exe" }
if (-not $env:MYSQL_ROOT_PASSWORD) { throw "请先设置 MYSQL_ROOT_PASSWORD" }
$env:MYSQL_PWD = $env:MYSQL_ROOT_PASSWORD
$db = "laminar_ops"

function Query($sql) {
  return (& $mysql --host=127.0.0.1 --user=root --default-character-set=utf8mb4 -N $db -e $sql 2>$null)
}

$company = Query "select company_id,company_name from biz_company where company_id=9 and company_code='TESTCO2';"
$hospital = Query "select hospital_id,hospital_name from biz_hospital where hospital_id=14 and hospital_name like '%测试%';"
if (-not $company -or -not $hospital) {
  Write-Host "测试公司/医院不存在或已被清理。" -ForegroundColor Yellow
  exit 0
}
if (($company -split "`t")[1] -notlike "*测试*") { throw "公司9不是测试公司，停止" }

Write-Host "待清理：$company / $hospital" -ForegroundColor Cyan

$companyTables = Query "select table_name from information_schema.columns where table_schema='$db' and column_name='company_id' order by table_name;"
$hospitalTables = Query "select table_name from information_schema.columns where table_schema='$db' and column_name='hospital_id' order by table_name;"

if (-not $Execute) {
  Write-Host "预演：将清理 company_id=9 的表 $($companyTables.Count) 张，hospital_id=14 的表 $($hospitalTables.Count) 张。" -ForegroundColor Yellow
  Write-Host "确认后执行： powershell -File tools/cleanup-test-company.ps1 -Execute"
  exit 0
}

$sql = "SET FOREIGN_KEY_CHECKS=0; START TRANSACTION;`n"
foreach ($table in $companyTables) {
  if ($table.Trim()) { $sql += "DELETE FROM ``$($table.Trim())`` WHERE ``company_id``=9;`n" }
}
foreach ($table in $hospitalTables) {
  if ($table.Trim()) { $sql += "DELETE FROM ``$($table.Trim())`` WHERE ``hospital_id``=14;`n" }
}
$sql += "DELETE FROM ``biz_company`` WHERE ``company_id``=9;`n"
$sql += "DELETE FROM ``biz_hospital`` WHERE ``hospital_id``=14;`n"
$sql += "COMMIT; SET FOREIGN_KEY_CHECKS=1;`n"

$sql | & $mysql --host=127.0.0.1 --user=root --default-character-set=utf8mb4 $db 2>$null
if ($LASTEXITCODE -ne 0) { throw "清理失败，事务未提交" }
Write-Host "测试公司/医院及关联数据已清理。" -ForegroundColor Green

