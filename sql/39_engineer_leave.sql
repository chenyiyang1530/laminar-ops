-- 工程师请假审批（区域负责人 -> 总区域负责人）
ALTER TABLE `biz_region` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '上级区域' AFTER `company_id`;

CREATE TABLE IF NOT EXISTS `biz_engineer_leave` (
  `leave_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `engineer_id`   BIGINT       NOT NULL,
  `company_id`    BIGINT       DEFAULT NULL,
  `leave_type`    VARCHAR(30)  DEFAULT 'personal',
  `start_date`    DATE         NOT NULL,
  `end_date`      DATE         NOT NULL,
  `days`          INT          DEFAULT 1,
  `reason`        VARCHAR(500) DEFAULT NULL,
  `status`        VARCHAR(20)  DEFAULT 'PENDING_REGION',
  `current_step`  INT          DEFAULT 1,
  `handover_engineer_id` BIGINT DEFAULT NULL,
  `created_by`    BIGINT       DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`leave_id`),
  KEY `idx_leave_engineer` (`engineer_id`),
  KEY `idx_leave_company` (`company_id`),
  KEY `idx_leave_status` (`status`),
  KEY `idx_leave_dates` (`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程师请假申请';

CREATE TABLE IF NOT EXISTS `biz_approval_task` (
  `task_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`    VARCHAR(30)  NOT NULL,
  `biz_id`      BIGINT       NOT NULL,
  `step_no`     INT          DEFAULT 1,
  `approver_id` BIGINT       NOT NULL,
  `status`      VARCHAR(20)  DEFAULT 'PENDING',
  `action`      VARCHAR(20)  DEFAULT NULL,
  `comment`     VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `acted_at`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  KEY `idx_task_approver` (`approver_id`,`status`),
  KEY `idx_task_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用审批任务';
