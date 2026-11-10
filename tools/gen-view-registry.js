const fs = require('fs')
const path = require('path')

const viewsRoot = path.join(process.cwd(), 'src', 'views')
const files = []

function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name)
    const stat = fs.statSync(full)
    if (stat.isDirectory()) walk(full)
    else if (name.endsWith('.vue')) files.push(full)
  }
}

walk(viewsRoot)
const entries = files
  .map((f) => path.relative(viewsRoot, f).replace(/\\/g, '/').replace(/\.vue$/, ''))
  .sort()
  .map((key) => `  '${key}': () => import('../views/${key}.vue')`)

const content = `// 自动生成：视图组件注册表（请勿手工修改）\nexport const viewModules = {\n${entries.join(',\n')}\n}\n`
fs.writeFileSync(path.join(process.cwd(), 'src', 'router', 'views.js'), content)
console.log('views registry:', entries.length)
