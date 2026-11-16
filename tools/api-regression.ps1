param(
  [string]$BaseUrl = 'http://127.0.0.1:8081',
  [string]$CompanyCode = 'TEST-OPS',
  [string]$Password = $env:TEST_PASSWORD
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$script:passed = 0
$script:failed = 0

function Assert-True($condition, $name) {
  if ($condition) {
    $script:passed++
    Write-Host "PASS $name" -ForegroundColor Green
  } else {
    $script:failed++
    Write-Host "FAIL $name" -ForegroundColor Red
  }
}

function Login($username) {
  $body = @{ companyCode = $CompanyCode; username = $username; password = $Password } | ConvertTo-Json
  $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($body))
  if ($res.code -ne 200) { throw "login failed: $username" }
  return @{ Authorization = "Bearer $($res.data.token)" }
}

Write-Host "API regression @ $BaseUrl" -ForegroundColor Cyan

$tokens = @{}
foreach ($u in @('test_ceo', 'test_boss', 'test_area', 'test_region', 'test_eng')) {
  $tokens[$u] = Login $u
  Assert-True $true "login $u"
}

$features = Invoke-RestMethod -Uri "$BaseUrl/api/public/features"
Assert-True ($features.code -eq 200) 'public features endpoint'
Assert-True ($null -ne $features.data.demoLoginEnabled) 'demo login flag present'

$ceoInfo = Invoke-RestMethod -Uri "$BaseUrl/api/auth/info" -Headers $tokens['test_ceo']
Assert-True ($ceoInfo.data.menus.Count -ge 5) 'CEO menu tree'
Assert-True (-not ($ceoInfo.data.permissions -contains 'staff:approval:config')) 'CEO has no approval config permission'

$ceoRecords = Invoke-RestMethod -Uri "$BaseUrl/api/approval/company-records" -Headers $tokens['test_ceo']
Assert-True ($ceoRecords.code -eq 200 -and $ceoRecords.data.Count -ge 1) 'CEO sees company approval records'
$areaRecords = Invoke-RestMethod -Uri "$BaseUrl/api/approval/company-records" -Headers $tokens['test_area']
Assert-True ($areaRecords.code -eq 403) 'regional manager cannot see all company records'

$areaEngineers = Invoke-RestMethod -Uri "$BaseUrl/api/crud/engineer/page?pageNum=1&pageSize=20" -Headers $tokens['test_area']
Assert-True ($areaEngineers.code -eq 200 -and $areaEngineers.data.total -ge 3) 'area manager sees descendant engineers'

$dept = Invoke-RestMethod -Uri "$BaseUrl/api/system/dept/tree" -Headers $tokens['test_ceo']
Assert-True ($dept.code -eq 200 -and $dept.data.Count -ge 8) 'department tree contains test organization'

$areaModules = @(
  @('workorder', '/api/workorder/page?pageNum=1&pageSize=20'),
  @('alarm', '/api/alarm/page?pageNum=1&pageSize=20'),
  @('equipment', '/api/crud/equipment/page?pageNum=1&pageSize=20'),
  @('inspection', '/api/inspection/task/page?pageNum=1&pageSize=20'),
  @('maintenance', '/api/maintenance/task/page?pageNum=1&pageSize=20'),
  @('monitor', '/api/monitor/overview'),
  @('dashboard', '/api/dashboard/overview')
)
foreach ($m in $areaModules) {
  try {
    $res = Invoke-RestMethod -Uri "$BaseUrl$($m[1])" -Headers $tokens['test_area']
    Assert-True ($res.code -eq 200) "area scope $($m[0])"
  } catch {
    Assert-True $false "area scope $($m[0])"
  }
}

Write-Host ""
Write-Host "passed=$script:passed failed=$script:failed" -ForegroundColor Cyan
if ($script:failed -gt 0) { exit 1 }
