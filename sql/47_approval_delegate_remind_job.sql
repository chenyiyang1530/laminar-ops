-- 审批代理到期提醒任务
INSERT INTO `sys_job` (`job_name`,`bean_name`,`method_name`,`cron`,`status`,`remark`)
SELECT '审批代理到期提醒','approvalService','escalateDelegateExpiry','0 0 9 * * ?','0','每天检查即将到期的审批代理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `bean_name`='approvalService' AND `method_name`='escalateDelegateExpiry');
