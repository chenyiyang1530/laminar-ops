ALTER TABLE biz_part_equipment
  ADD COLUMN effective_from DATE NULL AFTER device_model,
  ADD COLUMN effective_to DATE NULL AFTER effective_from,
  ADD COLUMN priority INT NOT NULL DEFAULT 0 AFTER effective_to;

ALTER TABLE biz_part_requisition_item
  ADD COLUMN equipment_id BIGINT NULL AFTER part_id,
  ADD COLUMN equipment_name VARCHAR(150) NULL AFTER equipment_id;

ALTER TABLE biz_part_return
  ADD COLUMN equipment_id BIGINT NULL AFTER part_id;

INSERT INTO sys_dict_type(dict_name, dict_type, status, remark)
VALUES ('备件与设备关系', 'part_relation_type', '0', '备件兼容关系类型')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, is_default, status)
SELECT 1, '兼容件', 'compatible', 'part_relation_type', 'Y', '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='part_relation_type' AND dict_value='compatible');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, is_default, status)
SELECT 2, '原件', 'original', 'part_relation_type', 'N', '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='part_relation_type' AND dict_value='original');
INSERT INTO sys_dict_data(dict_sort, dict_label, dict_value, dict_type, is_default, status)
SELECT 3, '替代件', 'substitute', 'part_relation_type', 'N', '0'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='part_relation_type' AND dict_value='substitute');
