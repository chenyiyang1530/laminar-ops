-- 巡检管理与保养管理合并为一个入口
UPDATE sys_menu SET menu_name='巡检保养', visible=0 WHERE menu_id=5310;
UPDATE sys_menu SET visible=1 WHERE menu_id=5510;
