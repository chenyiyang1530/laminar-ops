-- 洁净区域一期下线：先备份，再把 area 归属迁移到 department
-- 不删除 biz_clean_area、不删除 area_id，保留回滚能力
CREATE TABLE IF NOT EXISTS biz_clean_area_bak_20261112 AS SELECT * FROM biz_clean_area;

UPDATE biz_workorder t JOIN biz_clean_area a ON a.area_id=t.area_id
SET t.department_id=a.department_id
WHERE t.department_id IS NULL AND a.department_id IS NOT NULL;

UPDATE biz_workorder_archive t JOIN biz_clean_area a ON a.area_id=t.area_id
SET t.department_id=a.department_id
WHERE t.department_id IS NULL AND a.department_id IS NOT NULL;

UPDATE biz_equipment t JOIN biz_clean_area a ON a.area_id=t.area_id
SET t.department_id=a.department_id
WHERE t.department_id IS NULL AND a.department_id IS NOT NULL;

UPDATE biz_alarm t JOIN biz_clean_area a ON a.area_id=t.area_id
SET t.department_id=a.department_id
WHERE t.department_id IS NULL AND a.department_id IS NOT NULL;

UPDATE biz_alarm_archive t JOIN biz_clean_area a ON a.area_id=t.area_id
SET t.department_id=a.department_id
WHERE t.department_id IS NULL AND a.department_id IS NOT NULL;

UPDATE biz_inspection_plan t JOIN biz_clean_area a ON a.area_id=t.area_id
SET t.department_id=a.department_id
WHERE t.department_id IS NULL AND a.department_id IS NOT NULL;

UPDATE biz_inspection_task t JOIN biz_clean_area a ON a.area_id=t.area_id
SET t.department_id=a.department_id
WHERE t.department_id IS NULL AND a.department_id IS NOT NULL;

INSERT INTO sys_schema_version(version, description, applied_at)
SELECT '65_clean_area_phase1.sql', 'backup clean area and migrate area to department', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_schema_version WHERE version = '65_clean_area_phase1.sql');
