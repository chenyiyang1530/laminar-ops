-- 登录标识：手机号/邮箱全局唯一，支持手机号或邮箱直接登录
UPDATE `sys_user` SET `phone`=NULL WHERE `phone`='';
UPDATE `sys_user` SET `email`=NULL WHERE `email`='';
ALTER TABLE `sys_user`
  ADD UNIQUE KEY `uk_user_phone` (`phone`),
  ADD UNIQUE KEY `uk_user_email` (`email`);
