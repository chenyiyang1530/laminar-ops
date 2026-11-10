-- 审批代理
CREATE TABLE IF NOT EXISTS `biz_approval_delegate` (
  `delegate_id`    BIGINT NOT NULL AUTO_INCREMENT,
  `company_id`     BIGINT DEFAULT NULL,
  `user_id`        BIGINT NOT NULL,
  `target_user_id` BIGINT NOT NULL,
  `start_date`     DATE   NOT NULL,
  `end_date`       DATE   NOT NULL,
  `status`         CHAR(1) DEFAULT '0',
  `create_time`    DATETIME DEFAULT NULL,
  PRIMARY KEY (`delegate_id`),
  KEY `idx_delegate_user` (`user_id`,`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批代理';
