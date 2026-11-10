-- 版本更新未读标记
ALTER TABLE `sys_user` ADD COLUMN `last_seen_release_id` BIGINT DEFAULT 0 COMMENT '最后已读版本记录ID' AFTER `must_change_pwd`;
