-- 62: 公司资质（公司持有，独立于员工证书）
CREATE TABLE IF NOT EXISTS `biz_company_qualification` (
  `qualification_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL COMMENT '所属公司',
  `qualification_name` varchar(200) NOT NULL COMMENT '资质名称',
  `qualification_type` varchar(50) DEFAULT NULL COMMENT '资质类型',
  `qualification_no` varchar(100) DEFAULT NULL COMMENT '资质编号',
  `level` varchar(30) DEFAULT NULL COMMENT '资质等级',
  `issuing_authority` varchar(200) DEFAULT NULL COMMENT '发证机关',
  `scope` varchar(1000) DEFAULT NULL COMMENT '许可范围',
  `issue_date` date DEFAULT NULL COMMENT '发证日期',
  `expire_date` date DEFAULT NULL COMMENT '到期日期',
  `file_url` varchar(500) DEFAULT NULL COMMENT '扫描件',
  `status` char(1) DEFAULT '0' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`qualification_id`),
  KEY `idx_company_qual_company` (`company_id`),
  KEY `idx_company_qual_expire` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司资质';

INSERT INTO sys_dict_type(dict_name, dict_type, status, remark)
SELECT '公司资质类型', 'company_qual_type', '0', '公司持有的资质类型' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='company_qual_type');

INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 1, '特种设备许可证', 'special_equipment', 'company_qual_type', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_type' AND dict_value='special_equipment');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 2, '安全生产许可证', 'safety_production', 'company_qual_type', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_type' AND dict_value='safety_production');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 3, '建筑业企业资质', 'construction', 'company_qual_type', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_type' AND dict_value='construction');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 4, 'ISO体系认证', 'iso', 'company_qual_type', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_type' AND dict_value='iso');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 5, '其他', 'other', 'company_qual_type', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_type' AND dict_value='other');

INSERT INTO sys_dict_type(dict_name, dict_type, status, remark)
SELECT '公司资质等级', 'company_qual_level', '0', '公司资质等级' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='company_qual_level');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 1, '一级', '1', 'company_qual_level', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_level' AND dict_value='1');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 2, '二级', '2', 'company_qual_level', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_level' AND dict_value='2');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 3, '三级', '3', 'company_qual_level', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_level' AND dict_value='3');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status)
SELECT 4, '不分级', 'none', 'company_qual_level', '', 'N', '0' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='company_qual_level' AND dict_value='none');

INSERT INTO sys_schema_version(version, description, applied_at)
SELECT '62_company_qualification.sql', 'company qualification', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_schema_version WHERE version='62_company_qualification.sql');
