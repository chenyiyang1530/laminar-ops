-- 设备台账明确维保公司，避免不同公司互相看到对方负责的设备
ALTER TABLE `biz_equipment` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '维保公司' AFTER `equipment_id`;

-- 按合同服务范围优先级回填：设备 > 洁净区域 > 科室 > 全院
UPDATE `biz_equipment` e
SET e.`company_id` = (
  SELECT c.`company_id`
  FROM `biz_contract` c
  LEFT JOIN `biz_contract_scope` s ON s.`contract_id` = c.`contract_id`
  WHERE c.`hospital_id` = e.`hospital_id`
    AND c.`company_id` IS NOT NULL
    AND c.`status` = '0'
    AND (c.`start_date` IS NULL OR c.`start_date` <= CURDATE())
    AND (c.`end_date` IS NULL OR c.`end_date` >= CURDATE())
    AND (
      NOT EXISTS (SELECT 1 FROM `biz_contract_scope` x WHERE x.`contract_id` = c.`contract_id`)
      OR s.`scope_type` = 'hospital'
      OR (s.`scope_type` = 'equipment' AND s.`scope_value` = e.`equipment_id`)
      OR (s.`scope_type` = 'area' AND s.`scope_value` = e.`area_id`)
      OR (s.`scope_type` = 'department' AND s.`scope_value` = e.`department_id`)
    )
  ORDER BY CASE s.`scope_type`
    WHEN 'equipment' THEN 1
    WHEN 'area' THEN 2
    WHEN 'department' THEN 3
    ELSE 4 END
  LIMIT 1
)
WHERE e.`company_id` IS NULL;

UPDATE `biz_equipment` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;
ALTER TABLE `biz_equipment` ADD KEY `idx_equipment_company` (`company_id`);

-- 历史工单/消息跟随设备归属公司
UPDATE `biz_workorder` w
JOIN `biz_equipment` e ON e.`equipment_id` = w.`equipment_id`
SET w.`company_id` = e.`company_id`
WHERE e.`company_id` IS NOT NULL;

UPDATE `biz_message` m
JOIN `biz_alarm` a ON a.`alarm_id` = m.`biz_id` AND m.`biz_type` = 'alarm'
JOIN `biz_equipment` e ON e.`equipment_id` = a.`equipment_id`
SET m.`company_id` = e.`company_id`
WHERE e.`company_id` IS NOT NULL;
