# 数据库与 Redis 凭证

## MySQL

应用不再使用 root。

专用账号：

```text
laminar_ops@127.0.0.1
```

权限：

```text
laminar_ops.* 的 SELECT/INSERT/UPDATE/DELETE
CREATE/ALTER/DROP/INDEX/REFERENCES
LOCK TABLES/SHOW VIEW/TRIGGER/EVENT
EXECUTE/CREATE ROUTINE/ALTER ROUTINE
SHOW_ROUTINE
```

应用连接：

```text
DB_USERNAME=laminar_ops
DB_PASSWORD=见 .secrets.local
```

不在代码中保存密码。

## Redis

本机 Redis 版本为 3.0，不支持 ACL 多用户。

因此只能使用：

```text
requirepass 密码认证
```

已配置强密码，并持久化到 Redis 服务配置。

Spring 配置：

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:}
```

注意：`redis-cli` 现在需要：

```bash
redis-cli -a "$REDIS_PASSWORD"
```

如果升级到 Redis 6+，建议改用 ACL：

```text
ACL SETUSER laminar_ops on >密码 ~* +@all
```

并配置：

```yaml
spring:
  data:
    redis:
      username: laminar_ops
      password: ...
```

## 仍需处理

```text
MySQL root 密码仍为开发密码，生产需修改或禁用远程 root
Redis 3.0 无 ACL，生产建议升级 Redis 6+
备份账号可继续使用应用账号（已授予 dump 所需权限）
```
