-- 历史告警/工单归档
CREATE TABLE IF NOT EXISTS `biz_alarm_archive` LIKE `biz_alarm`;
ALTER TABLE `biz_alarm_archive` ADD COLUMN `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间';

CREATE TABLE IF NOT EXISTS `biz_workorder_archive` LIKE `biz_workorder`;
ALTER TABLE `biz_workorder_archive` ADD COLUMN `archived_at` DATETIME DEFAULT NULL COMMENT '归档时间';

CREATE INDEX `idx_alarm_archive_time` ON `biz_alarm_archive` (`archived_at`);
CREATE INDEX `idx_workorder_archive_time` ON `biz_workorder_archive` (`archived_at`);
