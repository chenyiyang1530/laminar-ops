-- 公司独立组织架构：部门、岗位按公司隔离
ALTER TABLE `sys_dept` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `dept_id`;
ALTER TABLE `sys_post` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `post_id`;

UPDATE `sys_dept` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
UPDATE `sys_post` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;

ALTER TABLE `sys_post` DROP INDEX `uk_post_code`;
ALTER TABLE `sys_post` ADD UNIQUE KEY `uk_post_company_code` (`company_id`,`post_code`);

ALTER TABLE `sys_dept` ADD UNIQUE KEY `uk_dept_company_parent_name` (`company_id`,`parent_id`,`dept_name`);
