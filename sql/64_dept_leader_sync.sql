-- 部门负责人与地理区域主管保持一致
-- 关联了地理区域的部门，负责人取该区域主管姓名
UPDATE sys_dept d
JOIN biz_region r ON r.region_id = d.region_id
JOIN sys_user u ON u.user_id = r.manager_id
SET d.leader = u.nick_name
WHERE d.region_id IS NOT NULL;

INSERT INTO sys_schema_version(version, description, applied_at)
SELECT '64_dept_leader_sync.sql', 'sync department leader with region manager', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_schema_version WHERE version = '64_dept_leader_sync.sql');
