-- 自动归档任务 + 告警/工单慢查询索引
INSERT INTO `sys_job` (`job_name`,`bean_name`,`method_name`,`cron`,`status`,`remark`)
SELECT '历史数据自动归档','archiveService','autoArchive','0 0 2 1 * ?','0','每月1日凌晨2点归档保留期外的已关闭告警和工单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `bean_name`='archiveService' AND `method_name`='autoArchive');

CREATE INDEX `idx_alarm_company_status_time` ON `biz_alarm` (`company_id`,`status`,`first_time`);
CREATE INDEX `idx_alarm_company_time` ON `biz_alarm` (`company_id`,`first_time`);
CREATE INDEX `idx_alarm_company_eq_status` ON `biz_alarm` (`company_id`,`equipment_id`,`status`);
CREATE INDEX `idx_wo_company_status_create` ON `biz_workorder` (`company_id`,`status`,`create_time`);
CREATE INDEX `idx_wo_company_create` ON `biz_workorder` (`company_id`,`create_time`);
