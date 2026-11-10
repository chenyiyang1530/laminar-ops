-- 审批规则支持区域覆盖
ALTER TABLE `biz_approval_rule` ADD COLUMN `region_id` BIGINT DEFAULT NULL COMMENT '适用区域，空为全公司' AFTER `company_id`;
ALTER TABLE `biz_approval_rule_log` ADD COLUMN `region_id` BIGINT DEFAULT NULL AFTER `company_id`;
ALTER TABLE `biz_approval_rule` ADD KEY `idx_rule_region` (`company_id`,`region_id`,`leave_type`);
