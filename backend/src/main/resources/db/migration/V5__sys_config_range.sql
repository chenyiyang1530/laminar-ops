-- 平台参数范围校验
ALTER TABLE `sys_config`
  ADD COLUMN `min_value` DECIMAL(18,4) NULL COMMENT '数值下限' AFTER `value_type`,
  ADD COLUMN `max_value` DECIMAL(18,4) NULL COMMENT '数值上限' AFTER `min_value`;

UPDATE `sys_config` SET `min_value`=6,   `max_value`=32    WHERE `config_key`='sys.user.password.minLength';
UPDATE `sys_config` SET `min_value`=1,   `max_value`=20    WHERE `config_key`='sys.user.login.maxFail';
UPDATE `sys_config` SET `min_value`=1,   `max_value`=1440  WHERE `config_key`='sys.user.login.lockMinutes';
UPDATE `sys_config` SET `min_value`=0,   `max_value`=365   WHERE `config_key`='sys.user.password.expireDays';
UPDATE `sys_config` SET `min_value`=5,   `max_value`=1440  WHERE `config_key`='sys.session.idleMinutes';
UPDATE `sys_config` SET `min_value`=0,   `max_value`=3650  WHERE `config_key`='platform.login.logRetentionDays';
UPDATE `sys_config` SET `min_value`=0,   `max_value`=3650  WHERE `config_key`='platform.audit.logRetentionDays';
UPDATE `sys_config` SET `min_value`=0,   `max_value`=3650  WHERE `config_key`='platform.config.logRetentionDays';
UPDATE `sys_config` SET `min_value`=5,   `max_value`=100   WHERE `config_key`='platform.defaultPageSize';
UPDATE `sys_config` SET `min_value`=1,   `max_value`=100   WHERE `config_key`='platform.upload.maxSizeMb';
