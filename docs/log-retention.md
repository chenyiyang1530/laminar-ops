# 日志归档

## 策略

每天 03:30 执行 `LogRetentionJob`。

处理顺序：

```text
登录日志 sys_login_log
操作日志 sys_oper_log
参数日志 sys_config_log
```

对每种日志：

```text
超过保留天数
→ INSERT IGNORE 到归档表
→ 再删除原表数据
```

归档表：

```text
sys_login_log_archive
sys_oper_log_archive
sys_config_log_archive
```

归档表保留原表全部字段，并增加：

```text
archived_at
```

保留天数来自平台参数：

```text
platform.login.logRetentionDays
platform.audit.logRetentionDays
platform.config.logRetentionDays
```

设置为：

```text
0
```

表示不清理。

## 数据结构

Flyway：

```text
V6__log_archive.sql
```

如果归档表不存在，任务也会自动创建并补齐 `archived_at`。

## 注意

```text
归档表不会自动再次清理
长期运行后需要为归档表制定二级保留策略
归档表数据量较大时建议迁移到独立归档库
```
