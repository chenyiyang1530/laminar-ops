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
const dataFile = path.join(root, 'e2e', '.test-data.json')
const data = fs.existsSync(dataFile) ? JSON.parse(fs.readFileSync(dataFile, 'utf8')) : {}
const title = data.title || 'E2E-%'

execFileSync(mysql, [
  '--host=127.0.0.1', '--user=root', '--default-character-set=utf8mb4', 'laminar_ops', '-e',
  `
DELETE wl FROM biz_workorder_log wl JOIN biz_workorder w ON w.workorder_id=wl.workorder_id WHERE w.title LIKE 'E2E-%';
DELETE FROM biz_workorder WHERE title LIKE 'E2E-%';
DELETE FROM biz_engineer WHERE engineer_name='E2E工程师';
DELETE FROM biz_hospital_user WHERE user_id IN (SELECT user_id FROM sys_user WHERE username LIKE 'e2e_%');
DELETE FROM sys_user_role WHERE user_id IN (SELECT user_id FROM sys_user WHERE username LIKE 'e2e_%');
DELETE FROM sys_user WHERE username LIKE 'e2e_%';
`
], { env: { ...process.env, MYSQL_PWD: secrets.MYSQL_ROOT_PASSWORD }, stdio: 'pipe' })

if (fs.existsSync(dataFile)) fs.unlinkSync(dataFile)
console.log('E2E data cleaned')
