-- 一个用户只能绑定一个工程师，一个工程师只能绑定一个用户
ALTER TABLE `biz_engineer` ADD UNIQUE KEY `uk_engineer_user` (`user_id`);
