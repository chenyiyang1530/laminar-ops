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


### 文件存在性巡检

默认每天 03:30 扫描业务表中的文件路径：

```text
biz_certificate
biz_company_qualification
biz_contract
biz_document
biz_inspection_item
biz_workorder
biz_workorder_archive
```

发现文件丢失时写入超级管理员站内消息。

配置：

```text
FILE_CHECK_ENABLED=true
FILE_CHECK_CRON=0 30 3 * * *
```

手动执行：

```text
POST /api/system/file-check/run
```


### 文件巡检报告

管理端路径：系统管理 → 文件巡检。

页面包含：

```text
巡检记录：每次运行的检查数、丢失数、恢复数、状态、时间
丢失文件：按表/记录/字段列出，支持筛选和重新上传
```

后端接口：

```text
POST /api/system/file-check/run
GET  /api/system/file-check/runs
GET  /api/system/file-check/items
POST /api/system/file-check/items/{id}/recover
```

巡检使用 4 个线程、每批 500 条分批扫描，避免大表一次性加载。
巡检结果写入 `sys_file_check_run` 和 `sys_file_check_item`，保留历史。
重新上传成功后立即把巡检项标记为已恢复；下一轮巡检会自动修正状态。


### 文件巡检增强配置

```text
FILE_CHECK_BATCH_SIZE=500
FILE_CHECK_THREADS=4
```

严重等级和 SLA：

```text
P1 证书/资质/合同：24 小时
P2 文档/巡检图片：72 小时
P3 工单图片：168 小时
```

报告支持：

```text
最近 30 天丢失/恢复趋势图
按公司、医院、文件类型、等级筛选
勾选多个丢失文件批量上传恢复
消息中心点击文件巡检通知直接打开对应巡检项
```

巡检失败、定时巡检完成、文件恢复会写入 `sys_oper_log`。

