
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `biz_alarm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_alarm` (
  `alarm_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `alarm_no` varchar(64) NOT NULL,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint NOT NULL,
  `rule_id` bigint DEFAULT NULL,
  `point_id` bigint DEFAULT NULL,
  `metric` varchar(30) DEFAULT NULL,
  `level` char(1) DEFAULT '3',
  `alarm_value` decimal(12,3) DEFAULT NULL,
  `threshold` decimal(12,3) DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0未确认 1已确认 2处理中 3已消缺 4已忽略',
  `first_time` datetime NOT NULL,
  `last_time` datetime DEFAULT NULL,
  `duration` int DEFAULT '0',
  `confirm_by` varchar(64) DEFAULT NULL,
  `confirm_time` datetime DEFAULT NULL,
  `handle_by` varchar(64) DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `close_by` varchar(64) DEFAULT NULL,
  `close_time` datetime DEFAULT NULL,
  `workorder_id` bigint DEFAULT NULL,
  `root_cause` varchar(500) DEFAULT NULL,
  `solution` varchar(1000) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `active_point` bigint GENERATED ALWAYS AS ((case when ((`status` in (_utf8mb4'0',_utf8mb4'1',_utf8mb4'2')) and (`point_id` is not null)) then `point_id` else NULL end)) STORED,
  PRIMARY KEY (`alarm_id`),
  UNIQUE KEY `uk_alarm_no` (`alarm_no`),
  UNIQUE KEY `uk_alarm_active_point` (`active_point`),
  KEY `idx_alarm_eq` (`equipment_id`,`status`),
  KEY `idx_alarm_time` (`first_time`),
  KEY `idx_alarm_hospital` (`hospital_id`),
  KEY `idx_alarm_company` (`company_id`),
  KEY `idx_alarm_company_status_time` (`company_id`,`status`,`first_time`),
  KEY `idx_alarm_company_eq_status` (`company_id`,`equipment_id`,`status`),
  KEY `idx_alarm_company_time` (`company_id`,`first_time`)
) ENGINE=InnoDB AUTO_INCREMENT=10183 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_alarm_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_alarm_archive` (
  `alarm_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `alarm_no` varchar(64) NOT NULL,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint NOT NULL,
  `point_id` bigint DEFAULT NULL,
  `metric` varchar(30) DEFAULT NULL,
  `level` char(1) DEFAULT '3',
  `alarm_value` decimal(12,3) DEFAULT NULL,
  `threshold` decimal(12,3) DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0未确认 1已确认 2处理中 3已消缺 4已忽略',
  `first_time` datetime NOT NULL,
  `last_time` datetime DEFAULT NULL,
  `duration` int DEFAULT '0',
  `confirm_by` varchar(64) DEFAULT NULL,
  `confirm_time` datetime DEFAULT NULL,
  `handle_by` varchar(64) DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `close_by` varchar(64) DEFAULT NULL,
  `close_time` datetime DEFAULT NULL,
  `workorder_id` bigint DEFAULT NULL,
  `root_cause` varchar(500) DEFAULT NULL,
  `solution` varchar(1000) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `active_point` bigint GENERATED ALWAYS AS ((case when ((`status` in (_utf8mb4'0',_utf8mb4'1',_utf8mb4'2')) and (`point_id` is not null)) then `point_id` else NULL end)) STORED,
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `rule_id` bigint DEFAULT NULL,
  PRIMARY KEY (`alarm_id`),
  UNIQUE KEY `uk_alarm_no` (`alarm_no`),
  UNIQUE KEY `uk_alarm_active_point` (`active_point`),
  KEY `idx_alarm_eq` (`equipment_id`,`status`),
  KEY `idx_alarm_time` (`first_time`),
  KEY `idx_alarm_hospital` (`hospital_id`),
  KEY `idx_alarm_archive_time` (`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=10183 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_alarm_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_alarm_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `alarm_id` bigint NOT NULL,
  `action` varchar(30) NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `from_status` char(1) DEFAULT NULL,
  `to_status` char(1) DEFAULT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_al_log` (`alarm_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20337 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警追溯日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_alarm_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_alarm_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `rule_name` varchar(100) NOT NULL,
  `required_skill_ids` varchar(500) DEFAULT NULL,
  `device_type_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `metric` varchar(30) NOT NULL,
  `level` char(1) DEFAULT '3',
  `operator` varchar(20) DEFAULT 'gt',
  `threshold` decimal(12,3) DEFAULT NULL,
  `threshold2` decimal(12,3) DEFAULT NULL,
  `duration` int DEFAULT '0',
  `auto_workorder` char(1) DEFAULT '0',
  `notify_roles` varchar(200) DEFAULT NULL,
  `enabled` char(1) DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警规则';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_approval_action_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_approval_action_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `biz_type` varchar(30) NOT NULL,
  `biz_id` bigint NOT NULL,
  `action` varchar(20) NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `target_user_id` bigint DEFAULT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_action_biz` (`biz_type`,`biz_id`),
  KEY `idx_action_task` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批操作轨迹';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_approval_delegate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_approval_delegate` (
  `delegate_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `target_user_id` bigint NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `status` char(1) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`delegate_id`),
  KEY `idx_delegate_user` (`user_id`,`start_date`,`end_date`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_approval_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_approval_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL COMMENT '适用区域，空为全公司',
  `leave_type` varchar(30) NOT NULL,
  `threshold_days` int DEFAULT '3',
  `second_level` char(1) DEFAULT '1',
  `status` char(1) DEFAULT '0',
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_company` (`company_id`,`leave_type`),
  KEY `idx_rule_region` (`company_id`,`region_id`,`leave_type`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_approval_rule_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_approval_rule_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `leave_type` varchar(30) NOT NULL,
  `old_threshold` int DEFAULT NULL,
  `old_second` char(1) DEFAULT NULL,
  `new_threshold` int DEFAULT NULL,
  `new_second` char(1) DEFAULT NULL,
  `changed_by` varchar(64) DEFAULT NULL,
  `change_time` datetime DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_rule_log_company` (`company_id`,`change_time`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_approval_rule_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_approval_rule_version` (
  `version_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `version_no` int NOT NULL,
  `status` varchar(20) DEFAULT 'PUBLISHED',
  `snapshot_json` longtext,
  `created_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `published_by` varchar(64) DEFAULT NULL,
  `published_time` datetime DEFAULT NULL,
  PRIMARY KEY (`version_id`),
  KEY `idx_version_scope` (`company_id`,`region_id`,`version_no`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_approval_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_approval_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(30) NOT NULL,
  `biz_id` bigint NOT NULL,
  `step_no` int DEFAULT '1',
  `approver_id` bigint NOT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `action` varchar(20) DEFAULT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `acted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  KEY `idx_task_approver` (`approver_id`,`status`),
  KEY `idx_task_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_campus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_campus` (
  `campus_id` bigint NOT NULL AUTO_INCREMENT,
  `hospital_id` bigint NOT NULL,
  `campus_code` varchar(50) DEFAULT NULL,
  `campus_name` varchar(100) NOT NULL,
  `address` varchar(500) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `latitude` decimal(10,6) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `bed_count` int DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`campus_id`),
  UNIQUE KEY `uk_campus_code` (`campus_code`),
  KEY `idx_campus_hospital` (`hospital_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医院院区';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_cert_type_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_cert_type_request` (
  `request_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `cert_type` varchar(100) NOT NULL,
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `requested_by` bigint DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `review_comment` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `review_time` datetime DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `idx_cert_type_status` (`status`,`company_id`),
  KEY `idx_cert_type_name` (`cert_type`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书类型字典申请';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_certificate` (
  `cert_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `engineer_id` bigint NOT NULL,
  `cert_type` varchar(50) DEFAULT NULL,
  `cert_name` varchar(100) NOT NULL,
  `cert_no` varchar(100) DEFAULT NULL,
  `issuing_authority` varchar(200) DEFAULT NULL,
  `issue_date` date DEFAULT NULL,
  `expire_date` date DEFAULT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `remind_days` int DEFAULT '60',
  `status` char(1) DEFAULT '0' COMMENT '0有效 1即将到期 2已过期',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`cert_id`),
  KEY `idx_cert_eng` (`engineer_id`),
  KEY `idx_cert_expire` (`expire_date`),
  KEY `idx_cert_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资质证书';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_clean_area_bak_20261112`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_clean_area_bak_20261112` (
  `area_id` bigint NOT NULL DEFAULT '0',
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint NOT NULL,
  `department_id` bigint DEFAULT NULL,
  `area_code` varchar(50) DEFAULT NULL,
  `area_name` varchar(100) NOT NULL,
  `area_type` varchar(30) DEFAULT NULL,
  `clean_level` varchar(20) DEFAULT NULL,
  `area_size` decimal(10,2) DEFAULT NULL,
  `design_temp_min` decimal(5,1) DEFAULT NULL,
  `design_temp_max` decimal(5,1) DEFAULT NULL,
  `design_humidity_min` decimal(5,1) DEFAULT NULL,
  `design_humidity_max` decimal(5,1) DEFAULT NULL,
  `design_pressure` decimal(6,1) DEFAULT NULL,
  `clean_require` varchar(500) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_company` (
  `company_id` bigint NOT NULL AUTO_INCREMENT,
  `company_name` varchar(200) NOT NULL,
  `company_code` varchar(50) DEFAULT NULL,
  `total_manager_id` bigint DEFAULT NULL COMMENT '总负责人用户ID',
  `address` varchar(500) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `contact` varchar(50) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`company_id`),
  UNIQUE KEY `uk_company_code` (`company_code`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保公司';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_company_campus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_company_campus` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `campus_id` bigint NOT NULL,
  `hospital_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `service_status` char(1) DEFAULT '0',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_campus` (`company_id`,`campus_id`),
  KEY `idx_company_campus_campus` (`campus_id`),
  KEY `idx_company_campus_hospital` (`hospital_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司院区服务范围';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_company_code_alias`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_company_code_alias` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `alias_code` varchar(64) NOT NULL,
  `company_id` bigint NOT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_alias_code` (`alias_code`),
  KEY `idx_company_alias` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司编码历史别名';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_company_hospital`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_company_hospital` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `hospital_id` bigint NOT NULL,
  `contract_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `service_status` char(1) DEFAULT '0',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_hospital` (`company_id`,`hospital_id`),
  KEY `idx_company_hospital_hospital` (`hospital_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司医院服务关系';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_company_qualification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_company_qualification` (
  `qualification_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL COMMENT '所属公司',
  `qualification_name` varchar(200) NOT NULL COMMENT '资质名称',
  `qualification_type` varchar(50) DEFAULT NULL COMMENT '资质类型',
  `qualification_no` varchar(100) DEFAULT NULL COMMENT '资质编号',
  `level` varchar(30) DEFAULT NULL COMMENT '资质等级',
  `issuing_authority` varchar(200) DEFAULT NULL COMMENT '发证机关',
  `scope` varchar(1000) DEFAULT NULL COMMENT '许可范围',
  `issue_date` date DEFAULT NULL COMMENT '发证日期',
  `expire_date` date DEFAULT NULL COMMENT '到期日期',
  `file_url` varchar(500) DEFAULT NULL COMMENT '扫描件',
  `status` char(1) DEFAULT '0' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`qualification_id`),
  KEY `idx_company_qual_company` (`company_id`),
  KEY `idx_company_qual_expire` (`expire_date`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司资质';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_contact` (
  `contact_id` bigint NOT NULL AUTO_INCREMENT,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `contact_name` varchar(50) NOT NULL,
  `position` varchar(50) DEFAULT NULL,
  `contact_type` varchar(30) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `tel` varchar(30) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `duty` varchar(200) DEFAULT NULL,
  `is_primary` char(1) DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`contact_id`),
  KEY `idx_contact_hospital` (`hospital_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户对接人';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_contract` (
  `contract_id` bigint NOT NULL AUTO_INCREMENT,
  `contract_no` varchar(64) NOT NULL,
  `contract_name` varchar(200) NOT NULL,
  `hospital_id` bigint NOT NULL,
  `company_id` bigint DEFAULT NULL,
  `contract_type` varchar(30) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `amount` decimal(14,2) DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `warn_days` int DEFAULT '90',
  `status` char(1) DEFAULT '0' COMMENT '0执行中 1已终止 2已过期 3未开始',
  `attachment` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`contract_id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_contract_hospital` (`hospital_id`),
  KEY `idx_contract_end` (`end_date`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保合同';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_contract_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_contract_scope` (
  `scope_id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint NOT NULL,
  `scope_type` varchar(20) NOT NULL,
  `scope_value` bigint DEFAULT NULL,
  PRIMARY KEY (`scope_id`),
  UNIQUE KEY `uk_contract_scope` (`contract_id`,`scope_type`,`scope_value`),
  KEY `idx_scope_target` (`scope_type`,`scope_value`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合同服务范围';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_department` (
  `department_id` bigint NOT NULL AUTO_INCREMENT,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `dept_code` varchar(50) DEFAULT NULL,
  `department_name` varchar(100) NOT NULL,
  `dept_type` varchar(30) DEFAULT NULL,
  `parent_id` bigint DEFAULT '0',
  `director` varchar(50) DEFAULT NULL,
  `contact_id` bigint DEFAULT NULL COMMENT '对接人ID',
  `phone` varchar(30) DEFAULT NULL,
  `location` varchar(200) DEFAULT NULL,
  `sort` int DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`department_id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_dept_hospital` (`hospital_id`),
  KEY `idx_dept_campus` (`campus_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医院科室';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_device_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_device_type` (
  `type_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `parent_id` bigint DEFAULT NULL,
  `type_code` varchar(50) NOT NULL,
  `type_name` varchar(100) NOT NULL,
  `icon` varchar(100) DEFAULT NULL,
  `monitor_enabled` char(1) DEFAULT '1',
  `param_template` text,
  `sort` int DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`type_id`),
  UNIQUE KEY `uk_company_type_code` (`company_id`,`type_code`),
  KEY `idx_device_type_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备类型';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_document` (
  `doc_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `doc_name` varchar(200) NOT NULL,
  `doc_category` varchar(30) DEFAULT NULL,
  `hospital_id` bigint DEFAULT NULL,
  `campus_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `file_name` varchar(200) DEFAULT NULL,
  `file_size` bigint DEFAULT '0',
  `file_type` varchar(30) DEFAULT NULL,
  `version` varchar(30) DEFAULT 'V1.0',
  `uploader_id` bigint DEFAULT NULL,
  `uploader_name` varchar(64) DEFAULT NULL,
  `upload_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`doc_id`),
  KEY `idx_doc_hospital` (`hospital_id`),
  KEY `idx_doc_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图纸文档';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_engineer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_engineer` (
  `engineer_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `company_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `engineer_name` varchar(50) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `gender` char(1) DEFAULT '0',
  `job_title` varchar(50) DEFAULT NULL,
  `team_title` varchar(30) DEFAULT NULL COMMENT '工程师团队职务标签',
  `specialty` varchar(200) DEFAULT NULL,
  `entry_date` date DEFAULT NULL,
  `on_duty` char(1) DEFAULT '1',
  `max_open_orders` int DEFAULT '5' COMMENT '最大在手工单数',
  `latitude` decimal(10,6) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`engineer_id`),
  UNIQUE KEY `uk_engineer_user` (`user_id`),
  KEY `idx_eng_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保工程师';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_engineer_hospital`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_engineer_hospital` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `engineer_id` bigint NOT NULL,
  `hospital_id` bigint NOT NULL,
  `is_manager` char(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eng_hosp` (`engineer_id`,`hospital_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程师服务医院';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_engineer_leave`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_engineer_leave` (
  `leave_id` bigint NOT NULL AUTO_INCREMENT,
  `engineer_id` bigint NOT NULL,
  `company_id` bigint DEFAULT NULL,
  `leave_type` varchar(30) DEFAULT 'personal',
  `start_date` date NOT NULL,
  `start_period` varchar(2) NOT NULL DEFAULT 'AM',
  `end_date` date NOT NULL,
  `end_period` varchar(2) NOT NULL DEFAULT 'PM',
  `days` decimal(5,1) NOT NULL DEFAULT '1.0',
  `need_second` tinyint DEFAULT '1' COMMENT '是否需要二级审批',
  `reason` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING_REGION',
  `current_step` int DEFAULT '1',
  `approver_chain` varchar(1000) DEFAULT NULL COMMENT '审批人链，逗号分隔用户ID',
  `handover_engineer_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`leave_id`),
  KEY `idx_leave_engineer` (`engineer_id`),
  KEY `idx_leave_company` (`company_id`),
  KEY `idx_leave_status` (`status`),
  KEY `idx_leave_dates` (`start_date`,`end_date`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_engineer_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_engineer_skill` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `engineer_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  `level` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eng_skill` (`engineer_id`,`skill_id`)
) ENGINE=InnoDB AUTO_INCREMENT=307 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程师技能';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_equipment` (
  `equipment_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '维保公司',
  `equipment_code` varchar(50) DEFAULT NULL,
  `equipment_name` varchar(200) NOT NULL,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `device_type_id` bigint NOT NULL,
  `category` varchar(30) DEFAULT NULL,
  `model` varchar(100) DEFAULT NULL,
  `brand` varchar(100) DEFAULT NULL,
  `serial_no` varchar(100) DEFAULT NULL,
  `supplier` varchar(200) DEFAULT NULL,
  `install_date` date DEFAULT NULL,
  `warranty_end` date DEFAULT NULL,
  `install_location` varchar(200) DEFAULT NULL,
  `usage_location` varchar(255) DEFAULT NULL,
  `contract_id` bigint DEFAULT NULL,
  `run_status` char(1) DEFAULT '0' COMMENT '0运行 1停机 2故障 3离线',
  `monitor_status` char(1) DEFAULT '0' COMMENT '0未接入 1在线 2离线 3异常',
  `protocol` varchar(30) DEFAULT NULL,
  `comm_address` varchar(100) DEFAULT NULL,
  `rated_power` decimal(10,2) DEFAULT NULL,
  `last_heartbeat` datetime DEFAULT NULL,
  `next_maintain_date` date DEFAULT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0在用 1停用 2报废',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `simulated` char(1) DEFAULT '0',
  PRIMARY KEY (`equipment_id`),
  UNIQUE KEY `uk_eq_code` (`equipment_code`),
  KEY `idx_eq_hospital` (`hospital_id`),
  KEY `idx_eq_type` (`device_type_id`),
  KEY `idx_equipment_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备台账';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_hospital`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_hospital` (
  `hospital_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `hospital_name` varchar(200) NOT NULL,
  `hospital_code` varchar(50) DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `hospital_type` varchar(30) DEFAULT NULL,
  `grade` varchar(20) DEFAULT NULL,
  `nature` varchar(20) DEFAULT NULL,
  `address` varchar(500) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `latitude` decimal(10,6) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `contact_person` varchar(50) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`hospital_id`),
  UNIQUE KEY `uk_hospital_code` (`hospital_code`),
  KEY `idx_hospital_region` (`region_id`),
  KEY `idx_hospital_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医院';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_hospital_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_hospital_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `hospital_id` bigint NOT NULL,
  `department_id` bigint DEFAULT NULL,
  `role_type` varchar(20) DEFAULT 'admin',
  `status` char(1) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hu_user` (`user_id`),
  KEY `idx_hu_hospital` (`hospital_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='院方用户绑定';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_inspection_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_inspection_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `item_name` varchar(200) NOT NULL,
  `standard` varchar(500) DEFAULT NULL,
  `result` char(1) DEFAULT NULL,
  `item_value` varchar(100) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `sort` int DEFAULT '0',
  PRIMARY KEY (`item_id`),
  KEY `idx_ii_task` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=128 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检检查项';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_inspection_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_inspection_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `plan_no` varchar(50) DEFAULT NULL,
  `plan_name` varchar(100) NOT NULL,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `plan_type` varchar(30) DEFAULT 'routine',
  `cycle_type` varchar(20) DEFAULT 'day',
  `cycle_value` int DEFAULT '1',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `next_run_date` date DEFAULT NULL,
  `item_template` text,
  `assignee_id` bigint DEFAULT NULL,
  `remind_days` int DEFAULT '1',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`plan_id`),
  KEY `idx_ip_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检计划';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_inspection_plan_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_inspection_plan_equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `equipment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspection_plan_equipment` (`plan_id`,`equipment_id`),
  KEY `idx_inspection_plan_equipment_plan` (`plan_id`),
  KEY `idx_inspection_plan_equipment_equipment` (`equipment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检计划适用设备';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_inspection_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_inspection_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `task_no` varchar(50) DEFAULT NULL,
  `plan_id` bigint DEFAULT NULL,
  `task_type` varchar(20) DEFAULT 'plan',
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `plan_time` date NOT NULL,
  `assignee_id` bigint DEFAULT NULL,
  `assignee_name` varchar(50) DEFAULT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0待执行 1执行中 2已完成 3已逾期 4已取消',
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `result` char(1) DEFAULT NULL,
  `problem_desc` varchar(1000) DEFAULT NULL,
  `workorder_id` bigint DEFAULT NULL,
  `score` int DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_it_no` (`task_no`),
  UNIQUE KEY `uk_it_task_no` (`task_no`),
  KEY `idx_it_status` (`status`),
  KEY `idx_it_date` (`plan_time`),
  KEY `idx_it_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检任务';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_knowledge` (
  `article_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `title` varchar(200) NOT NULL,
  `category` varchar(30) DEFAULT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `content` mediumtext,
  `tags` varchar(200) DEFAULT NULL,
  `author_id` bigint DEFAULT NULL,
  `author_name` varchar(64) DEFAULT NULL,
  `view_count` int DEFAULT '0',
  `status` char(1) DEFAULT '1',
  `publish_time` datetime DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`article_id`),
  KEY `idx_knowledge_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_leave_handover`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_leave_handover` (
  `handover_id` bigint NOT NULL AUTO_INCREMENT,
  `leave_id` bigint NOT NULL,
  `engineer_id` bigint NOT NULL,
  `workorder_id` bigint NOT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `create_time` datetime DEFAULT NULL,
  `done_time` datetime DEFAULT NULL,
  PRIMARY KEY (`handover_id`),
  UNIQUE KEY `uk_leave_handover` (`leave_id`,`workorder_id`),
  KEY `idx_handover_workorder` (`workorder_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_maintenance_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_maintenance_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `item_name` varchar(200) NOT NULL,
  `standard` varchar(500) DEFAULT NULL,
  `result` char(1) DEFAULT NULL,
  `item_value` varchar(100) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `sort` int DEFAULT '0',
  PRIMARY KEY (`item_id`),
  KEY `idx_mi_task` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养项目';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_maintenance_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_maintenance_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `plan_no` varchar(50) DEFAULT NULL,
  `plan_name` varchar(100) NOT NULL,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `device_type_id` bigint DEFAULT NULL,
  `maintenance_level` varchar(30) DEFAULT 'routine',
  `cycle_type` varchar(20) DEFAULT 'month',
  `cycle_value` int DEFAULT '1',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `next_run_date` date DEFAULT NULL,
  `content` text,
  `assignee_id` bigint DEFAULT NULL,
  `remind_days` int DEFAULT '7',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`plan_id`),
  KEY `idx_mp_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养计划';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_maintenance_plan_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_maintenance_plan_equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `equipment_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_equipment` (`plan_id`,`equipment_id`),
  KEY `idx_plan_equipment_plan` (`plan_id`),
  KEY `idx_plan_equipment_equipment` (`equipment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养计划适用设备';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_maintenance_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_maintenance_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `task_no` varchar(50) DEFAULT NULL,
  `plan_id` bigint DEFAULT NULL,
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `plan_date` date NOT NULL,
  `assignee_id` bigint DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `result` char(1) DEFAULT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `parts_used` varchar(500) DEFAULT NULL,
  `workorder_id` bigint DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_mt_no` (`task_no`),
  UNIQUE KEY `uk_mt_task_no` (`task_no`),
  KEY `idx_mt_status` (`status`),
  KEY `idx_mt_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养任务';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_message` (
  `msg_id` bigint NOT NULL AUTO_INCREMENT,
  `msg_type` varchar(30) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `level` char(1) DEFAULT '3',
  `biz_type` varchar(30) DEFAULT NULL,
  `biz_id` bigint DEFAULT NULL,
  `biz_key` varchar(128) DEFAULT NULL,
  `receiver_id` bigint NOT NULL DEFAULT '0',
  `receiver_role` varchar(200) DEFAULT NULL,
  `hospital_id` bigint DEFAULT NULL,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `is_read` char(1) DEFAULT '0',
  `read_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`msg_id`),
  UNIQUE KEY `uk_msg_key` (`biz_key`,`receiver_id`),
  KEY `idx_msg_receiver` (`receiver_id`,`is_read`),
  KEY `idx_msg_time` (`create_time`),
  KEY `idx_msg_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10515 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_monitor_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_monitor_data` (
  `data_id` bigint NOT NULL AUTO_INCREMENT,
  `equipment_id` bigint NOT NULL,
  `point_id` bigint NOT NULL,
  `metric` varchar(30) DEFAULT NULL,
  `value` decimal(12,3) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `collect_time` datetime NOT NULL,
  PRIMARY KEY (`data_id`),
  KEY `idx_md_point` (`point_id`,`collect_time`),
  KEY `idx_md_equip` (`equipment_id`,`collect_time`)
) ENGINE=InnoDB AUTO_INCREMENT=125504 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测历史数据';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_monitor_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_monitor_point` (
  `point_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `equipment_id` bigint NOT NULL,
  `point_code` varchar(50) NOT NULL,
  `point_name` varchar(100) NOT NULL,
  `metric` varchar(30) NOT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `warn_lower` decimal(12,3) DEFAULT NULL,
  `warn_upper` decimal(12,3) DEFAULT NULL,
  `alarm_lower` decimal(12,3) DEFAULT NULL,
  `alarm_upper` decimal(12,3) DEFAULT NULL,
  `precision` int DEFAULT '1',
  `collect_interval` int DEFAULT '30',
  `enabled` char(1) DEFAULT '1',
  `sort` int DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`point_id`),
  UNIQUE KEY `uk_point` (`equipment_id`,`point_code`),
  KEY `idx_point_metric` (`metric`),
  KEY `idx_point_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测点位';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_part` (
  `part_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `part_code` varchar(50) NOT NULL,
  `part_name` varchar(100) NOT NULL,
  `part_category` varchar(30) DEFAULT NULL,
  `spec` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT '个',
  `brand` varchar(100) DEFAULT NULL,
  `safety_stock` int DEFAULT '0',
  `current_stock` int DEFAULT '0',
  `price` decimal(12,2) DEFAULT '0.00',
  `supplier` varchar(200) DEFAULT NULL,
  `warehouse` varchar(100) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`part_id`),
  UNIQUE KEY `uk_company_part_code` (`company_id`,`part_code`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件台账';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_part_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_part_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `part_id` bigint NOT NULL,
  `record_type` varchar(20) NOT NULL,
  `quantity` int NOT NULL,
  `before_stock` int DEFAULT '0',
  `after_stock` int DEFAULT '0',
  `workorder_id` bigint DEFAULT NULL,
  `requisition_id` bigint DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `record_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`record_id`),
  KEY `idx_pr_part` (`part_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件出入库流水';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_part_requisition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_part_requisition` (
  `req_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `req_no` varchar(50) DEFAULT NULL,
  `workorder_id` bigint DEFAULT NULL,
  `hospital_id` bigint DEFAULT NULL,
  `applicant_id` bigint DEFAULT NULL,
  `applicant_name` varchar(64) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `apply_time` datetime DEFAULT NULL,
  `approve_by` varchar(64) DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `out_by` varchar(64) DEFAULT NULL,
  `out_time` datetime DEFAULT NULL,
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`req_id`),
  UNIQUE KEY `uk_req_no` (`req_no`),
  KEY `idx_req_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件领用单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_part_requisition_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_part_requisition_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `req_id` bigint NOT NULL,
  `part_id` bigint NOT NULL,
  `part_name` varchar(100) DEFAULT NULL,
  `spec` varchar(100) DEFAULT NULL,
  `quantity` int DEFAULT '1',
  `price` decimal(12,2) DEFAULT '0.00',
  `amount` decimal(12,2) DEFAULT '0.00',
  PRIMARY KEY (`item_id`),
  KEY `idx_pri_req` (`req_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件领用明细';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_region` (
  `region_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL COMMENT '上级区域',
  `region_name` varchar(100) NOT NULL,
  `region_code` varchar(50) DEFAULT NULL,
  `manager` varchar(50) DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`region_id`),
  UNIQUE KEY `uk_region_manager` (`manager_id`),
  UNIQUE KEY `uk_region_name` (`company_id`,`region_name`),
  UNIQUE KEY `uk_region_code` (`company_id`,`region_code`),
  KEY `idx_region_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务区域';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_skill` (
  `skill_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `skill_name` varchar(100) NOT NULL,
  `skill_code` varchar(50) DEFAULT NULL,
  `category` varchar(30) DEFAULT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`skill_id`),
  UNIQUE KEY `uk_company_skill_code` (`company_id`,`skill_code`),
  KEY `idx_skill_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能标签';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_workorder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_workorder` (
  `workorder_id` bigint NOT NULL AUTO_INCREMENT,
  `workorder_no` varchar(64) NOT NULL,
  `title` varchar(200) NOT NULL,
  `fault_desc` text,
  `fault_level` char(1) DEFAULT '2',
  `order_type` varchar(30) DEFAULT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0待受理 1待派工 2处理中 3待验收 4待回访 5已完成 6已取消',
  `source` varchar(30) DEFAULT 'hospital' COMMENT 'hospital/alarm/inspection/maintenance/internal',
  `service_type` char(1) DEFAULT '0',
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `required_skill_ids` varchar(500) DEFAULT NULL,
  `company_id` bigint DEFAULT NULL COMMENT '承接维保公司',
  `engineer_id` bigint DEFAULT NULL,
  `report_contact` varchar(50) DEFAULT NULL,
  `report_phone` varchar(30) DEFAULT NULL,
  `sla_deadline` datetime DEFAULT NULL,
  `receive_time` datetime DEFAULT NULL,
  `receive_by` varchar(64) DEFAULT NULL,
  `dispatch_time` datetime DEFAULT NULL,
  `dispatch_by` varchar(64) DEFAULT NULL,
  `dispatch_score` int DEFAULT NULL,
  `score_detail` text,
  `score_version` varchar(20) DEFAULT NULL,
  `arrive_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `accept_time` datetime DEFAULT NULL,
  `accept_by` varchar(64) DEFAULT NULL,
  `accept_result` char(1) DEFAULT NULL,
  `accept_remark` varchar(500) DEFAULT NULL,
  `quote_amount` decimal(12,2) DEFAULT NULL,
  `quote_status` char(1) DEFAULT '0',
  `quote_remark` varchar(500) DEFAULT NULL,
  `callback_time` datetime DEFAULT NULL,
  `callback_by` varchar(64) DEFAULT NULL,
  `callback_score` int DEFAULT NULL,
  `callback_content` varchar(500) DEFAULT NULL,
  `close_time` datetime DEFAULT NULL,
  `close_by` varchar(64) DEFAULT NULL,
  `alarm_id` bigint DEFAULT NULL,
  `inspection_task_id` bigint DEFAULT NULL,
  `maintenance_task_id` bigint DEFAULT NULL,
  `cancel_reason` varchar(500) DEFAULT NULL,
  `downtime_minutes` int DEFAULT '0',
  `response_minutes` int DEFAULT NULL,
  `travel_fee` decimal(10,2) DEFAULT '0.00',
  `total_cost` decimal(12,2) DEFAULT '0.00',
  `repair_report` text,
  `finish_root_cause` varchar(500) DEFAULT NULL,
  `finish_solution` varchar(1000) DEFAULT NULL,
  `finish_parts` varchar(500) DEFAULT NULL,
  `finish_image` varchar(500) DEFAULT NULL,
  `acceptance_image` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`workorder_id`),
  UNIQUE KEY `uk_wo_no` (`workorder_no`),
  KEY `idx_wo_hospital` (`hospital_id`),
  KEY `idx_wo_status` (`status`),
  KEY `idx_wo_engineer` (`engineer_id`),
  KEY `idx_wo_create` (`create_time`),
  KEY `idx_wo_company` (`company_id`),
  KEY `idx_wo_company_status_create` (`company_id`,`status`,`create_time`),
  KEY `idx_wo_company_create` (`company_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保工单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_workorder_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_workorder_archive` (
  `workorder_id` bigint NOT NULL AUTO_INCREMENT,
  `workorder_no` varchar(64) NOT NULL,
  `title` varchar(200) NOT NULL,
  `fault_desc` text,
  `fault_level` char(1) DEFAULT '2',
  `status` char(1) DEFAULT '0' COMMENT '0待受理 1待派工 2处理中 3待验收 4待回访 5已完成 6已取消',
  `source` varchar(30) DEFAULT 'hospital' COMMENT 'hospital/alarm/inspection/maintenance/internal',
  `service_type` char(1) DEFAULT '0',
  `hospital_id` bigint NOT NULL,
  `campus_id` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `equipment_id` bigint DEFAULT NULL,
  `company_id` bigint DEFAULT NULL COMMENT '承接维保公司',
  `engineer_id` bigint DEFAULT NULL,
  `report_contact` varchar(50) DEFAULT NULL,
  `report_phone` varchar(30) DEFAULT NULL,
  `sla_deadline` datetime DEFAULT NULL,
  `receive_time` datetime DEFAULT NULL,
  `receive_by` varchar(64) DEFAULT NULL,
  `dispatch_time` datetime DEFAULT NULL,
  `dispatch_by` varchar(64) DEFAULT NULL,
  `arrive_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `accept_time` datetime DEFAULT NULL,
  `accept_by` varchar(64) DEFAULT NULL,
  `accept_result` char(1) DEFAULT NULL,
  `accept_remark` varchar(500) DEFAULT NULL,
  `quote_amount` decimal(12,2) DEFAULT NULL,
  `quote_status` char(1) DEFAULT '0',
  `quote_remark` varchar(500) DEFAULT NULL,
  `callback_time` datetime DEFAULT NULL,
  `callback_by` varchar(64) DEFAULT NULL,
  `callback_score` int DEFAULT NULL,
  `callback_content` varchar(500) DEFAULT NULL,
  `close_time` datetime DEFAULT NULL,
  `close_by` varchar(64) DEFAULT NULL,
  `alarm_id` bigint DEFAULT NULL,
  `inspection_task_id` bigint DEFAULT NULL,
  `maintenance_task_id` bigint DEFAULT NULL,
  `cancel_reason` varchar(500) DEFAULT NULL,
  `downtime_minutes` int DEFAULT '0',
  `travel_fee` decimal(10,2) DEFAULT '0.00',
  `total_cost` decimal(12,2) DEFAULT '0.00',
  `repair_report` text,
  `acceptance_image` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `order_type` varchar(30) DEFAULT NULL,
  `required_skill_ids` varchar(500) DEFAULT NULL,
  `dispatch_score` int DEFAULT NULL,
  `score_detail` text,
  `score_version` varchar(20) DEFAULT NULL,
  `response_minutes` int DEFAULT NULL,
  `finish_root_cause` varchar(500) DEFAULT NULL,
  `finish_solution` varchar(1000) DEFAULT NULL,
  `finish_parts` varchar(500) DEFAULT NULL,
  `finish_image` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`workorder_id`),
  UNIQUE KEY `uk_wo_no` (`workorder_no`),
  KEY `idx_wo_hospital` (`hospital_id`),
  KEY `idx_wo_status` (`status`),
  KEY `idx_wo_engineer` (`engineer_id`),
  KEY `idx_wo_create` (`create_time`),
  KEY `idx_wo_company` (`company_id`),
  KEY `idx_workorder_archive_time` (`archived_at`)
) ENGINE=InnoDB AUTO_INCREMENT=3011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保工单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_workorder_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_workorder_cost` (
  `cost_id` bigint NOT NULL AUTO_INCREMENT,
  `workorder_id` bigint NOT NULL,
  `cost_type` varchar(30) DEFAULT NULL COMMENT 'part备件/labor人工/travel差旅/outsource外包/other',
  `cost_name` varchar(100) DEFAULT NULL,
  `quantity` decimal(10,2) DEFAULT '1.00',
  `price` decimal(12,2) DEFAULT '0.00',
  `amount` decimal(12,2) DEFAULT '0.00',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`cost_id`),
  KEY `idx_cost_wo` (`workorder_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单费用明细';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `biz_workorder_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_workorder_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `workorder_id` bigint NOT NULL,
  `action` varchar(30) NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `from_status` char(1) DEFAULT NULL,
  `to_status` char(1) DEFAULT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_wo_log` (`workorder_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3074 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单流转日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT,
  `config_name` varchar(64) NOT NULL,
  `config_key` varchar(100) NOT NULL,
  `group_code` varchar(32) NOT NULL DEFAULT 'platform',
  `config_value` varchar(500) DEFAULT NULL,
  `value_type` varchar(20) NOT NULL DEFAULT 'STRING',
  `default_value` varchar(500) DEFAULT NULL,
  `options_json` varchar(500) DEFAULT NULL,
  `is_sensitive` tinyint NOT NULL DEFAULT '0',
  `sort` int NOT NULL DEFAULT '0',
  `status` char(1) NOT NULL DEFAULT '0',
  `config_type` char(1) NOT NULL DEFAULT 'N',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_sys_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统参数配置';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_config_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `config_id` bigint DEFAULT NULL,
  `config_key` varchar(100) NOT NULL,
  `old_value` varchar(500) DEFAULT NULL,
  `new_value` varchar(500) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数修改日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `parent_id` bigint DEFAULT '0',
  `region_id` bigint DEFAULT NULL,
  `dept_name` varchar(100) NOT NULL,
  `leader` varchar(50) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `sort` int DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`dept_id`),
  UNIQUE KEY `uk_dept_company_parent_name` (`company_id`,`parent_id`,`dept_name`),
  UNIQUE KEY `uk_dept_region` (`region_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司组织部门';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT,
  `dict_sort` int DEFAULT '0',
  `dict_label` varchar(100) NOT NULL,
  `dict_value` varchar(100) NOT NULL,
  `dict_type` varchar(100) NOT NULL,
  `css_class` varchar(100) DEFAULT NULL,
  `list_class` varchar(100) DEFAULT NULL,
  `is_default` char(1) DEFAULT 'N',
  `status` char(1) DEFAULT '0',
  PRIMARY KEY (`dict_code`),
  KEY `idx_dd_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=207 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT,
  `dict_name` varchar(100) NOT NULL,
  `dict_type` varchar(100) NOT NULL,
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT,
  `job_name` varchar(100) NOT NULL,
  `bean_name` varchar(100) NOT NULL,
  `method_name` varchar(100) NOT NULL,
  `cron` varchar(100) NOT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0启用 1停用',
  `last_run_time` datetime DEFAULT NULL,
  `last_result` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `browser` varchar(100) DEFAULT NULL,
  `os` varchar(100) DEFAULT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0成功 1失败',
  `msg` varchar(500) DEFAULT NULL,
  `login_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ll_time` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=994 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT,
  `menu_name` varchar(64) NOT NULL,
  `parent_id` bigint DEFAULT '0',
  `order_num` int DEFAULT '0',
  `path` varchar(200) DEFAULT NULL,
  `component` varchar(200) DEFAULT NULL,
  `perms` varchar(200) DEFAULT NULL,
  `menu_type` char(1) DEFAULT 'C' COMMENT 'M目录 C菜单 F按钮',
  `icon` varchar(100) DEFAULT NULL,
  `visible` char(1) DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`menu_id`),
  KEY `idx_menu_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=926011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) DEFAULT NULL,
  `business_type` varchar(20) DEFAULT NULL,
  `method` varchar(200) DEFAULT NULL,
  `request_method` varchar(10) DEFAULT NULL,
  `oper_name` varchar(64) DEFAULT NULL,
  `dept_name` varchar(100) DEFAULT NULL,
  `oper_url` varchar(255) DEFAULT NULL,
  `oper_ip` varchar(64) DEFAULT NULL,
  `oper_param` varchar(2000) DEFAULT NULL,
  `json_result` varchar(2000) DEFAULT NULL,
  `status` char(1) DEFAULT '0' COMMENT '0成功 1失败',
  `error_msg` varchar(2000) DEFAULT NULL,
  `cost_time` bigint DEFAULT '0',
  `oper_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ol_time` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=906 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_optimization_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_optimization_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `module` varchar(100) DEFAULT NULL,
  `priority` varchar(20) DEFAULT 'medium' COMMENT 'urgent/high/medium/low',
  `status` varchar(20) DEFAULT 'todo' COMMENT 'todo/doing/done/hold',
  `owner` varchar(100) DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `sort` int DEFAULT '0',
  `description` text,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台优化事项';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `post_code` varchar(50) NOT NULL,
  `post_name` varchar(64) NOT NULL,
  `sort` int DEFAULT '0',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `uk_post_company_code` (`company_id`,`post_code`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_release_note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_release_note` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` varchar(30) NOT NULL,
  `title` varchar(200) NOT NULL,
  `release_date` date NOT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `features` text,
  `improvements` text,
  `fixes` text,
  `status` char(1) DEFAULT '0' COMMENT '0发布 1草稿',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_release_version` (`version`),
  KEY `idx_release_date` (`release_date`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本更新记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `role_type` varchar(20) DEFAULT 'custom' COMMENT 'standard/custom',
  `role_name` varchar(64) NOT NULL,
  `role_key` varchar(64) NOT NULL,
  `sort` int DEFAULT '0',
  `data_scope` varchar(20) DEFAULT 'self' COMMENT 'all/region/hospital/self',
  `status` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_company_role_key` (`company_id`,`role_key`),
  KEY `idx_role_company` (`company_id`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_schema_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_schema_version` (
  `version` varchar(120) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `applied_at` datetime DEFAULT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库迁移版本';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT '登录账号',
  `password` varchar(128) NOT NULL COMMENT '密码(SHA-256)',
  `must_change_pwd` tinyint DEFAULT '0' COMMENT '是否必须修改密码',
  `last_seen_release_id` bigint DEFAULT '0' COMMENT '最后已读版本记录ID',
  `login_fail_count` int DEFAULT '0' COMMENT '连续登录失败次数',
  `lock_until` datetime DEFAULT NULL COMMENT '锁定截止时间',
  `pwd_update_time` datetime DEFAULT NULL COMMENT '密码更新时间',
  `nick_name` varchar(64) NOT NULL COMMENT '姓名',
  `avatar` varchar(255) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone_verified` tinyint DEFAULT '0' COMMENT '手机号是否已验证',
  `phone_verified_at` datetime DEFAULT NULL COMMENT '手机号验证时间',
  `email_verified` tinyint DEFAULT '0' COMMENT '邮箱是否已验证',
  `email_verified_at` datetime DEFAULT NULL COMMENT '邮箱验证时间',
  `gender` char(1) DEFAULT '0' COMMENT '0男 1女 2未知',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `post_id` bigint DEFAULT NULL COMMENT '岗位',
  `company_id` bigint DEFAULT NULL COMMENT '所属公司',
  `pref_page_size` int DEFAULT '10' COMMENT '默认每页条数',
  `user_type` char(1) DEFAULT '1' COMMENT '0超管 1公司 2院方',
  `status` char(1) DEFAULT '0' COMMENT '0正常 1停用',
  `login_ip` varchar(64) DEFAULT NULL,
  `login_date` datetime DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_company_username` (`company_id`,`username`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sys_verification_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_verification_code` (
  `verification_id` bigint NOT NULL AUTO_INCREMENT,
  `target_type` varchar(10) NOT NULL COMMENT 'phone/email',
  `target` varchar(100) NOT NULL,
  `scene` varchar(30) DEFAULT 'bind' COMMENT 'bind',
  `code_hash` varchar(128) NOT NULL,
  `expires_at` datetime NOT NULL,
  `attempts` int DEFAULT '0',
  `used` tinyint DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`verification_id`),
  KEY `idx_verify_target` (`target_type`,`target`,`scene`,`used`),
  KEY `idx_verify_expire` (`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='验证码';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

