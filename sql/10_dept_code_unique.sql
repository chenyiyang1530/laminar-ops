-- 科室编码唯一（编码重复会直接返回友好提示）
ALTER TABLE `biz_department` ADD UNIQUE KEY `uk_dept_code` (`dept_code`);
