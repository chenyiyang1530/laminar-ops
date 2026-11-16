-- 设备类型/设备台账并入医院档案，隐藏独立设备管理入口
UPDATE sys_menu SET visible=1 WHERE menu_id IN (3010,3100,3200);

