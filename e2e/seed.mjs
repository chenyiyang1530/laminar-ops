import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'

const root = path.resolve(import.meta.dirname, '..')
const mysql = process.env.MYSQL_BIN || 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe'
const secretsFile = path.join(root, '.secrets.local')
const secrets = fs.existsSync(secretsFile) ? Object.fromEntries(
  fs.readFileSync(secretsFile, 'utf8')
    .split(/\r?\n/)
    .filter((line) => line.includes('='))
    .map((line) => {
      const index = line.indexOf('=')
      return [line.slice(0, index), line.slice(index + 1)]
    })
) : {}
const rootPassword = process.env.MYSQL_ROOT_PASSWORD || secrets.MYSQL_ROOT_PASSWORD
if (!rootPassword) throw new Error('缺少 MYSQL_ROOT_PASSWORD 或 .secrets.local')
const env = { ...process.env, MYSQL_PWD: rootPassword }

function sql(statement) {
  return execFileSync(mysql, [
    '--host=127.0.0.1', '--user=root', '--default-character-set=utf8mb4', 'laminar_ops', '-N', '-e', statement
  ], { env, encoding: 'utf8' })
}

const title = `E2E-${Date.now()}`
const companyCode = 'E2E_TEST'
const password = '123456'
const hash = '8a571e9e0c92983528d524794b28dc8e1b48d5ccf4e0f9874a1d0cfd7f7869fa'

