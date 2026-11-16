-- 医院与公司的关系已改为 biz_company_hospital 多对多，删除历史单公司字段
ALTER TABLE `biz_hospital` DROP COLUMN `company_id`;
