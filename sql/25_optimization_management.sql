-- 平台优化管理：记录后续待优化事项
CREATE TABLE IF NOT EXISTS `sys_optimization_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(200) NOT NULL,
  `module`      VARCHAR(100) DEFAULT NULL,
  `priority`    VARCHAR(20)  DEFAULT 'medium' COMMENT 'urgent/high/medium/low',
  `status`      VARCHAR(20)  DEFAULT 'todo' COMMENT 'todo/doing/done/hold',
  `owner`       VARCHAR(100) DEFAULT NULL,
  `due_date`    DATE         DEFAULT NULL,
  `sort`        INT          DEFAULT 0,
  `description` TEXT         DEFAULT NULL,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台优化事项';

INSERT INTO `sys_dict_type` (`dict_name`,`dict_type`,`status`,`remark`)
SELECT '优化紧急程度','optimization_priority','0','优化管理的紧急程度'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type`='optimization_priority');

INSERT INTO `sys_dict_type` (`dict_name`,`dict_type`,`status`,`remark`)
SELECT '优化状态','optimization_status','0','优化管理的处理状态'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type`='optimization_status');

INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 1,'紧急','urgent','optimization_priority','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_priority' AND `dict_value`='urgent');
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 2,'高','high','optimization_priority','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_priority' AND `dict_value`='high');
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 3,'中','medium','optimization_priority','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_priority' AND `dict_value`='medium');
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 4,'低','low','optimization_priority','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_priority' AND `dict_value`='low');

INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 1,'待处理','todo','optimization_status','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_status' AND `dict_value`='todo');
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 2,'进行中','doing','optimization_status','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_status' AND `dict_value`='doing');
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 3,'已完成','done','optimization_status','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_status' AND `dict_value`='done');
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`status`)
SELECT 4,'暂缓','hold','optimization_status','0'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type`='optimization_status' AND `dict_value`='hold');

INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '巡检、保养计划/任务按公司隔离','巡检保养','high','todo','计划和任务增加公司归属，按公司/设备过滤',1,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='巡检、保养计划/任务按公司隔离');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '图纸文档、知识库按公司归属','资料','high','todo','明确公司私有/平台共享，并补齐数据权限',2,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='图纸文档、知识库按公司归属');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '统一租户过滤 + 双公司隔离自动化测试','架构','high','todo','用统一 tenant 过滤替代资源白名单，并补隔离测试',3,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='统一租户过滤 + 双公司隔离自动化测试');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '角色按公司隔离','权限','medium','todo','支持公司自定义角色，角色只能分配本公司用户',4,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='角色按公司隔离');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '登录安全加固','安全','medium','todo','首次登录强制改密、失败次数锁定、限流',5,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='登录安全加固');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '版本端点与部署检查','运维','medium','todo','增加版本接口/启动日志，避免旧进程占用端口',6,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='版本端点与部署检查');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '历史告警/工单清理与归档','数据','low','todo','降低模拟数据量，增加归档与清理策略',7,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='历史告警/工单清理与归档');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '列表导出与列配置','体验','low','todo','常用列表支持导出和自定义列',8,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='列表导出与列配置');
INSERT INTO `sys_optimization_item` (`title`,`module`,`priority`,`status`,`description`,`sort`,`create_time`)
SELECT '客户主数据公司隔离','客户','high','done','医院/院区/科室/洁净区/对接人已按公司过滤，公司只读',9,NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_optimization_item` WHERE `title`='客户主数据公司隔离');
