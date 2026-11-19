CREATE TABLE IF NOT EXISTS biz_part_adjustment (
  adjustment_id BIGINT NOT NULL AUTO_INCREMENT,
  adjustment_no VARCHAR(64) NOT NULL,
  company_id BIGINT NULL,
  part_id BIGINT NOT NULL,
  warehouse VARCHAR(100) NOT NULL DEFAULT 'DEFAULT',
  adjust_type VARCHAR(10) NOT NULL COMMENT 'in/out',
  quantity INT NOT NULL,
  reason_type VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0待审批 1已通过 2已驳回',
  applicant_id BIGINT NULL,
  applicant_name VARCHAR(64) NULL,
  approve_by VARCHAR(64) NULL,
  approve_time DATETIME NULL,
  before_on_hand INT NULL,
  after_on_hand INT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (adjustment_id),
  UNIQUE KEY uk_part_adjustment_no (adjustment_no),
  KEY idx_part_adjustment_company_status (company_id, status),
  KEY idx_part_adjustment_part (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存调整单';

CREATE TABLE IF NOT EXISTS biz_part_return (
  return_id BIGINT NOT NULL AUTO_INCREMENT,
  return_no VARCHAR(64) NOT NULL,
  company_id BIGINT NULL,
  req_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  part_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (return_id),
  UNIQUE KEY uk_part_return_no (return_no),
  KEY idx_part_return_req (req_id),
  KEY idx_part_return_part (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件退料审计';

CREATE TABLE IF NOT EXISTS biz_part_reconcile_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  part_id BIGINT NOT NULL,
  part_name VARCHAR(100) NULL,
  warehouse VARCHAR(100) NULL,
  book_stock INT NOT NULL DEFAULT 0,
  stock_sum INT NOT NULL DEFAULT 0,
  difference INT NOT NULL DEFAULT 0,
  checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_part_reconcile_part_time (part_id, checked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存对账差异';
