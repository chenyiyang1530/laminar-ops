-- 个人中心默认分页条数
ALTER TABLE `sys_user` ADD COLUMN `pref_page_size` INT DEFAULT 10 COMMENT '默认每页条数' AFTER `company_id`;
