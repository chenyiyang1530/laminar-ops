-- 公司隔离：历史工单归属公司
UPDATE `biz_workorder` w
JOIN (SELECT `hospital_id`, MIN(`company_id`) AS company_id
      FROM `biz_contract`
      WHERE `company_id` IS NOT NULL AND `status`='0'
      GROUP BY `hospital_id`) c ON c.`hospital_id` = w.`hospital_id`
SET w.`company_id` = c.`company_id`
WHERE w.`company_id` IS NULL;
