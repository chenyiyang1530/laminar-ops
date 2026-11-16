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
D:\ideaWork\redis8\Redis-8.10.3-Windows-x64-msys2\users.acl
```

用户：

```text
laminar_ops
```

默认用户关闭，只允许 ACL 用户认证。

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
