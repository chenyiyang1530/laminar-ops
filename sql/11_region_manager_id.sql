-- 区域主管改为关联系统用户（manager 保留历史姓名）
ALTER TABLE `biz_region` ADD COLUMN `manager_id` BIGINT DEFAULT NULL AFTER `manager`;

UPDATE `biz_region` r
JOIN `sys_user` u ON u.`nick_name` = r.`manager`
SET r.`manager_id` = u.`user_id`;
