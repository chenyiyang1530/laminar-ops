-- 每页条数改为“未设置则使用平台默认值”
ALTER TABLE `sys_user`
  MODIFY COLUMN `pref_page_size` INT NULL DEFAULT NULL COMMENT '默认每页条数，NULL 表示使用平台默认值';

-- 旧版本默认写入 10，统一视为未设置；用户重新选择后会再次写入
UPDATE `sys_user` SET `pref_page_size` = NULL WHERE `pref_page_size` = 10;
