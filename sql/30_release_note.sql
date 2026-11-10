-- 版本更新记录
CREATE TABLE IF NOT EXISTS `sys_release_note` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `version`        VARCHAR(30)  NOT NULL,
  `title`          VARCHAR(200) NOT NULL,
  `release_date`   DATE         NOT NULL,
  `summary`        VARCHAR(500) DEFAULT NULL,
  `features`       TEXT         DEFAULT NULL,
  `improvements`   TEXT         DEFAULT NULL,
  `fixes`          TEXT         DEFAULT NULL,
  `status`         CHAR(1)      DEFAULT '0' COMMENT '0发布 1草稿',
  `create_by`      VARCHAR(64)  DEFAULT NULL,
  `create_time`    DATETIME     DEFAULT NULL,
  `update_by`      VARCHAR(64)  DEFAULT NULL,
  `update_time`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_release_version` (`version`),
  KEY `idx_release_date` (`release_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本更新记录';

INSERT INTO `sys_release_note` (`version`,`title`,`release_date`,`summary`,`features`,`improvements`,`fixes`,`status`,`create_time`)
SELECT 'v1.6.0','租户隔离与安全加固','2026-11-03','完成多公司数据隔离、统一租户过滤和登录安全加固',
'客户主数据按公司隔离\n巡检/保养计划任务按公司隔离\n图纸文档、知识库按公司归属\n角色按公司隔离\n统一租户过滤\n首次登录强制改密、连续失败锁定',
'个人中心支持设置默认每页条数\n优化管理支持紧急程度和状态\n设备大类改为全局字典',
'修复工作台 company_id 字段歧义导致 CEO 登录报“数据关联校验失败”\n修复设备类型跨公司共享\n修复公司4设备引用公司1类型的问题',
'0',NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_release_note` WHERE `version`='v1.6.0');

INSERT INTO `sys_release_note` (`version`,`title`,`release_date`,`summary`,`features`,`improvements`,`fixes`,`status`,`create_time`)
SELECT 'v1.5.0','组织与权限细化','2026-11-02','公司组织架构、岗位、登录编码和客户主数据权限细化',
'公司独立部门与岗位\n公司编码登录与历史编码兼容\n客户主数据按合同医院收敛\n角色管理移入组织设置',
'字典管理支持展开维护字典数据\n工作台增加待办任务\n列表支持更多分页条数',
'修复重复用户名创建失败\n修复客户主数据跨公司可见',
'0',NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_release_note` WHERE `version`='v1.5.0');

INSERT INTO `sys_release_note` (`version`,`title`,`release_date`,`summary`,`features`,`improvements`,`fixes`,`status`,`create_time`)
SELECT 'v1.4.0','设备与监测基础能力','2026-11-01','设备台账、设备类型、监测点位和告警规则基础能力完善',
'设备类型按公司独立\n设备台账按公司归属\n监测点位与告警规则\n工单与备件领用',
'设备类型支持设备大类字典\n设备详情展示监测数据',
'修复设备类型缺少大类时的保存错误\n修复告警消息公司归属',
'0',NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_release_note` WHERE `version`='v1.4.0');
