-- 审批链、唯一区域负责人、唯一总负责人
ALTER TABLE `biz_engineer_leave` ADD COLUMN `approver_chain` VARCHAR(1000) DEFAULT NULL COMMENT '审批人链，逗号分隔用户ID' AFTER `current_step`;
ALTER TABLE `biz_company` ADD COLUMN `total_manager_id` BIGINT DEFAULT NULL COMMENT '总负责人用户ID' AFTER `company_code`;
ALTER TABLE `biz_region` ADD UNIQUE KEY `uk_region_manager` (`manager_id`);
UPDATE `biz_company` SET `total_manager_id`=2 WHERE `company_id`=1 AND `total_manager_id` IS NULL;
UPDATE `biz_company` SET `total_manager_id`=8 WHERE `company_id`=4 AND `total_manager_id` IS NULL;
