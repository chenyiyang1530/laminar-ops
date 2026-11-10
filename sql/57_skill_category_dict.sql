-- 技能标签分类字典
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`list_class`)
SELECT x.s, x.l, x.v, 'skill_category', '' FROM (
  SELECT 1 s, '设备' l, '设备' v UNION ALL
  SELECT 2, '检测', '检测' UNION ALL
  SELECT 3, '自控', '自控' UNION ALL
  SELECT 4, '运维', '运维' UNION ALL
  SELECT 5, '安全', '安全' UNION ALL
  SELECT 6, '管理', '管理' UNION ALL
  SELECT 7, '通用', '通用'
) x
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_dict_data` d WHERE d.`dict_type`='skill_category' AND d.`dict_value`=x.v
);
