-- =============================================================
-- 演示数据：客户档案 / 合同 / 设备 / 点位 / 告警 / 工单 / 巡检 / 保养 / 备件 / 预算
-- =============================================================
USE `laminar_ops`;

-- 公司 / 区域 / 医院 / 院区
INSERT INTO `biz_company` (`company_id`,`company_name`,`company_code`,`address`,`phone`,`contact`,`status`,`create_time`) VALUES
(1,'厦门洁净维保科技有限公司','CLEAN-XM','厦门市湖里区高崎南五路 208 号','0592-8800000','陈志强','0',NOW());

INSERT INTO `biz_region` (`region_id`,`company_id`,`region_name`,`region_code`,`manager`,`phone`,`status`,`create_time`) VALUES
(1,1,'厦门区域','XM','黄建国','13800000005','0',NOW()),
(2,1,'泉州区域','QZ','王强','13800000008','0',NOW());

INSERT INTO `biz_hospital` (`hospital_id`,`hospital_name`,`hospital_code`,`region_id`,`hospital_type`,`grade`,`nature`,`address`,`longitude`,`latitude`,`phone`,`contact_person`,`status`,`create_time`) VALUES
(1,'厦门大学附属第一医院','HXM001',1,'general','三甲','public','厦门市思明区镇海路 55 号',118.082000,24.446000,'0592-2137270','李敏','0',NOW()),
(2,'厦门市中医院','HXM002',1,'tcm','三甲','public','厦门市湖里区仙岳路 1739 号',118.145000,24.502000,'0592-5579666','陈刚','0',NOW()),
(3,'泉州市第一医院','HQZ001',2,'general','三甲','public','泉州市鲤城区东街 248 号',118.576000,24.913000,'0595-22277031','刘洋','0',NOW());

INSERT INTO `biz_campus` (`campus_id`,`hospital_id`,`campus_code`,`campus_name`,`address`,`longitude`,`latitude`,`phone`,`bed_count`,`status`,`create_time`) VALUES
(1,1,'HXM001-01','总院区','厦门市思明区镇海路 55 号',118.082000,24.446000,'0592-2137270',1500,'0',NOW()),
(2,1,'HXM001-02','杏林院区','厦门市集美区杏林洪埭路 1 号',118.062000,24.565000,'0592-6248000',500,'0',NOW()),
(3,2,'HXM002-01','总院区','厦门市湖里区仙岳路 1739 号',118.145000,24.502000,'0592-5579666',800,'0',NOW()),
(4,3,'HQZ001-01','城东院区','泉州市丰泽区城东街 248 号',118.610000,24.925000,'0595-28280000',1000,'0',NOW());

INSERT INTO `biz_department` (`department_id`,`hospital_id`,`campus_id`,`dept_code`,`department_name`,`dept_type`,`director`,`phone`,`location`,`sort`,`status`,`create_time`) VALUES
(1,1,1,'XM-SS','手术室','operating','赵主任','0592-2137001','住院部 3 号楼 4 层',1,'0',NOW()),
(2,1,1,'XM-ICU','重症医学科','icu','孙主任','0592-2137002','住院部 2 号楼 5 层',2,'0',NOW()),
(3,1,1,'XM-CSSD','消毒供应中心','cssd','钱护士长','0592-2137003','医技楼 1 层',3,'0',NOW()),
(4,1,1,'XM-PIVAS','静脉用药调配中心','pharmacy','周药师','0592-2137004','门诊楼 5 层',4,'0',NOW()),
(5,1,2,'XM-XL-SS','杏林手术室','operating','吴主任','0592-6248001','杏林院区 1 号楼 3 层',5,'0',NOW()),
(6,2,3,'ZY-SS','手术室','operating','郑主任','0592-5579601','住院部 4 层',6,'0',NOW()),
(7,3,4,'QZ-SS','手术室','operating','王主任','0595-28280001','外科楼 5 层',7,'0',NOW()),
(8,3,4,'QZ-ICU','重症医学科','icu','黄主任','0595-28280002','外科楼 6 层',8,'0',NOW());

