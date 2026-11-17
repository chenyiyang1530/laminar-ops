CREATE TABLE IF NOT EXISTS sys_file_check_run (
    run_id BIGINT NOT NULL AUTO_INCREMENT,
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'manual',
    status VARCHAR(20) NOT NULL DEFAULT 'running',
    checked INT NOT NULL DEFAULT 0,
    missing INT NOT NULL DEFAULT 0,
    recovered INT NOT NULL DEFAULT 0,
    start_time DATETIME NOT NULL,
    end_time DATETIME NULL,
    error_msg VARCHAR(500) NULL,
    PRIMARY KEY (run_id),
    KEY idx_file_run_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_file_check_item (
    item_id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    record_id BIGINT NOT NULL,
    file_column VARCHAR(64) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    company_id BIGINT NULL,
    hospital_id BIGINT NULL,
    status CHAR(1) NOT NULL DEFAULT '0',
    first_seen DATETIME NOT NULL,
    last_seen DATETIME NOT NULL,
    resolved_time DATETIME NULL,
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_file_check_item (table_name, record_id, file_column),
    KEY idx_file_item_status (status),
    KEY idx_file_item_run (run_id),
    KEY idx_file_item_company (company_id),
    KEY idx_file_item_hospital (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, perms, menu_type, icon, visible, status, create_time)
SELECT '文件巡检', 9200, 80, '/system/file-check', 'system/file-check/index', 'system:filecheck:list', 'C', 'DocumentChecked', '0', '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:filecheck:list');

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, perms, menu_type, icon, visible, status, create_time)
SELECT '执行巡检', (SELECT menu_id FROM sys_menu WHERE perms = 'system:filecheck:list' LIMIT 1), 1, '#', NULL, 'system:filecheck:run', 'F', NULL, '0', '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:filecheck:run');

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, perms, menu_type, icon, visible, status, create_time)
SELECT '重新上传', (SELECT menu_id FROM sys_menu WHERE perms = 'system:filecheck:list' LIMIT 1), 2, '#', NULL, 'system:filecheck:edit', 'F', NULL, '0', '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:filecheck:edit');

INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE perms IN ('system:filecheck:list', 'system:filecheck:run', 'system:filecheck:edit');
