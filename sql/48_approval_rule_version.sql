-- 审批规则版本快照
CREATE TABLE IF NOT EXISTS `biz_approval_rule_version` (
  `version_id`     BIGINT      NOT NULL AUTO_INCREMENT,
  `company_id`     BIGINT      DEFAULT NULL,
  `region_id`      BIGINT      DEFAULT NULL,
  `version_no`     INT         NOT NULL,
  `status`         VARCHAR(20) DEFAULT 'PUBLISHED',
  `snapshot_json`  LONGTEXT,
  `created_by`     VARCHAR(64) DEFAULT NULL,
  `create_time`    DATETIME    DEFAULT NULL,
  `published_by`   VARCHAR(64) DEFAULT NULL,
  `published_time` DATETIME    DEFAULT NULL,
  PRIMARY KEY (`version_id`),
  KEY `idx_version_scope` (`company_id`,`region_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批规则版本快照';