INSERT INTO `biz_clean_area` (`area_id`,`hospital_id`,`campus_id`,`department_id`,`area_code`,`area_name`,`area_type`,`clean_level`,`area_size`,`design_temp_min`,`design_temp_max`,`design_humidity_min`,`design_humidity_max`,`design_pressure`,`clean_require`,`status`,`create_time`) VALUES
(1,1,1,1,'XM-OR-01','总院手术部百级洁净区','operating','100',320.00,21.0,25.0,45.0,60.0,15.0,'I 级手术室，1-4 号手术间及走廊','0',NOW()),
(2,1,1,2,'XM-ICU-01','总院 ICU 万级洁净区','icu','10000',260.00,22.0,26.0,45.0,65.0,10.0,'重症监护病区整体万级','0',NOW()),
(3,1,1,3,'XM-CSSD-01','总院 CSSD 无菌物品存放区','cssd','100000',150.00,20.0,24.0,40.0,60.0,8.0,'无菌物品存放，十万级','0',NOW()),
(4,1,1,4,'XM-PIVAS-01','总院静配中心洁净区','pharmacy','10000',180.00,20.0,24.0,45.0,60.0,12.0,'静脉用药调配中心','0',NOW()),
(5,1,2,5,'XM-XL-OR-01','杏林手术室洁净区','operating','1000',200.00,21.0,25.0,45.0,60.0,12.0,'千级手术间 3 间','0',NOW()),
(6,2,3,6,'ZY-OR-01','中医院手术室洁净区','operating','10000',240.00,21.0,25.0,45.0,60.0,10.0,'万级手术间 4 间','0',NOW()),
(7,3,4,7,'QZ-OR-01','泉州城东手术室百级洁净区','operating','100',350.00,21.0,25.0,45.0,60.0,15.0,'I 级手术间 5 间','0',NOW()),
(8,3,4,8,'QZ-ICU-01','泉州城东 ICU 洁净区','icu','10000',300.00,22.0,26.0,45.0,65.0,10.0,'ICU 万级洁净区','0',NOW());

INSERT INTO `biz_contact` (`contact_id`,`hospital_id`,`campus_id`,`department_id`,`contact_name`,`position`,`contact_type`,`phone`,`email`,`duty`,`is_primary`,`status`,`create_time`) VALUES
(1,1,1,NULL,'李敏','设备科科长','equipment_dept','13800000006','limin@xmfy.cn','设备维保总协调、工单验收',  '1','0',NOW()),
(2,1,1,NULL,'王晓','院感科主任','infection_dept','13900000012','wangx@xmfy.cn','洁净区院感核查、检测报告', '0','0',NOW()),
(3,1,1,1,'张晓芸','手术室护士长','dept_leader','13800000007','zhangxy@xmfy.cn','手术室报修与现场配合','0','0',NOW()),
(4,2,3,NULL,'陈刚','设备科工程师','equipment_dept','13900000021','cheng@zyyy.cn','中医院设备维保对接','1','0',NOW()),
(5,3,4,NULL,'刘洋','设备科科长','equipment_dept','13900000031','liuy@qzyy.cn','泉州院区维保统筹','1','0',NOW()),
(6,3,4,NULL,'林芳','院感科','infection_dept','13900000032','linf@qzyy.cn','洁净区院感核查','0','0',NOW());

-- 合同
INSERT INTO `biz_contract` (`contract_id`,`contract_no`,`contract_name`,`hospital_id`,`company_id`,`contract_type`,`start_date`,`end_date`,`sign_date`,`amount`,`manager_id`,`warn_days`,`status`,`remark`,`create_time`) VALUES
(1,'HT-2024-001','厦门大学附属第一医院层流净化设备维保合同',1,1,'full','2024-01-01','2026-12-31','2023-12-20',1200000.00,1,90,'0','含层流机组、净化空调、新风、除湿全包维保',NOW()),
(2,'HT-2025-002','厦门市中医院净化空调系统维保合同',2,1,'half','2025-03-01','2027-02-28','2025-02-18',680000.00,2,90,'0','半包：人工+常规耗材',NOW()),
(3,'HT-2023-003','泉州市第一医院洁净手术部维保合同',3,1,'full','2023-12-01','2026-11-30','2023-11-15',960000.00,4,90,'0','即将到期，需续签',NOW()),
(4,'HT-2024-004','厦门大学附属第一医院杏林院区净化系统维保',1,1,'full','2024-07-01','2026-06-30','2024-06-15',420000.00,1,90,'2','已到期，待续签',NOW());

