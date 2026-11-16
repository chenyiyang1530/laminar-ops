-- 设备类型/台账第一阶段：类型层级、类型必填、基础类型数据
-- 设备大类已移除：类型名称 + 上级类型即可表达层级。
-- 已应用到 laminar_ops，保留脚本便于新环境初始化。

ALTER TABLE biz_device_type ADD COLUMN parent_id BIGINT NULL AFTER company_id;
ALTER TABLE biz_device_type ADD KEY idx_device_type_parent (parent_id);
ALTER TABLE biz_equipment MODIFY COLUMN device_type_id BIGINT NOT NULL;

INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,NULL,'LAM','层流机组','1',10,'0',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_device_type WHERE company_id=7 AND type_code='LAM');
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'LAM-WARD','层流病房机组','1',10,'0',NOW(),NOW()
FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='LAM'
AND NOT EXISTS (SELECT 1 FROM biz_device_type x WHERE x.company_id=7 AND x.type_code='LAM-WARD');
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'LAM-OR','洁净手术室机组','1',20,'0',NOW(),NOW()
FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='LAM'
AND NOT EXISTS (SELECT 1 FROM biz_device_type x WHERE x.company_id=7 AND x.type_code='LAM-OR');

INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,NULL,'PUR','净化空调','1',20,'0',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_device_type WHERE company_id=7 AND type_code='PUR');
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,NULL,'FRESH','新风机组','1',30,'0',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_device_type WHERE company_id=7 AND type_code='FRESH');
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,NULL,'DEHUM','除湿机组','1',40,'0',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_device_type WHERE company_id=7 AND type_code='DEHUM');
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,NULL,'OXY','供氧汇流排','0',50,'0',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_device_type WHERE company_id=7 AND type_code='OXY');
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,NULL,'VAC','负压真空机组','0',60,'0',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM biz_device_type WHERE company_id=7 AND type_code='VAC');

-- 设备入口收敛：设备台账/类型并入医院档案，独立入口隐藏（visible=1 表示隐藏）
UPDATE sys_menu SET visible=1 WHERE menu_id IN (3010,3100,3200);
