# Push the current main branch to GitHub and Gitee using tokens from .secrets.local.
# Run: pwsh -NoProfile -File .\tools\push-all.ps1
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

$secrets = Read-Secrets
$githubToken = Get-Value 'GITHUB_TOKEN' $secrets
$giteeToken = Get-Value 'GITEE_TOKEN' $secrets
$giteeUser = Get-Value 'GITEE_USERNAME' $secrets
if (!$giteeUser) { $giteeUser = 'chenyiyang1530' }

$pushed = @()
$failed = @()
if ($githubToken) {
    if (Push-WithToken 'origin' 'x-access-token' $githubToken) { $pushed += 'origin' } else { $failed += 'github' }
} else {
    Write-Host 'GitHub: 未配置 GITHUB_TOKEN，跳过。' -ForegroundColor Yellow
}
if ($giteeToken) {
    if (Push-WithToken 'gitee' $giteeUser $giteeToken) { $pushed += 'gitee' } else { $failed += 'gitee' }
} else {
    Write-Host 'Gitee: 未配置 GITEE_TOKEN，跳过；请配置一次后会自动复用。' -ForegroundColor Yellow
}

if (!$pushed.Count) { exit 2 }

$local = (git rev-parse HEAD).Trim()
Write-Host "local = $local"
foreach ($remote in $pushed) {
    $remoteHead = (git -c http.sslBackend=schannel ls-remote $remote refs/heads/main).Split("`t")[0]
    Write-Host "$remote = $remoteHead"
    if ($local -ne $remoteHead) { $failed += $remote }
}
if ($failed.Count) {
    Write-Host ("推送失败: " + ($failed -join ', ')) -ForegroundColor Red
    exit 1
}
Write-Host ("已同步: " + ($pushed -join ', ')) -ForegroundColor Green
