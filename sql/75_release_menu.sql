-- 版本更新页：仅通过头像下拉入口访问，系统管理菜单中隐藏
INSERT IGNORE INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,perms,menu_type,icon,visible,status,create_time)
VALUES (9260,'版本更新',9200,20,'/release','release/index','system:release:list','C','Document','1','0',NOW());
INSERT IGNORE INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,perms,menu_type,icon,visible,status,create_time)
VALUES (926010,'版本更新修改',9260,1,'#',NULL,'system:release:edit','F',NULL,'1','0',NOW());
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) VALUES
(1,9260),(1,926010),
(213,9260),(213,926010),
(214,9260),(214,926010);
