CREATE TABLE IF NOT EXISTS biz_monitor_test_log (
  test_id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  equipment_code VARCHAR(50) NULL,
  equipment_name VARCHAR(100) NULL,
  point_code VARCHAR(50) NULL,
  point_name VARCHAR(100) NULL,
  test_value DECIMAL(12,3) NULL,
  unit VARCHAR(20) NULL,
  result CHAR(1) NOT NULL DEFAULT '1' COMMENT '1成功 0失败',
  message VARCHAR(500) NULL,
  alarm_triggered CHAR(1) NOT NULL DEFAULT '0',
  operator VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (test_id),
  KEY idx_monitor_test_company_time (company_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备接入测试上报记录';