-- 设备类型
INSERT INTO `biz_device_type` (`type_id`,`type_code`,`type_name`,`category`,`icon`,`monitor_enabled`,`param_template`,`sort`,`status`,`remark`,`create_time`) VALUES
(1,'LAM-01','层流机组','laminar','cpu','1','[{"metric":"temperature","unit":"℃"},{"metric":"humidity","unit":"%RH"},{"metric":"pressure","unit":"Pa"},{"metric":"wind_speed","unit":"m/s"},{"metric":"particle","unit":"粒/L"}]',1,'0','手术室/ICU 层流送风机组',NOW()),
(2,'AC-01','净化空调机组','purifier','wind-power','1','[{"metric":"temperature","unit":"℃"},{"metric":"humidity","unit":"%RH"},{"metric":"pressure","unit":"Pa"},{"metric":"wind_speed","unit":"m/s"}]',2,'0','洁净区净化空调',NOW()),
(3,'FA-01','新风机组','fresh_air','refresh','1','[{"metric":"temperature","unit":"℃"},{"metric":"humidity","unit":"%RH"},{"metric":"pressure","unit":"Pa"}]',3,'0','洁净区新风',NOW()),
(4,'DH-01','除湿机组','dehumidifier','odometer','1','[{"metric":"temperature","unit":"℃"},{"metric":"humidity","unit":"%RH"}]',4,'0','洁净区除湿',NOW()),
(5,'OX-01','供氧汇流排','oxygen','first-aid-kit','1','[{"metric":"oxygen_pressure","unit":"MPa"}]',5,'0','医用氧气汇流排',NOW()),
(6,'VAC-01','负压真空机组','vacuum','c-scale-to-original','1','[{"metric":"vacuum","unit":"kPa"}]',6,'0','医用真空负压机组',NOW());

