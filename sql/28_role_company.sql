-- 角色按公司隔离：平台标准角色 company_id 为空，公司自定义角色归属本公司
ALTER TABLE `sys_role` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司，NULL 为平台标准角色' AFTER `role_id`;
ALTER TABLE `sys_role` DROP INDEX `uk_role_key`;
ALTER TABLE `sys_role` ADD UNIQUE KEY `uk_company_role_key` (`company_id`,`role_key`);
ALTER TABLE `sys_role` ADD KEY `idx_role_company` (`company_id`);
