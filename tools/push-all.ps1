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

$secrets = Read-Secrets
$githubToken = Get-Value 'GITHUB_TOKEN' $secrets
$giteeToken = Get-Value 'GITEE_TOKEN' $secrets
$giteeUser = Get-Value 'GITEE_USERNAME' $secrets
if (!$giteeUser) { $giteeUser = 'chenyiyang1530' }

if (!$githubToken -or !$giteeToken) {
    Write-Host '缺少凭据：请在 .secrets.local 中配置 GITHUB_TOKEN 和 GITEE_TOKEN。' -ForegroundColor Yellow
    Write-Host 'GitHub Token 需要 Contents: Read and write；Gitee Token 需要 projects 权限。' -ForegroundColor Yellow
    exit 2
}

function Push-WithToken($remote, $username, $token) {
    $pair = "${username}:${token}"
    $basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
    Write-Host "Pushing $remote ..." -ForegroundColor Cyan
    & git -c http.sslBackend=schannel -c "http.extraHeader=Authorization: Basic $basic" push $remote main
    if ($LASTEXITCODE -ne 0) { throw "push $remote failed" }
}

Push-WithToken 'origin' 'x-access-token' $githubToken
Push-WithToken 'gitee' $giteeUser $giteeToken

$local = (git rev-parse HEAD).Trim()
$github = (git ls-remote origin refs/heads/main).Split("`t")[0]
$gitee = (git ls-remote gitee refs/heads/main).Split("`t")[0]
Write-Host "local  = $local"
Write-Host "github = $github"
Write-Host "gitee  = $gitee"
if ($local -ne $github -or $local -ne $gitee) {
    throw '远程 HEAD 不一致，请检查推送结果'
}
Write-Host 'GitHub 和 Gitee 已同步。' -ForegroundColor Green
