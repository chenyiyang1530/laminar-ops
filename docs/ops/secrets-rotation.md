# 生产密钥轮换

## 1. 先备份

```powershell
$env:DB_PASSWORD="..."
powershell -File tools/restore-drill.ps1
```

## 2. 轮换敏感参数加密密钥

使用超级管理员账号调用：

```http
POST /api/system/config/rotate-key
Content-Type: application/json

{"newSecret":"新的至少32位随机密钥"}
```

成功后数据库中的敏感参数已用新密钥加密。

## 3. 立即重启

必须用新的 `CONFIG_SECRET` 重启，旧进程仍持有旧密钥：

```powershell
$env:CONFIG_SECRET="新的密钥"
.\start.ps1
```

## 4. 轮换其他密钥

```text
JWT_SECRET       重启即可，旧 Token 全部失效
FILE_SIGN_SECRET 重启即可，旧签名 URL 失效
MONITOR_TOKEN    重启后所有网关都要换新 Token
DB_PASSWORD      先在 MySQL 改密码，再改环境变量重启
```

## 5. HTTPS

生产推荐：

```text
Nginx 终止 TLS
Spring Boot 只监听 127.0.0.1:8080
```

本地测试可生成自签名证书：

```powershell
.\tools\gen-self-signed-cert.ps1
```

然后使用 `https` profile：

```powershell
java -jar backend\target\laminar-ops-1.0.0.jar --spring.profiles.active=https
```

不要把私钥、P12/JKS 提交仓库。
