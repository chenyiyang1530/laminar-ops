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

$company = Query "select company_id,company_code,company_name from biz_company where company_code='TESTCO2' limit 1;"
if (-not $company) {
  Write-Host "未找到 TESTCO2，测试数据已清理或本库不是该环境。" -ForegroundColor Yellow
  exit 0
}
$companyParts = ($company -split "`t")
$companyId = [int]$companyParts[0]
$companyName = $companyParts[2]
if ($companyName -notlike "*测试*") {
  throw "TESTCO2 不是测试公司，停止执行：$companyName"
}

$userRows = Query "select user_id,username from sys_user where username='testco2admin' and company_id=$companyId;"
$woRows = Query "select workorder_id,workorder_no from biz_workorder where company_id=$companyId;"

Write-Host "测试公司：$companyId / TESTCO2 / $companyName"
Write-Host "测试账号：$userRows"
Write-Host "测试工单：$woRows"
Write-Host ""

if (-not $Execute) {
  Write-Host "当前为预演模式，没有删除任何数据。" -ForegroundColor Yellow
  Write-Host "确认后执行： powershell -ExecutionPolicy Bypass -File tools/cleanup-test-data.ps1 -Execute"
  exit 0
}

$sql = @"
START TRANSACTION;
DELETE wl FROM biz_workorder_log wl JOIN biz_workorder w ON w.workorder_id=wl.workorder_id WHERE w.company_id=$companyId;
DELETE FROM biz_workorder WHERE company_id=$companyId;
DELETE FROM biz_alarm_log WHERE alarm_id IN (SELECT alarm_id FROM biz_alarm WHERE company_id=$companyId);
DELETE FROM biz_alarm WHERE company_id=$companyId;
DELETE FROM sys_user_role WHERE user_id IN (SELECT user_id FROM sys_user WHERE company_id=$companyId AND username='testco2admin');
DELETE FROM biz_hospital_user WHERE user_id IN (SELECT user_id FROM sys_user WHERE company_id=$companyId AND username='testco2admin');
DELETE FROM sys_user WHERE company_id=$companyId AND username='testco2admin';
COMMIT;
"@
& $mysql --host=127.0.0.1 --user=root --default-character-set=utf8mb4 $db -e $sql 2>$null
if ($LASTEXITCODE -ne 0) { throw "清理失败，事务已回滚" }
Write-Host "测试账号和测试工单已清理。" -ForegroundColor Green
Write-Host "测试公司/测试医院主数据保留，需人工确认后再删除。"

