-- 数据库迁移版本记录
CREATE TABLE IF NOT EXISTS `sys_schema_version` (
  `version`     VARCHAR(120) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `applied_at`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库迁移版本';
