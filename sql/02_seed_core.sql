-- =============================================================
-- 初始化数据：角色 / 用户 / 组织 / 岗位 / 字典
-- 默认密码 123456（SHA-256 加盐）
-- =============================================================
USE `laminar_ops`;

-- 角色
INSERT INTO `sys_role` (`role_id`,`role_name`,`role_key`,`sort`,`data_scope`,`status`,`remark`) VALUES
(1,'超级管理员','super_admin',1,'all','0','平台全部权限'),
(100,'维保经理','maintenance_manager',2,'all','0','我方管理端，全业务管理'),
(101,'区域主管','regional_manager',3,'region','0','按区域查看医院与人员数据'),
(102,'维保工程师','engineer',4,'self','0','工单执行、巡检保养执行'),
(103,'公司高管','company_executive',5,'all','0','公司经营视角，查看全部业务数据'),
(104,'部门员工','department_staff',6,'self','0','仅作为部门成员，无业务菜单权限'),
(200,'院方管理员','hospital_admin',5,'hospital','0','医院端：本院全部数据'),
(201,'院方科室用户','hospital_dept',6,'self','0','医院端：本科室数据、报修与验收');

UPDATE `sys_role` SET `role_type`='standard' WHERE `role_id` IN (1,100,101,102,103,104,200,201);

-- 公司内部组织
INSERT INTO `sys_dept` (`dept_id`,`parent_id`,`dept_name`,`leader`,`phone`,`sort`,`status`,`create_time`) VALUES
(1,0,'总经办','周明','0592-8800001',1,'0',NOW()),
(2,0,'维保中心','黄建国','0592-8800002',2,'0',NOW()),
(3,2,'技术部','林伟','0592-8800003',1,'0',NOW()),
(4,2,'客服部','陈晓','0592-8800004',2,'0',NOW()),
(5,0,'财务部','吴静','0592-8800005',3,'0',NOW()),
(6,0,'人事行政部','郑芳','0592-8800006',4,'0',NOW());

-- 岗位
INSERT INTO `sys_post` (`post_id`,`post_code`,`post_name`,`sort`,`status`,`remark`) VALUES
(1,'GM','总经理',1,'0',''),
(2,'MM','维保经理',2,'0',''),
(3,'RM','区域主管',3,'0',''),
(4,'ENG','维保工程师',4,'0',''),
(5,'CS','客服专员',5,'0',''),
(6,'FIN','财务',6,'0','');

-- 用户（密码 123456）
SET @pwd = SHA2(CONCAT('123456','::laminar'),256);
INSERT INTO `sys_user` (`user_id`,`username`,`password`,`nick_name`,`phone`,`email`,`gender`,`dept_id`,`post_id`,`user_type`,`status`,`create_time`,`remark`) VALUES
(1,'admin',@pwd,'系统管理员','13800000001','admin@laminar.cn','0',1,1,'0','0',NOW(),'平台超级管理员'),
(2,'manager',@pwd,'陈志强','13800000002','manager@laminar.cn','0',2,2,'1','0',NOW(),'维保经理'),
(3,'engineer1',@pwd,'林伟','13800000003','linwei@laminar.cn','0',3,4,'1','0',NOW(),'层流机组工程师'),
(4,'engineer2',@pwd,'苏婷','13800000004','suting@laminar.cn','1',3,4,'1','0',NOW(),'净化空调工程师'),
(5,'regional1',@pwd,'黄建国','13800000005','huangjg@laminar.cn','0',2,3,'1','0',NOW(),'厦门区域主管'),
(6,'hospital1',@pwd,'李敏','13800000006','limin@xmfy.cn','1',NULL,NULL,'2','0',NOW(),'厦门大学附属第一医院 设备科'),
(7,'doctor1',@pwd,'张晓芸','13800000007','zhangxy@xmfy.cn','1',NULL,NULL,'2','0',NOW(),'厦门大学附属第一医院 手术室护士长');

INSERT INTO `sys_user_role` (`user_id`,`role_id`) VALUES
(1,1),(2,100),(3,102),(4,102),(5,101),(6,200),(7,201);

