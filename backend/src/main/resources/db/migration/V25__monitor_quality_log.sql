CREATE TABLE IF NOT EXISTS biz_monitor_quality_log (
  quality_id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  equipment_code VARCHAR(50) NULL,
  equipment_name VARCHAR(100) NULL,
  point_code VARCHAR(50) NULL,
  point_name VARCHAR(100) NULL,
  metric VARCHAR(30) NULL,
  value DECIMAL(14,3) NULL,
  previous_value DECIMAL(14,3) NULL,
  quality_type VARCHAR(20) NOT NULL COMMENT 'RANGE/JUMP/TIMEOUT',
  message VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (quality_id),
  KEY idx_quality_company_time (company_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关数据质量记录';
