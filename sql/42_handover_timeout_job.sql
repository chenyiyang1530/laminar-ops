-- 请假待改派超时提醒任务
INSERT INTO `sys_job` (`job_name`,`bean_name`,`method_name`,`cron`,`status`,`remark`)
SELECT '改派超时提醒','leaveService','escalateHandoverTimeouts','0 0 * * * ?','0','每小时检查超过24小时未完成的请假待改派'
WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `bean_name`='leaveService' AND `method_name`='escalateHandoverTimeouts');
