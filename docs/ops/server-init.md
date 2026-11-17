# 服务器初始化：Redis 8 + 旧 Redis 清理

## 前置条件

```text
Windows 管理员 PowerShell
Redis 8 RedisService 发行包
REDIS_PASSWORD 环境变量
```

发行包目录示例：

```text
D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service
```

## 执行

```powershell
$env:REDIS_PASSWORD = "Redis强密码"

cd D:\ideaWork\laminar-ops

powershell -ExecutionPolicy Bypass -File `
  .\tools\server-init-redis.ps1 `
  -RedisDist "D:\ideaWork\redis8-service\Redis-8.10.3-Windows-x64-msys2-with-Service" `
  -InstallRoot "D:\laminar-redis" `
  -Port 6380
```

默认会：

```text
创建 D:\laminar-redis
复制 Redis8 程序
生成 users.acl
生成 redis-laminar.conf
停止占用 6380 的前台 Redis
安装 Redis8 服务
配置 5s/10s/30s 三级自动重启
启动服务
验证 6380 和 ACL 登录
停止并禁用旧 Redis 3
```

## 生成的文件

```text
D:\laminar-redis\redis
D:\laminar-redis\data
D:\laminar-redis\logs\redis.log
D:\laminar-redis\redis\redis-laminar.conf
D:\laminar-redis\redis\users.acl
```

## 幂等

重复执行时：

```text
如果 Redis8 服务已存在，更新配置后重启
如果不存在，安装新服务
不会覆盖业务数据库
```

## 注意

```text
脚本会停止旧 Redis 服务 Redis
只有确认应用已使用 6380 后再执行
ACL 文件和密码不要提交仓库
```

## 应用配置

```text
REDIS_HOST=127.0.0.1
REDIS_PORT=6380
REDIS_USERNAME=laminar_ops
REDIS_PASSWORD=...
```
