# 平台设计说明

## 1. 角色与数据边界

| 角色 key | 使用者 | 数据范围 |
| --- | --- | --- |
| `super_admin` | 平台管理员 | 全部 |
| `maintenance_manager` | 我方维保经理 | 全部业务 |
| `regional_manager` | 区域主管 | 本区域医院 |
| `engineer` | 维保工程师 | 本人工单/任务 |
| `hospital_admin` | 院方管理员 | 本院全部（`biz_hospital_user` 绑定） |
| `hospital_dept` | 院方科室用户 | 本科室 |

后台接口：`/api/**`，`@Perm("模块:资源:动作")` 注解 + JWT 拦截器校验，权限来源于 `sys_role_menu` → `sys_menu.perms`。
院方接口：`/api/portal/**`，拦截器强制校验 `userType=2`，Service 用绑定医院覆盖查询条件，禁止跨院。

## 2. 核心数据模型

```
公司 biz_company
 └─ 区域 biz_region
     └─ 医院 biz_hospital
         ├─ 院区 biz_campus
         │   └─ 科室 biz_department
         │       └─ 洁净区域 biz_clean_area
         │           └─ 设备 biz_equipment（device_type_id → biz_device_type）
         │               └─ 监测点位 biz_monitor_point
         │                   └─ 历史数据 biz_monitor_data
         ├─ 对接人 biz_contact
         └─ 合同 biz_contract
工单 biz_workorder ─┬─ 费用明细 biz_workorder_cost
                    ├─ 流转日志 biz_workorder_log
                    ├─ 告警 biz_alarm ── 追溯 biz_alarm_log
                    ├─ 巡检任务 biz_inspection_task
                    └─ 保养任务 biz_maintenance_task
工程师 biz_engineer ─┬─ 技能 biz_engineer_skill
                     ├─ 服务医院 biz_engineer_hospital
                     └─ 证书 biz_certificate
备件 biz_part ─ 流水 biz_part_record / 领用 biz_part_requisition(_item)
文档 biz_document / 知识库 biz_knowledge / 预算 biz_budget(_execution) / 消息 biz_message
```

## 3. 状态机

**告警**：`0 未确认 → 1 已确认 → 2 处理中 → 3 已消缺`，旁支 `4 已忽略`。
同一设备+点位在未消缺期间合并，刷新 `last_time/duration`；所有动作写 `biz_alarm_log`。

**工单**：`0 待受理 → 1 待派工 → 2 处理中 → 3 待验收 → 4 待回访 → 5 已完成`，旁支 `6 已取消`。
验收驳回回到 2；保外工单在验收前录入报价；回访记录 1-5 分满意度。

**巡检/保养任务**：`0 待执行 → 1 执行中 → 2 已完成`，`3 已逾期`、`4 已取消`；异常项可转工单。

## 4. 告警与采集

- 演示环境：`monitorCollectTask` 每 30 秒为启用点位生成数值（随机游走，8% 概率越限），写 Redis 最新值 + 历史表，并匹配 `biz_alarm_rule` 生成/合并告警。
- 生产：设备网关通过 `/api/open/monitor/report`（token 校验）上报，字段 `equipmentCode/pointCode/value/collectTime`。
- 告警级别：1 紧急、2 重要、3 一般、4 提示；通知落 `biz_message`，前端 60 秒轮询未读数。

## 5. 接口约定

统一响应：`{ code: 200, msg: "操作成功", data: ... }`；分页：`{ code, data: { records: [], total } }`。
认证：`Authorization: Bearer <token>`；未登录 401，无权限 403。

## 6. 定时任务

| 任务 | 频率 | 说明 |
| --- | --- | --- |
| `monitorCollectTask.collect` | 30s | 采集模拟 + 告警判定 |
| `alarmOfflineTask.check` | 1min | 心跳超时告警 |
| `contractWarnTask.remind` | 每日 09:00 | 合同 90/60/30 天提醒 |
| `certWarnTask.remind` | 每日 09:10 | 证书 60/30 天提醒 |
| `inspectionTask.generate` | 每日 00:10 | 生成巡检任务、标记逾期 |
| `maintenanceTask.generate` | 每日 00:20 | 生成保养任务 |
| `workorderCallbackTask.remind` | 每日 10:00 | 待回访提醒 |
| `certStatusTask.refresh` | 每日 00:30 | 证书/合同状态刷新 |

任务注册在 `sys_job`，可在“系统管理-定时任务”启停与手动触发。
