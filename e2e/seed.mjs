import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'

const root = path.resolve(import.meta.dirname, '..')
const mysql = process.env.MYSQL_BIN || 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe'
const secrets = Object.fromEntries(
  fs.readFileSync(path.join(root, '.secrets.local'), 'utf8')
    .split(/\r?\n/)
    .filter((line) => line.includes('='))
    .map((line) => {
      const index = line.indexOf('=')
      return [line.slice(0, index), line.slice(index + 1)]
    })
)

function sql(statement) {
  execFileSync(mysql, [
    '--host=127.0.0.1',
    '--user=root',
    '--default-character-set=utf8mb4',
    'laminar_ops',
    '-e',
    statement
  ], { env: { ...process.env, MYSQL_PWD: secrets.MYSQL_ROOT_PASSWORD }, stdio: 'pipe' })
}

const title = `E2E-${Date.now()}`
const password = '123456'
const hash = '8a571e9e0c92983528d524794b28dc8e1b48d5ccf4e0f9874a1d0cfd7f7869fa'
const companyId = 7
const hospitalId = 11
const equipmentId = 19
const regionId = 14

sql(`
DELETE wl FROM biz_workorder_log wl JOIN biz_workorder w ON w.workorder_id=wl.workorder_id WHERE w.title LIKE 'E2E-%';
DELETE FROM biz_workorder WHERE title LIKE 'E2E-%';
DELETE FROM biz_hospital_user WHERE user_id IN (SELECT user_id FROM sys_user WHERE username LIKE 'e2e_%');
DELETE FROM sys_user_role WHERE user_id IN (SELECT user_id FROM sys_user WHERE username LIKE 'e2e_%');
DELETE FROM biz_engineer WHERE engineer_name='E2E工程师';
DELETE FROM sys_user WHERE username LIKE 'e2e_%';

INSERT INTO sys_user(username,password,nick_name,user_type,company_id,status,must_change_pwd,create_time)
VALUES('e2e_company','${hash}','E2E公司管理员','1',${companyId},'0',0,NOW());
SET @company_user=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@company_user,213);

INSERT INTO sys_user(username,password,nick_name,user_type,company_id,status,must_change_pwd,create_time)
VALUES('e2e_engineer','${hash}','E2E工程师','1',${companyId},'0',0,NOW());
SET @engineer_user=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@engineer_user,102);
INSERT INTO biz_engineer(user_id,company_id,region_id,engineer_name,phone,on_duty,status,create_time)
VALUES(@engineer_user,${companyId},${regionId},'E2E工程师','13900009999','1','0',NOW());

INSERT INTO sys_user(username,password,nick_name,user_type,status,must_change_pwd,create_time)
VALUES('e2e_hospital','${hash}','E2E院方管理员','2','0',0,NOW());
SET @hospital_user=LAST_INSERT_ID();
INSERT INTO sys_user_role(user_id,role_id) VALUES(@hospital_user,200);
INSERT INTO biz_hospital_user(user_id,hospital_id,role_type,status,create_time)
VALUES(@hospital_user,${hospitalId},'admin','0',NOW());

INSERT INTO biz_workorder(workorder_no,title,fault_desc,fault_level,status,source,service_type,hospital_id,equipment_id,company_id,report_contact,report_phone,create_time)
VALUES(CONCAT('WO-E2E-',DATE_FORMAT(NOW(),'%Y%m%d%H%i%s')),'${title}','E2E 登录到验收测试工单','2','0','hospital','0',${hospitalId},${equipmentId},${companyId},'E2E','13900009999',NOW());
`)

const workorderNo = execFileSync(mysql, [
  '--host=127.0.0.1', '--user=root', '--default-character-set=utf8mb4', 'laminar_ops', '-N', '-e',
  `select workorder_no from biz_workorder where title='${title}' order by workorder_id desc limit 1`
], { env: { ...process.env, MYSQL_PWD: secrets.MYSQL_ROOT_PASSWORD }, encoding: 'utf8' }).trim()

fs.mkdirSync(path.join(root, 'e2e'), { recursive: true })
fs.writeFileSync(path.join(root, 'e2e', '.test-data.json'), JSON.stringify({
  title, workorderNo, password, companyId, hospitalId, equipmentId
}, null, 2))
console.log(`E2E data ready: ${title} / ${workorderNo}`)
