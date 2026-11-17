import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))
test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 })

test('角色管理移动端截图基线', async ({ page }) => {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })
  await page.goto('/app/staff/people?tab=role')
  await expect(page.getByRole('heading', { name: '角色管理' })).toBeVisible({ timeout: 15_000 })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(overflow).toBeLessThanOrEqual(2)
  await expect(page).toHaveScreenshot('role-management-mobile.png', {
    fullPage: false,
    animations: 'disabled',
    caret: 'hide',
    maxDiffPixelRatio: 0.02,
    maskColor: '#f5f7fa',
    mask: [page.locator('.el-alert'), page.locator('.el-loading-mask'), page.locator('.el-message')]
  })
})

