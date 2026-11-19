CREATE TABLE IF NOT EXISTS biz_part_stocktake (
  stocktake_id BIGINT NOT NULL AUTO_INCREMENT,
  stocktake_no VARCHAR(64) NOT NULL,
  company_id BIGINT NULL,
  warehouse VARCHAR(100) NOT NULL,
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0待审批 1已通过 2已驳回',
  applicant_id BIGINT NULL,
  applicant_name VARCHAR(64) NULL,
  approve_by VARCHAR(64) NULL,
  approve_time DATETIME NULL,
  reason VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (stocktake_id),
  UNIQUE KEY uk_part_stocktake_no (stocktake_no),
  KEY idx_part_stocktake_status (status),
  KEY idx_part_stocktake_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件盘点单';

CREATE TABLE IF NOT EXISTS biz_part_stocktake_item (
  item_id BIGINT NOT NULL AUTO_INCREMENT,
  stocktake_id BIGINT NOT NULL,
  part_id BIGINT NOT NULL,
  part_name VARCHAR(150) NULL,
  book_qty INT NOT NULL DEFAULT 0,
  actual_qty INT NOT NULL DEFAULT 0,
  difference INT NOT NULL DEFAULT 0,
  reason VARCHAR(300) NULL,
  PRIMARY KEY (item_id),
  KEY idx_part_stocktake_item_st (stocktake_id),
  KEY idx_part_stocktake_item_part (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件盘点明细';
