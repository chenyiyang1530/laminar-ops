const fs = require('fs');
const file = process.argv[2];
const table = process.argv[3];
const sql = fs.readFileSync(file, 'utf8');
const start = sql.indexOf('INSERT INTO `' + table + '`');
if (start < 0) { console.error('not found'); process.exit(1); }
const open = sql.indexOf('(', start);
const close = sql.indexOf(')', open);
const cols = sql.slice(open + 1, close).split(',').map(s => s.trim()).filter(Boolean);
const valuesIdx = sql.indexOf('VALUES', close) + 6;
let end = sql.indexOf(';\n', valuesIdx);
const body = sql.slice(valuesIdx, end);
// split rows at depth 0
const rows = [];
let depth = 0, cur = '', inStr = false;
for (let i = 0; i < body.length; i++) {
  const ch = body[i];
  if (inStr) {
    cur += ch;
    if (ch === "'" && body[i + 1] === "'") { cur += body[++i]; }
    else if (ch === "'") inStr = false;
    continue;
  }
  if (ch === "'") { inStr = true; cur += ch; continue; }
  if (ch === '(') { depth++; if (depth === 1) { cur = ''; continue; } }
  if (ch === ')') { depth--; if (depth === 0) { rows.push(cur); continue; } }
  if (depth > 0) cur += ch;
}
let vals = 0;
rows.forEach((r, idx) => {
  let d = 0; vals = 1; let s = false;
  for (let i = 0; i < r.length; i++) {
    const ch = r[i];
    if (s) { if (ch === "'" && r[i + 1] === "'") i++; else if (ch === "'") s = false; continue; }
    if (ch === "'") s = true;
    else if (ch === '(') d++;
    else if (ch === ')') d--;
    else if (ch === ',' && d === 0) vals++;
  }
  console.log(`row ${idx + 1}: ${vals} values, cols=${cols.length} ${vals === cols.length ? 'OK' : 'MISMATCH'}`);
});
