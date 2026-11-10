-- =============================================================
-- laminar-ops 层流净化设备智能监测运维一体化平台
-- 数据库结构（MySQL 8 / utf8mb4）
-- =============================================================
CREATE DATABASE IF NOT EXISTS `laminar_ops` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `laminar_ops`;

-- -------------------------------------------------------------
-- 系统：用户 / 角色 / 菜单 / 部门 / 岗位 / 字典 / 日志 / 定时任务
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(64)  NOT NULL COMMENT '登录账号',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码(SHA-256)',
  `must_change_pwd` TINYINT DEFAULT 0 COMMENT '是否必须修改密码',
  `last_seen_release_id` BIGINT DEFAULT 0 COMMENT '最后已读版本记录ID',
  `login_fail_count` INT DEFAULT 0 COMMENT '连续登录失败次数',
  `lock_until`   DATETIME     DEFAULT NULL COMMENT '锁定截止时间',
  `pwd_update_time` DATETIME  DEFAULT NULL COMMENT '密码更新时间',
  `nick_name`   VARCHAR(64)  NOT NULL COMMENT '姓名',
  `avatar`      VARCHAR(255) DEFAULT NULL,
  `phone`       VARCHAR(30)  DEFAULT NULL,
  `email`       VARCHAR(100) DEFAULT NULL,
  `phone_verified` TINYINT DEFAULT 0 COMMENT '手机号是否已验证',
  `phone_verified_at` DATETIME DEFAULT NULL,
  `email_verified` TINYINT DEFAULT 0 COMMENT '邮箱是否已验证',
  `email_verified_at` DATETIME DEFAULT NULL,
  `gender`      CHAR(1)      DEFAULT '0' COMMENT '0男 1女 2未知',
  `dept_id`     BIGINT       DEFAULT NULL COMMENT '所属部门',
  `post_id`     BIGINT       DEFAULT NULL COMMENT '岗位',
  `company_id`  BIGINT       DEFAULT NULL COMMENT '所属公司',
  `pref_page_size` INT       DEFAULT 10 COMMENT '默认每页条数',
  `user_type`   CHAR(1)      DEFAULT '1' COMMENT '0超管 1公司 2院方',
  `status`      CHAR(1)      DEFAULT '0' COMMENT '0正常 1停用',
  `login_ip`    VARCHAR(64)  DEFAULT NULL,
  `login_date`  DATETIME     DEFAULT NULL,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  `remark`      VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_company_username` (`company_id`,`username`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS `sys_verification_code` (
  `verification_id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(10) NOT NULL COMMENT 'phone/email',
  `target` VARCHAR(100) NOT NULL,
  `scene` VARCHAR(30) DEFAULT 'bind' COMMENT 'bind',
  `code_hash` VARCHAR(128) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `attempts` INT DEFAULT 0,
  `used` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`verification_id`),
  KEY `idx_verify_target` (`target_type`,`target`,`scene`,`used`),
  KEY `idx_verify_expire` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='验证码';

CREATE TABLE IF NOT EXISTS `biz_cert_type_request` (
  `request_id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT DEFAULT NULL,
  `cert_type` VARCHAR(100) NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `requested_by` BIGINT DEFAULT NULL,
  `reviewed_by` BIGINT DEFAULT NULL,
  `review_comment` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `review_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `idx_cert_type_status` (`status`,`company_id`),
  KEY `idx_cert_type_name` (`cert_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书类型字典申请';

CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`  BIGINT       DEFAULT NULL COMMENT '所属公司，NULL 为平台标准角色',
  `role_type`   VARCHAR(20)  DEFAULT 'custom' COMMENT 'standard/custom',
  `role_name`   VARCHAR(64)  NOT NULL,
  `role_key`    VARCHAR(64)  NOT NULL,
  `sort`        INT          DEFAULT 0,
  `data_scope`  VARCHAR(20)  DEFAULT 'self' COMMENT 'all/region/hospital/self',
  `status`      CHAR(1)      DEFAULT '0',
  `remark`      VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_company_role_key` (`company_id`,`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色';

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `menu_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `menu_name`   VARCHAR(64)  NOT NULL,
  `parent_id`   BIGINT       DEFAULT 0,
  `order_num`   INT          DEFAULT 0,
  `path`        VARCHAR(200) DEFAULT NULL,
  `component`   VARCHAR(200) DEFAULT NULL,
  `perms`       VARCHAR(200) DEFAULT NULL,
  `menu_type`   CHAR(1)      DEFAULT 'C' COMMENT 'M目录 C菜单 F按钮',
  `icon`        VARCHAR(100) DEFAULT NULL,
  `visible`     CHAR(1)      DEFAULT '0',
  `status`      CHAR(1)      DEFAULT '0',
  `create_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`menu_id`),
  KEY `idx_menu_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单';

CREATE TABLE IF NOT EXISTS `sys_dept` (
  `dept_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`  BIGINT       DEFAULT NULL,
  `parent_id`   BIGINT       DEFAULT 0,
  `region_id`   BIGINT       DEFAULT NULL,
  `dept_name`   VARCHAR(100) NOT NULL,
  `leader`      VARCHAR(50)  DEFAULT NULL,
  `phone`       VARCHAR(30)  DEFAULT NULL,
  `email`       VARCHAR(100) DEFAULT NULL,
  `sort`        INT          DEFAULT 0,
  `status`      CHAR(1)      DEFAULT '0',
  `create_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`dept_id`),
  UNIQUE KEY `uk_dept_company_parent_name` (`company_id`,`parent_id`,`dept_name`),
  UNIQUE KEY `uk_dept_region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司组织部门';

CREATE TABLE IF NOT EXISTS `sys_post` (
  `post_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`  BIGINT       DEFAULT NULL,
  `post_code`   VARCHAR(50)  NOT NULL,
  `post_name`   VARCHAR(64)  NOT NULL,
  `sort`        INT          DEFAULT 0,
  `status`      CHAR(1)      DEFAULT '0',
  `remark`      VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `uk_post_company_code` (`company_id`,`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位';

CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `dict_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_name` VARCHAR(100) NOT NULL,
  `dict_type` VARCHAR(100) NOT NULL,
  `status`    CHAR(1)      DEFAULT '0',
  `remark`    VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型';

CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `dict_code`  BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_sort`  INT          DEFAULT 0,
  `dict_label` VARCHAR(100) NOT NULL,
  `dict_value` VARCHAR(100) NOT NULL,
  `dict_type`  VARCHAR(100) NOT NULL,
  `css_class`  VARCHAR(100) DEFAULT NULL,
  `list_class` VARCHAR(100) DEFAULT NULL,
  `is_default` CHAR(1)      DEFAULT 'N',
  `status`     CHAR(1)      DEFAULT '0',
  PRIMARY KEY (`dict_code`),
  KEY `idx_dd_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据';

CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(64)  DEFAULT NULL,
  `ip`         VARCHAR(64)  DEFAULT NULL,
  `browser`    VARCHAR(100) DEFAULT NULL,
  `os`         VARCHAR(100) DEFAULT NULL,
  `status`     CHAR(1)      DEFAULT '0' COMMENT '0成功 1失败',
  `msg`        VARCHAR(500) DEFAULT NULL,
  `login_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ll_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志';

CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(100) DEFAULT NULL,
  `business_type` VARCHAR(20)  DEFAULT NULL,
  `method`        VARCHAR(200) DEFAULT NULL,
  `request_method` VARCHAR(10) DEFAULT NULL,
  `oper_name`     VARCHAR(64)  DEFAULT NULL,
  `dept_name`     VARCHAR(100) DEFAULT NULL,
  `oper_url`      VARCHAR(255) DEFAULT NULL,
  `oper_ip`       VARCHAR(64)  DEFAULT NULL,
  `oper_param`    VARCHAR(2000) DEFAULT NULL,
  `json_result`   VARCHAR(2000) DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0' COMMENT '0成功 1失败',
  `error_msg`     VARCHAR(2000) DEFAULT NULL,
  `cost_time`     BIGINT       DEFAULT 0,
  `oper_time`     DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ol_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作审计日志';

CREATE TABLE IF NOT EXISTS `sys_job` (
  `job_id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `job_name`      VARCHAR(100) NOT NULL,
  `bean_name`     VARCHAR(100) NOT NULL,
  `method_name`   VARCHAR(100) NOT NULL,
  `cron`          VARCHAR(100) NOT NULL,
  `status`        CHAR(1)      DEFAULT '0' COMMENT '0启用 1停用',
  `last_run_time` DATETIME     DEFAULT NULL,
  `last_result`   VARCHAR(500) DEFAULT NULL,
  `remark`        VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务';

