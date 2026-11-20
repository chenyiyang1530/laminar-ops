CREATE TABLE IF NOT EXISTS biz_control_user_equipment (
  user_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  company_id BIGINT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, equipment_id),
  KEY idx_control_scope_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司子管理员可控制设备范围';

ALTER TABLE biz_control_command
  ADD COLUMN review_by VARCHAR(64) NULL AFTER operator_name,
  ADD COLUMN review_time DATETIME NULL AFTER review_by,
  ADD COLUMN review_remark VARCHAR(500) NULL AFTER review_time,
  ADD COLUMN interlock_snapshot TEXT NULL AFTER review_remark;

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, create_time)
SELECT '远程控制复核', 3020, 62, '#', NULL, 'device:control:approve', 'F', '0', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='device:control:approve');

INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, create_time)
SELECT '远程控制范围', 3020, 63, '#', NULL, 'device:control:scope', 'F', '0', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='device:control:scope');

INSERT INTO sys_role_menu(role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('device:control:approve','device:control:scope')
WHERE r.role_key IN ('super_admin','company_admin')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id
  );
