-- 院方验收权限：只有医院账号可见/可操作
INSERT IGNORE INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,perms,menu_type,icon,visible,status,create_time)
VALUES (510016,'工单验收',5100,16,'#',NULL,'workorder:accept','F',NULL,'0','0',NOW());
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES
(200,5000),(200,5110),(200,510016),
(201,5000),(201,5110),(201,510016);
