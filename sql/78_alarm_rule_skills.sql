-- 告警规则绑定需要技能，并带入转出的工单
ALTER TABLE biz_alarm ADD COLUMN rule_id BIGINT NULL AFTER equipment_id;
ALTER TABLE biz_alarm_rule ADD COLUMN required_skill_ids VARCHAR(500) NULL AFTER rule_name;
ALTER TABLE biz_workorder ADD COLUMN required_skill_ids VARCHAR(500) NULL AFTER equipment_id;
