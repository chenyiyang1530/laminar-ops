-- 部门与地理区域映射
ALTER TABLE `sys_dept` ADD COLUMN `region_id` BIGINT DEFAULT NULL AFTER `parent_id`;
ALTER TABLE `sys_dept` ADD UNIQUE KEY `uk_dept_region` (`region_id`);
