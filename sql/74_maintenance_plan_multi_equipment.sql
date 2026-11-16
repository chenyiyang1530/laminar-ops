-- 保养计划支持多设备
CREATE TABLE IF NOT EXISTS biz_maintenance_plan_equipment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_equipment (plan_id,equipment_id),
  KEY idx_plan_equipment_plan (plan_id),
  KEY idx_plan_equipment_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保养计划适用设备';

INSERT IGNORE INTO biz_maintenance_plan_equipment(plan_id,equipment_id)
SELECT plan_id,equipment_id FROM biz_maintenance_plan WHERE equipment_id IS NOT NULL;
