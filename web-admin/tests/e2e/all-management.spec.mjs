import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test('所有管理页面可打开且无接口错误', async ({ page }) => {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })

  const menus = await page.evaluate(async () => {
    const token = localStorage.getItem('laminar_token') || localStorage.getItem('token')
    const res = await fetch('/api/auth/info', { headers: { Authorization: `Bearer ${token}` } })
    return (await res.json()).data.menus
  })

  const routes = []
  function walk(items, parent) {
    for (const menu of items || []) {
      const full = menu.path && !menu.path.startsWith('/') && parent ? `${parent}/${menu.path}` : menu.path
      if (menu.menuType === 'C' && full) routes.push({ path: full, name: menu.menuName })
      if (menu.children) walk(menu.children, full)
    }
  }
  walk(menus, '')

  const failures = []
  const errors = []
  const realTables = process.env.REAL_TABLE_MODE === '1'
  page.on('pageerror', (e) => errors.push(e.message))
  page.on('response', (res) => {
    if (res.url().includes('/api/') && res.status() >= 400) {
      failures.push(`${res.status()} ${res.request().method()} ${new URL(res.url()).pathname}`)
    }
  })

  for (const route of routes) {
    await page.goto(`/app${route.path}`)
    await page.waitForTimeout(900)
    const body = await page.locator('body').innerText()
    if (body.includes('系统异常') || body.includes('页面不存在')) {
      failures.push(`页面异常：${route.name} ${route.path}`)
    }
    const slug = route.path.replace(/^\//, '').replace(/[^a-zA-Z0-9]+/g, '-')
    const masks = realTables
      ? [page.locator('.page-card canvas'), page.locator('.page-card .trend-box'), page.locator('.page-card .el-alert')]
      : [
          page.locator('.page-card .el-table'),
          page.locator('.page-card .el-pagination'),
          page.locator('.page-card canvas'),
          page.locator('.page-card .trend-box'),
          page.locator('.page-card .el-alert')
        ]
    await expect(page).toHaveScreenshot(`management-${slug}${realTables ? '-real' : ''}.png`, {
      fullPage: false,
      animations: 'disabled',
      caret: 'hide',
      maxDiffPixelRatio: 0.05,
      maskColor: '#f5f7fa',
      mask: masks
    })
  }

  const unique = [...new Set(failures)]
  expect(errors, `页面 JS 异常：${errors.join('; ')}`).toHaveLength(0)
  expect(unique, `管理页面接口/渲染异常：\n${unique.join('\n')}`).toHaveLength(0)
})

