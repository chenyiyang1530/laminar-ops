ALTER TABLE sys_file_check_item
    ADD COLUMN escalation_level INT NOT NULL DEFAULT 0 AFTER escalated_time,
    ADD COLUMN last_escalation_time DATETIME NULL AFTER escalation_level;

CREATE TABLE IF NOT EXISTS sys_file_check_lock (
    lock_name VARCHAR(64) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    locked_at DATETIME NOT NULL,
    PRIMARY KEY (lock_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_file_notify_recipient (
    recipient_id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NULL,
    hospital_id BIGINT NULL,
    email VARCHAR(200) NOT NULL,
    enabled CHAR(1) NOT NULL DEFAULT '1',
    create_time DATETIME NOT NULL,
    PRIMARY KEY (recipient_id),
    KEY idx_file_recipient_company (company_id),
    KEY idx_file_recipient_hospital (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_async_job (
    job_id BIGINT NOT NULL AUTO_INCREMENT,
    job_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'running',
    progress INT NOT NULL DEFAULT 0,
    message VARCHAR(2000) NULL,
    result_path VARCHAR(500) NULL,
    create_by VARCHAR(64) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (job_id),
    KEY idx_async_job_status (status),
    KEY idx_async_job_type (job_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
