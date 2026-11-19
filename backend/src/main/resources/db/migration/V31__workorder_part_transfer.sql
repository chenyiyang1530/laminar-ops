CREATE TABLE IF NOT EXISTS biz_workorder_part (
  usage_id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NULL,
  workorder_id BIGINT NOT NULL,
  equipment_id BIGINT NULL,
  equipment_name VARCHAR(150) NULL,
  part_id BIGINT NOT NULL,
  part_name VARCHAR(150) NULL,
  quantity INT NOT NULL DEFAULT 1,
  source VARCHAR(30) NOT NULL DEFAULT 'workorder',
  requisition_item_id BIGINT NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (usage_id),
  KEY idx_wo_part_workorder (workorder_id),
  KEY idx_wo_part_part (part_id),
  KEY idx_wo_part_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单实际使用备件';

CREATE TABLE IF NOT EXISTS biz_part_transfer (
  transfer_id BIGINT NOT NULL AUTO_INCREMENT,
  transfer_no VARCHAR(64) NOT NULL,
  company_id BIGINT NULL,
  part_id BIGINT NOT NULL,
  from_warehouse VARCHAR(100) NOT NULL,
  to_warehouse VARCHAR(100) NOT NULL,
  quantity INT NOT NULL,
  reason VARCHAR(500) NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transfer_id),
  UNIQUE KEY uk_part_transfer_no (transfer_no),
  KEY idx_part_transfer_part (part_id),
  KEY idx_part_transfer_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备件仓库调拨';
