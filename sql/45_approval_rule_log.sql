-- 审批规则变更历史
CREATE TABLE IF NOT EXISTS `biz_approval_rule_log` (
  `log_id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT      DEFAULT NULL,
  `leave_type`    VARCHAR(30) NOT NULL,
  `old_threshold` INT         DEFAULT NULL,
  `old_second`    CHAR(1)     DEFAULT NULL,
  `new_threshold` INT         DEFAULT NULL,
  `new_second`    CHAR(1)     DEFAULT NULL,
  `changed_by`    VARCHAR(64) DEFAULT NULL,
  `change_time`   DATETIME    DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_rule_log_company` (`company_id`,`change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批规则变更历史';
