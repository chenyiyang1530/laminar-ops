ALTER TABLE biz_part_adjustment
  ADD COLUMN review_by VARCHAR(64) NULL AFTER approve_time,
  ADD COLUMN review_time DATETIME NULL AFTER review_by;

ALTER TABLE biz_part_reconcile_log
  ADD COLUMN resolved CHAR(1) NOT NULL DEFAULT '0' AFTER difference,
  ADD COLUMN resolved_by VARCHAR(64) NULL AFTER resolved,
  ADD COLUMN resolve_note VARCHAR(500) NULL AFTER resolved_by,
  ADD COLUMN resolved_at DATETIME NULL AFTER resolve_note;

ALTER TABLE biz_part_equipment
  MODIFY COLUMN equipment_id BIGINT NULL,
  ADD COLUMN device_type_id BIGINT NULL AFTER equipment_id,
  ADD COLUMN device_model VARCHAR(100) NULL AFTER device_type_id;

ALTER TABLE biz_part_equipment
  ADD KEY idx_part_equipment_type (device_type_id);
