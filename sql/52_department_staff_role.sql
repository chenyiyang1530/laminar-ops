-- 部门员工角色（仅组织成员，无业务菜单）
INSERT INTO `sys_role` (`role_id`,`role_name`,`role_key`,`sort`,`data_scope`,`status`,`remark`)
SELECT 104,'部门员工','department_staff',6,'self','0','仅作为部门成员，无业务菜单权限'
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_id`=104);
