-- 派工评分快照与工单类型
ALTER TABLE biz_workorder
  ADD COLUMN order_type VARCHAR(30) NULL AFTER fault_level,
  ADD COLUMN dispatch_score INT NULL AFTER dispatch_by,
  ADD COLUMN score_detail TEXT NULL AFTER dispatch_score,
  ADD COLUMN score_version VARCHAR(20) NULL AFTER score_detail;
