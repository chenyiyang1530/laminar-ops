-- 登录安全加固
ALTER TABLE `sys_user`
  ADD COLUMN `must_change_pwd` TINYINT DEFAULT 0 COMMENT '是否必须修改密码' AFTER `password`,
  ADD COLUMN `login_fail_count` INT DEFAULT 0 COMMENT '连续登录失败次数' AFTER `must_change_pwd`,
  ADD COLUMN `lock_until` DATETIME DEFAULT NULL COMMENT '锁定截止时间' AFTER `login_fail_count`,
  ADD COLUMN `pwd_update_time` DATETIME DEFAULT NULL COMMENT '密码更新时间' AFTER `lock_until`;