-- 字典类型
INSERT INTO `sys_dict_type` (`dict_id`,`dict_name`,`dict_type`,`status`,`remark`) VALUES
(1,'用户性别','sys_user_sex','0',''),
(2,'通用状态','sys_normal_disable','0',''),
(3,'医院类型','hospital_type','0',''),
(4,'医院等级','hospital_grade','0',''),
(5,'医院性质','hospital_nature','0',''),
(6,'科室类型','dept_type','0',''),
(7,'洁净级别','clean_level','0',''),
(8,'洁净区域类型','clean_area_type','0',''),
(9,'对接人类型','contact_type','0',''),
(10,'合同类型','contract_type','0',''),
(11,'设备大类','device_category','0',''),
(12,'设备运行状态','run_status','0',''),
(13,'设备接入状态','monitor_status','0',''),
(14,'监测指标','monitor_metric','0',''),
(15,'告警级别','alarm_level','0',''),
(16,'告警状态','alarm_status','0',''),
(17,'工单状态','workorder_status','0',''),
(18,'工单来源','workorder_source','0',''),
(19,'故障等级','fault_level','0',''),
(20,'服务类型','service_type','0',''),
(21,'巡检状态','inspection_status','0',''),
(22,'巡检类型','inspection_type','0',''),
(23,'保养级别','maintenance_level','0',''),
(24,'保养状态','maintenance_status','0',''),
(25,'证书类型','cert_type','0',''),
(26,'证书状态','cert_status','0',''),
(27,'备件类别','part_category','0',''),
(28,'文档分类','doc_category','0',''),
(29,'知识分类','knowledge_category','0',''),
(30,'消息类型','message_type','0',''),
(31,'预算科目','budget_subject','0',''),
(32,'预算状态','budget_status','0',''),
(33,'告警规则比较符','alarm_operator','0',''),
(34,'工程师职称','engineer_title','0','');

-- 字典数据
INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`list_class`) VALUES
(1,'男','0','sys_user_sex',''),(2,'女','1','sys_user_sex',''),(3,'未知','2','sys_user_sex',''),
(1,'正常','0','sys_normal_disable','success'),(2,'停用','1','sys_normal_disable','danger'),

(1,'综合医院','general','hospital_type','primary'),(2,'专科医院','specialty','hospital_type','success'),
(3,'中医医院','tcm','hospital_type','warning'),(4,'妇幼保健院','maternal','hospital_type','info'),
(1,'三级甲等','三甲','hospital_grade','danger'),(2,'三级乙等','三乙','hospital_grade','warning'),
(3,'二级甲等','二甲','hospital_grade','primary'),(4,'二级乙等','二乙','hospital_grade','info'),
(1,'公立','public','hospital_nature','primary'),(2,'民营','private','hospital_nature','warning'),

(1,'手术室','operating','dept_type','danger'),(2,'重症监护(ICU)','icu','dept_type','warning'),
(3,'消毒供应中心','cssd','dept_type','primary'),(4,'静配中心','pharmacy','dept_type','success'),
(5,'检验实验室','lab','dept_type','info'),(6,'病房','ward','dept_type',''),

(1,'百级(I级)','100','clean_level','danger'),(2,'千级(II级)','1000','clean_level','warning'),
(3,'万级(III级)','10000','clean_level','primary'),(4,'十万级(IV级)','100000','clean_level','info'),
(5,'三十万级','300000','clean_level',''),

(1,'手术室','operating','clean_area_type',''),(2,'重症监护','icu','clean_area_type',''),
(3,'消毒供应','cssd','clean_area_type',''),(4,'静配中心','pharmacy','clean_area_type',''),
(5,'检验实验室','lab','clean_area_type',''),(6,'负压隔离病房','isolation','clean_area_type',''),

(1,'院方设备科','equipment_dept','contact_type',''),(2,'院方院感科','infection_dept','contact_type',''),
(3,'科室负责人','dept_leader','contact_type',''),(4,'施工方','contractor','contact_type',''),
(5,'监理','supervisor','contact_type',''),(6,'其他','other','contact_type',''),

(1,'全包','full','contract_type','success'),(2,'半包','half','contract_type','warning'),(3,'单次维修','repair','contract_type','info'),

(1,'层流机组','laminar','device_category','primary'),(2,'净化空调','purifier','device_category','success'),
(3,'新风机组','fresh_air','device_category','info'),(4,'除湿机组','dehumidifier','device_category','warning'),
(5,'供氧汇流排','oxygen','device_category','danger'),(6,'负压真空机组','vacuum','device_category',''),

(1,'运行','0','run_status','success'),(2,'停机','1','run_status','info'),(3,'故障','2','run_status','danger'),(4,'离线','3','run_status','warning'),
(1,'未接入','0','monitor_status','info'),(2,'在线','1','monitor_status','success'),(3,'离线','2','monitor_status','warning'),(4,'异常','3','monitor_status','danger'),

(1,'温度','temperature','monitor_metric','℃'),(2,'湿度','humidity','monitor_metric','%RH'),
(3,'压差','pressure','monitor_metric','Pa'),(4,'风速','wind_speed','monitor_metric','m/s'),
(5,'颗粒物','particle','monitor_metric','粒/L'),(6,'氧气压力','oxygen_pressure','monitor_metric','MPa'),
(7,'真空度','vacuum','monitor_metric','kPa'),(8,'运行状态','run_state','monitor_metric',''),

(1,'紧急','1','alarm_level','danger'),(2,'重要','2','alarm_level','warning'),(3,'一般','3','alarm_level','primary'),(4,'提示','4','alarm_level','info'),
(1,'未确认','0','alarm_status','danger'),(2,'已确认','1','alarm_status','warning'),(3,'处理中','2','alarm_status','primary'),(4,'已消缺','3','alarm_status','success'),(5,'已忽略','4','alarm_status','info'),

