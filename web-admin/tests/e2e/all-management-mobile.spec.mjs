import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))
test.setTimeout(120_000)
test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 })

test('23 个管理页面移动端截图基线', async ({ page }) => {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })

  const menus = await page.evaluate(async () => {
    const token = localStorage.getItem('laminar_token')
    const res = await fetch('/api/auth/info', { headers: { Authorization: `Bearer ${token}` } })
    return (await res.json()).data.menus
  })
  const routes = []
  function walk(items, parent) {
    for (const menu of items || []) {
      const full = menu.path && !menu.path.startsWith('/') && parent ? `${parent}/${menu.path}` : menu.path
      if (menu.menuType === 'C' && full) routes.push(full)
      if (menu.children) walk(menu.children, full)
    }
  }
  walk(menus, '')

  for (const route of routes) {
    await page.goto(`/app${route}`)
    await page.waitForTimeout(600)
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
    if (overflow > 2) {
      throw new Error(`页面横向溢出 ${overflow}px：${route}`)
    }
    const slug = route.replace(/^\//, '').replace(/[^a-zA-Z0-9]+/g, '-')
    await expect(page).toHaveScreenshot(`management-mobile-${slug}.png`, {
      fullPage: false,
      animations: 'disabled',
      caret: 'hide',
      maxDiffPixelRatio: 0.05,
      maskColor: '#f5f7fa',
      mask: [
        page.locator('.page-card .el-table'),
        page.locator('.page-card .el-pagination'),
        page.locator('.page-card canvas'),
        page.locator('.page-card .trend-box'),
        page.locator('.page-card .el-alert'),
        page.locator('.el-loading-mask'),
        page.locator('.el-message')
      ]
    })
  }
})

