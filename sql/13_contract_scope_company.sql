-- 多公司维保：合同服务范围 + 工单承接公司
CREATE TABLE IF NOT EXISTS `biz_contract_scope` (
  `scope_id`    BIGINT      NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT      NOT NULL,
  `scope_type`  VARCHAR(20) NOT NULL COMMENT 'hospital/department/area/equipment',
  `scope_value` BIGINT      DEFAULT NULL,
  PRIMARY KEY (`scope_id`),
  UNIQUE KEY `uk_contract_scope` (`contract_id`,`scope_type`,`scope_value`),
  KEY `idx_scope_target` (`scope_type`,`scope_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合同服务范围';

ALTER TABLE `biz_workorder`
  ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '承接维保公司（按合同范围自动匹配）' AFTER `equipment_id`,
  ADD KEY `idx_wo_company` (`company_id`);
