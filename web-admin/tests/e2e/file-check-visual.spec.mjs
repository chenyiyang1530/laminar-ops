import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const data = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, '../../../e2e/.test-data.json'), 'utf8'))

test.skip(process.env.CI === 'true', 'CI Linux 需要单独生成文件巡检页面基线')

test('文件巡检页面视觉基线', async ({ page }) => {
  await page.goto('/app/login')
  await page.getByPlaceholder(/公司编码/).fill(data.companyCode)
  await page.getByPlaceholder('手机号 / 邮箱 / 登录账号', { exact: true }).fill('e2e_superadmin')
  await page.getByPlaceholder('请输入密码').fill(data.password)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForURL(/\/app\/dashboard/, { timeout: 20_000 })

  await page.goto('/app/system/file-check')
  await expect(page.getByText('文件巡检报告').first()).toBeVisible({ timeout: 15_000 })
  await expect(page).toHaveScreenshot('file-check-page.png', {
    animations: 'disabled',
    caret: 'hide',
    maxDiffPixelRatio: 0.05,
    mask: [page.locator('.trend-box')]
  })
})
