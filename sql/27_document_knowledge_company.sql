-- 图纸文档、知识库按公司归属
ALTER TABLE `biz_document` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `doc_id`;
ALTER TABLE `biz_knowledge` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `article_id`;

UPDATE `biz_document` d
LEFT JOIN `biz_equipment` e ON e.`equipment_id` = d.`equipment_id`
LEFT JOIN `sys_user` u ON u.`user_id` = d.`uploader_id`
SET d.`company_id` = COALESCE(e.`company_id`, u.`company_id`, (
  SELECT MIN(c.`company_id`) FROM `biz_contract` c
  WHERE c.`hospital_id` = d.`hospital_id` AND c.`company_id` IS NOT NULL AND c.`status` = '0'
))
WHERE d.`company_id` IS NULL;

UPDATE `biz_knowledge` k
LEFT JOIN `sys_user` u ON u.`user_id` = k.`author_id`
SET k.`company_id` = COALESCE(u.`company_id`, (SELECT MIN(`company_id`) FROM `biz_company`))
WHERE k.`company_id` IS NULL;

UPDATE `biz_document` SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`) WHERE `company_id` IS NULL;

ALTER TABLE `biz_document` ADD KEY `idx_doc_company` (`company_id`);
ALTER TABLE `biz_knowledge` ADD KEY `idx_knowledge_company` (`company_id`);
