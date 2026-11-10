-- 标准角色标记：8 个平台标准角色
ALTER TABLE `sys_role`
  ADD COLUMN `role_type` VARCHAR(20) DEFAULT 'custom' COMMENT 'standard/custom' AFTER `company_id`;

UPDATE `sys_role` SET `role_type`='standard'
WHERE `role_id` IN (1,100,101,102,103,104,200,201);
