-- 监测点位直接归属公司
ALTER TABLE `biz_monitor_point` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `point_id`;
UPDATE `biz_monitor_point` p
LEFT JOIN `biz_equipment` e ON e.`equipment_id` = p.`equipment_id`
SET p.`company_id` = e.`company_id`
WHERE p.`company_id` IS NULL;
UPDATE `biz_monitor_point` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
ALTER TABLE `biz_monitor_point` ADD KEY `idx_point_company` (`company_id`);
