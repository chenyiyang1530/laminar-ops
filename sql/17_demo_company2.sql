-- 多公司演示数据：福建阿斯兰（company_id=4）
-- 账号初始密码 Laminar@2026，首次登录需修改
SET @pwd = 'ca74bf03c92496a050a7f4e5da2a2ba4cbfb890a99a4270b230e424f7e3be5b8';

INSERT IGNORE INTO `sys_user` (`username`,`password`,`nick_name`,`phone`,`user_type`,`company_id`,`status`,`create_time`)
VALUES ('ceo_asl', @pwd, '阿斯兰CEO', '13900000001', '1', 4, '0', NOW());
INSERT IGNORE INTO `sys_user` (`username`,`password`,`nick_name`,`phone`,`user_type`,`company_id`,`status`,`create_time`)
VALUES ('regional_asl', @pwd, '阿斯兰区域主管', '13900000002', '1', 4, '0', NOW());
INSERT IGNORE INTO `sys_user` (`username`,`password`,`nick_name`,`phone`,`user_type`,`company_id`,`status`,`create_time`)
VALUES ('engineer_asl', @pwd, '阿斯兰工程师', '13900000003', '1', 4, '0', NOW());

INSERT IGNORE INTO `sys_user_role` (`user_id`,`role_id`)
SELECT `user_id`, 100 FROM `sys_user` WHERE `username`='ceo_asl';
INSERT IGNORE INTO `sys_user_role` (`user_id`,`role_id`)
SELECT `user_id`, 101 FROM `sys_user` WHERE `username`='regional_asl';
INSERT IGNORE INTO `sys_user_role` (`user_id`,`role_id`)
SELECT `user_id`, 102 FROM `sys_user` WHERE `username`='engineer_asl';

INSERT INTO `biz_engineer` (`user_id`,`company_id`,`region_id`,`engineer_name`,`phone`,`gender`,`job_title`,`entry_date`,`on_duty`,`status`,`create_time`)
SELECT u.`user_id`, 4, 1, '陈海', '13900000002', '0', '中级工程师', '2024-03-01', '1', '0', NOW()
FROM `sys_user` u WHERE u.`username`='regional_asl'
  AND NOT EXISTS (SELECT 1 FROM `biz_engineer` e WHERE e.`user_id`=u.`user_id`);

INSERT INTO `biz_engineer` (`user_id`,`company_id`,`region_id`,`engineer_name`,`phone`,`gender`,`job_title`,`entry_date`,`on_duty`,`status`,`create_time`)
SELECT u.`user_id`, 4, 1, '林晓', '13900000003', '1', '初级工程师', '2025-06-01', '1', '0', NOW()
FROM `sys_user` u WHERE u.`username`='engineer_asl'
  AND NOT EXISTS (SELECT 1 FROM `biz_engineer` e WHERE e.`user_id`=u.`user_id`);

INSERT IGNORE INTO `biz_engineer_hospital` (`engineer_id`,`hospital_id`)
SELECT e.`engineer_id`, 1 FROM `biz_engineer` e
JOIN `sys_user` u ON u.`user_id`=e.`user_id`
WHERE u.`username` IN ('regional_asl','engineer_asl');

INSERT IGNORE INTO `biz_company_region` (`company_id`,`region_id`,`manager_id`,`create_time`)
SELECT 4, 1, e.`engineer_id`, NOW()
FROM `biz_engineer` e JOIN `sys_user` u ON u.`user_id`=e.`user_id`
WHERE u.`username`='regional_asl';

UPDATE `biz_company_region`
SET `manager_id` = (SELECT e.`engineer_id` FROM `biz_engineer` e JOIN `sys_user` u ON u.`user_id`=e.`user_id` WHERE u.`username`='regional_asl')
WHERE `company_id`=4 AND `region_id`=3;

INSERT IGNORE INTO `biz_contract`
(`contract_no`,`contract_name`,`hospital_id`,`company_id`,`contract_type`,`start_date`,`end_date`,`sign_date`,`amount`,`manager_id`,`warn_days`,`status`,`remark`,`create_time`)
VALUES
('HT-ASL-2026-001','阿斯兰-手术室净化专项维保','1',4,'maintenance','2026-01-01','2027-12-31','2025-12-20',360000,
 (SELECT e.`engineer_id` FROM `biz_engineer` e JOIN `sys_user` u ON u.`user_id`=e.`user_id` WHERE u.`username`='regional_asl'),
 90,'0','多公司演示：只承接手术室范围',NOW());

INSERT IGNORE INTO `biz_contract_scope` (`contract_id`,`scope_type`,`scope_value`)
SELECT c.`contract_id`, 'department', 1 FROM `biz_contract` c WHERE c.`contract_no`='HT-ASL-2026-001';

