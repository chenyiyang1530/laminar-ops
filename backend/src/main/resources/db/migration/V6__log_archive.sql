-- 日志先归档再删除
CREATE TABLE IF NOT EXISTS `sys_login_log_archive` LIKE `sys_login_log`;
ALTER TABLE `sys_login_log_archive` ADD COLUMN `archived_at` DATETIME NULL;

CREATE TABLE IF NOT EXISTS `sys_oper_log_archive` LIKE `sys_oper_log`;
ALTER TABLE `sys_oper_log_archive` ADD COLUMN `archived_at` DATETIME NULL;

CREATE TABLE IF NOT EXISTS `sys_config_log_archive` LIKE `sys_config_log`;
ALTER TABLE `sys_config_log_archive` ADD COLUMN `archived_at` DATETIME NULL;
