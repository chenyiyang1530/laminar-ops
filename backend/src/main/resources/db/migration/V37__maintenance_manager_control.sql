INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 100, m.menu_id
FROM sys_menu m
WHERE m.perms IN ('device:control:view','device:control')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=100 AND rm.menu_id=m.menu_id
  );
