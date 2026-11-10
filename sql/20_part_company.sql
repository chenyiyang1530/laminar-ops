-- 备件与领用按公司隔离
ALTER TABLE `biz_part` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `part_id`;
ALTER TABLE `biz_part_requisition` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `req_id`;

UPDATE `biz_part` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
UPDATE `biz_part_requisition` r
LEFT JOIN `sys_user` u ON u.`user_id` = r.`applicant_id`
SET r.`company_id` = COALESCE(u.`company_id`, (SELECT MIN(`company_id`) FROM `biz_company`))
WHERE r.`company_id` IS NULL;

ALTER TABLE `biz_part` DROP INDEX `uk_part_code`;
ALTER TABLE `biz_part` ADD UNIQUE KEY `uk_company_part_code` (`company_id`,`part_code`);
ALTER TABLE `biz_part_requisition` ADD KEY `idx_req_company` (`company_id`);
