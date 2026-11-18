CREATE TABLE IF NOT EXISTS biz_monitor_token_log (
  token_log_id BIGINT NOT NULL AUTO_INCREMENT,
  gateway_id BIGINT NOT NULL,
  company_id BIGINT NULL,
  action VARCHAR(20) NOT NULL COMMENT 'CREATE/ROTATE',
  operator VARCHAR(64) NULL,
  token_suffix VARCHAR(12) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (token_log_id),
  KEY idx_token_log_gateway (gateway_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关Token轮换审计';
