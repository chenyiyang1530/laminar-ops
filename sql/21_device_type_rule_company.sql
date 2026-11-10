-- 设备类型、告警规则按公司隔离
ALTER TABLE `biz_device_type` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `type_id`;
ALTER TABLE `biz_alarm_rule` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `rule_id`;

UPDATE `biz_device_type` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
UPDATE `biz_alarm_rule` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;

ALTER TABLE `biz_device_type` DROP INDEX `uk_dt_code`;
ALTER TABLE `biz_device_type` ADD UNIQUE KEY `uk_company_type_code` (`company_id`,`type_code`);
ALTER TABLE `biz_alarm_rule` ADD KEY `idx_rule_company` (`company_id`);
