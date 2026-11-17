import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(import.meta.dirname, '..')
const source = path.join(root, 'web-admin', 'tests', 'e2e', '__screenshots__')
const output = path.join(root, 'docs', 'visual-report')
const imageRoot = path.join(output, 'images')
fs.rmSync(output, { recursive: true, force: true })
fs.mkdirSync(imageRoot, { recursive: true })

const groups = []
const resultsFile = path.join(root, 'web-admin', 'test-results', 'results.json')
const statusFile = path.join(root, 'docs', 'visual-status.json')
const testStatus = new Map()
if (fs.existsSync(resultsFile)) {
  const results = JSON.parse(fs.readFileSync(resultsFile, 'utf8'))
  const walk = (suites) => {
    for (const suite of suites || []) {
      if (suite.file) {
        const passed = (suite.specs || []).every((spec) => spec.ok !== false)
        testStatus.set(path.basename(suite.file), passed ? 'passed' : 'failed')
      }
      walk(suite.suites)
    }
  }
  walk(results.suites)
  fs.writeFileSync(statusFile, JSON.stringify(Object.fromEntries(testStatus), null, 2) + '\n')
} else if (fs.existsSync(statusFile)) {
  for (const [name, status] of Object.entries(JSON.parse(fs.readFileSync(statusFile, 'utf8')))) {
    testStatus.set(name, status)
  }
}
if (fs.existsSync(source)) {
  for (const spec of fs.readdirSync(source).sort()) {
    const dir = path.join(source, spec)
    if (!fs.statSync(dir).isDirectory()) continue
    const images = fs.readdirSync(dir).filter((name) => name.endsWith('.png')).sort()
    if (!images.length) continue
    const target = path.join(imageRoot, spec)
    fs.mkdirSync(target, { recursive: true })
    const cards = images.map((name) => {
      const from = path.join(dir, name)
      fs.copyFileSync(from, path.join(target, name))
      const platform = name.includes('-linux') ? 'linux' : name.includes('-win32') ? 'windows' : 'other'
      const date = new Date(fs.statSync(from).mtime).toISOString().slice(0, 10)
      return { name, label: name.replace(/\.png$/, '').replace(/-(win32|linux)$/, ''), platform, date }
    })
    groups.push({ spec, cards, status: testStatus.get(spec) || 'unknown' })
  }
}

const esc = (value) => String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
const total = groups.reduce((n, g) => n + g.cards.length, 0)
const body = groups.map((group) => `
  <section>
    <h2>${esc(group.spec)} <small>${group.cards.length} 张</small> <span class="status ${group.status}">${group.status === 'passed' ? '通过' : group.status === 'failed' ? '失败' : '未运行'}</span></h2>
    <div class="grid">
      ${group.cards.map((card) => `<figure data-platform="${card.platform}" data-date="${card.date}"><img src="images/${encodeURIComponent(group.spec)}/${encodeURIComponent(card.name)}" loading="lazy"><figcaption>${esc(card.label)}</figcaption></figure>`).join('')}
    </div>
  </section>`).join('')

const html = `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Laminar Ops 截图基线报告</title>
<style>
:root{--bg:#f5f7fa;--surface:#fff;--ink:#17212b;--muted:#66758a;--line:#e6ebf2;--accent:#1f6feb}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font-family:Inter,"Microsoft YaHei",system-ui,sans-serif}
header{padding:28px 36px 20px;background:linear-gradient(135deg,#0b0f16,#16243a);color:#fff}
header h1{margin:0 0 8px;font-size:24px}header p{margin:0;color:#9fb0c7;font-size:13px}
main{padding:24px 36px 48px}section{margin-bottom:32px}h2{font-size:16px;margin:0 0 14px;display:flex;gap:10px;align-items:center}
h2 small{font-size:12px;color:var(--muted);font-weight:400}
.status{display:inline-flex;align-items:center;height:20px;padding:0 8px;border-radius:10px;font-size:10px;font-weight:600}
.status.passed{background:#e8f7ee;color:#16803a}.status.failed{background:#fdecec;color:#c62828}.status.unknown{background:#eef2f7;color:#66758a}
.filters{display:flex;flex-wrap:wrap;gap:14px;align-items:flex-end;margin-bottom:22px;padding:14px 16px;background:var(--surface);border:1px solid var(--line);border-radius:10px}
.filters label{display:flex;flex-direction:column;gap:4px;font-size:11px;color:var(--muted)}
.filters select,.filters input{height:32px;border:1px solid var(--line);border-radius:6px;padding:0 9px;background:#fff;color:var(--ink);font-size:12px}
#visible{font-size:12px;color:var(--muted);margin-left:auto}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:14px}
figure{margin:0;background:var(--surface);border:1px solid var(--line);border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(20,40,70,.04)}
figure.hide{display:none}
figure img{display:block;width:100%;height:145px;object-fit:cover;object-position:top;background:#eef2f7}
figcaption{padding:8px 10px;font-size:11px;color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
footer{padding:0 36px 30px;color:var(--muted);font-size:12px}
</style>
</head>
<body>
<header><h1>Laminar Ops 截图基线报告</h1><p>共 ${total} 张截图，按测试文件和页面分组。文件基于当前平台生成。</p></header>
<main>
  <div class="filters">
    <label>平台
      <select id="platformFilter">
        <option value="">全部平台</option>
        <option value="windows">Windows</option>
        <option value="linux">Linux</option>
        <option value="other">其他</option>
      </select>
    </label>
    <label>日期
      <input id="dateFilter" type="date">
    </label>
    <button id="clearFilter" type="button" style="height:32px;border:1px solid var(--line);border-radius:6px;background:#fff;color:var(--muted);font-size:12px;padding:0 12px">清除</button>
    <span id="visible"></span>
  </div>
  ${body || '<p>暂无截图基线。</p>'}
</main>
<footer>生成时间：${new Date().toISOString()} · 来源：web-admin/tests/e2e/__screenshots__</footer>
<script>
const platform = document.getElementById('platformFilter');
const date = document.getElementById('dateFilter');
const visible = document.getElementById('visible');
const initial = new URLSearchParams(location.search);
platform.value = initial.get('platform') || '';
date.value = initial.get('date') || '';
function apply() {
  let count = 0;
  for (const card of document.querySelectorAll('figure[data-platform]')) {
    const okPlatform = !platform.value || card.dataset.platform === platform.value;
    const okDate = !date.value || card.dataset.date === date.value;
    card.classList.toggle('hide', !(okPlatform && okDate));
    if (okPlatform && okDate) count++;
  }
  for (const section of document.querySelectorAll('section')) {
    const shown = section.querySelectorAll('figure:not(.hide)').length;
    section.style.display = shown ? '' : 'none';
  }
  visible.textContent = '显示 ' + count + ' / ${total} 张';
  const url = new URL(location.href);
  if (platform.value) url.searchParams.set('platform', platform.value); else url.searchParams.delete('platform');
  if (date.value) url.searchParams.set('date', date.value); else url.searchParams.delete('date');
  history.replaceState(null, '', url);
}
platform.addEventListener('change', apply);
date.addEventListener('change', apply);
document.getElementById('clearFilter').addEventListener('click', () => { platform.value = ''; date.value = ''; apply(); });
apply();
</script>
</body>
</html>`

fs.writeFileSync(path.join(output, 'index.html'), html)
console.log(`visual report: ${path.join(output, 'index.html')} (${total} screenshots)`)
