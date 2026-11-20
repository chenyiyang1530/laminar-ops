INSERT INTO sys_menu(menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status, create_time)
SELECT '远程启动权限', 3020, 61, '#', NULL, 'device:control', 'F', '0', '0', now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='device:control');

INSERT INTO sys_role_menu(role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms='device:control'
WHERE r.role_key IN ('super_admin','company_admin','company_sub_admin')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id
  );
