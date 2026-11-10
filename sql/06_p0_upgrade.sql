-- P0 修复升级脚本（已有数据库执行一次；新库直接用 04_seed_ops.sql 即可）
-- 目的：让院方角色（200/201）接收本院告警与合同提醒，消息查询按 hospital_id 隔离。
UPDATE `biz_message`
SET `receiver_role` = '100,101,200,201'
WHERE `msg_type` IN ('alarm', 'contract')
  AND `receiver_role` IN ('100,101', '100');
