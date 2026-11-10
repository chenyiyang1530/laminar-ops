-- 区域所属公司改为可选（区域主管/公司均非强制）
ALTER TABLE `biz_region` MODIFY COLUMN `company_id` BIGINT DEFAULT NULL;
