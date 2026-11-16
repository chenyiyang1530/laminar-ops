# 双公司租户隔离检查（PowerShell 7）
# 用法：
#   pwsh -NoProfile -File tools/tenant-isolation-test.ps1
#   pwsh -NoProfile -File tools/tenant-isolation-test.ps1 -BaseUrl http://127.0.0.1:8080
param(
  [string]$BaseUrl = 'http://127.0.0.1:8081',
  [string]$CompanyACode = 'CLEAN-XM',
  [string]$CompanyAUser = 'manager',
  [string]$CompanyBCode = 'FJASL-ZZ',
  [string]$CompanyBUser = 'ceo_asl',
  [string]$Password = $env:TEST_PASSWORD
)

$ErrorActionPreference = 'Stop'
$failures = New-Object System.Collections.Generic.List[string]

function Login($companyCode, $username) {
  $body = @{ companyCode = $companyCode; username = $username; password = $Password } | ConvertTo-Json
  $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($body))
  if ($res.code -ne 200 -or !$res.data.token) { throw "登录失败：$companyCode/$username $($res.msg)" }
  return @{ Authorization = "Bearer $($res.data.token)" }
}

function Page($headers, $resource) {
  $res = Invoke-RestMethod -Uri "$BaseUrl/api/crud/$resource/page?pageNum=1&pageSize=200" -Headers $headers
  if ($res.code -ne 200) { throw "$resource 列表失败：$($res.msg)" }
  return $res.data.records
}

function Check($condition, $message) {
  if ($condition) {
    Write-Host "PASS  $message" -ForegroundColor Green
  } else {
    Write-Host "FAIL  $message" -ForegroundColor Red
    $failures.Add($message)
  }
}

function HospitalIds($headers) {
  $res = Invoke-RestMethod -Uri "$BaseUrl/api/customer/hospital/page?pageNum=1&pageSize=200" -Headers $headers
  if ($res.code -ne 200) { throw "医院列表失败：$($res.msg)" }
  return @($res.data.records | ForEach-Object { $_.hospitalId })
}

Write-Host "登录公司A：$CompanyACode/$CompanyAUser"
$a = Login $CompanyACode $CompanyAUser
Write-Host "登录公司B：$CompanyBCode/$CompanyBUser"
$b = Login $CompanyBCode $CompanyBUser

$pk = @{
  company = 'companyId'; companyRegion = 'id'; contract = 'contractId';
  engineer = 'engineerId'; deviceType = 'typeId'; equipment = 'equipmentId';
  part = 'partId'; document = 'docId'; knowledge = 'articleId';
  inspectionPlan = 'planId'; maintenancePlan = 'planId'; budget = 'budgetId';
  post = 'postId'; campus = 'campusId'; department = 'departmentId';
  cleanArea = 'areaId'; contact = 'contactId'
}

$resources = @(
  'company', 'companyRegion', 'contract', 'engineer', 'deviceType', 'equipment',
  'part', 'document', 'knowledge', 'inspectionPlan', 'maintenancePlan',
  'budget', 'post'
)

foreach ($resource in $resources) {
  try {
    $aRecords = Page $a $resource
    $bRecords = Page $b $resource
    $key = $pk[$resource]
    $aIds = @($aRecords | ForEach-Object { $_.$key })
    $bIds = @($bRecords | ForEach-Object { $_.$key })
    $overlap = @($aIds | Where-Object { $bIds -contains $_ })
    Check ($overlap.Count -eq 0) "$resource 列表无跨公司重叠（A=$($aIds.Count), B=$($bIds.Count)）"

    if ($aIds.Count -gt 0) {
      $targetId = $aIds[0]
      $detail = Invoke-RestMethod -Uri "$BaseUrl/api/crud/$resource/$targetId" -Headers $b
      Check ($detail.code -ne 200) "$resource 跨公司详情被拒绝（id=$targetId，B code=$($detail.code)）"
    }
  } catch {
    Check $false "$resource 检查异常：$($_.Exception.Message)"
  }
}

# 客户主数据是共享资产：两家公司可同时服务同一家医院，因此允许同一医院下的
# 院区/科室/洁净区/对接人重叠；但每家公司只能看到自己合同医院下的数据。
Write-Host ''
Write-Host '检查共享客户主数据（按合同医院收敛）'
try {
  $aHosp = HospitalIds $a
  $bHosp = HospitalIds $b
  Check ($bHosp.Count -gt 0) "公司B 医院列表非空（$($bHosp.Count) 家）"
  $privateHospital = @($aHosp | Where-Object { $bHosp -notcontains $_ } | Select-Object -First 1)
  if ($privateHospital.Count -gt 0) {
    $detail = Invoke-RestMethod -Uri "$BaseUrl/api/customer/hospital/$($privateHospital[0])" -Headers $b
    Check ($detail.code -ne 200) "公司B 访问非合同医院被拒绝（hospitalId=$($privateHospital[0])，code=$($detail.code)）"
  }
  foreach ($resource in @('campus', 'department', 'cleanArea', 'contact')) {
    $bRecords = Page $b $resource
    $outside = @($bRecords | Where-Object { $bHosp -notcontains $_.hospitalId })
    Check ($outside.Count -eq 0) "$resource 数据均属于公司B合同医院（B=$($bRecords.Count)）"
  }
} catch {
  Check $false "共享客户主数据检查异常：$($_.Exception.Message)"
}

Write-Host ''
if ($failures.Count -eq 0) {
  Write-Host '全部隔离检查通过。' -ForegroundColor Green
  exit 0
}
Write-Host "失败 $($failures.Count) 项：" -ForegroundColor Red
$failures | ForEach-Object { Write-Host " - $_" }
exit 1
