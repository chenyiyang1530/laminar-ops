-- 手机号/邮箱归属验证
ALTER TABLE `sys_user`
  ADD COLUMN `phone_verified` TINYINT DEFAULT 0 COMMENT '手机号是否已验证' AFTER `email`,
  ADD COLUMN `phone_verified_at` DATETIME DEFAULT NULL COMMENT '手机号验证时间' AFTER `phone_verified`,
  ADD COLUMN `email_verified` TINYINT DEFAULT 0 COMMENT '邮箱是否已验证' AFTER `phone_verified_at`,
  ADD COLUMN `email_verified_at` DATETIME DEFAULT NULL COMMENT '邮箱验证时间' AFTER `email_verified`;

CREATE TABLE IF NOT EXISTS `sys_verification_code` (
  `verification_id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(10) NOT NULL COMMENT 'phone/email',
  `target` VARCHAR(100) NOT NULL,
  `scene` VARCHAR(30) DEFAULT 'bind' COMMENT 'bind',
  `code_hash` VARCHAR(128) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `attempts` INT DEFAULT 0,
  `used` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`verification_id`),
  KEY `idx_verify_target` (`target_type`,`target`,`scene`,`used`),
  KEY `idx_verify_expire` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='验证码';
