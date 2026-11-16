-- 公司院区服务范围
CREATE TABLE IF NOT EXISTS biz_company_campus (
  id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  campus_id BIGINT NOT NULL,
  hospital_id BIGINT NOT NULL,
  contract_id BIGINT NULL,
  service_status CHAR(1) DEFAULT '0',
  start_date DATE NULL,
  end_date DATE NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_company_campus (company_id,campus_id),
  KEY idx_company_campus_campus (campus_id),
  KEY idx_company_campus_hospital (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司院区服务范围';

INSERT IGNORE INTO biz_company_campus(company_id,campus_id,hospital_id,service_status,start_date)
SELECT ch.company_id,c.campus_id,c.hospital_id,'0',curdate()
FROM biz_company_hospital ch JOIN biz_campus c ON c.hospital_id=ch.hospital_id;
