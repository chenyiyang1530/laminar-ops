-- 清理公司 1/4/6，保留公司 7（福建阿斯兰）及平台管理员
-- 执行前已做全库备份：backup_before_company_cleanup_20261112.sql
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE tmp_company (id BIGINT PRIMARY KEY);
INSERT INTO tmp_company VALUES (1),(4),(6);

CREATE TEMPORARY TABLE tmp_hospital (id BIGINT PRIMARY KEY);
INSERT INTO tmp_hospital SELECT hospital_id FROM biz_hospital WHERE company_id IS NULL OR company_id IN (1,4,6);
CREATE TEMPORARY TABLE tmp_engineer (id BIGINT PRIMARY KEY);
INSERT INTO tmp_engineer SELECT engineer_id FROM biz_engineer WHERE company_id IN (1,4,6);
CREATE TEMPORARY TABLE tmp_user (id BIGINT PRIMARY KEY);
INSERT INTO tmp_user SELECT user_id FROM sys_user WHERE company_id IN (1,4,6);
CREATE TEMPORARY TABLE tmp_role (id BIGINT PRIMARY KEY);
INSERT INTO tmp_role SELECT role_id FROM sys_role WHERE company_id IN (1,4,6);
CREATE TEMPORARY TABLE tmp_contract (id BIGINT PRIMARY KEY);
INSERT INTO tmp_contract SELECT contract_id FROM biz_contract WHERE company_id IN (1,4,6);
CREATE TEMPORARY TABLE tmp_workorder (id BIGINT PRIMARY KEY);
INSERT INTO tmp_workorder SELECT workorder_id FROM biz_workorder WHERE company_id IN (1,4,6);

-- 工单/备件子表
DELETE FROM biz_workorder_cost WHERE workorder_id IN (SELECT id FROM tmp_workorder);
DELETE FROM biz_workorder_log WHERE workorder_id IN (SELECT id FROM tmp_workorder);
DELETE FROM biz_part_record WHERE workorder_id IN (SELECT id FROM tmp_workorder);
DELETE FROM biz_leave_handover WHERE engineer_id IN (SELECT id FROM tmp_engineer) OR workorder_id IN (SELECT id FROM tmp_workorder);
DELETE FROM biz_workorder_archive WHERE company_id IN (1,4,6);
DELETE FROM biz_workorder WHERE workorder_id IN (SELECT id FROM tmp_workorder);

-- 告警/巡检/保养
DELETE FROM biz_alarm_archive WHERE company_id IN (1,4,6);
DELETE FROM biz_alarm WHERE company_id IN (1,4,6);
DELETE FROM biz_inspection_task WHERE company_id IN (1,4,6);
DELETE FROM biz_inspection_plan WHERE company_id IN (1,4,6);
DELETE FROM biz_maintenance_task WHERE company_id IN (1,4,6);
DELETE FROM biz_maintenance_plan WHERE company_id IN (1,4,6);

-- 设备/点位
DELETE FROM biz_monitor_point WHERE company_id IN (1,4,6);
DELETE FROM biz_equipment WHERE company_id IN (1,4,6);

-- 合同及范围
DELETE FROM biz_contract_scope WHERE contract_id IN (SELECT id FROM tmp_contract);
DELETE FROM biz_contract WHERE contract_id IN (SELECT id FROM tmp_contract);

-- 工程师及证书/技能/关系
DELETE FROM biz_engineer_skill WHERE engineer_id IN (SELECT id FROM tmp_engineer);
DELETE FROM biz_engineer_hospital WHERE engineer_id IN (SELECT id FROM tmp_engineer);
DELETE FROM biz_certificate WHERE company_id IN (1,4,6) OR engineer_id IN (SELECT id FROM tmp_engineer);
DELETE FROM biz_cert_type_request WHERE company_id IN (1,4,6);
DELETE FROM biz_engineer_leave WHERE company_id IN (1,4,6) OR engineer_id IN (SELECT id FROM tmp_engineer);
DELETE FROM biz_engineer WHERE engineer_id IN (SELECT id FROM tmp_engineer);

-- 客户档案（按医院）
DELETE FROM biz_hospital_user WHERE hospital_id IN (SELECT id FROM tmp_hospital) OR user_id IN (SELECT id FROM tmp_user);
DELETE FROM biz_contact WHERE hospital_id IN (SELECT id FROM tmp_hospital);
DELETE FROM biz_department WHERE hospital_id IN (SELECT id FROM tmp_hospital);
DELETE FROM biz_campus WHERE hospital_id IN (SELECT id FROM tmp_hospital);
DELETE FROM biz_clean_area WHERE hospital_id IN (SELECT id FROM tmp_hospital);
DELETE FROM biz_hospital WHERE hospital_id IN (SELECT id FROM tmp_hospital);

-- 其他公司级资料
DELETE FROM biz_company_code_alias WHERE company_id IN (1,4,6);
DELETE FROM biz_company_qualification WHERE company_id IN (1,4,6);
DELETE FROM biz_document WHERE company_id IN (1,4,6);
DELETE FROM biz_knowledge WHERE company_id IN (1,4,6);
DELETE FROM biz_message WHERE company_id IN (1,4,6);
DELETE FROM biz_part_requisition WHERE company_id IN (1,4,6) OR hospital_id IN (SELECT id FROM tmp_hospital);
DELETE FROM biz_part WHERE company_id IN (1,4,6);
DELETE FROM biz_device_type WHERE company_id IN (1,4,6);
DELETE FROM biz_skill WHERE company_id IN (1,4,6);
DELETE FROM biz_region WHERE company_id IN (1,4,6);
DELETE FROM biz_approval_delegate WHERE company_id IN (1,4,6);
DELETE FROM biz_approval_rule WHERE company_id IN (1,4,6);
DELETE FROM biz_approval_rule_log WHERE company_id IN (1,4,6);
DELETE FROM biz_approval_rule_version WHERE company_id IN (1,4,6);
DELETE FROM sys_dept WHERE company_id IN (1,4,6);
DELETE FROM sys_post WHERE company_id IN (1,4,6);

-- 用户与角色
DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM tmp_user) OR role_id IN (SELECT id FROM tmp_role);
DELETE FROM sys_role_menu WHERE role_id IN (SELECT id FROM tmp_role);
DELETE FROM sys_user WHERE user_id IN (SELECT id FROM tmp_user);
DELETE FROM sys_role WHERE role_id IN (SELECT id FROM tmp_role);
DELETE FROM biz_company WHERE company_id IN (1,4,6);

DROP TEMPORARY TABLE IF EXISTS tmp_company;
DROP TEMPORARY TABLE IF EXISTS tmp_hospital;
DROP TEMPORARY TABLE IF EXISTS tmp_engineer;
DROP TEMPORARY TABLE IF EXISTS tmp_user;
DROP TEMPORARY TABLE IF EXISTS tmp_role;
DROP TEMPORARY TABLE IF EXISTS tmp_contract;
DROP TEMPORARY TABLE IF EXISTS tmp_workorder;
SET FOREIGN_KEY_CHECKS = 1;