-- -------------------------------------------------------------
-- 客户档案
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_company` (
  `company_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `company_name` VARCHAR(200) NOT NULL,
  `company_code` VARCHAR(50)  DEFAULT NULL,
  `total_manager_id` BIGINT   DEFAULT NULL,
  `address`      VARCHAR(500) DEFAULT NULL,
  `phone`        VARCHAR(30)  DEFAULT NULL,
  `contact`      VARCHAR(50)  DEFAULT NULL,
  `status`       CHAR(1)      DEFAULT '0',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  DEFAULT NULL,
  `create_time`  DATETIME     DEFAULT NULL,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`company_id`),
  UNIQUE KEY `uk_company_code` (`company_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保公司';

CREATE TABLE IF NOT EXISTS `biz_company_region` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `region_id`  BIGINT NOT NULL,
  `manager_id` BIGINT DEFAULT NULL,
  `remark`     VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_region` (`company_id`,`region_id`),
  KEY `idx_cr_region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司服务区域';

CREATE TABLE IF NOT EXISTS `biz_company_code_alias` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `alias_code` VARCHAR(64) NOT NULL,
  `company_id` BIGINT      NOT NULL,
  `create_time` DATETIME   DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_alias_code` (`alias_code`),
  KEY `idx_company_alias` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司编码历史别名';

CREATE TABLE IF NOT EXISTS `biz_region` (
  `region_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`  BIGINT       DEFAULT NULL,
  `parent_id`   BIGINT       DEFAULT NULL,
  `region_name` VARCHAR(100) NOT NULL,
  `region_code` VARCHAR(50)  DEFAULT NULL,
  `manager`     VARCHAR(50)  DEFAULT NULL,
  `manager_id`  BIGINT       DEFAULT NULL,
  `phone`       VARCHAR(30)  DEFAULT NULL,
  `status`      CHAR(1)      DEFAULT '0',
  `remark`      VARCHAR(500) DEFAULT NULL,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`region_id`),
  KEY `idx_region_company` (`company_id`),
  UNIQUE KEY `uk_region_manager` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务区域';

CREATE TABLE IF NOT EXISTS `biz_hospital` (
  `hospital_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `hospital_name` VARCHAR(200) NOT NULL,
  `hospital_code` VARCHAR(50)  DEFAULT NULL,
  `region_id`     BIGINT       DEFAULT NULL,
  `hospital_type` VARCHAR(30)  DEFAULT NULL,
  `grade`         VARCHAR(20)  DEFAULT NULL,
  `nature`        VARCHAR(20)  DEFAULT NULL,
  `address`       VARCHAR(500) DEFAULT NULL,
  `longitude`     DECIMAL(10,6) DEFAULT NULL,
  `latitude`      DECIMAL(10,6) DEFAULT NULL,
  `phone`         VARCHAR(30)  DEFAULT NULL,
  `contact_person` VARCHAR(50) DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0',
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`hospital_id`),
  UNIQUE KEY `uk_hospital_code` (`hospital_code`),
  KEY `idx_hospital_region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医院';

CREATE TABLE IF NOT EXISTS `biz_campus` (
  `campus_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `hospital_id` BIGINT       NOT NULL,
  `campus_code` VARCHAR(50)  DEFAULT NULL,
  `campus_name` VARCHAR(100) NOT NULL,
  `address`     VARCHAR(500) DEFAULT NULL,
  `longitude`   DECIMAL(10,6) DEFAULT NULL,
  `latitude`    DECIMAL(10,6) DEFAULT NULL,
  `phone`       VARCHAR(30)  DEFAULT NULL,
  `bed_count`   INT          DEFAULT 0,
  `status`      CHAR(1)      DEFAULT '0',
  `remark`      VARCHAR(500) DEFAULT NULL,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   VARCHAR(64)  DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`campus_id`),
  UNIQUE KEY `uk_campus_code` (`campus_code`),
  KEY `idx_campus_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医院院区';

CREATE TABLE IF NOT EXISTS `biz_department` (
  `department_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `hospital_id`     BIGINT       NOT NULL,
  `campus_id`       BIGINT       DEFAULT NULL,
  `dept_code`       VARCHAR(50)  DEFAULT NULL,
  `department_name` VARCHAR(100) NOT NULL,
  `dept_type`       VARCHAR(30)  DEFAULT NULL,
  `parent_id`       BIGINT       DEFAULT 0,
  `director`        VARCHAR(50)  DEFAULT NULL,
  `phone`           VARCHAR(30)  DEFAULT NULL,
  `location`        VARCHAR(200) DEFAULT NULL,
  `sort`            INT          DEFAULT 0,
  `status`          CHAR(1)      DEFAULT '0',
  `remark`          VARCHAR(500) DEFAULT NULL,
  `create_by`       VARCHAR(64)  DEFAULT NULL,
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       VARCHAR(64)  DEFAULT NULL,
  `update_time`     DATETIME     DEFAULT NULL,
  PRIMARY KEY (`department_id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_dept_hospital` (`hospital_id`),
  KEY `idx_dept_campus` (`campus_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='医院科室';

CREATE TABLE IF NOT EXISTS `biz_clean_area` (
  `area_id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `hospital_id`   BIGINT       NOT NULL,
  `campus_id`     BIGINT       NOT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `area_code`     VARCHAR(50)  DEFAULT NULL,
  `area_name`     VARCHAR(100) NOT NULL,
  `area_type`     VARCHAR(30)  DEFAULT NULL,
  `clean_level`   VARCHAR(20)  DEFAULT NULL,
  `area_size`     DECIMAL(10,2) DEFAULT NULL,
  `design_temp_min`     DECIMAL(5,1) DEFAULT NULL,
  `design_temp_max`     DECIMAL(5,1) DEFAULT NULL,
  `design_humidity_min` DECIMAL(5,1) DEFAULT NULL,
  `design_humidity_max` DECIMAL(5,1) DEFAULT NULL,
  `design_pressure`     DECIMAL(6,1) DEFAULT NULL,
  `clean_require` VARCHAR(500) DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0',
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`area_id`),
  UNIQUE KEY `uk_area_code` (`area_code`),
  KEY `idx_area_campus` (`campus_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='洁净区域';

CREATE TABLE IF NOT EXISTS `biz_contact` (
  `contact_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `hospital_id`   BIGINT       NOT NULL,
  `campus_id`     BIGINT       DEFAULT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `contact_name`  VARCHAR(50)  NOT NULL,
  `position`      VARCHAR(50)  DEFAULT NULL,
  `contact_type`  VARCHAR(30)  DEFAULT NULL,
  `phone`         VARCHAR(30)  DEFAULT NULL,
  `tel`           VARCHAR(30)  DEFAULT NULL,
  `email`         VARCHAR(100) DEFAULT NULL,
  `duty`          VARCHAR(200) DEFAULT NULL,
  `is_primary`    CHAR(1)      DEFAULT '0',
  `status`        CHAR(1)      DEFAULT '0',
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`contact_id`),
  KEY `idx_contact_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户对接人';

-- -------------------------------------------------------------
-- 合同
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_contract` (
  `contract_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `contract_no`   VARCHAR(64)  NOT NULL,
  `contract_name` VARCHAR(200) NOT NULL,
  `hospital_id`   BIGINT       NOT NULL,
  `company_id`    BIGINT       DEFAULT NULL,
  `contract_type` VARCHAR(30)  DEFAULT NULL,
  `start_date`    DATE         DEFAULT NULL,
  `end_date`      DATE         DEFAULT NULL,
  `sign_date`     DATE         DEFAULT NULL,
  `amount`        DECIMAL(14,2) DEFAULT NULL,
  `manager_id`    BIGINT       DEFAULT NULL,
  `warn_days`     INT          DEFAULT 90,
  `status`        CHAR(1)      DEFAULT '0' COMMENT '0执行中 1已终止 2已过期 3未开始',
  `attachment`    VARCHAR(500) DEFAULT NULL,
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`contract_id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_contract_hospital` (`hospital_id`),
  KEY `idx_contract_end` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保合同';

CREATE TABLE IF NOT EXISTS `biz_contract_scope` (
  `scope_id`    BIGINT      NOT NULL AUTO_INCREMENT,
  `contract_id` BIGINT      NOT NULL,
  `scope_type`  VARCHAR(20) NOT NULL COMMENT 'hospital/department/area/equipment',
  `scope_value` BIGINT      DEFAULT NULL,
  PRIMARY KEY (`scope_id`),
  UNIQUE KEY `uk_contract_scope` (`contract_id`,`scope_type`,`scope_value`),
  KEY `idx_scope_target` (`scope_type`,`scope_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合同服务范围';

-- -------------------------------------------------------------
-- 设备与监测
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_device_type` (
  `type_id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`      BIGINT       DEFAULT NULL,
  `type_code`       VARCHAR(50)  NOT NULL,
  `type_name`       VARCHAR(100) NOT NULL,
  `category`        VARCHAR(30)  NOT NULL,
  `icon`            VARCHAR(100) DEFAULT NULL,
  `monitor_enabled` CHAR(1)      DEFAULT '1',
  `param_template`  TEXT         DEFAULT NULL,
  `sort`            INT          DEFAULT 0,
  `status`          CHAR(1)      DEFAULT '0',
  `remark`          VARCHAR(500) DEFAULT NULL,
  `create_by`       VARCHAR(64)  DEFAULT NULL,
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       VARCHAR(64)  DEFAULT NULL,
  `update_time`     DATETIME     DEFAULT NULL,
  PRIMARY KEY (`type_id`),
  UNIQUE KEY `uk_company_type_code` (`company_id`,`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备类型';

CREATE TABLE IF NOT EXISTS `biz_equipment` (
  `equipment_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`      BIGINT       DEFAULT NULL,
  `equipment_code`  VARCHAR(50)  DEFAULT NULL,
  `equipment_name`  VARCHAR(200) NOT NULL,
  `hospital_id`     BIGINT       NOT NULL,
  `campus_id`       BIGINT       DEFAULT NULL,
  `department_id`   BIGINT       DEFAULT NULL,
  `area_id`         BIGINT       DEFAULT NULL,
  `device_type_id`  BIGINT       DEFAULT NULL,
  `category`        VARCHAR(30)  DEFAULT NULL,
  `model`           VARCHAR(100) DEFAULT NULL,
  `brand`           VARCHAR(100) DEFAULT NULL,
  `serial_no`       VARCHAR(100) DEFAULT NULL,
  `supplier`        VARCHAR(200) DEFAULT NULL,
  `install_date`    DATE         DEFAULT NULL,
  `warranty_end`    DATE         DEFAULT NULL,
  `install_location` VARCHAR(200) DEFAULT NULL,
  `usage_location`  VARCHAR(255) DEFAULT NULL,
  `contract_id`     BIGINT       DEFAULT NULL,
  `run_status`      CHAR(1)      DEFAULT '0' COMMENT '0运行 1停机 2故障 3离线',
  `monitor_status`  CHAR(1)      DEFAULT '0' COMMENT '0未接入 1在线 2离线 3异常',
  `protocol`        VARCHAR(30)  DEFAULT NULL,
  `comm_address`    VARCHAR(100) DEFAULT NULL,
  `rated_power`     DECIMAL(10,2) DEFAULT NULL,
  `last_heartbeat`  DATETIME     DEFAULT NULL,
  `next_maintain_date` DATE     DEFAULT NULL,
  `status`          CHAR(1)      DEFAULT '0' COMMENT '0在用 1停用 2报废',
  `remark`          VARCHAR(500) DEFAULT NULL,
  `create_by`       VARCHAR(64)  DEFAULT NULL,
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       VARCHAR(64)  DEFAULT NULL,
  `update_time`     DATETIME     DEFAULT NULL,
  PRIMARY KEY (`equipment_id`),
  UNIQUE KEY `uk_eq_code` (`equipment_code`),
  KEY `idx_eq_hospital` (`hospital_id`),
  KEY `idx_eq_area` (`area_id`),
  KEY `idx_eq_type` (`device_type_id`),
  KEY `idx_equipment_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备台账';

CREATE TABLE IF NOT EXISTS `biz_monitor_point` (
  `point_id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`       BIGINT       DEFAULT NULL,
  `equipment_id`     BIGINT       NOT NULL,
  `point_code`       VARCHAR(50)  NOT NULL,
  `point_name`       VARCHAR(100) NOT NULL,
  `metric`           VARCHAR(30)  NOT NULL,
  `unit`             VARCHAR(20)  DEFAULT NULL,
  `warn_lower`       DECIMAL(12,3) DEFAULT NULL,
  `warn_upper`       DECIMAL(12,3) DEFAULT NULL,
  `alarm_lower`      DECIMAL(12,3) DEFAULT NULL,
  `alarm_upper`      DECIMAL(12,3) DEFAULT NULL,
  `precision`        INT          DEFAULT 1,
  `collect_interval` INT          DEFAULT 30,
  `enabled`          CHAR(1)      DEFAULT '1',
  `sort`             INT          DEFAULT 0,
  `remark`           VARCHAR(500) DEFAULT NULL,
  `create_by`        VARCHAR(64)  DEFAULT NULL,
  `create_time`      DATETIME     DEFAULT NULL,
  `update_by`        VARCHAR(64)  DEFAULT NULL,
  `update_time`      DATETIME     DEFAULT NULL,
  PRIMARY KEY (`point_id`),
  UNIQUE KEY `uk_point` (`equipment_id`, `point_code`),
  KEY `idx_point_metric` (`metric`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测点位';

CREATE TABLE IF NOT EXISTS `biz_monitor_data` (
  `data_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `equipment_id` BIGINT       NOT NULL,
  `point_id`     BIGINT       NOT NULL,
  `metric`       VARCHAR(30)  DEFAULT NULL,
  `value`        DECIMAL(12,3) DEFAULT NULL,
  `status`       CHAR(1)      DEFAULT '0',
  `collect_time` DATETIME     NOT NULL,
  PRIMARY KEY (`data_id`),
  KEY `idx_md_point` (`point_id`, `collect_time`),
  KEY `idx_md_equip` (`equipment_id`, `collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测历史数据';

CREATE TABLE IF NOT EXISTS `biz_alarm_rule` (
  `rule_id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`     BIGINT       DEFAULT NULL,
  `rule_name`      VARCHAR(100) NOT NULL,
  `device_type_id` BIGINT       DEFAULT NULL,
  `equipment_id`   BIGINT       DEFAULT NULL,
  `metric`         VARCHAR(30)  NOT NULL,
  `level`          CHAR(1)      DEFAULT '3',
  `operator`       VARCHAR(20)  DEFAULT 'gt',
  `threshold`      DECIMAL(12,3) DEFAULT NULL,
  `threshold2`     DECIMAL(12,3) DEFAULT NULL,
  `duration`       INT          DEFAULT 0,
  `auto_workorder` CHAR(1)      DEFAULT '0',
  `notify_roles`   VARCHAR(200) DEFAULT NULL,
  `enabled`        CHAR(1)      DEFAULT '1',
  `remark`         VARCHAR(500) DEFAULT NULL,
  `create_by`      VARCHAR(64)  DEFAULT NULL,
  `create_time`    DATETIME     DEFAULT NULL,
  `update_by`      VARCHAR(64)  DEFAULT NULL,
  `update_time`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警规则';

CREATE TABLE IF NOT EXISTS `biz_alarm` (
  `alarm_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT       DEFAULT NULL,
  `alarm_no`      VARCHAR(64)  NOT NULL,
  `hospital_id`   BIGINT       NOT NULL,
  `campus_id`     BIGINT       DEFAULT NULL,
  `area_id`       BIGINT       DEFAULT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `equipment_id`  BIGINT       NOT NULL,
  `point_id`      BIGINT       DEFAULT NULL,
  `metric`        VARCHAR(30)  DEFAULT NULL,
  `level`         CHAR(1)      DEFAULT '3',
  `alarm_value`   DECIMAL(12,3) DEFAULT NULL,
  `threshold`     DECIMAL(12,3) DEFAULT NULL,
  `content`       VARCHAR(500) DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0' COMMENT '0未确认 1已确认 2处理中 3已消缺 4已忽略',
  `first_time`    DATETIME     NOT NULL,
  `last_time`     DATETIME     DEFAULT NULL,
  `duration`      INT          DEFAULT 0,
  `confirm_by`    VARCHAR(64)  DEFAULT NULL,
  `confirm_time`  DATETIME     DEFAULT NULL,
  `handle_by`     VARCHAR(64)  DEFAULT NULL,
  `handle_time`   DATETIME     DEFAULT NULL,
  `close_by`      VARCHAR(64)  DEFAULT NULL,
  `close_time`    DATETIME     DEFAULT NULL,
  `workorder_id`  BIGINT       DEFAULT NULL,
  `root_cause`    VARCHAR(500) DEFAULT NULL,
  `solution`      VARCHAR(1000) DEFAULT NULL,
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`alarm_id`),
  UNIQUE KEY `uk_alarm_no` (`alarm_no`),
  KEY `idx_alarm_eq` (`equipment_id`, `status`),
  KEY `idx_alarm_time` (`first_time`),
  KEY `idx_alarm_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警';

CREATE TABLE IF NOT EXISTS `biz_alarm_log` (
  `log_id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `alarm_id`      BIGINT       NOT NULL,
  `action`        VARCHAR(30)  NOT NULL,
  `operator_id`   BIGINT       DEFAULT NULL,
  `operator_name` VARCHAR(64)  DEFAULT NULL,
  `from_status`   CHAR(1)      DEFAULT NULL,
  `to_status`     CHAR(1)      DEFAULT NULL,
  `content`       VARCHAR(1000) DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_al_log` (`alarm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警追溯日志';

-- -------------------------------------------------------------
-- 工单
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_workorder` (
  `workorder_id`  BIGINT       NOT NULL AUTO_INCREMENT,
  `workorder_no`  VARCHAR(64)  NOT NULL,
  `title`         VARCHAR(200) NOT NULL,
  `fault_desc`    TEXT         DEFAULT NULL,
  `fault_level`   CHAR(1)      DEFAULT '2',
  `status`        CHAR(1)      DEFAULT '0' COMMENT '0待受理 1待派工 2处理中 3待验收 4待回访 5已完成 6已取消',
  `source`        VARCHAR(30)  DEFAULT 'hospital' COMMENT 'hospital/alarm/inspection/maintenance/internal',
  `service_type`  CHAR(1)      DEFAULT '0',
  `hospital_id`   BIGINT       NOT NULL,
  `campus_id`     BIGINT       DEFAULT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `area_id`       BIGINT       DEFAULT NULL,
  `equipment_id`  BIGINT       DEFAULT NULL,
  `company_id`    BIGINT       DEFAULT NULL COMMENT '承接维保公司（按合同范围自动匹配）',
  `engineer_id`   BIGINT       DEFAULT NULL,
  `report_contact` VARCHAR(50) DEFAULT NULL,
  `report_phone`  VARCHAR(30)  DEFAULT NULL,
  `sla_deadline`  DATETIME     DEFAULT NULL,
  `receive_time`  DATETIME     DEFAULT NULL,
  `receive_by`    VARCHAR(64)  DEFAULT NULL,
  `dispatch_time` DATETIME     DEFAULT NULL,
  `dispatch_by`   VARCHAR(64)  DEFAULT NULL,
  `arrive_time`   DATETIME     DEFAULT NULL,
  `finish_time`   DATETIME     DEFAULT NULL,
  `accept_time`   DATETIME     DEFAULT NULL,
  `accept_by`     VARCHAR(64)  DEFAULT NULL,
  `accept_result` CHAR(1)      DEFAULT NULL,
  `accept_remark` VARCHAR(500) DEFAULT NULL,
  `quote_amount`  DECIMAL(12,2) DEFAULT NULL,
  `quote_status`  CHAR(1)      DEFAULT '0',
  `quote_remark`  VARCHAR(500) DEFAULT NULL,
  `callback_time` DATETIME     DEFAULT NULL,
  `callback_by`   VARCHAR(64)  DEFAULT NULL,
  `callback_score` INT         DEFAULT NULL,
  `callback_content` VARCHAR(500) DEFAULT NULL,
  `close_time`    DATETIME     DEFAULT NULL,
  `close_by`      VARCHAR(64)  DEFAULT NULL,
  `alarm_id`      BIGINT       DEFAULT NULL,
  `inspection_task_id` BIGINT  DEFAULT NULL,
  `maintenance_task_id` BIGINT DEFAULT NULL,
  `cancel_reason` VARCHAR(500) DEFAULT NULL,
  `downtime_minutes` INT       DEFAULT 0,
  `travel_fee`    DECIMAL(10,2) DEFAULT 0,
  `total_cost`    DECIMAL(12,2) DEFAULT 0,
  `repair_report` TEXT         DEFAULT NULL,
  `acceptance_image` VARCHAR(500) DEFAULT NULL,
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`workorder_id`),
  UNIQUE KEY `uk_wo_no` (`workorder_no`),
  KEY `idx_wo_hospital` (`hospital_id`),
  KEY `idx_wo_status` (`status`),
  KEY `idx_wo_engineer` (`engineer_id`),
  KEY `idx_wo_create` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保工单';

CREATE TABLE IF NOT EXISTS `biz_workorder_cost` (
  `cost_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `workorder_id` BIGINT       NOT NULL,
  `cost_type`    VARCHAR(30)  DEFAULT NULL COMMENT 'part备件/labor人工/travel差旅/outsource外包/other',
  `cost_name`    VARCHAR(100) DEFAULT NULL,
  `quantity`     DECIMAL(10,2) DEFAULT 1,
  `price`        DECIMAL(12,2) DEFAULT 0,
  `amount`       DECIMAL(12,2) DEFAULT 0,
  `remark`       VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`cost_id`),
  KEY `idx_cost_wo` (`workorder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单费用明细';

CREATE TABLE IF NOT EXISTS `biz_workorder_log` (
  `log_id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `workorder_id`  BIGINT       NOT NULL,
  `action`        VARCHAR(30)  NOT NULL,
  `operator_id`   BIGINT       DEFAULT NULL,
  `operator_name` VARCHAR(64)  DEFAULT NULL,
  `from_status`   CHAR(1)      DEFAULT NULL,
  `to_status`     CHAR(1)      DEFAULT NULL,
  `content`       VARCHAR(1000) DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_wo_log` (`workorder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单流转日志';

-- -------------------------------------------------------------
-- 巡检
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_inspection_plan` (
  `plan_id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT       DEFAULT NULL,
  `plan_no`       VARCHAR(50)  DEFAULT NULL,
  `plan_name`     VARCHAR(100) NOT NULL,
  `hospital_id`   BIGINT       NOT NULL,
  `campus_id`     BIGINT       DEFAULT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `area_id`       BIGINT       DEFAULT NULL,
  `equipment_id`  BIGINT       DEFAULT NULL,
  `plan_type`     VARCHAR(30)  DEFAULT 'routine',
  `cycle_type`    VARCHAR(20)  DEFAULT 'day',
  `cycle_value`   INT          DEFAULT 1,
  `start_date`    DATE         DEFAULT NULL,
  `end_date`      DATE         DEFAULT NULL,
  `next_run_date` DATE         DEFAULT NULL,
  `item_template` TEXT         DEFAULT NULL,
  `assignee_id`   BIGINT       DEFAULT NULL,
  `remind_days`   INT          DEFAULT 1,
  `status`        CHAR(1)      DEFAULT '0',
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检计划';

CREATE TABLE IF NOT EXISTS `biz_inspection_task` (
  `task_id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT       DEFAULT NULL,
  `task_no`       VARCHAR(50)  DEFAULT NULL,
  `plan_id`       BIGINT       DEFAULT NULL,
  `task_type`     VARCHAR(20)  DEFAULT 'plan',
  `hospital_id`   BIGINT       NOT NULL,
  `campus_id`     BIGINT       DEFAULT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `area_id`       BIGINT       DEFAULT NULL,
  `equipment_id`  BIGINT       DEFAULT NULL,
  `plan_time`     DATE         NOT NULL,
  `assignee_id`   BIGINT       DEFAULT NULL,
  `assignee_name` VARCHAR(50)  DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0' COMMENT '0待执行 1执行中 2已完成 3已逾期 4已取消',
  `start_time`    DATETIME     DEFAULT NULL,
  `finish_time`   DATETIME     DEFAULT NULL,
  `result`        CHAR(1)      DEFAULT NULL,
  `problem_desc`  VARCHAR(1000) DEFAULT NULL,
  `workorder_id`  BIGINT       DEFAULT NULL,
  `score`         INT          DEFAULT NULL,
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_it_no` (`task_no`),
  KEY `idx_it_status` (`status`),
  KEY `idx_it_date` (`plan_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检任务';

CREATE TABLE IF NOT EXISTS `biz_inspection_item` (
  `item_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`    BIGINT       NOT NULL,
  `item_name`  VARCHAR(200) NOT NULL,
  `standard`   VARCHAR(500) DEFAULT NULL,
  `result`     CHAR(1)      DEFAULT NULL,
  `item_value` VARCHAR(100) DEFAULT NULL,
  `image_url`  VARCHAR(500) DEFAULT NULL,
  `remark`     VARCHAR(500) DEFAULT NULL,
  `sort`       INT          DEFAULT 0,
  PRIMARY KEY (`item_id`),
  KEY `idx_ii_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检检查项';

-- -------------------------------------------------------------
-- 保养
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_maintenance_plan` (
  `plan_id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`     BIGINT       DEFAULT NULL,
  `plan_no`        VARCHAR(50)  DEFAULT NULL,
  `plan_name`      VARCHAR(100) NOT NULL,
  `hospital_id`    BIGINT       NOT NULL,
  `campus_id`      BIGINT       DEFAULT NULL,
  `department_id`  BIGINT       DEFAULT NULL,
  `equipment_id`   BIGINT       DEFAULT NULL,
  `device_type_id` BIGINT       DEFAULT NULL,
  `maintenance_level` VARCHAR(30) DEFAULT 'routine',
  `cycle_type`     VARCHAR(20)  DEFAULT 'month',
  `cycle_value`    INT          DEFAULT 1,
  `start_date`     DATE         DEFAULT NULL,
  `end_date`       DATE         DEFAULT NULL,
  `next_run_date`  DATE         DEFAULT NULL,
  `content`        TEXT         DEFAULT NULL,
  `assignee_id`    BIGINT       DEFAULT NULL,
  `remind_days`    INT          DEFAULT 7,
  `status`         CHAR(1)      DEFAULT '0',
  `remark`         VARCHAR(500) DEFAULT NULL,
  `create_by`      VARCHAR(64)  DEFAULT NULL,
  `create_time`    DATETIME     DEFAULT NULL,
  `update_by`      VARCHAR(64)  DEFAULT NULL,
  `update_time`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养计划';

CREATE TABLE IF NOT EXISTS `biz_maintenance_task` (
  `task_id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT       DEFAULT NULL,
  `task_no`       VARCHAR(50)  DEFAULT NULL,
  `plan_id`       BIGINT       DEFAULT NULL,
  `hospital_id`   BIGINT       NOT NULL,
  `campus_id`     BIGINT       DEFAULT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `equipment_id`  BIGINT       DEFAULT NULL,
  `plan_date`     DATE         NOT NULL,
  `assignee_id`   BIGINT       DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0',
  `start_time`    DATETIME     DEFAULT NULL,
  `finish_time`   DATETIME     DEFAULT NULL,
  `result`        CHAR(1)      DEFAULT NULL,
  `content`       VARCHAR(1000) DEFAULT NULL,
  `parts_used`    VARCHAR(500) DEFAULT NULL,
  `workorder_id`  BIGINT       DEFAULT NULL,
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_mt_no` (`task_no`),
  KEY `idx_mt_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养任务';

CREATE TABLE IF NOT EXISTS `biz_maintenance_item` (
  `item_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`    BIGINT       NOT NULL,
  `item_name`  VARCHAR(200) NOT NULL,
  `standard`   VARCHAR(500) DEFAULT NULL,
  `result`     CHAR(1)      DEFAULT NULL,
  `item_value` VARCHAR(100) DEFAULT NULL,
  `remark`     VARCHAR(500) DEFAULT NULL,
  `sort`       INT          DEFAULT 0,
  PRIMARY KEY (`item_id`),
  KEY `idx_mi_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保养项目';

-- -------------------------------------------------------------
-- 人员 / 技能 / 证书
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_skill` (
  `skill_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT       DEFAULT NULL,
  `skill_name` VARCHAR(100) NOT NULL,
  `skill_code` VARCHAR(50)  DEFAULT NULL,
  `category`   VARCHAR(30)  DEFAULT NULL,
  `status`     CHAR(1)      DEFAULT '0',
  `remark`     VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME    DEFAULT NULL,
  PRIMARY KEY (`skill_id`),
  UNIQUE KEY `uk_company_skill_code` (`company_id`,`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能标签';

CREATE TABLE IF NOT EXISTS `biz_engineer` (
  `engineer_id`  BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       DEFAULT NULL,
  `company_id`   BIGINT       DEFAULT NULL,
  `region_id`    BIGINT       DEFAULT NULL,
  `engineer_name` VARCHAR(50) NOT NULL,
  `phone`        VARCHAR(30)  DEFAULT NULL,
  `gender`       CHAR(1)      DEFAULT '0',
  `job_title`    VARCHAR(50)  DEFAULT NULL,
  `specialty`    VARCHAR(200) DEFAULT NULL,
  `entry_date`   DATE         DEFAULT NULL,
  `on_duty`      CHAR(1)      DEFAULT '1',
  `max_open_orders` INT       DEFAULT 5 COMMENT '最大在手工单数',
  `latitude`     DECIMAL(10,6) DEFAULT NULL,
  `longitude`    DECIMAL(10,6) DEFAULT NULL,
  `status`       CHAR(1)      DEFAULT '0',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  DEFAULT NULL,
  `create_time`  DATETIME     DEFAULT NULL,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`engineer_id`),
  UNIQUE KEY `uk_engineer_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维保工程师';

CREATE TABLE IF NOT EXISTS `biz_engineer_skill` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `engineer_id` BIGINT NOT NULL,
  `skill_id`    BIGINT NOT NULL,
  `level`       VARCHAR(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eng_skill` (`engineer_id`, `skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程师技能';

CREATE TABLE IF NOT EXISTS `biz_engineer_hospital` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `engineer_id` BIGINT NOT NULL,
  `hospital_id` BIGINT NOT NULL,
  `is_manager`  CHAR(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eng_hosp` (`engineer_id`, `hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程师服务医院';

CREATE TABLE IF NOT EXISTS `biz_certificate` (
  `cert_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`   BIGINT       DEFAULT NULL,
  `engineer_id`  BIGINT       NOT NULL,
  `cert_type`    VARCHAR(50)  DEFAULT NULL,
  `cert_name`    VARCHAR(100) NOT NULL,
  `cert_no`      VARCHAR(100) DEFAULT NULL,
  `issuing_authority` VARCHAR(200) DEFAULT NULL,
  `issue_date`   DATE         DEFAULT NULL,
  `expire_date`  DATE         DEFAULT NULL,
  `file_url`     VARCHAR(500) DEFAULT NULL,
  `remind_days`  INT          DEFAULT 60,
  `status`       CHAR(1)      DEFAULT '0' COMMENT '0有效 1即将到期 2已过期',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  DEFAULT NULL,
  `create_time`  DATETIME     DEFAULT NULL,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`cert_id`),
  KEY `idx_cert_eng` (`engineer_id`),
  KEY `idx_cert_expire` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资质证书';

-- -------------------------------------------------------------
-- 备件
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_part` (
  `part_id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT       DEFAULT NULL,
  `part_code`     VARCHAR(50)  NOT NULL,
  `part_name`     VARCHAR(100) NOT NULL,
  `part_category` VARCHAR(30)  DEFAULT NULL,
  `spec`          VARCHAR(100) DEFAULT NULL,
  `unit`          VARCHAR(20)  DEFAULT '个',
  `brand`         VARCHAR(100) DEFAULT NULL,
  `safety_stock`  INT          DEFAULT 0,
  `current_stock` INT          DEFAULT 0,
  `price`         DECIMAL(12,2) DEFAULT 0,
  `supplier`      VARCHAR(200) DEFAULT NULL,
  `warehouse`     VARCHAR(100) DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0',
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`part_id`),
  UNIQUE KEY `uk_company_part_code` (`company_id`,`part_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件台账';

CREATE TABLE IF NOT EXISTS `biz_part_record` (
  `record_id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `part_id`      BIGINT       NOT NULL,
  `record_type`  VARCHAR(20)  NOT NULL,
  `quantity`     INT          NOT NULL,
  `before_stock` INT          DEFAULT 0,
  `after_stock`  INT          DEFAULT 0,
  `workorder_id` BIGINT       DEFAULT NULL,
  `requisition_id` BIGINT     DEFAULT NULL,
  `operator_id`  BIGINT       DEFAULT NULL,
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `record_time`  DATETIME     DEFAULT NULL,
  `remark`       VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`record_id`),
  KEY `idx_pr_part` (`part_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件出入库流水';

CREATE TABLE IF NOT EXISTS `biz_part_requisition` (
  `req_id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT       DEFAULT NULL,
  `req_no`        VARCHAR(50)  DEFAULT NULL,
  `workorder_id`  BIGINT       DEFAULT NULL,
  `hospital_id`   BIGINT       DEFAULT NULL,
  `applicant_id`  BIGINT       DEFAULT NULL,
  `applicant_name` VARCHAR(64) DEFAULT NULL,
  `status`        CHAR(1)      DEFAULT '0',
  `apply_time`    DATETIME     DEFAULT NULL,
  `approve_by`    VARCHAR(64)  DEFAULT NULL,
  `approve_time`  DATETIME     DEFAULT NULL,
  `out_by`        VARCHAR(64)  DEFAULT NULL,
  `out_time`      DATETIME     DEFAULT NULL,
  `total_amount`  DECIMAL(12,2) DEFAULT 0,
  `remark`        VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`req_id`),
  UNIQUE KEY `uk_req_no` (`req_no`),
  KEY `idx_req_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件领用单';

CREATE TABLE IF NOT EXISTS `biz_part_requisition_item` (
  `item_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `req_id`    BIGINT       NOT NULL,
  `part_id`   BIGINT       NOT NULL,
  `part_name` VARCHAR(100) DEFAULT NULL,
  `spec`      VARCHAR(100) DEFAULT NULL,
  `quantity`  INT          DEFAULT 1,
  `price`     DECIMAL(12,2) DEFAULT 0,
  `amount`    DECIMAL(12,2) DEFAULT 0,
  PRIMARY KEY (`item_id`),
  KEY `idx_pri_req` (`req_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备件领用明细';

-- -------------------------------------------------------------
-- 文档 / 知识库 / 消息 / 预算
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_document` (
  `doc_id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`   BIGINT       DEFAULT NULL,
  `doc_name`     VARCHAR(200) NOT NULL,
  `doc_category` VARCHAR(30)  DEFAULT NULL,
  `hospital_id`  BIGINT       DEFAULT NULL,
  `campus_id`    BIGINT       DEFAULT NULL,
  `equipment_id` BIGINT       DEFAULT NULL,
  `file_url`     VARCHAR(500) DEFAULT NULL,
  `file_name`    VARCHAR(200) DEFAULT NULL,
  `file_size`    BIGINT       DEFAULT 0,
  `file_type`    VARCHAR(30)  DEFAULT NULL,
  `version`      VARCHAR(30)  DEFAULT 'V1.0',
  `uploader_id`  BIGINT       DEFAULT NULL,
  `uploader_name` VARCHAR(64) DEFAULT NULL,
  `upload_time`  DATETIME     DEFAULT NULL,
  `tags`         VARCHAR(200) DEFAULT NULL,
  `remark`       VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`doc_id`),
  KEY `idx_doc_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图纸文档';

CREATE TABLE IF NOT EXISTS `biz_knowledge` (
  `article_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `company_id`   BIGINT       DEFAULT NULL,
  `title`        VARCHAR(200) NOT NULL,
  `category`     VARCHAR(30)  DEFAULT NULL,
  `summary`      VARCHAR(500) DEFAULT NULL,
  `content`      MEDIUMTEXT   DEFAULT NULL,
  `tags`         VARCHAR(200) DEFAULT NULL,
  `author_id`    BIGINT       DEFAULT NULL,
  `author_name`  VARCHAR(64)  DEFAULT NULL,
  `view_count`   INT          DEFAULT 0,
  `status`       CHAR(1)      DEFAULT '1',
  `publish_time` DATETIME     DEFAULT NULL,
  `create_by`    VARCHAR(64)  DEFAULT NULL,
  `create_time`  DATETIME     DEFAULT NULL,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库';

CREATE TABLE IF NOT EXISTS `biz_message` (
  `msg_id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `msg_type`      VARCHAR(30)  NOT NULL,
  `title`         VARCHAR(200) NOT NULL,
  `content`       VARCHAR(1000) DEFAULT NULL,
  `level`         CHAR(1)      DEFAULT '3',
  `biz_type`      VARCHAR(30)  DEFAULT NULL,
  `biz_id`        BIGINT       DEFAULT NULL,
  `biz_key`       VARCHAR(128) DEFAULT NULL,
  `receiver_id`   BIGINT       NOT NULL DEFAULT 0,
  `receiver_role` VARCHAR(200) DEFAULT NULL,
  `hospital_id`   BIGINT       DEFAULT NULL,
  `company_id`    BIGINT       DEFAULT NULL,
  `is_read`       CHAR(1)      DEFAULT '0',
  `read_time`     DATETIME     DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`msg_id`),
  UNIQUE KEY `uk_msg_key` (`biz_key`, `receiver_id`),
  KEY `idx_msg_receiver` (`receiver_id`, `is_read`),
  KEY `idx_msg_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知';

CREATE TABLE IF NOT EXISTS `biz_budget` (
  `budget_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `budget_year`   INT          NOT NULL,
  `budget_name`   VARCHAR(100) DEFAULT NULL,
  `hospital_id`   BIGINT       DEFAULT NULL,
  `contract_id`   BIGINT       DEFAULT NULL,
  `company_id`    BIGINT       DEFAULT NULL,
  `subject`       VARCHAR(30)  NOT NULL,
  `amount`        DECIMAL(14,2) DEFAULT 0,
  `used_amount`   DECIMAL(14,2) DEFAULT 0,
  `status`        CHAR(1)      DEFAULT '0',
  `remark`        VARCHAR(500) DEFAULT NULL,
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`budget_id`),
  KEY `idx_b_year` (`budget_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算';

CREATE TABLE IF NOT EXISTS `biz_budget_execution` (
  `exec_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `budget_id`   BIGINT       NOT NULL,
  `workorder_id` BIGINT      DEFAULT NULL,
  `amount`      DECIMAL(12,2) NOT NULL,
  `occur_date`  DATE         DEFAULT NULL,
  `exec_type`   CHAR(1)      DEFAULT '1',
  `remark`      VARCHAR(500) DEFAULT NULL,
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`exec_id`),
  KEY `idx_be_budget` (`budget_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算执行';

CREATE TABLE IF NOT EXISTS `biz_hospital_user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `hospital_id`   BIGINT       NOT NULL,
  `department_id` BIGINT       DEFAULT NULL,
  `role_type`     VARCHAR(20)  DEFAULT 'admin',
  `status`        CHAR(1)      DEFAULT '0',
  `create_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hu_user` (`user_id`),
  KEY `idx_hu_hospital` (`hospital_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='院方用户绑定';

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

CREATE TABLE IF NOT EXISTS `biz_alarm_archive` LIKE `biz_alarm`;
CREATE TABLE IF NOT EXISTS `biz_workorder_archive` LIKE `biz_workorder`;

CREATE INDEX `idx_alarm_company_status_time` ON `biz_alarm` (`company_id`,`status`,`first_time`);
CREATE INDEX `idx_alarm_company_time` ON `biz_alarm` (`company_id`,`first_time`);
CREATE INDEX `idx_alarm_company_eq_status` ON `biz_alarm` (`company_id`,`equipment_id`,`status`);
CREATE INDEX `idx_wo_company_status_create` ON `biz_workorder` (`company_id`,`status`,`create_time`);
CREATE INDEX `idx_wo_company_create` ON `biz_workorder` (`company_id`,`create_time`);

CREATE TABLE IF NOT EXISTS `biz_engineer_leave` (
  `leave_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `engineer_id`   BIGINT       NOT NULL,
  `company_id`    BIGINT       DEFAULT NULL,
  `leave_type`    VARCHAR(30)  DEFAULT 'personal',
  `start_date`    DATE         NOT NULL,
  `end_date`      DATE         NOT NULL,
  `days`          INT          DEFAULT 1,
  `need_second`   TINYINT      DEFAULT 1,
  `reason`        VARCHAR(500) DEFAULT NULL,
  `status`        VARCHAR(20)  DEFAULT 'PENDING_REGION',
  `current_step`  INT          DEFAULT 1,
  `approver_chain` VARCHAR(1000) DEFAULT NULL,
  `handover_engineer_id` BIGINT DEFAULT NULL,
  `created_by`    BIGINT       DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  PRIMARY KEY (`leave_id`),
  KEY `idx_leave_engineer` (`engineer_id`),
  KEY `idx_leave_company` (`company_id`),
  KEY `idx_leave_status` (`status`),
  KEY `idx_leave_dates` (`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程师请假申请';

CREATE TABLE IF NOT EXISTS `biz_approval_task` (
  `task_id`     BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`    VARCHAR(30)  NOT NULL,
  `biz_id`      BIGINT       NOT NULL,
  `step_no`     INT          DEFAULT 1,
  `approver_id` BIGINT       NOT NULL,
  `status`      VARCHAR(20)  DEFAULT 'PENDING',
  `action`      VARCHAR(20)  DEFAULT NULL,
  `comment`     VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `acted_at`    DATETIME     DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  KEY `idx_task_approver` (`approver_id`,`status`),
  KEY `idx_task_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用审批任务';

CREATE TABLE IF NOT EXISTS `biz_leave_handover` (
  `handover_id`  BIGINT      NOT NULL AUTO_INCREMENT,
  `leave_id`     BIGINT      NOT NULL,
  `engineer_id`  BIGINT      NOT NULL,
  `workorder_id` BIGINT      NOT NULL,
  `status`       VARCHAR(20) DEFAULT 'PENDING',
  `create_time`  DATETIME    DEFAULT NULL,
  `done_time`    DATETIME    DEFAULT NULL,
  PRIMARY KEY (`handover_id`),
  UNIQUE KEY `uk_leave_handover` (`leave_id`,`workorder_id`),
  KEY `idx_handover_workorder` (`workorder_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假待改派工单';

CREATE TABLE IF NOT EXISTS `biz_approval_rule` (
  `rule_id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT      DEFAULT NULL,
  `region_id`     BIGINT      DEFAULT NULL,
  `leave_type`    VARCHAR(30) NOT NULL,
  `threshold_days` INT        DEFAULT 3,
  `second_level`  CHAR(1)     DEFAULT '1',
  `status`        CHAR(1)     DEFAULT '0',
  `update_by`     VARCHAR(64) DEFAULT NULL,
  `update_time`   DATETIME    DEFAULT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_company` (`company_id`,`leave_type`),
  KEY `idx_rule_region` (`company_id`,`region_id`,`leave_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='请假审批规则';

CREATE TABLE IF NOT EXISTS `biz_approval_delegate` (
  `delegate_id`    BIGINT NOT NULL AUTO_INCREMENT,
  `company_id`     BIGINT DEFAULT NULL,
  `user_id`        BIGINT NOT NULL,
  `target_user_id` BIGINT NOT NULL,
  `start_date`     DATE   NOT NULL,
  `end_date`       DATE   NOT NULL,
  `status`         CHAR(1) DEFAULT '0',
  `create_time`    DATETIME DEFAULT NULL,
  PRIMARY KEY (`delegate_id`),
  KEY `idx_delegate_user` (`user_id`,`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批代理';

CREATE TABLE IF NOT EXISTS `biz_approval_rule_log` (
  `log_id`        BIGINT      NOT NULL AUTO_INCREMENT,
  `company_id`    BIGINT      DEFAULT NULL,
  `region_id`     BIGINT      DEFAULT NULL,
  `leave_type`    VARCHAR(30) NOT NULL,
  `old_threshold` INT         DEFAULT NULL,
  `old_second`    CHAR(1)     DEFAULT NULL,
  `new_threshold` INT         DEFAULT NULL,
  `new_second`    CHAR(1)     DEFAULT NULL,
  `changed_by`    VARCHAR(64) DEFAULT NULL,
  `change_time`   DATETIME    DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_rule_log_company` (`company_id`,`change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批规则变更历史';

CREATE TABLE IF NOT EXISTS `biz_approval_rule_version` (
  `version_id`     BIGINT      NOT NULL AUTO_INCREMENT,
  `company_id`     BIGINT      DEFAULT NULL,
  `region_id`      BIGINT      DEFAULT NULL,
  `version_no`     INT         NOT NULL,
  `status`         VARCHAR(20) DEFAULT 'PUBLISHED',
  `snapshot_json`  LONGTEXT,
  `created_by`     VARCHAR(64) DEFAULT NULL,
  `create_time`    DATETIME    DEFAULT NULL,
  `published_by`   VARCHAR(64) DEFAULT NULL,
  `published_time` DATETIME    DEFAULT NULL,
  PRIMARY KEY (`version_id`),
  KEY `idx_version_scope` (`company_id`,`region_id`,`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批规则版本快照';
