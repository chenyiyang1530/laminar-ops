-- 告警直接归属公司，避免设备归属变化影响历史告警
ALTER TABLE `biz_alarm` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `alarm_id`;

UPDATE `biz_alarm` a
LEFT JOIN `biz_equipment` e ON e.`equipment_id` = a.`equipment_id`
SET a.`company_id` = COALESCE(e.`company_id`, (
  SELECT MIN(c.`company_id`) FROM `biz_contract` c
  WHERE c.`hospital_id` = a.`hospital_id` AND c.`company_id` IS NOT NULL AND c.`status` = '0'
))
WHERE a.`company_id` IS NULL;

UPDATE `biz_alarm` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
ALTER TABLE `biz_alarm` ADD KEY `idx_alarm_company` (`company_id`);

-- 已存在的归档表同步字段
ALTER TABLE `biz_alarm_archive` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `alarm_id`;
