-- 巡检/保养计划与任务按公司隔离
ALTER TABLE `biz_inspection_plan` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `plan_id`;
ALTER TABLE `biz_inspection_task` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `task_id`;
ALTER TABLE `biz_maintenance_plan` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `plan_id`;
ALTER TABLE `biz_maintenance_task` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `task_id`;

UPDATE `biz_inspection_plan` p
LEFT JOIN `biz_equipment` e ON e.`equipment_id` = p.`equipment_id`
LEFT JOIN `biz_engineer` en ON en.`engineer_id` = p.`assignee_id`
SET p.`company_id` = COALESCE(e.`company_id`, en.`company_id`, (
  SELECT MIN(c.`company_id`) FROM `biz_contract` c
  WHERE c.`hospital_id` = p.`hospital_id` AND c.`company_id` IS NOT NULL AND c.`status` = '0'
))
WHERE p.`company_id` IS NULL;

UPDATE `biz_maintenance_plan` p
LEFT JOIN `biz_equipment` e ON e.`equipment_id` = p.`equipment_id`
LEFT JOIN `biz_engineer` en ON en.`engineer_id` = p.`assignee_id`
SET p.`company_id` = COALESCE(e.`company_id`, en.`company_id`, (
  SELECT MIN(c.`company_id`) FROM `biz_contract` c
  WHERE c.`hospital_id` = p.`hospital_id` AND c.`company_id` IS NOT NULL AND c.`status` = '0'
))
WHERE p.`company_id` IS NULL;

UPDATE `biz_inspection_task` t
LEFT JOIN `biz_inspection_plan` p ON p.`plan_id` = t.`plan_id`
LEFT JOIN `biz_equipment` e ON e.`equipment_id` = t.`equipment_id`
SET t.`company_id` = COALESCE(p.`company_id`, e.`company_id`, (
  SELECT MIN(c.`company_id`) FROM `biz_contract` c
  WHERE c.`hospital_id` = t.`hospital_id` AND c.`company_id` IS NOT NULL AND c.`status` = '0'
))
WHERE t.`company_id` IS NULL;

UPDATE `biz_maintenance_task` t
LEFT JOIN `biz_maintenance_plan` p ON p.`plan_id` = t.`plan_id`
LEFT JOIN `biz_equipment` e ON e.`equipment_id` = t.`equipment_id`
SET t.`company_id` = COALESCE(p.`company_id`, e.`company_id`, (
  SELECT MIN(c.`company_id`) FROM `biz_contract` c
  WHERE c.`hospital_id` = t.`hospital_id` AND c.`company_id` IS NOT NULL AND c.`status` = '0'
))
WHERE t.`company_id` IS NULL;

UPDATE `biz_inspection_plan` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
UPDATE `biz_maintenance_plan` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
UPDATE `biz_inspection_task` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
UPDATE `biz_maintenance_task` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;

ALTER TABLE `biz_inspection_plan` ADD KEY `idx_ip_company` (`company_id`);
ALTER TABLE `biz_inspection_task` ADD KEY `idx_it_company` (`company_id`);
ALTER TABLE `biz_maintenance_plan` ADD KEY `idx_mp_company` (`company_id`);
ALTER TABLE `biz_maintenance_task` ADD KEY `idx_mt_company` (`company_id`);