const result = sql(`
DELETE FROM biz_message WHERE biz_type='workorder' AND biz_id IN
  (SELECT workorder_id FROM biz_workorder WHERE title LIKE 'E2E-%');
DELETE wl FROM biz_workorder_log wl JOIN biz_workorder w ON w.workorder_id=wl.workorder_id WHERE w.title LIKE 'E2E-%';
DELETE FROM biz_workorder WHERE title LIKE 'E2E-%';
DELETE FROM biz_document WHERE doc_name LIKE 'E2E%';
DELETE FROM biz_hospital_user WHERE user_id IN (SELECT user_id FROM sys_user WHERE username LIKE 'e2e_%');
DELETE FROM sys_user_role WHERE user_id IN (SELECT user_id FROM sys_user WHERE username LIKE 'e2e_%');
DELETE FROM biz_engineer WHERE engineer_name='E2E工程师';
DELETE FROM sys_user WHERE username LIKE 'e2e_%';
DELETE FROM biz_department WHERE dept_code LIKE 'E2E-%';
DELETE FROM biz_equipment WHERE equipment_code LIKE 'E2E-%';
DELETE FROM biz_company_hospital WHERE company_id IN (SELECT company_id FROM biz_company WHERE company_code='${companyCode}')
  OR hospital_id IN (SELECT hospital_id FROM biz_hospital WHERE hospital_code='E2E-HOSP');
DELETE FROM biz_hospital WHERE hospital_code='E2E-HOSP';
DELETE FROM biz_device_type WHERE type_code='E2E-DEV';
DELETE FROM biz_company WHERE company_code='${companyCode}';

INSERT INTO biz_company(company_name,company_code,status,create_time)
VALUES('E2E测试公司','${companyCode}','0',NOW());
SET @company_id=LAST_INSERT_ID();

INSERT INTO biz_hospital(hospital_name,hospital_code,status,create_time)
VALUES('E2E测试医院','E2E-HOSP','0',NOW());
SET @hospital_id=LAST_INSERT_ID();

INSERT INTO biz_department(hospital_id,dept_code,department_name,status,create_time)
VALUES(@hospital_id,'E2E-DEPT-A','E2E科室A','0',NOW());
SET @dept_a=LAST_INSERT_ID();

INSERT INTO biz_department(hospital_id,dept_code,department_name,status,create_time)
VALUES(@hospital_id,'E2E-DEPT-B','E2E科室B','0',NOW());
SET @dept_b=LAST_INSERT_ID();

INSERT INTO biz_device_type(company_id,type_code,type_name,status,create_time)
VALUES(@company_id,'E2E-DEV','E2E设备类型','0',NOW());
SET @device_type_id=LAST_INSERT_ID();

INSERT INTO biz_equipment(company_id,equipment_code,equipment_name,hospital_id,device_type_id,status,create_time)
VALUES(@company_id,'E2E-AHU','E2E测试机组',@hospital_id,@device_type_id,'0',NOW());
SET @equipment_id=LAST_INSERT_ID();
INSERT INTO biz_monitor_point(company_id,equipment_id,point_code,point_name,metric,unit,warn_lower,warn_upper,alarm_lower,alarm_upper,collect_interval,enabled,create_time)
VALUES(@company_id,@equipment_id,'E2E-TEMP','E2E送风温度','temperature','℃',18,26,15,30,30,'1',NOW());

INSERT INTO sys_user(username,password,nick_name,user_type,company_id,status,must_change_pwd,create_time)
VALUES('e2e_company','${hash}','E2E公司管理员','1',@company_id,'0',0,NOW());
SET @company_user=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@company_user,213);

INSERT INTO sys_user(username,password,nick_name,user_type,company_id,status,must_change_pwd,create_time)
VALUES('e2e_superadmin','${hash}','E2E超级管理员','1',@company_id,'0',0,NOW());
SET @super_user=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@super_user,1);

INSERT INTO sys_user(username,password,nick_name,user_type,company_id,status,must_change_pwd,create_time)
VALUES('e2e_engineer','${hash}','E2E工程师','1',@company_id,'0',0,NOW());
SET @engineer_user=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@engineer_user,102);
INSERT INTO biz_engineer(user_id,company_id,engineer_name,phone,on_duty,status,create_time)
VALUES(@engineer_user,@company_id,'E2E工程师','13900009999','1','0',NOW());

INSERT INTO sys_user(username,password,nick_name,user_type,status,must_change_pwd,create_time)
VALUES('e2e_hospital','${hash}','E2E院方管理员','2','0',0,NOW());
SET @hospital_user=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@hospital_user,200);
INSERT INTO biz_hospital_user(user_id,hospital_id,role_type,status,create_time)
VALUES(@hospital_user,@hospital_id,'admin','0',NOW());

INSERT INTO sys_user(username,password,nick_name,user_type,status,must_change_pwd,create_time)
VALUES('e2e_subadmin_a','${hash}','E2E科室A管理员','2','0',0,NOW());
SET @subadmin_a=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@subadmin_a,202);
INSERT INTO biz_hospital_user(user_id,hospital_id,department_id,role_type,status,create_time)
VALUES(@subadmin_a,@hospital_id,@dept_a,'dept','0',NOW());

INSERT INTO biz_workorder(workorder_no,title,fault_desc,fault_level,status,source,service_type,hospital_id,department_id,equipment_id,company_id,report_contact,report_phone,create_time)
VALUES(CONCAT('WO-E2E-',DATE_FORMAT(NOW(),'%Y%m%d%H%i%s')),'${title}','E2E 登录到验收测试工单','2','0','hospital','0',@hospital_id,NULL,@equipment_id,@company_id,'E2E','13900009999',NOW());

INSERT INTO biz_workorder(workorder_no,title,fault_desc,fault_level,status,source,service_type,hospital_id,department_id,equipment_id,company_id,report_contact,report_phone,create_time)
VALUES(CONCAT('WO-E2E-A-',DATE_FORMAT(NOW(),'%Y%m%d%H%i%s')),'${title}-A','E2E 科室A工单','2','0','hospital','0',@hospital_id,@dept_a,@equipment_id,@company_id,'E2E','13900009999',NOW());

INSERT INTO biz_workorder(workorder_no,title,fault_desc,fault_level,status,source,service_type,hospital_id,department_id,equipment_id,company_id,report_contact,report_phone,create_time)
VALUES(CONCAT('WO-E2E-B-',DATE_FORMAT(NOW(),'%Y%m%d%H%i%s')),'${title}-B','E2E 科室B工单','2','0','hospital','0',@hospital_id,@dept_b,@equipment_id,@company_id,'E2E','13900009999',NOW());

INSERT INTO biz_document(company_id,doc_name,file_url,upload_time,uploader_name)
VALUES(@company_id,'E2E文件丢失','company/${companyCode}/document/209901/missing.pdf',NOW(),'E2E');

SELECT CONCAT(@company_id, '|', @hospital_id, '|', @equipment_id, '|', @company_user, '|', @engineer_user, '|', @hospital_user, '|', @subadmin_a, '|', @dept_a, '|', @dept_b);
`)

const ids = result.trim().split('|').map(Number)
const workorderNo = sql(`SELECT workorder_no FROM biz_workorder WHERE title='${title}' ORDER BY workorder_id DESC LIMIT 1`).trim()
fs.mkdirSync(path.join(root, 'e2e'), { recursive: true })
fs.writeFileSync(path.join(root, 'e2e', '.test-data.json'), JSON.stringify({
  title, workorderNo, password, companyCode,
  companyId: ids[0], hospitalId: ids[1], equipmentId: ids[2],
  companyUserId: ids[3], engineerUserId: ids[4], hospitalUserId: ids[5],
  subadminUserId: ids[6], deptAId: ids[7], deptBId: ids[8],
  deptTitleA: `${title}-A`, deptTitleB: `${title}-B`,
  subadminUsername: 'e2e_subadmin_a'
}, null, 2))
console.log(`E2E data ready: ${title} / ${workorderNo}`)
