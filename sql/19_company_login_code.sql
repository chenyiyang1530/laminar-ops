-- 租户登录：公司编码 + 用户名；公司编码历史别名，编码变更后旧编码仍可登录
CREATE TABLE IF NOT EXISTS `biz_company_code_alias` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `alias_code` VARCHAR(64) NOT NULL,
  `company_id` BIGINT      NOT NULL,
  `create_time` DATETIME   DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_alias_code` (`alias_code`),
  KEY `idx_company_alias` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司编码历史别名';

ALTER TABLE `sys_user` DROP INDEX `uk_user_name`;
ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_company_username` (`company_id`,`username`);
