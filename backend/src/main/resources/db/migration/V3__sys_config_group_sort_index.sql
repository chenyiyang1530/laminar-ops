-- 平台参数按分组、排序查询
CREATE INDEX `idx_sys_config_group_sort` ON `sys_config` (`group_code`, `sort`);