INSERT INTO `biz_workorder`
(`workorder_no`,`title`,`fault_desc`,`fault_level`,`status`,`source`,`service_type`,`hospital_id`,`department_id`,`equipment_id`,`company_id`,`engineer_id`,`create_time`)
SELECT 'WO-ASL-DEMO-001','阿斯兰-手术室净化机组检修','多公司演示工单：手术室设备由阿斯兰承接','2','5','internal','0',1,1,1,4,e.`engineer_id`,NOW()
FROM `biz_engineer` e JOIN `sys_user` u ON u.`user_id`=e.`user_id`
WHERE u.`username`='engineer_asl'
  AND NOT EXISTS (SELECT 1 FROM `biz_workorder` w WHERE w.`workorder_no`='WO-ASL-DEMO-001');

INSERT INTO `biz_message` (`msg_type`,`title`,`content`,`level`,`biz_type`,`receiver_id`,`receiver_role`,`hospital_id`,`company_id`,`is_read`,`create_time`)
SELECT 'system', '阿斯兰演示消息', '该消息只对福建阿斯兰公司可见', '3', 'demo', 0, '100,101', 1, 4, '0', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `biz_message` m WHERE m.`title`='阿斯兰演示消息');

-- 阿斯兰更多工单/费用演示
INSERT INTO `biz_workorder`
(`workorder_no`,`title`,`fault_desc`,`fault_level`,`status`,`source`,`service_type`,`hospital_id`,`department_id`,`equipment_id`,`company_id`,`engineer_id`,`create_time`)
SELECT 'WO-ASL-DEMO-002','阿斯兰-手术室空调异响处理','手术室净化空调运行异响，需检查风机','3','2','internal','0',1,1,2,4,e.`engineer_id`,NOW()
FROM `biz_engineer` e JOIN `sys_user` u ON u.`user_id`=e.`user_id`
WHERE u.`username`='engineer_asl' AND NOT EXISTS (SELECT 1 FROM `biz_workorder` w WHERE w.`workorder_no`='WO-ASL-DEMO-002');

INSERT INTO `biz_workorder`
(`workorder_no`,`title`,`fault_desc`,`fault_level`,`status`,`source`,`service_type`,`hospital_id`,`department_id`,`equipment_id`,`company_id`,`engineer_id`,`create_time`)
SELECT 'WO-ASL-DEMO-003','阿斯兰-新风机组滤网更换','手术室新风机组滤网到期更换','2','0','internal','0',1,1,4,4,NULL,NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `biz_workorder` w WHERE w.`workorder_no`='WO-ASL-DEMO-003');

INSERT INTO `biz_workorder`
(`workorder_no`,`title`,`fault_desc`,`fault_level`,`status`,`source`,`service_type`,`hospital_id`,`department_id`,`equipment_id`,`company_id`,`engineer_id`,`create_time`)
SELECT 'WO-ASL-DEMO-004','阿斯兰-除湿机组季度保养','手术室除湿机组季度保养完成','3','5','maintenance','1',1,1,5,4,e.`engineer_id`,NOW()
FROM `biz_engineer` e JOIN `sys_user` u ON u.`user_id`=e.`user_id`
WHERE u.`username`='engineer_asl' AND NOT EXISTS (SELECT 1 FROM `biz_workorder` w WHERE w.`workorder_no`='WO-ASL-DEMO-004');

INSERT INTO `biz_workorder_cost` (`workorder_id`,`cost_type`,`cost_name`,`quantity`,`price`,`amount`,`remark`)
SELECT w.`workorder_id`, 'labor', '阿斯兰人工费', 1, 1200, 1200, '多公司演示费用'
FROM `biz_workorder` w WHERE w.`workorder_no`='WO-ASL-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM `biz_workorder_cost` c WHERE c.`workorder_id`=w.`workorder_id` AND c.`cost_type`='labor');

INSERT INTO `biz_workorder_cost` (`workorder_id`,`cost_type`,`cost_name`,`quantity`,`price`,`amount`,`remark`)
SELECT w.`workorder_id`, 'part', '高效过滤器', 2, 800, 1600, '多公司演示备件'
FROM `biz_workorder` w WHERE w.`workorder_no`='WO-ASL-DEMO-004'
  AND NOT EXISTS (SELECT 1 FROM `biz_workorder_cost` c WHERE c.`workorder_id`=w.`workorder_id` AND c.`cost_type`='part');



UPDATE sys_user SET must_change_pwd=1 WHERE username IN ('ceo_asl','regional_asl','engineer_asl');
