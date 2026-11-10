-- 审批超时提醒任务
INSERT INTO `sys_job` (`job_name`,`bean_name`,`method_name`,`cron`,`status`,`remark`)
SELECT '审批超时提醒','approvalService','escalateTimeouts','0 0 * * * ?','0','每小时检查超过24小时未处理的审批并通知'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `bean_name`='approvalService' AND `method_name`='escalateTimeouts');
