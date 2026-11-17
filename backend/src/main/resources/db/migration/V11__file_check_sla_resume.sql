ALTER TABLE sys_file_check_item
    ADD COLUMN escalated CHAR(1) NOT NULL DEFAULT '0' AFTER resolved_time,
    ADD COLUMN escalated_time DATETIME NULL AFTER escalated;

CREATE TABLE IF NOT EXISTS sys_file_check_checkpoint (
    run_id BIGINT NOT NULL,
    ref_key VARCHAR(160) NOT NULL,
    last_id BIGINT NOT NULL DEFAULT 0,
    checked INT NOT NULL DEFAULT 0,
    missing INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'running',
    updated_time DATETIME NOT NULL,
    PRIMARY KEY (run_id, ref_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_file_item_escalation ON sys_file_check_item (status, sla_deadline, escalated);
