-- 技能标签、资质证书按公司归属
ALTER TABLE `biz_skill` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `skill_id`;
ALTER TABLE `biz_certificate` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `cert_id`;

UPDATE `biz_skill` s
LEFT JOIN (
  SELECT es.`skill_id`, MIN(e.`company_id`) AS `company_id`
  FROM `biz_engineer_skill` es
  JOIN `biz_engineer` e ON e.`engineer_id` = es.`engineer_id`
  GROUP BY es.`skill_id`
) x ON x.`skill_id` = s.`skill_id`
SET s.`company_id` = COALESCE(x.`company_id`, (SELECT MIN(`company_id`) FROM `biz_company`))
WHERE s.`company_id` IS NULL;

UPDATE `biz_certificate` c
LEFT JOIN `biz_engineer` e ON e.`engineer_id` = c.`engineer_id`
SET c.`company_id` = COALESCE(e.`company_id`, (SELECT MIN(`company_id`) FROM `biz_company`))
WHERE c.`company_id` IS NULL;

ALTER TABLE `biz_skill` DROP INDEX `uk_skill_code`;
ALTER TABLE `biz_skill` ADD UNIQUE KEY `uk_company_skill_code` (`company_id`,`skill_code`);
ALTER TABLE `biz_skill` ADD KEY `idx_skill_company` (`company_id`);
ALTER TABLE `biz_certificate` ADD KEY `idx_cert_company` (`company_id`);
