# 备份与恢复

## 备份内容

```text
MySQL：laminar_ops
上传文件：backend/upload
```

## 加密

备份生成后会自动加密为：

```text
laminar-ops-时间.zip.enc
```

加密方式：

```text
AES-256-CBC
HMAC-SHA256 完整性校验
PBKDF2-SHA256 100000 次派生
```

密码来自：

```text
BACKUP_ENCRYPTION_PASSWORD
```

密码已保存在本机 `.secrets.local`，不会提交仓库。

解密：

```powershell
& "C:\Program Files\PowerShell\7\pwsh.exe" -NoProfile `
  -File D:\ideaWork\laminar-ops\tools\decrypt-backup.ps1 `
  -EncryptedPath D:\backups\laminar-ops\laminar-ops-时间.zip.enc `
  -OutputPath D:\ideaWork\restore.zip
```

然后按恢复步骤导入解压后的 SQL 和 upload 目录。


## 应用内定时备份

后端已增加 `BackupJob`，默认每天 02:00 调用：

```text
tools/backup.ps1
```

配置：

```yaml
laminar:
  backup:
    enabled: ${BACKUP_ENABLED:true}
    script: ${BACKUP_SCRIPT:D:/ideaWork/laminar-ops/tools/backup.ps1}
    dir: ${BACKUP_DIR:D:/backups/laminar-ops}
    cron: ${BACKUP_CRON:0 0 2 * * *}
```

超级管理员也可以手动触发：

```text
POST /api/system/backup/run
```

## Windows 计划任务（可选）

如果不想依赖应用进程，可以注册系统计划任务：

先设置数据库密码（只设置一次）：

```powershell
setx DB_PASSWORD "你的数据库密码"
```

以**管理员身份**打开 PowerShell 7，执行：

```powershell
& "C:\Program Files\PowerShell\7\pwsh.exe" -NoProfile `
  -File D:\ideaWork\laminar-ops\tools\install-backup-task.ps1
```

默认：

```text
任务名：LaminarOpsBackup
执行时间：每天 02:00
备份目录：D:\backups\laminar-ops
```

也可以手动执行备份：

```powershell
powershell -ExecutionPolicy Bypass -File D:\ideaWork\laminar-ops\tools\backup.ps1
```

## 恢复演练

不要直接在正式库上演练。执行：

```powershell
$env:DB_PASSWORD="你的数据库密码"
& "C:\Program Files\PowerShell\7\pwsh.exe" -NoProfile `
  -File D:\ideaWork\laminar-ops\tools\restore-drill.ps1
```

脚本会：

```text
导出源库
创建临时库 laminar_ops_drill_时间戳
恢复到临时库
校验表数量和 sys_user
删除临时库
```

演练成功输出示例：

```text
恢复成功：表 74 张，用户 36 个
```


## 恢复

1. 停止后端。
2. 解压备份 zip。
3. 恢复数据库：

```bash
mysql --host=127.0.0.1 --user=root -p < laminar_ops.sql
```

4. 把备份中的 `upload` 目录复制回 `backend/upload`。
5. 启动后端，检查健康接口：

```text
http://127.0.0.1:8080/actuator/health
```

## 注意

- 备份文件包含数据库和上传文件，属于敏感数据，必须限制目录权限。
- 恢复前先停止服务，避免写入过程中被覆盖。
- 定期做恢复演练，不要只备份不验证。
- 生产环境建议使用对象存储/异地备份。
