-- 第二批补充：模拟设备标记（已有库执行一次）
-- 仅 simulated='1' 的设备参与模拟采集；真实网关设备不会被模拟数据“保活”，离线检测才有意义。
ALTER TABLE `biz_equipment` ADD COLUMN `simulated` CHAR(1) DEFAULT '0' COMMENT '是否模拟设备 1是 0否';

-- 演示环境：现有 12 台设备均为模拟设备；接入真实网关时把对应设备改为 0
UPDATE `biz_equipment` SET `simulated` = '1';
