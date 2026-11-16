-- 平台参数设置、在线用户菜单（仅超级管理员）
CREATE TABLE IF NOT EXISTS sys_config (
  config_id BIGINT NOT NULL AUTO_INCREMENT,
  config_name VARCHAR(64) NOT NULL,
  config_key VARCHAR(100) NOT NULL,
  group_code VARCHAR(32) NOT NULL DEFAULT 'platform',
  config_value VARCHAR(500) DEFAULT NULL,
  value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
  default_value VARCHAR(500) DEFAULT NULL,
  options_json VARCHAR(500) DEFAULT NULL,
  is_sensitive TINYINT NOT NULL DEFAULT 0,
  sort INT NOT NULL DEFAULT 0,
  status CHAR(1) NOT NULL DEFAULT '0',
  config_type CHAR(1) NOT NULL DEFAULT 'N',
  remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台参数配置';

CREATE TABLE IF NOT EXISTS sys_config_log (
  log_id BIGINT NOT NULL AUTO_INCREMENT,
  config_id BIGINT DEFAULT NULL,
  config_key VARCHAR(100) NOT NULL,
  old_value VARCHAR(500) DEFAULT NULL,
  new_value VARCHAR(500) DEFAULT NULL,
  operator_id BIGINT DEFAULT NULL,
  operator_name VARCHAR(64) DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数修改日志';

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,perms,menu_type,icon,visible,status,create_time)
VALUES
 (9270,'平台参数',9200,13,'/system/config','system/config/index','system:config:list','C','Setting',0,0,NOW()),
 (9271,'在线用户',9200,14,'/system/online','system/online/index','system:online:list','C','User',0,0,NOW())
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name),path=VALUES(path),component=VALUES(component),perms=VALUES(perms),visible=VALUES(visible),status=VALUES(status);

INSERT INTO sys_role_menu(role_id,menu_id) VALUES (1,9270),(1,9271)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

INSERT INTO sys_config(config_name,config_key,group_code,config_value,value_type,default_value,sort,config_type,remark,create_time,update_time)
VALUES
('用户管理-账号初始密码','sys.user.initPassword','account_security','Laminar@2026','STRING','Laminar@2026',10,'Y','新建账号、重置密码时使用的初始密码',NOW(),NOW()),
('账号自助-验证码开关','sys.account.captchaEnabled','account_security','false','BOOLEAN','false',20,'Y','登录时是否启用图形验证码',NOW(),NOW()),
('密码最小长度','sys.user.password.minLength','account_security','8','INTEGER','8',30,'Y','密码长度下限，建议 8-32',NOW(),NOW()),
('密码必须包含字母','sys.user.password.requireLetter','account_security','true','BOOLEAN','true',31,'Y','新密码是否必须包含字母',NOW(),NOW()),
('密码必须包含数字','sys.user.password.requireNumber','account_security','true','BOOLEAN','true',32,'Y','新密码是否必须包含数字',NOW(),NOW()),
('密码必须包含特殊字符','sys.user.password.requireSpecial','account_security','false','BOOLEAN','false',33,'Y','新密码是否必须包含特殊字符',NOW(),NOW()),
('密码有效期','sys.user.password.expireDays','account_security','0','INTEGER','0',34,'Y','密码多少天后过期，0 表示不过期',NOW(),NOW()),
('登录失败次数上限','sys.user.login.maxFail','account_security','5','INTEGER','5',40,'Y','连续失败达到次数后锁定，0 表示不锁定',NOW(),NOW()),
('账号锁定时长','sys.user.login.lockMinutes','account_security','15','INTEGER','15',41,'Y','账号锁定时长（分钟）',NOW(),NOW()),
('会话空闲超时','sys.session.idleMinutes','session','30','INTEGER','30',50,'Y','在线会话空闲多少分钟后过期',NOW(),NOW()),
('站内通知开关','platform.notify.inAppEnabled','notify','true','BOOLEAN','true',60,'Y','是否发送站内消息通知',NOW(),NOW()),
('邮件通知开关','platform.notify.emailEnabled','notify','false','BOOLEAN','false',61,'Y','预留：当前版本未接入邮件服务，开启不会发送',NOW(),NOW()),
('短信通知开关','platform.notify.smsEnabled','notify','false','BOOLEAN','false',62,'Y','预留：当前版本未接入短信服务，开启不会发送',NOW(),NOW()),
('登录日志保留天数','platform.login.logRetentionDays','log','180','INTEGER','180',70,'Y','超过天数的登录日志自动清理',NOW(),NOW()),
('操作日志保留天数','platform.audit.logRetentionDays','log','180','INTEGER','180',71,'Y','超过天数的操作日志自动清理',NOW(),NOW()),
('参数日志保留天数','platform.config.logRetentionDays','log','180','INTEGER','180',72,'Y','超过天数的参数修改日志自动清理',NOW(),NOW()),
('平台名称','platform.name','platform','层流智能运维','STRING','层流智能运维',80,'Y','登录页与侧边栏显示的平台名称',NOW(),NOW()),
('平台 Logo','platform.logoUrl','platform','','STRING','',81,'Y','平台 Logo 地址，留空使用默认标记',NOW(),NOW()),
('默认每页条数','platform.defaultPageSize','platform','10','INTEGER','10',82,'Y','用户未自定义时列表默认每页条数',NOW(),NOW()),
('演示登录开关','platform.demoLoginEnabled','platform','false','BOOLEAN','false',83,'Y','是否在登录页展示演示账号快捷登录',NOW(),NOW()),
('维护模式','platform.maintenance.enabled','platform','false','BOOLEAN','false',84,'Y','开启后仅超级管理员可登录和访问接口',NOW(),NOW()),
('维护提示语','platform.maintenance.message','platform','平台正在维护，请稍后再试','STRING','平台正在维护，请稍后再试',85,'Y','维护模式下登录页提示语',NOW(),NOW()),
('上传文件大小上限','platform.upload.maxSizeMb','upload','20','INTEGER','20',90,'Y','单文件大小上限（MB）',NOW(),NOW()),
('允许上传类型','platform.upload.allowedTypes','upload','jpg,jpeg,png,webp,pdf,doc,docx,xls,xlsx','STRING','jpg,jpeg,png,webp,pdf,doc,docx,xls,xlsx',91,'Y','允许上传的文件扩展名，逗号分隔',NOW(),NOW())
ON DUPLICATE KEY UPDATE config_name=VALUES(config_name),group_code=VALUES(group_code),value_type=VALUES(value_type),default_value=VALUES(default_value),sort=VALUES(sort),remark=VALUES(remark);

UPDATE sys_config SET is_sensitive=1 WHERE config_key='sys.user.initPassword';


