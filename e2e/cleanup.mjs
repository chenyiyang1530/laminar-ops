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

execFileSync(mysql, [
  '--host=127.0.0.1', '--user=root', '--default-character-set=utf8mb4', 'laminar_ops', '-e',
  `
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
DELETE FROM biz_company_hospital WHERE company_id IN (SELECT company_id FROM biz_company WHERE company_code='E2E_TEST')
  OR hospital_id IN (SELECT hospital_id FROM biz_hospital WHERE hospital_code='E2E-HOSP');
DELETE FROM biz_hospital WHERE hospital_code='E2E-HOSP';
DELETE FROM biz_device_type WHERE type_code='E2E-DEV';
DELETE FROM biz_company WHERE company_code='E2E_TEST';
`
], { env: { ...process.env, MYSQL_PWD: rootPassword }, stdio: 'pipe' })

const dataFile = path.join(root, 'e2e', '.test-data.json')
if (fs.existsSync(dataFile)) fs.unlinkSync(dataFile)
console.log('E2E data cleaned')
