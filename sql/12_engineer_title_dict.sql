-- 工程师职称字典（已有库升级；新库由 02_seed_core.sql 提供）
INSERT INTO `sys_dict_type` (`dict_name`,`dict_type`,`status`,`remark`)
SELECT '工程师职称','engineer_title','0','' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type`='engineer_title');

INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`list_class`)
SELECT * FROM (
  SELECT 1 AS s,'助理工程师' AS l,'assistant' AS v,'engineer_title' AS t,'' AS c
  UNION ALL SELECT 2,'中级工程师','intermediate','engineer_title',''
  UNION ALL SELECT 3,'高级工程师','senior','engineer_title',''
  UNION ALL SELECT 4,'正高级工程师','professor','engineer_title',''
  UNION ALL SELECT 5,'技师','technician','engineer_title',''
  UNION ALL SELECT 6,'高级技师','senior_technician','engineer_title',''
  UNION ALL SELECT 7,'区域主管','regional_manager','engineer_title',''
) x
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` d WHERE d.`dict_type`='engineer_title' AND d.`dict_value`=x.v);

UPDATE `biz_engineer`
SET `job_title` = CASE `job_title`
  WHEN '助理工程师' THEN 'assistant'
  WHEN '中级工程师' THEN 'intermediate'
  WHEN '高级工程师' THEN 'senior'
  WHEN '正高级工程师' THEN 'professor'
  WHEN '技师' THEN 'technician'
  WHEN '高级技师' THEN 'senior_technician'
  WHEN '区域主管' THEN 'regional_manager'
  ELSE `job_title`
END
WHERE `job_title` IN ('助理工程师','中级工程师','高级工程师','正高级工程师','技师','高级技师','区域主管');
