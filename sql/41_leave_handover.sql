-- 请假审批有在手单时生成待改派任务
CREATE TABLE IF NOT EXISTS `biz_leave_handover` (
  `handover_id`  BIGINT      NOT NULL AUTO_INCREMENT,
  `leave_id`     BIGINT      NOT NULL,
  `engineer_id`  BIGINT      NOT NULL,
  `workorder_id` BIGINT      NOT NULL,
  `status`       VARCHAR(20) DEFAULT 'PENDING',
  `create_time`  DATETIME    DEFAULT NULL,
  `done_time`    DATETIME    DEFAULT NULL,
  PRIMARY KEY (`handover_id`),
  UNIQUE KEY `uk_leave_handover` (`leave_id`,`workorder_id`),
  KEY `idx_handover_workorder` (`workorder_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假待改派工单';
