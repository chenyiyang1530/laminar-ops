UPDATE sys_role
SET role_type = 'standard'
WHERE company_id IS NULL
  AND role_id IN (1, 100, 101, 102, 103, 104, 200, 201, 202, 213, 214);
