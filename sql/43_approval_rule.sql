-- 请假类型审批规则
ALTER TABLE `biz_engineer_leave` ADD COLUMN `need_second` TINYINT DEFAULT 1 COMMENT '是否需要二级审批' AFTER `days`;

CREATE TABLE IF NOT EXISTS `biz_approval_rule` (
  `rule_id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT      DEFAULT NULL,
  `leave_type`    VARCHAR(30) NOT NULL,
  `threshold_days` INT        DEFAULT 3,
  `second_level`  CHAR(1)     DEFAULT '1',
  `status`        CHAR(1)     DEFAULT '0',
  `update_by`     VARCHAR(64) DEFAULT NULL,
  `update_time`   DATETIME    DEFAULT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_company` (`company_id`,`leave_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假审批规则';
