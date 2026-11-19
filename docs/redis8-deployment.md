# Windows Redis8 部署与开机自启

本文记录本机/Windows Server 上 Redis8 的服务化部署、应用开机启动、健康检查和告警配置。

## 1. 当前实例

```text
Redis 版本：Redis 8.x Windows Service
服务名：Redis8
端口：6380
安装目录：D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service
配置：redis-laminar.conf
数据目录：D:\ideaWork\redis8-service\data
ACL 用户：laminar_ops
```

启动类型：

```text
Automatic
```

异常恢复：

```text
第 1 次失败：5 秒后重启
第 2 次失败：10 秒后重启
第 3 次失败：30 秒后重启
重置周期：86400 秒
```

## 2. 安装 Redis8 服务

以管理员身份打开 PowerShell：

```powershell
Start-Process pwsh -Verb RunAs -ArgumentList @(
  '-NoProfile',
  '-ExecutionPolicy',
  'Bypass',
  '-File',
  'D:\ideaWork\laminar-ops\tools\install-redis8-service-elevated.ps1'
)
```

确认 UAC 后执行。脚本会：

```text
停止占用 6380 的前台 Redis
安装 Redis8 服务
设置自动启动
启动服务
配置失败自动重启
检查 6380 监听
```

## 3. 验证 Redis8

```powershell
Get-Service Redis8
Test-NetConnection 127.0.0.1 -Port 6380 -InformationLevel Quiet
sc.exe qc Redis8
sc.exe qfailure Redis8
```

期望：

```text
Status     : Running
StartType  : Automatic
6380       : True
```

## 4. 应用开机自启

后端使用 Windows 计划任务，在系统启动时运行：

```text
任务名：LaminarOpsBackend
运行身份：SYSTEM
触发器：系统启动
失败重启：3 次，每次间隔 1 分钟
```

安装：

```powershell
Start-Process pwsh -Verb RunAs -ArgumentList @(
  '-NoProfile',
  '-ExecutionPolicy',
  'Bypass',
  '-File',
  'D:\ideaWork\laminar-ops\tools\install-laminar-autostart-elevated.ps1'
)
```

任务启动的包装脚本：

```text
tools\run-backend-autostart.ps1
```

它会：

```text
读取 .secrets.local
确认 Redis8 已启动
启动后端 jar
应用退出后 10 秒自动重启
```

验证：

```powershell
schtasks /Query /TN LaminarOpsBackend /V /FO LIST
Invoke-WebRequest http://127.0.0.1:8080/actuator/health
```

静默启动测试（会停止当前后端再启动任务）：

```powershell
schtasks /End /TN LaminarOpsBackend
schtasks /Run /TN LaminarOpsBackend
```

## 5. 应用级健康检查

新增 Redis 健康检查组件：

```text
/actuator/health
```

返回组件：

```text
redisLaminar
```

检查内容：

```text
PING
延迟
used_memory
maxmemory
内存使用率
SLOWLOG 条数
```

受保护接口：

```text
GET /api/system/monitor/redis-health
```

需要权限：

```text
monitor:cache:view
```

## 6. Redis 内存与慢查询告警

Redis 配置：

```text
maxmemory 1gb
maxmemory-policy volatile-lru
slowlog-log-slower-than 10000
slowlog-max-len 256
```

应用监控参数：

```text
laminar.redis.monitor-enabled=true
laminar.redis.memory-warning-percent=80
laminar.redis.disk-warning-percent=85
laminar.redis.slowlog-warning-count=1
```

巡检周期：

```text
每 5 分钟
```

告警内容：

```text
Redis 内存超过 80%
Redis 数据盘超过 85%
Redis SLOWLOG 条数达到阈值
```

告警写入平台管理员站内消息，相同告警 5 分钟内抑制重复。

## 7. 旧 Redis 兼容说明

当前机器可能还存在旧的 Redis 服务：

```text
服务名：Redis
端口：6379
```

本应用使用 Redis8：

```text
6380
```

确认应用稳定后，建议停用旧服务，避免端口、内存和密码混淆：

```powershell
Stop-Service Redis
Set-Service Redis -StartupType Disabled
```

该操作需要管理员权限。

## 8. 重启验收

服务器重启后按顺序检查：

```powershell
Get-Service Redis8
Test-NetConnection 127.0.0.1 -Port 6380 -InformationLevel Quiet
Invoke-WebRequest http://127.0.0.1:8080/actuator/health
schtasks /Query /TN LaminarOpsBackend
```

期望：

```text
Redis8: Running / Automatic
6380: True
应用 health: 200 UP
LaminarOpsBackend: Running
```
