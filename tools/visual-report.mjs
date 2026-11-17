import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(import.meta.dirname, '..')
const source = path.join(root, 'web-admin', 'tests', 'e2e', '__screenshots__')
const output = path.join(root, 'docs', 'visual-report')
const imageRoot = path.join(output, 'images')
fs.rmSync(output, { recursive: true, force: true })
fs.mkdirSync(imageRoot, { recursive: true })

const groups = []
if (fs.existsSync(source)) {
  for (const spec of fs.readdirSync(source).sort()) {
    const dir = path.join(source, spec)
    if (!fs.statSync(dir).isDirectory()) continue
    const images = fs.readdirSync(dir).filter((name) => name.endsWith('.png')).sort()
    if (!images.length) continue
    const target = path.join(imageRoot, spec)
    fs.mkdirSync(target, { recursive: true })
    const cards = images.map((name) => {
      fs.copyFileSync(path.join(dir, name), path.join(target, name))
      return { name, label: name.replace(/\.png$/, '').replace(/-win32$/, '') }
    })
    groups.push({ spec, cards })
  }
}

const esc = (value) => String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
const total = groups.reduce((n, g) => n + g.cards.length, 0)
const body = groups.map((group) => `
  <section>
    <h2>${esc(group.spec)} <small>${group.cards.length} 张</small></h2>
    <div class="grid">
      ${group.cards.map((card) => `<figure><img src="images/${encodeURIComponent(group.spec)}/${encodeURIComponent(card.name)}" loading="lazy"><figcaption>${esc(card.label)}</figcaption></figure>`).join('')}
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
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:14px}
figure{margin:0;background:var(--surface);border:1px solid var(--line);border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(20,40,70,.04)}
figure img{display:block;width:100%;height:145px;object-fit:cover;object-position:top;background:#eef2f7}
figcaption{padding:8px 10px;font-size:11px;color:var(--muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
footer{padding:0 36px 30px;color:var(--muted);font-size:12px}
</style>
</head>
<body>
<header><h1>Laminar Ops 截图基线报告</h1><p>共 ${total} 张截图，按测试文件和页面分组。文件基于当前平台生成。</p></header>
<main>${body || '<p>暂无截图基线。</p>'}</main>
<footer>生成时间：${new Date().toISOString()} · 来源：web-admin/tests/e2e/__screenshots__</footer>
</body>
</html>`

fs.writeFileSync(path.join(output, 'index.html'), html)
console.log(`visual report: ${path.join(output, 'index.html')} (${total} screenshots)`)
