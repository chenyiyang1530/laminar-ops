ALTER TABLE sys_role_assign_log
  ADD COLUMN role_name_snapshot VARCHAR(64) NULL AFTER role_id;

UPDATE sys_role_assign_log l
JOIN sys_role r ON r.role_id = l.role_id
SET l.role_name_snapshot = r.role_name
WHERE l.role_name_snapshot IS NULL;
