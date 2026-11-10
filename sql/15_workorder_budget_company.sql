-- 公司隔离：历史工单归属公司 + 预算归属公司
UPDATE `biz_workorder` w
JOIN (SELECT `hospital_id`, MIN(`company_id`) AS company_id
      FROM `biz_contract`
      WHERE `company_id` IS NOT NULL AND `status`='0'
      GROUP BY `hospital_id`) c ON c.`hospital_id` = w.`hospital_id`
SET w.`company_id` = c.`company_id`
WHERE w.`company_id` IS NULL;

ALTER TABLE `biz_budget` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `contract_id`;

UPDATE `biz_budget` b
LEFT JOIN `biz_contract` c ON c.`contract_id` = b.`contract_id`
SET b.`company_id` = COALESCE(c.`company_id`,
    (SELECT MIN(c2.`company_id`) FROM `biz_contract` c2
     WHERE c2.`hospital_id` = b.`hospital_id` AND c2.`company_id` IS NOT NULL AND c2.`status`='0'))
WHERE b.`company_id` IS NULL;
