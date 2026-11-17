param(
  [string]$Owner = "chenyiyang1530",
  [string]$Repo = "laminar-ops",
  [string]$Branch = "main",
  [string[]]$Contexts = @("Frontend E2E login to acceptance"),
  [switch]$RequireReviews
)

$ErrorActionPreference = "Stop"
if (-not $env:GITHUB_TOKEN) {
  throw "请先设置 GITHUB_TOKEN（需要有仓库 Administration 权限）"
}
$headers = @{
  Authorization = "Bearer $env:GITHUB_TOKEN"
  Accept = "application/vnd.github+json"
  "X-GitHub-Api-Version" = "2022-11-28"
}
$body = @{
  required_status_checks = @{
    strict = $true
    contexts = $Contexts
  }
  enforce_admins = $false
  required_pull_request_reviews = if ($RequireReviews) {
    @{ required_approving_review_count = 1; dismiss_stale_reviews = $true }
  } else {
    $null
  }
  restrictions = $null
  allow_force_pushes = $false
  allow_deletions = $false
} | ConvertTo-Json -Depth 8

$uri = "https://api.github.com/repos/$Owner/$Repo/branches/$Branch/protection"
Invoke-RestMethod -Method Put -Uri $uri -Headers $headers -Body $body | Out-Null
Write-Host "已设置 $Owner/$Repo:$Branch 分支保护，required checks: $($Contexts -join ', ')"
