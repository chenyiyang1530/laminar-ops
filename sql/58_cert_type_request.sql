-- 证书类型字典申请：公司新增证书时输入的新类型提交管理员审核
CREATE TABLE IF NOT EXISTS `biz_cert_type_request` (
  `request_id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT DEFAULT NULL,
  `cert_type` VARCHAR(100) NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `requested_by` BIGINT DEFAULT NULL,
  `reviewed_by` BIGINT DEFAULT NULL,
  `review_comment` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `review_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `idx_cert_type_status` (`status`,`company_id`),
  KEY `idx_cert_type_name` (`cert_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书类型字典申请';
