CREATE TABLE IF NOT EXISTS biz_monitor_gateway (
  gateway_id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  gateway_code VARCHAR(50) NOT NULL,
  gateway_name VARCHAR(100) NOT NULL,
  protocol VARCHAR(30) NULL,
  address VARCHAR(200) NULL,
  token VARCHAR(100) NOT NULL,
  enabled CHAR(1) NOT NULL DEFAULT '1',
  last_seen DATETIME NULL,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL,
  PRIMARY KEY (gateway_id),
  UNIQUE KEY uk_monitor_gateway_code (gateway_code),
  UNIQUE KEY uk_monitor_gateway_token (token),
  KEY idx_monitor_gateway_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备接入网关';

ALTER TABLE biz_equipment
  ADD COLUMN gateway_id BIGINT NULL COMMENT '接入网关' AFTER device_type_id;
