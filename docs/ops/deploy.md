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
停止 8080 进程
复制新版本
启动后端
检查 /actuator/health
失败时恢复上一版本并重启
```

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
