-- 正确的设备类型层级
-- 设备类型已移除 category，仅保留类型名称 + 上级类型。
-- 执行前确认 biz_equipment / biz_maintenance_plan 无引用；本脚本会重建公司7类型。

START TRANSACTION;
DELETE FROM biz_device_type WHERE company_id=7;
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time) VALUES
(7,NULL,'LAM','层流机组','1',10,'0',NOW(),NOW()),
(7,NULL,'COLD_HEAT','冷热源机组','1',20,'0',NOW(),NOW()),
(7,NULL,'VAC','负压真空机组','1',30,'0',NOW(),NOW()),
(7,NULL,'AIR','空压机组','1',40,'0',NOW(),NOW()),
(7,NULL,'OXY','供氧汇流排','0',50,'0',NOW(),NOW()),
(7,NULL,'CO2','二氧化碳汇流排','0',60,'0',NOW(),NOW()),
(7,NULL,'N2O','笑气汇流排','0',70,'0',NOW(),NOW());

INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'LAM-MAU','自取新风机组（MAU）','1',10,'0',NOW(),NOW() FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='LAM';
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'LAM-AHU','循环机组（AHU）','1',20,'0',NOW(),NOW() FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='LAM';
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'LAM-PAU','新风机组（PAU）','1',30,'0',NOW(),NOW() FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='LAM';
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'LAM-IRC','内循环机组','1',40,'0',NOW(),NOW() FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='LAM';
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'COLD-MOD','模块机组','1',10,'0',NOW(),NOW() FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='COLD_HEAT';
INSERT INTO biz_device_type(company_id,parent_id,type_code,type_name,monitor_enabled,sort,status,create_time,update_time)
SELECT 7,t.type_id,'COLD-SCREW','螺杆机组','1',20,'0',NOW(),NOW() FROM biz_device_type t WHERE t.company_id=7 AND t.type_code='COLD_HEAT';
COMMIT;

-- 默认全部不监测；上级关闭后下级必须保持关闭
UPDATE biz_device_type SET monitor_enabled='0';
