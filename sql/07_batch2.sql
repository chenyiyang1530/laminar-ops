-- 第二批修复：编号唯一、告警去重、离线检测任务（已有库执行一次）
-- 1. 关闭历史重复的活动告警，保留最新一条
UPDATE `biz_alarm` a
JOIN (SELECT `point_id`, MAX(`alarm_id`) AS keep_id
      FROM `biz_alarm`
      WHERE `status` IN ('0','1','2') AND `point_id` IS NOT NULL
      GROUP BY `point_id` HAVING COUNT(*) > 1) d
  ON a.`point_id` = d.`point_id` AND a.`alarm_id` <> d.keep_id
SET a.`status` = '3', a.`close_time` = NOW(),
    a.`root_cause` = '历史重复告警合并', a.`solution` = '系统自动合并关闭'
WHERE a.`status` IN ('0','1','2');

-- 2. 同一测点只允许一条活动告警（活动态生成列 + 唯一索引）
ALTER TABLE `biz_alarm`
  ADD COLUMN `active_point` BIGINT
  GENERATED ALWAYS AS (CASE WHEN `status` IN ('0','1','2') AND `point_id` IS NOT NULL THEN `point_id` ELSE NULL END) STORED,
  ADD UNIQUE KEY `uk_alarm_active_point` (`active_point`);

-- 3. 巡检/保养任务编号唯一
ALTER TABLE `biz_inspection_task` ADD UNIQUE KEY `uk_it_task_no` (`task_no`);
ALTER TABLE `biz_maintenance_task` ADD UNIQUE KEY `uk_mt_task_no` (`task_no`);

-- 4. 离线检测任务（每分钟）
INSERT IGNORE INTO `sys_job` (`job_id`,`job_name`,`bean_name`,`method_name`,`cron`,`status`,`remark`) VALUES
(1,'监测采集','monitorCollectTask','collect','0/30 * * * * ?','0','模拟采集/网关数据入库与告警判定'),
(2,'巡检任务生成','inspectionService','generate','0 10 0 * * ?','0','按巡检计划生成任务'),
(3,'保养任务生成','maintenanceService','generate','0 20 0 * * ?','0','按保养计划生成任务'),
(4,'合同/证书到期提醒','reminderTask','remind','0 0 9 * * ?','0','每日推送到期提醒'),
(5,'设备离线检测','monitorCollectTask','checkOffline','0 * * * * ?','0','心跳超时标记离线并生成告警');
