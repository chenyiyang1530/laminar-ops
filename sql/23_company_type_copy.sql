-- 给已有设备但尚无自有类型的公司复制一份独立设备类型，并重绑设备
INSERT INTO `biz_device_type`
(`company_id`,`type_code`,`type_name`,`category`,`icon`,`monitor_enabled`,`param_template`,`sort`,`status`,`remark`,`create_time`)
SELECT 4, CONCAT(`type_code`,'-ASL'), `type_name`, `category`, `icon`, `monitor_enabled`, `param_template`, `sort`, `status`, `remark`, NOW()
FROM `biz_device_type`
WHERE `company_id` = 1;

UPDATE `biz_equipment` e
JOIN `biz_device_type` oldt ON oldt.`type_id` = e.`device_type_id`
JOIN `biz_device_type` newt ON newt.`company_id` = 4 AND newt.`type_code` = CONCAT(oldt.`type_code`,'-ASL')
SET e.`device_type_id` = newt.`type_id`
WHERE e.`company_id` = 4;

-- 清理历史遗留的无公司类型（无设备引用）
DELETE FROM `biz_device_type`
WHERE `company_id` IS NULL
  AND `type_id` NOT IN (SELECT DISTINCT `device_type_id` FROM `biz_equipment` WHERE `device_type_id` IS NOT NULL);
