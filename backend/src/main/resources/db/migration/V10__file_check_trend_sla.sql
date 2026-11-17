ALTER TABLE sys_file_check_item
    ADD COLUMN file_type VARCHAR(20) NULL AFTER file_path,
    ADD COLUMN severity VARCHAR(4) NULL AFTER file_type,
    ADD COLUMN sla_hours INT NULL AFTER severity,
    ADD COLUMN sla_deadline DATETIME NULL AFTER sla_hours;

CREATE INDEX idx_file_item_first_seen ON sys_file_check_item (first_seen);
CREATE INDEX idx_file_item_resolved ON sys_file_check_item (resolved_time);
