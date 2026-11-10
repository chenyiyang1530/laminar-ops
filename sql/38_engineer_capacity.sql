-- 工程师最大在手工单数，默认 5，可按工程师调整
ALTER TABLE `biz_engineer` ADD COLUMN `max_open_orders` INT DEFAULT 5 COMMENT '最大在手工单数' AFTER `on_duty`;
