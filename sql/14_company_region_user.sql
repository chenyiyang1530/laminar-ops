-- 公司-区域多对多 + 用户公司归属
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

ALTER TABLE `sys_user` ADD COLUMN `company_id` BIGINT DEFAULT NULL COMMENT '所属公司' AFTER `post_id`;

-- 现有区域关系迁移到公司-区域表（保留历史 region.company_id / manager_id）
INSERT IGNORE INTO `biz_company_region` (`company_id`,`region_id`,`manager_id`,`create_time`)
SELECT `company_id`,`region_id`,`manager_id`,NOW() FROM `biz_region` WHERE `company_id` IS NOT NULL;

-- 未归属公司的区域，挂到唯一公司，便于演示
INSERT IGNORE INTO `biz_company_region` (`company_id`,`region_id`,`manager_id`,`create_time`)
SELECT (SELECT MIN(`company_id`) FROM `biz_company`),`region_id`,`manager_id`,NOW()
FROM `biz_region` WHERE `company_id` IS NULL;

-- 公司内部用户归属公司（超管、院方账号不设置）
UPDATE `sys_user`
SET `company_id` = (SELECT MIN(`company_id`) FROM `biz_company`)
WHERE `user_type` = '1' AND `company_id` IS NULL;
