CREATE TABLE IF NOT EXISTS biz_modbus_template (
  template_id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  gateway_id BIGINT NULL,
  template_code VARCHAR(50) NOT NULL,
  template_name VARCHAR(100) NOT NULL,
  config_json LONGTEXT NOT NULL,
  enabled CHAR(1) NOT NULL DEFAULT '1',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL,
  PRIMARY KEY (template_id),
  UNIQUE KEY uk_modbus_template_code (template_code),
  KEY idx_modbus_template_company (company_id),
  KEY idx_modbus_template_gateway (gateway_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Modbus寄存器映射模板';
