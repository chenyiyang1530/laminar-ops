ALTER TABLE sys_user
  ADD COLUMN account_scope VARCHAR(20) NULL COMMENT 'platform/company/hospital',
  ADD COLUMN owner_company_id BIGINT NULL COMMENT '归属公司',
  ADD COLUMN owner_hospital_id BIGINT NULL COMMENT '归属医院',
  ADD COLUMN owner_department_id BIGINT NULL COMMENT '归属科室';

UPDATE sys_user u
LEFT JOIN biz_hospital_user hu ON hu.user_id = u.user_id
SET u.account_scope = CASE
      WHEN u.user_type = '2' THEN 'hospital'
      WHEN u.company_id IS NULL THEN 'platform'
      WHEN EXISTS (
        SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
        WHERE ur.user_id = u.user_id AND r.role_key = 'super_admin'
      ) THEN 'platform'
      ELSE 'company'
    END,
    u.owner_company_id = CASE WHEN u.user_type = '1' THEN u.company_id ELSE NULL END,
    u.owner_hospital_id = hu.hospital_id,
    u.owner_department_id = hu.department_id;

CREATE INDEX idx_sys_user_account_scope ON sys_user(account_scope);
CREATE INDEX idx_sys_user_owner_hospital ON sys_user(owner_hospital_id);
