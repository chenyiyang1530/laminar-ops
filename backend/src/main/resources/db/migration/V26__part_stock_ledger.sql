CREATE TABLE IF NOT EXISTS biz_part_stock (
  stock_id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  part_id BIGINT NOT NULL,
  warehouse VARCHAR(100) NOT NULL DEFAULT 'DEFAULT',
  on_hand_qty INT NOT NULL DEFAULT 0,
  reserved_qty INT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (stock_id),
  UNIQUE KEY uk_part_stock (part_id, warehouse),
  KEY idx_part_stock_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存余额';

INSERT INTO biz_part_stock(company_id, part_id, warehouse, on_hand_qty, reserved_qty)
SELECT company_id, part_id, COALESCE(NULLIF(warehouse, ''), 'DEFAULT'), COALESCE(current_stock, 0), 0
FROM biz_part
ON DUPLICATE KEY UPDATE on_hand_qty = VALUES(on_hand_qty);

CREATE TABLE IF NOT EXISTS biz_part_txn (
  txn_id BIGINT NOT NULL AUTO_INCREMENT,
  txn_no VARCHAR(64) NOT NULL,
  company_id BIGINT NULL,
  part_id BIGINT NOT NULL,
  warehouse VARCHAR(100) NOT NULL DEFAULT 'DEFAULT',
  txn_type VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  before_on_hand INT NOT NULL DEFAULT 0,
  after_on_hand INT NOT NULL DEFAULT 0,
  before_reserved INT NOT NULL DEFAULT 0,
  after_reserved INT NOT NULL DEFAULT 0,
  ref_type VARCHAR(30) NULL,
  ref_id BIGINT NULL,
  idempotency_key VARCHAR(100) NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  txn_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (txn_id),
  UNIQUE KEY uk_part_txn_no (txn_no),
  UNIQUE KEY uk_part_txn_idem (idempotency_key),
  KEY idx_part_txn_part_time (part_id, txn_time),
  KEY idx_part_txn_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件库存流水';

CREATE TABLE IF NOT EXISTS biz_part_equipment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  part_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  relation_type VARCHAR(20) NOT NULL DEFAULT 'compatible',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_part_equipment (part_id, equipment_id),
  KEY idx_part_equipment_equipment (equipment_id),
  KEY idx_part_equipment_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件兼容设备';

ALTER TABLE biz_part_requisition
  ADD COLUMN request_id VARCHAR(100) NULL COMMENT '客户端幂等键' AFTER company_id,
  ADD UNIQUE KEY uk_part_req_request (company_id, request_id);

ALTER TABLE biz_part_requisition_item
  ADD COLUMN reserved_qty INT NOT NULL DEFAULT 0 AFTER quantity,
  ADD COLUMN outbound_qty INT NOT NULL DEFAULT 0 AFTER reserved_qty,
  ADD COLUMN returned_qty INT NOT NULL DEFAULT 0 AFTER outbound_qty;
