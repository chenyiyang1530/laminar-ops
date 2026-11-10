-- 消息按公司隔离
ALTER TABLE `biz_message` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `hospital_id`;
ALTER TABLE `biz_message` ADD KEY `idx_msg_company` (`company_id`);

UPDATE `biz_message` m
SET m.`company_id` = (
  SELECT MIN(c.`company_id`) FROM `biz_contract` c
  WHERE c.`hospital_id` = m.`hospital_id` AND c.`company_id` IS NOT NULL AND c.`status`='0'
)
WHERE m.`company_id` IS NULL AND m.`hospital_id` IS NOT NULL;
