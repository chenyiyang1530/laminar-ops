const fs = require('fs');
const file = process.argv[2];
const table = process.argv[3];
let sql = fs.readFileSync(file, 'utf8');
const start = sql.indexOf('INSERT INTO `' + table + '`');
const open = sql.indexOf('(', start);
const close = sql.indexOf(')', open);
const cols = sql.slice(open + 1, close).split(',').map(s => s.trim()).filter(Boolean);
const valuesIdx = sql.indexOf('VALUES', close) + 6;
const end = sql.indexOf(';\n', valuesIdx);
const body = sql.slice(valuesIdx, end);

function splitTop(s) {
  const out = []; let depth = 0, cur = '', inStr = false;
  for (let i = 0; i < s.length; i++) {
    const ch = s[i];
    if (inStr) {
      cur += ch;
      if (ch === "'" && s[i + 1] === "'") cur += s[++i];
      else if (ch === "'") inStr = false;
      continue;
    }
    if (ch === "'") { inStr = true; cur += ch; continue; }
    if (ch === '(') depth++;
    if (ch === ')') depth--;
    if (ch === ',' && depth === 0) { out.push(cur.trim()); cur = ''; continue; }
    cur += ch;
  }
  out.push(cur.trim());
  return out;
}

const rows = [];
let depth = 0, cur = '', inStr = false;
for (let i = 0; i < body.length; i++) {
  const ch = body[i];
  if (inStr) { if (ch === "'" && body[i + 1] === "'") i++; else if (ch === "'") inStr = false; continue; }
  if (ch === "'") { inStr = true; continue; }
  if (ch === '(') { depth++; if (depth === 1) { cur = ''; continue; } }
  if (ch === ')') { depth--; if (depth === 0) { rows.push(cur); continue; } }
  if (depth > 0) cur += ch;
}

const fixed = rows.map(r => {
  const vals = splitTop(r);
  const missing = cols.length - vals.length;
  if (missing > 0) {
    // 在最后三个值（total_cost, repair_report, create_time）之前补 NULL
    vals.splice(vals.length - 3, 0, ...Array(missing).fill('NULL'));
  }
  return '  (' + vals.join(',') + ')';
});

const out = 'INSERT INTO `' + table + '` (' + cols.join(',') + ') VALUES\n' + fixed.join(',\n') + ';\n';
sql = sql.slice(0, start) + out + sql.slice(end + 2);
fs.writeFileSync(file, sql);
console.log('fixed rows=' + rows.length);
