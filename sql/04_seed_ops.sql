-- =============================================================
-- 演示数据（二）：告警 / 工单 / 巡检 / 保养 / 备件 / 文档 / 预算 / 消息
-- =============================================================
USE `laminar_ops`;

-- 告警
INSERT INTO `biz_alarm` (`alarm_id`,`alarm_no`,`hospital_id`,`campus_id`,`area_id`,`department_id`,`equipment_id`,`point_id`,`metric`,`level`,`alarm_value`,`threshold`,`content`,`status`,`first_time`,`last_time`,`duration`,`confirm_by`,`confirm_time`,`handle_by`,`handle_time`,`close_by`,`close_time`,`workorder_id`,`root_cause`,`solution`,`create_time`) VALUES
(1,'AL-20261029-001',3,4,7,7,9,(SELECT point_id FROM biz_monitor_point WHERE equipment_id=9 AND metric='pressure'),'pressure','1',3.200,5.000,'泉州城东手术室层流机组 1# 房间正压 3.2Pa，低于 5Pa 下限','0','2026-10-29 08:12:00','2026-10-29 09:15:00',3780,NULL,NULL,NULL,NULL,NULL,NULL,6,NULL,NULL,NOW()),
(2,'AL-20261028-002',1,1,2,2,6,(SELECT point_id FROM biz_monitor_point WHERE equipment_id=6 AND metric='temperature'),'temperature','2',26.800,26.000,'总院 ICU 层流机组 1# 送风温度 26.8℃，超过 26℃','1','2026-10-28 14:05:00','2026-10-28 14:40:00',2100,'陈志强','2026-10-28 14:20:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW()),
(3,'AL-20261027-003',2,3,6,6,8,(SELECT point_id FROM biz_monitor_point WHERE equipment_id=8 AND metric='humidity'),'humidity','3',68.500,65.000,'中医院手术室净化空调 1# 回风湿度 68.5%，超过 65%','2','2026-10-27 10:30:00','2026-10-27 11:05:00',2100,'陈志强','2026-10-27 10:45:00','苏婷','2026-10-27 11:00:00',NULL,NULL,NULL,NULL,NULL,NOW()),
(4,'AL-20261025-004',1,1,1,1,1,(SELECT point_id FROM biz_monitor_point WHERE equipment_id=1 AND metric='wind_speed'),'wind_speed','3',0.180,0.200,'总院手术部层流机组 1# 送风风速 0.18m/s，低于 0.20m/s','3','2026-10-25 09:10:00','2026-10-25 10:20:00',4200,'林伟','2026-10-25 09:20:00','林伟','2026-10-25 09:25:00','林伟','2026-10-25 10:20:00',11,'风机皮带打滑','更换 B-1200 皮带并调整张紧度，复测风速 0.38m/s',NOW()),
(5,'AL-20261029-005',3,4,8,8,12,(SELECT point_id FROM biz_monitor_point WHERE equipment_id=12 AND metric='oxygen_pressure'),'oxygen_pressure','1',0.280,0.300,'泉州城东供氧汇流排输出压力 0.28MPa，低于 0.30MPa','0','2026-10-29 07:40:00','2026-10-29 09:15:00',5700,NULL,NULL,NULL,NULL,NULL,NULL,10,NULL,NULL,NOW()),
(6,'AL-20261024-006',1,2,5,5,7,(SELECT point_id FROM biz_monitor_point WHERE equipment_id=7 AND metric='particle'),'particle','2',4200.000,3520.000,'杏林手术室层流机组 1# 悬浮粒子 4200 粒/L，超过 3520 粒/L','4','2026-10-24 16:20:00','2026-10-24 17:00:00',2400,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'待核实，粒子计数器正在校准','安排第三方检测复核，暂按忽略处理',NOW());

INSERT INTO `biz_alarm_log` (`alarm_id`,`action`,`operator_id`,`operator_name`,`from_status`,`to_status`,`content`,`create_time`) VALUES
(1,'create',NULL,'系统',NULL,'0','监测数据越限，自动生成告警', '2026-10-29 08:12:00'),
(2,'create',NULL,'系统',NULL,'0','监测数据越限，自动生成告警', '2026-10-28 14:05:00'),
(2,'confirm',2,'陈志强','0','1','已确认，安排观察温度趋势', '2026-10-28 14:20:00'),
(3,'create',NULL,'系统',NULL,'0','监测数据越限，自动生成告警', '2026-10-27 10:30:00'),
(3,'confirm',2,'陈志强','0','1','已确认，湿度过高', '2026-10-27 10:45:00'),
(3,'handle',2,'陈志强','1','2','派苏婷现场检查除湿能力', '2026-10-27 11:00:00'),
(4,'create',NULL,'系统',NULL,'0','监测数据越限，自动生成告警', '2026-10-25 09:10:00'),
(4,'confirm',1,'林伟','0','1','已确认，现场检查', '2026-10-25 09:20:00'),
(4,'handle',1,'林伟','1','2','检查发现皮带松动', '2026-10-25 09:25:00'),
(4,'close',1,'林伟','2','3','更换皮带，复测正常', '2026-10-25 10:20:00'),
(6,'create',NULL,'系统',NULL,'0','监测数据越限，自动生成告警', '2026-10-24 16:20:00'),
(6,'ignore',2,'陈志强','0','4','粒子计数器校准中，暂忽略', '2026-10-24 17:00:00');

-- 工单
-- 工单数据见 04b_seed_workorder.sql

INSERT INTO `biz_workorder_log` (`workorder_id`,`action`,`operator_id`,`operator_name`,`from_status`,`to_status`,`content`,`create_time`) VALUES
(1,'create',6,'李敏',NULL,'0','院方提交报修', '2026-10-20 14:10:00'),
(1,'receive',2,'陈志强','0','1','受理工单', '2026-10-20 14:30:00'),
(1,'dispatch',2,'陈志强','1','2','派工给林伟', '2026-10-20 15:10:00'),
(1,'arrive',1,'林伟','2','2','已到场签到', '2026-10-20 16:00:00'),
(2,'create',6,'李敏',NULL,'0','院方提交报修', '2026-10-21 10:05:00'),
(2,'receive',2,'陈志强','0','1','受理工单', '2026-10-21 10:20:00'),
(2,'dispatch',2,'陈志强','1','2','派工给苏婷', '2026-10-21 11:00:00'),
(2,'finish',2,'苏婷','2','3','处理完成，提交验收', '2026-10-21 17:20:00'),
(3,'create',2,'陈志强',NULL,'0','内部创建保养工单', '2026-10-22 08:40:00'),
(3,'dispatch',2,'陈志强','1','2','派工给林伟', '2026-10-22 09:10:00'),
(3,'finish',1,'林伟','2','3','完工提交验收', '2026-10-22 15:30:00'),
(3,'accept',6,'李敏','3','4','验收通过', '2026-10-22 16:10:00'),
(3,'callback',2,'陈志强','4','5','回访完成，满意度 5 分', '2026-10-25 09:05:00'),
(5,'create',5,'刘洋',NULL,'0','院方提交报修', '2026-10-24 08:20:00'),
(5,'receive',2,'陈志强','0','1','受理工单', '2026-10-24 08:30:00'),
(5,'dispatch',2,'陈志强','1','2','派工给王强', '2026-10-24 09:00:00'),
(6,'create',NULL,'系统',NULL,'0','告警 AL-20261029-001 自动生成工单', '2026-10-29 08:12:00'),
(11,'create',NULL,'系统',NULL,'0','告警 AL-20261025-004 自动生成工单', '2026-10-25 09:12:00'),
(11,'callback',2,'陈志强','4','5','回访完成', '2026-10-27 10:10:00');

INSERT INTO `biz_workorder_cost` (`workorder_id`,`cost_type`,`cost_name`,`quantity`,`price`,`amount`,`remark`) VALUES
(3,'part','H13 高效过滤器',4,980.00,3920.00,''),
(3,'labor','人工费',1,1200.00,1200.00,'4 人×3 小时'),
(3,'travel','交通费',1,240.00,240.00,''),
(3,'other','检漏耗材',1,500.00,500.00,''),
(11,'part','风机皮带 B-1200',2,45.00,90.00,''),
(11,'labor','人工费',1,230.00,230.00,''),
(12,'part','G4 初效过滤器',6,80.00,480.00,'');

-- 巡检
INSERT INTO `biz_inspection_plan` (`plan_id`,`plan_no`,`plan_name`,`hospital_id`,`campus_id`,`department_id`,`area_id`,`plan_type`,`cycle_type`,`cycle_value`,`start_date`,`end_date`,`next_run_date`,`item_template`,`assignee_id`,`remind_days`,`status`,`create_time`) VALUES
(1,'IP-2026-001','总院手术部洁净区日常巡检',1,1,1,1,'routine','day',1,'2026-01-01','2026-12-31','2026-10-30','["检查机组运行电流","检查初效/中效过滤器压差","检查房间正压","检查送风温湿度","检查冷凝水排放"]',1,1,'0',NOW()),
(2,'IP-2026-002','泉州城东手术室周巡检',3,4,7,7,'weekly','week',1,'2026-01-01','2026-12-31','2026-11-02','["检查层流机组运行状态","检查高效过滤器","检查房间压差","检查备用电源"]',4,1,'0',NOW()),
(3,'IP-2026-003','厦门院区洁净区月度专项巡检',1,NULL,NULL,NULL,'monthly','month',1,'2026-01-01','2026-12-31','2026-11-01','["洁净度检测","换气次数检测","压差梯度检查","温湿度记录核查"]',2,3,'0',NOW());

INSERT INTO `biz_inspection_task` (`task_id`,`task_no`,`plan_id`,`task_type`,`hospital_id`,`campus_id`,`department_id`,`area_id`,`equipment_id`,`plan_time`,`assignee_id`,`assignee_name`,`status`,`start_time`,`finish_time`,`result`,`problem_desc`,`create_time`) VALUES
(1,'IT-20261029-001',1,'plan',1,1,1,1,NULL,'2026-10-29',1,'林伟','1','2026-10-29 08:30:00',NULL,NULL,NULL,NOW()),
(2,'IT-20261028-001',1,'plan',1,1,1,1,NULL,'2026-10-28',1,'林伟','2','2026-10-28 08:20:00','2026-10-28 09:10:00','1',NULL,NOW()),
(3,'IT-20261026-001',2,'plan',3,4,7,7,NULL,'2026-10-26',4,'王强','3',NULL,NULL,NULL,'未按时执行，系统标记逾期',NOW());

INSERT INTO `biz_inspection_item` (`task_id`,`item_name`,`standard`,`result`,`item_value`,`remark`,`sort`) VALUES
(1,'检查机组运行电流','额定电流 ±10%',NULL,NULL,NULL,1),
(1,'检查初效/中效过滤器压差','初效 ≤120Pa，中效 ≤250Pa',NULL,NULL,NULL,2),
(1,'检查房间正压','≥10Pa',NULL,NULL,NULL,3),
(1,'检查送风温湿度','温度 21-25℃，湿度 45-60%',NULL,NULL,NULL,4),
(1,'检查冷凝水排放','排水通畅无积水',NULL,NULL,NULL,5),
(2,'检查机组运行电流','额定电流 ±10%','1','14.2A',NULL,1),
(2,'检查初效/中效过滤器压差','初效 ≤120Pa，中效 ≤250Pa','1','85Pa/160Pa',NULL,2),
(2,'检查房间正压','≥10Pa','1','16Pa',NULL,3),
(2,'检查送风温湿度','温度 21-25℃，湿度 45-60%','1','23.1℃/52%',NULL,4),
(2,'检查冷凝水排放','排水通畅无积水','1','正常',NULL,5),
(3,'检查层流机组运行状态','运行平稳无异响',NULL,NULL,NULL,1),
(3,'检查高效过滤器','无破损、压差正常',NULL,NULL,NULL,2),
(3,'检查房间压差','≥10Pa',NULL,NULL,NULL,3),
(3,'检查备用电源','切换正常',NULL,NULL,NULL,4);

-- 保养
INSERT INTO `biz_maintenance_plan` (`plan_id`,`plan_no`,`plan_name`,`hospital_id`,`campus_id`,`department_id`,`equipment_id`,`maintenance_level`,`cycle_type`,`cycle_value`,`start_date`,`end_date`,`next_run_date`,`content`,`assignee_id`,`remind_days`,`status`,`create_time`) VALUES
(1,'MP-2026-001','总院手术部层流机组季度保养',1,1,1,1,'level1','quarter',1,'2026-01-01','2026-12-31','2026-11-15','清洗初效/中效过滤器；检查风机皮带与轴承；校准温湿度传感器；检查加湿/除湿段；紧固电气接线',1,7,'0',NOW()),
(2,'MP-2026-002','泉州城东手术室层流机组月度保养',3,4,7,9,'routine','month',1,'2026-01-01','2026-12-31','2026-11-05','清洗初效过滤器；检查机组振动与噪声；检查冷凝水盘；记录运行参数',4,7,'0',NOW());

INSERT INTO `biz_maintenance_task` (`task_id`,`task_no`,`plan_id`,`hospital_id`,`campus_id`,`department_id`,`equipment_id`,`plan_date`,`assignee_id`,`status`,`start_time`,`finish_time`,`result`,`content`,`create_time`) VALUES
(1,'MT-20260915-001',1,1,1,1,1,'2026-09-15',1,'2','2026-09-15 09:00:00','2026-09-15 12:00:00','1','清洗过滤器，检查风机皮带，参数正常',NOW()),
(2,'MT-20261105-001',2,3,4,7,9,'2026-11-05',4,'0',NULL,NULL,NULL,NULL,NOW());

INSERT INTO `biz_maintenance_item` (`task_id`,`item_name`,`standard`,`result`,`item_value`,`remark`,`sort`) VALUES
(1,'清洗初效过滤器','压差恢复至 ≤80Pa','1','65Pa',NULL,1),
(1,'检查风机皮带','无裂纹、张紧度正常','1','正常',NULL,2),
(1,'校准温湿度传感器','误差 ≤0.5℃/3%RH','1','已校准',NULL,3),
(2,'清洗初效过滤器','压差恢复至 ≤80Pa',NULL,NULL,NULL,1),
(2,'检查机组振动与噪声','无明显异常','',NULL,NULL,2),
(2,'检查冷凝水盘','排水通畅','',NULL,NULL,3);

-- 备件
INSERT INTO `biz_part` (`part_id`,`part_code`,`part_name`,`part_category`,`spec`,`unit`,`brand`,`safety_stock`,`current_stock`,`price`,`supplier`,`warehouse`,`status`,`create_time`) VALUES
(1,'P-FLT-G4','G4 初效过滤器','filter','592×592×46','只','AAF',10,24,120.00,'厦门净化器材有限公司','厦门备件库 A-01','0',NOW()),
(2,'P-FLT-F8','F8 中效过滤器','filter','592×592×534','只','AAF',8,18,260.00,'厦门净化器材有限公司','厦门备件库 A-02','0',NOW()),
(3,'P-FLT-H13','H13 高效过滤器','filter','484×484×220','只','AAF',8,6,980.00,'厦门净化器材有限公司','厦门备件库 A-03','0',NOW()),
(4,'P-BELT-B1200','风机皮带 B-1200','fan','B-1200','条','盖茨',10,30,45.00,'泉州机电配件商行','厦门备件库 B-01','0',NOW()),
(5,'P-MOTOR-15','风机电机 1.5kW','motor','YE3-90L-4','台','西门子',2,4,1350.00,'泉州机电配件商行','厦门备件库 B-02','0',NOW()),
(6,'P-SEN-TH','温湿度传感器','sensor','RS485 壁挂式','只','霍尼韦尔',5,9,380.00,'厦门自控设备有限公司','厦门备件库 C-01','0',NOW()),
(7,'P-SEN-DP','微压差传感器','sensor','0-100Pa 4-20mA','只','霍尼韦尔',5,3,520.00,'厦门自控设备有限公司','厦门备件库 C-02','0',NOW()),
(8,'P-REF-R410','制冷剂 R410A','consumable','10kg/瓶','瓶','霍尼韦尔',6,12,260.00,'厦门制冷配件商行','厦门备件库 D-01','0',NOW());

INSERT INTO `biz_part_record` (`part_id`,`record_type`,`quantity`,`before_stock`,`after_stock`,`workorder_id`,`operator_name`,`record_time`,`remark`) VALUES
(1,'in',20,4,24,NULL,'吴静','2026-10-01 10:00:00','季度采购入库'),
(3,'in',10,0,10,NULL,'吴静','2026-10-01 10:10:00','季度采购入库'),
(3,'out',4,10,6,3,'林伟','2026-10-22 09:00:00','工单领用'),
(4,'out',2,32,30,11,'林伟','2026-10-25 09:30:00','工单领用'),
(6,'out',1,10,9,2,'苏婷','2026-10-21 13:00:00','工单领用');

INSERT INTO `biz_part_requisition` (`req_id`,`req_no`,`workorder_id`,`hospital_id`,`applicant_id`,`applicant_name`,`status`,`apply_time`,`approve_by`,`approve_time`,`out_by`,`out_time`,`total_amount`,`remark`) VALUES
(1,'RQ-20261022-001',3,1,1,'林伟','2','2026-10-22 08:50:00','陈志强','2026-10-22 08:55:00','吴静','2026-10-22 09:00:00',3920.00,'高效过滤器更换'),
(2,'RQ-20261029-001',NULL,1,2,'苏婷','0','2026-10-29 09:00:00',NULL,NULL,NULL,NULL,760.00,'压差传感器备货');

INSERT INTO `biz_part_requisition_item` (`req_id`,`part_id`,`part_name`,`spec`,`quantity`,`price`,`amount`) VALUES
(1,3,'H13 高效过滤器','484×484×220',4,980.00,3920.00),
(2,7,'微压差传感器','0-100Pa 4-20mA',2,380.00,760.00);

-- 文档 / 知识库
INSERT INTO `biz_document` (`doc_id`,`doc_name`,`doc_category`,`hospital_id`,`campus_id`,`equipment_id`,`file_url`,`file_name`,`file_size`,`file_type`,`version`,`uploader_id`,`uploader_name`,`upload_time`,`tags`,`remark`) VALUES
(1,'总院手术部洁净空调系统竣工图','drawing',1,1,NULL,'/upload/demo/xm-or-hvac-asbuilt.pdf','总院手术部洁净空调系统竣工图.pdf',5242880,'pdf','V1.0',1,'陈志强','2026-09-01 10:00:00','手术部,竣工图',''),
(2,'层流机组 ZK-5000 使用说明书','manual',1,1,1,'/upload/demo/zk5000-manual.pdf','层流机组ZK5000使用说明书.pdf',3145728,'pdf','V2.1',1,'陈志强','2026-09-02 10:00:00','层流机组,说明书',''),
(3,'2026 年第三季度洁净区检测报告','test_report',1,1,NULL,'/upload/demo/xm-q3-test-report.pdf','2026年第三季度洁净区检测报告.pdf',2097152,'pdf','V1.0',2,'苏婷','2026-10-08 15:00:00','洁净度,检测报告',''),
(4,'泉州城东手术部维保验收单','acceptance',3,4,NULL,'/upload/demo/qz-acceptance.pdf','泉州城东手术部维保验收单.pdf',1048576,'pdf','V1.0',4,'王强','2026-09-30 11:00:00','验收,手术部','');

INSERT INTO `biz_knowledge` (`article_id`,`title`,`category`,`summary`,`content`,`tags`,`author_id`,`author_name`,`view_count`,`status`,`publish_time`,`create_time`) VALUES
(1,'层流机组房间正压不足的排查思路','fault_case','从风量、回风、门禁、压差传感器四个方向逐项排查','## 现象\n手术室正压低于 5Pa，压差报警。\n\n## 排查步骤\n1. 检查送风量是否下降（过滤器阻力、皮带打滑）。\n2. 检查回风口是否被遮挡、回风阀开度。\n3. 检查房间门是否关闭、传递窗密封。\n4. 校验压差传感器零点。\n\n## 处置\n先恢复风量，再复测压差，必要时更换高效过滤器。','层流,压差,故障案例',1,'林伟',36,'1','2026-09-15 10:00:00',NOW()),
(2,'洁净手术部日常巡检标准作业程序','sop','巡检项目、判定标准与记录要求','## 巡检项目\n- 机组运行电流\n- 初效/中效压差\n- 房间正压\n- 送风温湿度\n- 冷凝水排放\n\n## 记录\n逐项填写实测值，异常项拍照并转工单。','巡检,SOP',2,'苏婷',28,'1','2026-09-20 10:00:00',NOW()),
(3,'净化空调系统突发停机应急预案','emergency','停机后的报告、处置与恢复流程','## 应急响应\n1. 5 分钟内通知科室与院感科。\n2. 评估手术安排，必要时暂停接台。\n3. 抢修恢复后做洁净度快速检测。\n4. 24 小时内提交事件报告。','应急,停机',3,'黄建国',19,'1','2026-09-25 10:00:00',NOW()),
(4,'医用供氧汇流排压力异常处理','fault_case','压力低的常见原因与处置','常见原因：汇流排切换阀故障、减压阀失效、管路泄漏、压力传感器漂移。\n\n处置：切换备用汇流排，通知院方设备科，排查泄漏点。','供氧,压力',4,'王强',12,'1','2026-10-01 10:00:00',NOW());

-- 预算
INSERT INTO `biz_budget` (`budget_id`,`budget_year`,`budget_name`,`hospital_id`,`contract_id`,`subject`,`amount`,`used_amount`,`status`,`remark`,`create_time`) VALUES
(1,2026,'2026 年厦门第一医院人工预算',1,1,'labor',300000.00,120000.00,'1','',NOW()),
(2,2026,'2026 年厦门第一医院备件预算',1,1,'parts',180000.00,76000.00,'1','',NOW()),
(3,2026,'2026 年厦门第一医院差旅预算',1,1,'travel',60000.00,21000.00,'1','',NOW()),
(4,2026,'2026 年厦门中医院备件预算',2,2,'parts',90000.00,34000.00,'1','',NOW()),
(5,2026,'2026 年泉州第一医院人工预算',3,3,'labor',260000.00,180000.00,'1','',NOW()),
(6,2026,'2026 年泉州第一医院外包预算',3,3,'outsource',80000.00,20000.00,'1','',NOW());

INSERT INTO `biz_budget_execution` (`budget_id`,`workorder_id`,`amount`,`occur_date`,`exec_type`,`remark`,`create_time`) VALUES
(2,3,3920.00,'2026-10-22','1','高效过滤器领用',NOW()),
(1,3,1200.00,'2026-10-22','1','人工费',NOW()),
(2,11,90.00,'2026-10-25','1','风机皮带领用',NOW()),
(5,5,800.00,'2026-10-24','1','负压机组抢修人工',NOW());

-- 站内消息
INSERT INTO `biz_message` (`msg_type`,`title`,`content`,`level`,`biz_type`,`biz_id`,`biz_key`,`receiver_id`,`receiver_role`,`hospital_id`,`is_read`,`create_time`) VALUES
('alarm','紧急告警：泉州城东手术室正压不足','层流机组 1# 房间正压 3.2Pa，低于 5Pa 下限','1','alarm',1,'alarm:1:create',0,'100,101,200,201',3,'0',NOW()),
('alarm','紧急告警：供氧汇流排压力低','输出压力 0.28MPa，低于 0.30MPa','1','alarm',5,'alarm:5:create',0,'100,101,200,201',3,'0',NOW()),
('workorder','新工单待受理：手术部净化空调异响','工单 WO-20261020-001 已提交','2','workorder',1,'workorder:1:create',0,'100',1,'1',NOW()),
('contract','合同到期提醒：泉州第一医院洁净手术部维保合同','合同将于 2026-11-30 到期，剩余 32 天，请及时续签','2','contract',3,'contract:3:30',0,'100,101,200,201',3,'0',NOW()),
('cert','证书即将到期：林伟-制冷与空调作业证','证书将于 2026-11-20 到期，剩余 22 天','2','certificate',1,'cert:1:30',0,'100',1,'0',NOW());

-- 院方账号绑定
INSERT INTO `biz_hospital_user` (`user_id`,`hospital_id`,`department_id`,`role_type`,`status`,`create_time`) VALUES
(6,1,NULL,'admin','0',NOW()),
(7,1,1,'dept','0',NOW());
