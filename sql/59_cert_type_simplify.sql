-- 证书类型收敛为两个大类：技能证书、特种设备作业证；具体证书放证书名称
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`list_class`)
SELECT 1,'技能证书','skill_certificate','cert_type','' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='cert_type' AND `dict_value`='skill_certificate');

UPDATE `sys_dict_data` SET `dict_sort`=2, `dict_label`='特种设备作业证'
WHERE `dict_type`='cert_type' AND `dict_value`='special_equipment';

UPDATE `biz_certificate`
SET `cert_type`='skill_certificate'
WHERE `cert_type` IS NOT NULL AND `cert_type` NOT IN ('skill_certificate','special_equipment');

UPDATE `biz_cert_type_request`
SET `cert_type`='skill_certificate'
WHERE `cert_type` NOT IN ('skill_certificate','special_equipment');

DELETE FROM `sys_dict_data`
WHERE `dict_type`='cert_type' AND `dict_value` NOT IN ('skill_certificate','special_equipment');
