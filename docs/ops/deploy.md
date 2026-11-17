# 部署与回滚

## Windows 单机部署

```powershell
& .\tools\deploy.ps1 `
  -JarPath .\backend\target\laminar-ops-1.0.0.jar `
  -AdminDist .\web-admin\dist `
  -PortalDist .\web-admin\dist-portal
```

脚本会：

```text
备份当前 jar/dist
检测旧 Redis 3 服务
应用已使用非 6379 端口且新 Redis 端口监听时，停止并禁用旧 Redis 3
停止 8080 进程
复制新版本
启动后端
检查 /actuator/health
失败时恢复上一版本并重启
```

## 旧 Redis 3 自动停用

条件：

```text
REDIS_PORT 不是 6379
127.0.0.1:REDIS_PORT 正在监听
Windows 服务 Redis 存在
```

满足时执行：

```text
Stop-Service Redis
Set-Service Redis -StartupType Disabled
sc.exe config Redis start= disabled
```

需要管理员权限；非管理员会输出警告但继续部署。


## Linux 发布目录

新增：

```text
tools/deploy-linux.sh
```

约定：

```text
/opt/laminar-ops/current       当前 release 软链
/opt/laminar-ops/releases      历史发布
systemctl restart laminar-ops 重启服务
```

工作流：

```text
.github/workflows/deploy.yml
```

需要配置 GitHub Environment Secrets：

```text
SSH_PRIVATE_KEY
SSH_USER
```

并在 workflow_dispatch 输入目标 host。

服务器需要：

```text
sudo 免密执行 deploy-linux.sh
systemd 服务 laminar-ops
/opt/laminar-ops/.env 管理密钥
```

## 回滚

脚本在新版本健康检查失败时：

```text
切回上一个 current 软链
重启服务
再次检查健康
```

回滚后仍失败会返回非零并停止发布。

## HTTPS

本地自签名证书仅用于测试：

```powershell
& .\tools\gen-self-signed-cert.ps1
```

生产请使用正式 CA 证书，并由 Nginx 终止 TLS：

```text
docs/security/nginx-laminar.conf.example
```

Spring Boot 证书路径通过环境变量提供：

```text
TLS_KEYSTORE
TLS_KEYSTORE_PASSWORD
TLS_KEY_ALIAS
```

不要提交私钥、P12/JKS 到仓库。

## 上传目录

`UPLOAD_DIR` 是必填部署变量，必须是绝对路径。
后端启动时会校验：

```text
不能为空
必须是绝对路径
目录必须存在
目录必须可写
```

Windows 示例：

```powershell
New-Item -ItemType Directory -Force D:\laminar-data\upload
$env:UPLOAD_DIR = "D:/laminar-data/upload"
```

Linux 示例：

```bash
sudo mkdir -p /var/lib/laminar-ops/upload
sudo chown -R laminar:laminar /var/lib/laminar-ops/upload
export UPLOAD_DIR=/var/lib/laminar-ops/upload
```

要求：

```text
上传目录不能放在发布包目录内
升级发布时不能删除或覆盖上传目录
备份必须同时备份 UPLOAD_DIR
运行账号需要读写权限
```

启动日志会输出校验结果：

```text
[启动] 上传目录校验通过：...
```