(1,'待受理','0','workorder_status','danger'),(2,'待派工','1','workorder_status','warning'),(3,'处理中','2','workorder_status','primary'),
(4,'待验收','3','workorder_status','warning'),(5,'待回访','4','workorder_status','info'),(6,'已完成','5','workorder_status','success'),(7,'已取消','6','workorder_status','info'),
(1,'院方报修','hospital','workorder_source',''),(2,'告警联动','alarm','workorder_source',''),
(3,'巡检异常','inspection','workorder_source',''),(4,'保养异常','maintenance','workorder_source',''),(5,'内部创建','internal','workorder_source',''),
(1,'P0 紧急','0','fault_level','danger'),(2,'P1 高','1','fault_level','warning'),(3,'P2 普通','2','fault_level','primary'),
(1,'保内','0','service_type','success'),(2,'保外','1','service_type','warning'),(3,'合同续约','2','service_type','info'),

(1,'待执行','0','inspection_status','warning'),(2,'执行中','1','inspection_status','primary'),(3,'已完成','2','inspection_status','success'),(4,'已逾期','3','inspection_status','danger'),(5,'已取消','4','inspection_status','info'),
(1,'日常巡检','routine','inspection_type',''),(2,'周检','weekly','inspection_type',''),(3,'月检','monthly','inspection_type',''),(4,'季检','quarterly','inspection_type',''),(5,'专项巡检','special','inspection_type',''),
(1,'日常保养','routine','maintenance_level',''),(2,'一级保养','level1','maintenance_level',''),(3,'二级保养','level2','maintenance_level',''),(4,'大修','overhaul','maintenance_level',''),
(1,'待执行','0','maintenance_status','warning'),(2,'执行中','1','maintenance_status','primary'),(3,'已完成','2','maintenance_status','success'),(4,'已逾期','3','maintenance_status','danger'),(5,'已取消','4','maintenance_status','info'),

(1,'技能证书','skill_certificate','cert_type',''),(2,'特种设备作业证','special_equipment','cert_type',''),
(1,'有效','0','cert_status','success'),(2,'即将到期','1','cert_status','warning'),(3,'已过期','2','cert_status','danger'),

(1,'过滤器','filter','part_category',''),(2,'风机','fan','part_category',''),(3,'电机','motor','part_category',''),
(4,'传感器','sensor','part_category',''),(5,'冷媒/耗材','consumable','part_category',''),(6,'控制器件','control','part_category',''),(7,'其他','other','part_category',''),
(1,'图纸','drawing','doc_category',''),(2,'设备说明书','manual','doc_category',''),(3,'验收报告','acceptance','doc_category',''),
(4,'检测报告','test_report','doc_category',''),(5,'合同附件','contract','doc_category',''),(6,'其他','other','doc_category',''),
(1,'故障案例','fault_case','knowledge_category',''),(2,'操作规程','sop','knowledge_category',''),
(3,'应急预案','emergency','knowledge_category',''),(4,'培训资料','training','knowledge_category',''),
(1,'告警','alarm','message_type','danger'),(2,'工单','workorder','message_type','primary'),(3,'巡检','inspection','message_type','warning'),
(4,'保养','maintenance','message_type','warning'),(5,'证书到期','cert','message_type','danger'),(6,'合同到期','contract','message_type','danger'),
(7,'预算','budget','message_type','info'),(8,'系统','system','message_type','info'),
(1,'人工成本','labor','budget_subject',''),(2,'备件耗材','parts','budget_subject',''),(3,'差旅交通','travel','budget_subject',''),
(4,'外包服务','outsource','budget_subject',''),(5,'设备采购','purchase','budget_subject',''),(6,'其他','other','budget_subject',''),
(1,'草稿','0','budget_status','info'),(2,'已生效','1','budget_status','success'),(3,'已关闭','2','budget_status','info'),
(1,'大于','gt','alarm_operator',''),(2,'大于等于','ge','alarm_operator',''),(3,'小于','lt','alarm_operator',''),
(4,'小于等于','le','alarm_operator',''),(5,'等于','eq','alarm_operator',''),(6,'区间','between','alarm_operator',''),(7,'离线','offline','alarm_operator',''),
(1,'助理工程师','assistant','engineer_title',''),(2,'中级工程师','intermediate','engineer_title',''),
(3,'高级工程师','senior','engineer_title',''),(4,'正高级工程师','professor','engineer_title',''),
(5,'技师','technician','engineer_title',''),(6,'高级技师','senior_technician','engineer_title',''),
(7,'区域主管','regional_manager','engineer_title',''),
(1,'设备','设备','skill_category',''),(2,'检测','检测','skill_category',''),
(3,'自控','自控','skill_category',''),(4,'运维','运维','skill_category',''),
(5,'安全','安全','skill_category',''),(6,'管理','管理','skill_category',''),
(7,'通用','通用','skill_category','');