-- 设备
INSERT INTO `biz_equipment` (`equipment_id`,`equipment_code`,`equipment_name`,`hospital_id`,`campus_id`,`department_id`,`area_id`,`device_type_id`,`category`,`model`,`brand`,`serial_no`,`install_date`,`install_location`,`usage_location`,`contract_id`,`run_status`,`monitor_status`,`protocol`,`comm_address`,`rated_power`,`status`,`create_time`) VALUES
(1,'EQ-XM-0001','总院手术部层流机组 1#',1,1,1,1,1,'laminar','ZK-5000','盾安','DA2023L001','2023-05-10','3 号楼屋面空调机房','手术部 1-2 号手术间',1,'0','1','Modbus TCP','192.168.10.11',15.0,'0',NOW()),
(2,'EQ-XM-0002','总院手术部层流机组 2#',1,1,1,1,1,'laminar','ZK-5000','盾安','DA2023L002','2023-05-10','3 号楼屋面空调机房','手术部 3-4 号手术间',1,'0','1','Modbus TCP','192.168.10.12',15.0,'0',NOW()),
(3,'EQ-XM-0003','总院手术部净化空调 1#',1,1,1,1,2,'purifier','JH-3000','美的','MD2022A031','2022-11-20','3 号楼 4 层空调机房','手术部走廊及辅助区',1,'0','1','BACnet IP','192.168.10.21',22.0,'0',NOW()),
(4,'EQ-XM-0004','总院手术部新风机组 1#',1,1,1,1,3,'fresh_air','XF-2000','格力','GR2022F012','2022-11-20','3 号楼屋面','手术部新风',1,'0','1','Modbus TCP','192.168.10.31',7.5,'0',NOW()),
(5,'EQ-XM-0005','总院手术部除湿机组 1#',1,1,1,1,4,'dehumidifier','CS-800','川井','CJ2021D008','2021-08-15','3 号楼 4 层设备间','手术部辅助区',1,'1','1','Modbus RTU','COM1-8',3.0,'0',NOW()),
(6,'EQ-XM-0006','总院 ICU 层流机组 1#',1,1,2,2,1,'laminar','ZK-4000','盾安','DA2023L006','2023-06-01','2 号楼屋面','ICU 病区',1,'0','1','Modbus TCP','192.168.10.13',12.0,'0',NOW()),
(7,'EQ-XM-0007','杏林手术室层流机组 1#',1,2,5,5,1,'laminar','ZK-3000','天加','TJ2024L003','2024-03-12','杏林 1 号楼屋面','杏林手术间 1-3',4,'0','1','Modbus TCP','192.168.20.11',11.0,'0',NOW()),
(8,'EQ-ZY-0001','中医院手术室净化空调 1#',2,3,6,6,2,'purifier','JH-2500','美的','MD2023A066','2023-09-01','住院部屋面','手术室洁净区',2,'0','1','BACnet IP','192.168.30.21',18.5,'0',NOW()),
(9,'EQ-QZ-0001','泉州城东手术室层流机组 1#',3,4,7,7,1,'laminar','ZK-5000','盾安','DA2024L011','2024-01-18','外科楼屋面','手术间 1-3',3,'2','1','Modbus TCP','192.168.40.11',15.0,'0',NOW()),
(10,'EQ-QZ-0002','泉州城东手术室层流机组 2#',3,4,7,7,1,'laminar','ZK-5000','盾安','DA2024L012','2024-01-18','外科楼屋面','手术间 4-5',3,'0','1','Modbus TCP','192.168.40.12',15.0,'0',NOW()),
(11,'EQ-QZ-0003','泉州城东 ICU 负压真空机组 1#',3,4,8,8,6,'vacuum','VAC-200','普旭','BX2022V005','2022-04-08','外科楼地下室','ICU 负压系统',3,'0','1','Modbus TCP','192.168.40.31',5.5,'0',NOW()),
(12,'EQ-QZ-0004','泉州城东供氧汇流排 1#',3,4,8,8,5,'oxygen','OX-10','普莱克斯','PX2020O002','2020-12-01','外科楼 1 层气体站','全院医用氧气',3,'0','2','网关','192.168.40.41',1.0,'0',NOW());

-- 监测点位（按设备类型自动生成）
INSERT IGNORE INTO `biz_monitor_point` (`equipment_id`,`point_code`,`point_name`,`metric`,`unit`,`warn_lower`,`warn_upper`,`alarm_lower`,`alarm_upper`,`precision`,`collect_interval`,`enabled`,`sort`,`create_time`)
SELECT e.equipment_id,'TEMP','送风温度','temperature','℃',22,25,21,26,1,30,'1',1,NOW()
FROM biz_equipment e JOIN biz_device_type t ON t.type_id=e.device_type_id WHERE t.type_code IN ('LAM-01','AC-01','FA-01','DH-01');

INSERT IGNORE INTO `biz_monitor_point` (`equipment_id`,`point_code`,`point_name`,`metric`,`unit`,`warn_lower`,`warn_upper`,`alarm_lower`,`alarm_upper`,`precision`,`collect_interval`,`enabled`,`sort`,`create_time`)
SELECT e.equipment_id,'HUM','回风湿度','humidity','%RH',45,60,40,65,1,30,'1',2,NOW()
FROM biz_equipment e JOIN biz_device_type t ON t.type_id=e.device_type_id WHERE t.type_code IN ('LAM-01','AC-01','FA-01','DH-01');

INSERT IGNORE INTO `biz_monitor_point` (`equipment_id`,`point_code`,`point_name`,`metric`,`unit`,`warn_lower`,`warn_upper`,`alarm_lower`,`alarm_upper`,`precision`,`collect_interval`,`enabled`,`sort`,`create_time`)
SELECT e.equipment_id,'PRES','房间正压','pressure','Pa',8,30,5,35,1,30,'1',3,NOW()
FROM biz_equipment e JOIN biz_device_type t ON t.type_id=e.device_type_id WHERE t.type_code IN ('LAM-01','AC-01','FA-01');

