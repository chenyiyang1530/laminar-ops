CREATE TABLE IF NOT EXISTS sys_role_assign_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  action VARCHAR(20) NOT NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  detail VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_role_assign_log_role (role_id, create_time),
  KEY idx_role_assign_log_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色分配历史';
