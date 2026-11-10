-- 公司高管角色（经营统筹，不参与维保总负责人审批）
INSERT INTO `sys_role` (`role_id`,`role_name`,`role_key`,`sort`,`data_scope`,`status`,`remark`)
SELECT 103,'公司高管','company_executive',5,'all','0','公司经营视角，查看全部业务数据'
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_id`=103);

INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 103,`menu_id` FROM `sys_role_menu` WHERE `role_id`=100
AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id`=103);
