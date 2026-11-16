-- 洁净区域二期：物理删除 area_id 和 biz_clean_area
-- 执行前已备份：backup_before_company_cleanup_20261112.sql 与 biz_clean_area_bak_20261112
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO biz_contract_scope(contract_id, scope_type, scope_value)
SELECT DISTINCT s.contract_id, 'department', a.department_id
FROM biz_contract_scope s JOIN biz_clean_area a ON a.area_id = s.scope_value
WHERE s.scope_type = 'area' AND a.department_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM biz_contract_scope x
    WHERE x.contract_id = s.contract_id AND x.scope_type = 'department' AND x.scope_value = a.department_id
  );
DELETE FROM biz_contract_scope WHERE scope_type = 'area';

UPDATE biz_workorder SET area_id = NULL WHERE area_id IS NOT NULL;
UPDATE biz_workorder_archive SET area_id = NULL WHERE area_id IS NOT NULL;
UPDATE biz_equipment SET area_id = NULL WHERE area_id IS NOT NULL;
UPDATE biz_alarm SET area_id = NULL WHERE area_id IS NOT NULL;
UPDATE biz_alarm_archive SET area_id = NULL WHERE area_id IS NOT NULL;
UPDATE biz_inspection_plan SET area_id = NULL WHERE area_id IS NOT NULL;
UPDATE biz_inspection_task SET area_id = NULL WHERE area_id IS NOT NULL;

ALTER TABLE biz_workorder DROP COLUMN area_id;
ALTER TABLE biz_workorder_archive DROP COLUMN area_id;
ALTER TABLE biz_equipment DROP COLUMN area_id;
ALTER TABLE biz_alarm DROP COLUMN area_id;
ALTER TABLE biz_alarm_archive DROP COLUMN area_id;
ALTER TABLE biz_inspection_plan DROP COLUMN area_id;
ALTER TABLE biz_inspection_task DROP COLUMN area_id;

DROP TABLE biz_clean_area;

INSERT INTO sys_schema_version(version, description, applied_at)
SELECT '67_clean_area_phase2.sql', 'drop clean area table and area_id columns', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_schema_version WHERE version = '67_clean_area_phase2.sql');

SET FOREIGN_KEY_CHECKS = 1;
