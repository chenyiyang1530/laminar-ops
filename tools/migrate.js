#!/usr/bin/env node
/**
 * 简易数据库迁移执行器。
 *
 * 首次接管已有数据库：
 *   node tools/migrate.js --baseline
 *
 * 之后执行未记录的迁移：
 *   node tools/migrate.js
 *
 * 环境变量：
 *   DB_HOST / DB_PORT / DB_USER / DB_PASSWORD / DB_NAME
 *   MYSQL_BIN（默认 Windows MySQL 8.0 路径）
 */
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const sqlDir = path.join(__dirname, '..', 'sql');
const mysql = process.env.MYSQL_BIN || 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe';
const args = [
  `--host=${process.env.DB_HOST || '127.0.0.1'}`,
  `--port=${process.env.DB_PORT || '3306'}`,
  `--user=${process.env.DB_USER || 'root'}`,
  `--password=${process.env.DB_PASSWORD || (() => { throw new Error('请设置 DB_PASSWORD 环境变量') })()}`,
  '--default-character-set=utf8mb4',
  process.env.DB_NAME || 'laminar_ops'
];

function run(input) {
  return execFileSync(mysql, args, { input, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] });
}

const files = fs.readdirSync(sqlDir)
  .filter((f) => /^\d+_.*\.sql$/.test(f))
  .sort((a, b) => parseInt(a, 10) - parseInt(b, 10));

run(fs.readFileSync(path.join(sqlDir, '53_schema_version.sql')));
const applied = new Set(run('select version from sys_schema_version;\n')
  .split(/\r?\n/).map((s) => s.trim()).filter(Boolean));

if (process.argv.includes('--baseline')) {
  for (const file of files) {
    if (file === '53_schema_version.sql') continue;
    if (applied.has(file)) continue;
    run(`insert ignore into sys_schema_version(version,description,applied_at) values('${file}','baseline',now());\n`);
  }
  console.log(`baseline recorded: ${files.length - 1} files`);
  process.exit(0);
}

let count = 0;
for (const file of files) {
  if (file === '53_schema_version.sql' || applied.has(file)) continue;
  console.log(`applying ${file}`);
  run(fs.readFileSync(path.join(sqlDir, file)));
  run(`insert into sys_schema_version(version,description,applied_at) values('${file}','applied',now());\n`);
  count++;
}
console.log(count === 0 ? 'database is up to date' : `applied ${count} migration(s)`);
