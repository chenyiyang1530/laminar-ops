# 数据库与 Redis 凭证

## MySQL

应用使用专用账号：

```text
laminar_ops@127.0.0.1
```

只授权 `laminar_ops.*`，不再使用 root 连接业务。

root：

```text
root@localhost
```

只允许本机，不允许远程 root。

root 密码已改为强密码，保存在 `.secrets.local` 的：

```text
MYSQL_ROOT_PASSWORD
```

脚本使用该变量：

```text
restore-drill.ps1
cleanup-test-company.ps1
cleanup-test-data.ps1
```

## Redis 8 + ACL

已使用 Redis 8.10.3 Windows 构建，监听：

```text
127.0.0.1:6380
```

ACL 文件：

```text
D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service\users.acl
```

用户：

```text
laminar_ops
```

默认用户关闭，只允许 ACL 用户认证。

### 服务安装

服务安装需要管理员 PowerShell：

```powershell
cd D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service
.\RedisService.exe install `
  -c .\redis-laminar.conf `
  --dir D:\ideaWork\redis8-service\data `
  --port 6380 `
  --service-name Redis8 `
  --start-mode auto
Start-Service Redis8
```

也可以运行：

```powershell
powershell -ExecutionPolicy Bypass -File `
  D:\ideaWork\laminar-ops\tools\install-redis8-service-elevated.ps1
```

当前若服务未安装，Redis 以前台进程运行在 6380。

### 服务日志与自动重启

Redis 日志：

```text
D:\ideaWork\redis8-service\redis.log
```

配置文件：

```text
logfile D:/ideaWork/redis8-service/redis.log
loglevel notice
```

服务安装后执行：

```powershell
powershell -ExecutionPolicy Bypass -File `
  D:\ideaWork\laminar-ops\tools\configure-redis8-recovery.ps1
```

恢复策略：

```text
第一次失败：5 秒后重启
第二次失败：10 秒后重启
后续失败：30 秒后重启
24 小时重置计数
```

服务未安装时可运行前台看护：

```powershell
powershell -ExecutionPolicy Bypass -File `
  D:\ideaWork\laminar-ops\tools\redis8-watchdog.ps1
```

看护日志：

```text
D:\ideaWork\redis8-service\watchdog.log
```



Spring 配置：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      username: ${REDIS_USERNAME:}
      password: ${REDIS_PASSWORD:}
```

`.secrets.local` 已设置：

```text
REDIS_HOST
REDIS_PORT=6380
REDIS_USERNAME=laminar_ops
REDIS_PASSWORD
```

旧的 Redis 3 服务仍在 6379，保留作为回滚；确认稳定后可停止。

## 注意

```text
Redis 8 是第三方 Windows 构建，生产建议 Linux 原生 Redis 7/8
ACL 文件包含密码，不要提交仓库
MySQL root 和管理脚本密码放在 .secrets.local
```
