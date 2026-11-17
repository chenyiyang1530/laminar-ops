# 加密备份恢复演练

## 演练记录

日期：2026-11-17

源备份：

```text
D:\backups\laminar-ops\laminar-ops-20261117_083703.zip.enc
```

步骤：

```text
1. 解密 .zip.enc
2. 解压出 laminar_ops.sql 和 upload
3. 创建临时库 laminar_ops_drill_20261117
4. 将 SQL 中的 laminar_ops 替换为临时库名
5. 导入临时库
6. 启动后端 18081 连接临时库
7. 使用临时 upload 目录
8. 健康检查、登录、工单查询、配置解密、签名下载
9. 停止临时实例
10. 删除临时库和临时目录
```

## 结果

```text
表数量：77
用户数量：36
工单数量：3
健康检查：200
登录：200
当前用户：drill_admin
工单分页：200
敏感配置解密：Laminar@2026
签名下载：200，3.3MB PDF
```

## 注意

```text
恢复演练必须使用临时数据库和临时 upload 目录
不要覆盖生产库
演练结束后删除临时库和临时目录
```

## 生产恢复流程

```text
停止后端
备份当前库和 upload
解密最新 .zip.enc
解压
恢复 laminar_ops.sql
恢复 upload
启动后端
检查 /actuator/health
抽查登录、工单、文件下载
```
