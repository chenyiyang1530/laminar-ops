const fs = require('fs');
const file = 'sql/04_seed_ops.sql';
let sql = fs.readFileSync(file, 'utf8');
const start = sql.indexOf('INSERT INTO `biz_workorder`');
const end = sql.indexOf(';\n', start) + 2;
if (start >= 0) {
  sql = sql.slice(0, start) + '-- 工单数据见 04b_seed_workorder.sql\n' + sql.slice(end);
  fs.writeFileSync(file, sql);
  console.log('removed corrupted block');
}
