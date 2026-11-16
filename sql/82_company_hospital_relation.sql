-- 医院主数据与公司服务关系拆分（阶段一：保留 biz_hospital.company_id 作为主归属兼容字段）
CREATE TABLE IF NOT EXISTS biz_company_hospital (
  id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  hospital_id BIGINT NOT NULL,
  contract_id BIGINT NULL,
  region_id BIGINT NULL,
  service_status CHAR(1) DEFAULT '0',
  start_date DATE NULL,
  end_date DATE NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_hospital (company_id,hospital_id),
  KEY idx_company_hospital_hospital (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司医院服务关系';

INSERT IGNORE INTO biz_company_hospital(company_id,hospital_id,region_id,service_status,start_date)
SELECT company_id,hospital_id,region_id,'0',curdate() FROM biz_hospital WHERE company_id IS NOT NULL;
