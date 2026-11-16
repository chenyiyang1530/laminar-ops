-- 61: 清理重复的“公司服务区域”表，区域/主管只保留 biz_region 一处
-- 1) 仅迁移同公司、且新表主管为空的旧主管数据；跨公司脏数据丢弃
-- 2) 用户部门按“工程师所属区域 → 部门”同步
-- 3) 部门与区域、区域名称/编码补唯一约束
-- 4) 删除 biz_company_region

START TRANSACTION;

UPDATE biz_region r
JOIN biz_company_region cr
  ON cr.region_id = r.region_id
 AND cr.company_id = r.company_id
SET r.manager_id = cr.manager_id
WHERE r.manager_id IS NULL
  AND cr.manager_id IS NOT NULL;

UPDATE sys_user u
JOIN biz_engineer e ON e.user_id = u.user_id
LEFT JOIN sys_dept d
  ON d.company_id = e.company_id
 AND d.region_id = e.region_id
SET u.dept_id = d.dept_id;

ALTER TABLE biz_region
  ADD UNIQUE KEY uk_region_name (company_id, region_name),
  ADD UNIQUE KEY uk_region_code (company_id, region_code);

DROP TABLE biz_company_region;

INSERT INTO sys_schema_version(version, description, applied_at)
VALUES ('61_cleanup_company_region.sql', 'remove duplicate company region table', NOW());

COMMIT;