INSERT IGNORE INTO `biz_monitor_point` (`equipment_id`,`point_code`,`point_name`,`metric`,`unit`,`warn_lower`,`warn_upper`,`alarm_lower`,`alarm_upper`,`precision`,`collect_interval`,`enabled`,`sort`,`create_time`)
SELECT e.equipment_id,'WIND','送风风速','wind_speed','m/s',0.25,0.45,0.20,0.50,2,30,'1',4,NOW()
FROM biz_equipment e JOIN biz_device_type t ON t.type_id=e.device_type_id WHERE t.type_code IN ('LAM-01','AC-01');

INSERT IGNORE INTO `biz_monitor_point` (`equipment_id`,`point_code`,`point_name`,`metric`,`unit`,`warn_lower`,`warn_upper`,`alarm_lower`,`alarm_upper`,`precision`,`collect_interval`,`enabled`,`sort`,`create_time`)
SELECT e.equipment_id,'PART','悬浮粒子(≥0.5μm)','particle','粒/L',NULL,3000,NULL,3520,0,60,'1',5,NOW()
FROM biz_equipment e JOIN biz_device_type t ON t.type_id=e.device_type_id WHERE t.type_code='LAM-01';

INSERT IGNORE INTO `biz_monitor_point` (`equipment_id`,`point_code`,`point_name`,`metric`,`unit`,`warn_lower`,`warn_upper`,`alarm_lower`,`alarm_upper`,`precision`,`collect_interval`,`enabled`,`sort`,`create_time`)
SELECT e.equipment_id,'O2P','汇流排输出压力','oxygen_pressure','MPa',0.35,0.55,0.30,0.60,2,30,'1',1,NOW()
FROM biz_equipment e JOIN biz_device_type t ON t.type_id=e.device_type_id WHERE t.type_code='OX-01';

INSERT IGNORE INTO `biz_monitor_point` (`equipment_id`,`point_code`,`point_name`,`metric`,`unit`,`warn_lower`,`warn_upper`,`alarm_lower`,`alarm_upper`,`precision`,`collect_interval`,`enabled`,`sort`,`create_time`)
SELECT e.equipment_id,'VAC','系统真空度','vacuum','kPa',-80,-65,-85,-60,1,30,'1',1,NOW()
FROM biz_equipment e JOIN biz_device_type t ON t.type_id=e.device_type_id WHERE t.type_code='VAC-01';

-- 告警规则
INSERT INTO `biz_alarm_rule` (`rule_id`,`rule_name`,`device_type_id`,`metric`,`level`,`operator`,`threshold`,`threshold2`,`duration`,`auto_workorder`,`notify_roles`,`enabled`,`remark`,`create_time`) VALUES
(1,'房间正压不足',NULL,'pressure','1','lt',5,NULL,30,'1','100,101','1','压差低于 5Pa 触发紧急告警',NOW()),
(2,'送风温度过高',NULL,'temperature','2','gt',26,NULL,120,'0','100,101','1','温度超过 26℃ 持续 2 分钟',NOW()),
(3,'相对湿度过高',NULL,'humidity','3','gt',65,NULL,300,'0','100','1','湿度超过 65%RH 持续 5 分钟',NOW()),
(4,'送风风速偏低',NULL,'wind_speed','3','lt',0.2,NULL,180,'0','100','1','风速低于 0.2m/s',NOW()),
(5,'悬浮粒子超标',1,'particle','2','gt',3520,NULL,0,'1','100,101','1','层流区域粒子数超标',NOW()),
(6,'医用氧气压力低',5,'oxygen_pressure','1','lt',0.30,NULL,0,'1','100,101','1','汇流排压力低于 0.30MPa',NOW()),
(7,'真空度不足',6,'vacuum','2','gt',-60,NULL,60,'1','100','1','负压系统真空度不足',NOW()),
(8,'设备离线',NULL,'heartbeat','2','offline',NULL,NULL,300,'0','100,101','1','心跳丢失超过 5 分钟',NOW());

