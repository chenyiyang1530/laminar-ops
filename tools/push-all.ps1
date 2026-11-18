param(
    [ValidateSet('all', 'github', 'gitee')]
    [string]$Remote = 'all',
    [switch]$NoOpen
)

# Push current main to GitHub and/or Gitee using tokens from .secrets.local.
# Examples:
#   pwsh -NoProfile -File .\tools\push-all.ps1
#   pwsh -NoProfile -File .\tools\push-all.ps1 -Remote gitee -NoOpen
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Read-Secrets {
    $map = @{}
    $file = Join-Path $root '.secrets.local'
    if (!(Test-Path $file)) { return $map }
    Get-Content $file -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (!$line -or $line.StartsWith('#') -or !$line.Contains('=')) { return }
        $parts = $line.Split('=', 2)
        $map[$parts[0].Trim()] = $parts[1].Trim()
    }
    return $map
}

function Get-Value($name, $secrets) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if (!$value -and $secrets.ContainsKey($name)) { $value = $secrets[$name] }
    return $value
}

function Push-WithToken($remote, $username, $token) {
    $pair = "${username}:${token}"
    $basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
    Write-Host "Pushing $remote ..." -ForegroundColor Cyan
    & git -c http.sslBackend=schannel -c "http.extraHeader=Authorization: Basic $basic" push $remote main
    return ($LASTEXITCODE -eq 0)
}

function Remote-Head($remote) {
    try {
        $line = (git -c http.sslBackend=schannel ls-remote $remote refs/heads/main 2>$null)
        if (!$line) { return 'missing' }
        return $line.Split("`t")[0]
    } catch {
        return 'unavailable'
    }
}

$secrets = Read-Secrets
$githubToken = Get-Value 'GITHUB_TOKEN' $secrets
$giteeToken = Get-Value 'GITEE_TOKEN' $secrets
$giteeUser = Get-Value 'GITEE_USERNAME' $secrets
if (!$giteeUser) { $giteeUser = 'chenyiyang1530' }

$wantedGithub = $Remote -in @('all', 'github')
$wantedGitee = $Remote -in @('all', 'gitee')
$pushed = @()
$failed = @()

if ($wantedGithub) {
    if (!$githubToken) {
        $failed += 'github(no token)'
    } elseif (Push-WithToken 'origin' 'x-access-token' $githubToken) {
        $pushed += 'origin'
    } else {
        $failed += 'github'
    }
}
if ($wantedGitee) {
    if (!$giteeToken) {
        Write-Host 'Gitee: 未配置 GITEE_TOKEN，跳过。' -ForegroundColor Yellow
        $failed += 'gitee(no token)'
    } elseif (Push-WithToken 'gitee' $giteeUser $giteeToken) {
        $pushed += 'gitee'
    } else {
        $failed += 'gitee'
    }
}

$local = (git rev-parse HEAD).Trim()
$originHead = Remote-Head 'origin'
$giteeHead = Remote-Head 'gitee'
$status = if ($failed.Count) { 'partial' } else { 'success' }
$stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
$logLine = "[$stamp] status=$status remote=$Remote local=$local github=$originHead gitee=$giteeHead pushed=$($pushed -join ',') failed=$($failed -join ',')"
$logFile = Join-Path $root '.git/push-all.log'
Add-Content -Path $logFile -Value $logLine -Encoding UTF8

Write-Host "local  = $local"
Write-Host "github = $originHead"
Write-Host "gitee  = $giteeHead"
Write-Host "log    = $logFile"

foreach ($target in $pushed) {
    $head = if ($target -eq 'origin') { $originHead } else { $giteeHead }
    if ($local -ne $head) { $failed += "$target(head mismatch)" }
}

if (!$NoOpen -and !$env:CI) {
    $commit = $local.Substring(0, [Math]::Min(12, $local.Length))
    $urls = @()
    if ($pushed -contains 'origin') { $urls += "https://github.com/chenyiyang1530/laminar-ops/commit/$commit" }
    if ($pushed -contains 'gitee') { $urls += "https://gitee.com/chenyiyang1530/laminar-ops/commit/$commit" }
    foreach ($url in $urls) {
        try { Start-Process $url | Out-Null } catch { Write-Host "无法打开浏览器: $url" -ForegroundColor Yellow }
    }
}

if ($failed.Count) {
    Write-Host ("未完成: " + ($failed -join ', ')) -ForegroundColor Red
    exit 1
}
Write-Host ("已同步: " + ($pushed -join ', ')) -ForegroundColor Green
