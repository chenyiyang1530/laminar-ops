-- 工单完工明细
ALTER TABLE biz_workorder
  ADD COLUMN finish_root_cause VARCHAR(500) NULL AFTER repair_report,
  ADD COLUMN finish_solution VARCHAR(1000) NULL AFTER finish_root_cause,
  ADD COLUMN finish_parts VARCHAR(500) NULL AFTER finish_solution,
  ADD COLUMN finish_image VARCHAR(500) NULL AFTER finish_parts;
