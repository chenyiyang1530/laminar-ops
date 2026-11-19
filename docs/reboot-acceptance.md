# 服务器重启验收

在维护窗口执行真实重启，重启完成后运行：

```powershell
& 'C:\Program Files\PowerShell\7\pwsh.exe' -NoProfile -File D:\ideaWork\laminar-ops\tools\reboot-acceptance.ps1
```

脚本检查：

```text
Redis8 服务 Running / Automatic
Redis8 6380 端口监听
旧 Redis 6379 已停用
LaminarOpsBackend 计划任务存在
应用 /actuator/health 返回 200
redisLaminar 组件为 UP
```

结果写入：

```text
D:\ideaWork\laminar-ops\logs\reboot-acceptance-<时间>.txt
```

维护窗口建议顺序：

1. 停止旧 Redis 6379 服务并设为 Disabled。
2. 重启服务器。
3. 等待 `LaminarOpsBackend` 启动。
4. 执行验收脚本。
5. 检查系统监控 → 服务监控中的任务状态和启动日志。

注意：任务状态读取需要 SYSTEM 或管理员权限；普通用户启动的后端会显示“无法读取计划任务”。
