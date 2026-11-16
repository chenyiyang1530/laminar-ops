# 生产环境安全建议

启动前必须设置 `.env.example` 中的环境变量，至少包括：

```text
DB_PASSWORD
JWT_SECRET
CONFIG_SECRET
MONITOR_TOKEN
VERIFICATION_DEV_MODE=false
CORS_ALLOWED_ORIGINS
```

`application-local.yml` 只用于本机开发，已加入 `.gitignore`，不要部署到生产。


后端本身仍监听内网 `127.0.0.1:8080`，由 Nginx 终止 HTTPS。

1. 准备证书：

```text
/etc/nginx/ssl/fullchain.pem
/etc/nginx/ssl/privkey.pem
```

2. 复制 `nginx-laminar.conf.example` 到 Nginx 配置目录，修改：

```text
server_name
证书路径
后端地址
```

3. 检查并重载：

```bash
nginx -t
nginx -s reload
```

## 敏感参数加密密钥

生产环境必须设置：

```bash
CONFIG_SECRET=一段足够长的随机字符串
```

要求：

```text
至少 32 位
不要与 JWT_SECRET 相同
备份并长期保存
修改后旧敏感参数将无法解密
```

如果未设置 `CONFIG_SECRET`，当前版本会回退使用 `JWT_SECRET` 派生密钥；生产环境不建议依赖这个回退。

4. 后端已配置：

```yaml
server:
  forward-headers-strategy: framework
```

用于正确识别 `X-Forwarded-Proto`、`X-Forwarded-For`。

5. 生产环境还需要：

```text
关闭演示登录
修改管理员初始密码
关闭 verification.dev-mode
设置 JWT secret、数据库密码、Redis 密码
限制 CORS 来源
定期备份 MySQL 和上传文件
```
