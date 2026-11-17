ALTER TABLE sys_role
  ADD COLUMN assignable CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否可分配';

UPDATE sys_role SET assignable = '0' WHERE role_id IN (1, 200, 201);
UPDATE sys_role SET assignable = '1' WHERE role_id IN (100, 101, 102, 103, 104, 202, 213, 214);
