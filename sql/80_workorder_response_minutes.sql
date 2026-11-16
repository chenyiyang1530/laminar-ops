-- 工单响应时长（告警/创建 → 到场）自动计算
ALTER TABLE biz_workorder ADD COLUMN response_minutes INT NULL AFTER downtime_minutes;