-- 工程师 / 技能 / 服务医院 / 证书
INSERT INTO `biz_engineer` (`engineer_id`,`user_id`,`company_id`,`region_id`,`engineer_name`,`phone`,`gender`,`job_title`,`specialty`,`entry_date`,`on_duty`,`latitude`,`longitude`,`status`,`create_time`) VALUES
(1,3,1,1,'林伟','13800000003','0','中级工程师','层流机组、净化空调','2021-03-01','1',24.480000,118.090000,'0',NOW()),
(2,4,1,1,'苏婷','13800000004','1','中级工程师','新风、除湿、洁净室检测','2022-07-15','1',24.460000,118.120000,'0',NOW()),
(3,5,1,1,'黄建国','13800000005','0','区域主管','区域统筹、层流机组','2019-05-20','1',24.450000,118.080000,'0',NOW()),
(4,NULL,1,2,'王强','13800000008','0','高级工程师','层流、负压真空、供氧','2018-09-01','1',24.910000,118.580000,'0',NOW());

INSERT INTO `biz_skill` (`skill_id`,`skill_name`,`skill_code`,`category`,`status`,`create_time`) VALUES
(1,'层流机组维保','SK-LAM','设备','0',NOW()),
(2,'净化空调维保','SK-AC','设备','0',NOW()),
(3,'新风系统维保','SK-FA','设备','0',NOW()),
(4,'除湿机组维保','SK-DH','设备','0',NOW()),
(5,'供氧/负压系统','SK-GAS','设备','0',NOW()),
(6,'电气与自控','SK-ELEC','通用','0',NOW()),
(7,'洁净室检测','SK-TEST','检测','0',NOW());

INSERT INTO `biz_engineer_skill` (`engineer_id`,`skill_id`,`level`) VALUES
(1,1,'高级'),(1,2,'高级'),(1,6,'中级'),
(2,2,'中级'),(2,3,'高级'),(2,4,'中级'),(2,7,'高级'),
(3,1,'高级'),(3,6,'高级'),
(4,1,'高级'),(4,5,'高级'),(4,6,'中级');

INSERT INTO `biz_engineer_hospital` (`engineer_id`,`hospital_id`,`is_manager`) VALUES
(1,1,'0'),(1,2,'0'),(2,1,'0'),(2,2,'0'),(3,1,'1'),(3,2,'1'),(3,3,'0'),(4,3,'1');

INSERT INTO `biz_certificate` (`cert_id`,`engineer_id`,`cert_type`,`cert_name`,`cert_no`,`issuing_authority`,`issue_date`,`expire_date`,`remind_days`,`status`,`create_time`) VALUES
(1,1,'skill_certificate','制冷与空调作业证','XMRZ-2021-0088','厦门市应急管理局','2021-11-20','2026-11-20',60,'1',NOW()),
(2,1,'skill_certificate','低压电工作业证','XMDG-2022-1024','厦门市应急管理局','2022-06-10','2027-06-10',60,'0',NOW()),
(3,2,'skill_certificate','洁净室检测员证','JMJC-2023-0156','中国洁净室检测中心','2023-12-10','2026-12-10',60,'1',NOW()),
(4,2,'skill_certificate','制冷与空调作业证','XMRZ-2022-0132','厦门市应急管理局','2022-09-01','2027-09-01',60,'0',NOW()),
(5,4,'special_equipment','特种设备作业证','QZTZ-2020-0451','泉州市市场监督管理局','2020-03-15','2027-03-15',60,'0',NOW()),
(6,3,'skill_certificate','安全生产管理人员证','XMAQ-2021-0077','厦门市应急管理局','2021-05-06','2028-05-06',60,'0',NOW()),
(7,1,'skill_certificate','焊工作业证','XMHA-2019-0233','厦门市应急管理局','2019-08-01','2026-08-01',60,'2',NOW());
